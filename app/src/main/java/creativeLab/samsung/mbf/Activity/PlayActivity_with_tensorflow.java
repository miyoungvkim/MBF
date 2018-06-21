package creativeLab.samsung.mbf.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;

import creativeLab.samsung.mbf.Extractor.AudioExtractorTask;
import creativeLab.samsung.mbf.Extractor.FullscreenVideoView;
import creativeLab.samsung.mbf.Extractor.ImageClassifier;
import creativeLab.samsung.mbf.Extractor.ImageClassifierQuantizedMobileNet;
import creativeLab.samsung.mbf.R;

public class PlayActivity_with_tensorflow extends AppCompatActivity {
    public static final int MY_PERMISSIONS_REQUEST = 1;
    private static final String TAG = "TAG";
    private static final String HANDLE_THREAD_NAME = "VideoBackground";
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(PlayActivity_with_tensorflow.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };
    private final Object lock = new Object();
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                }
            };
    MediaExtractor extractor;
    MediaCodec mediaCodec;
    //
    FullscreenVideoView myVideoView;
    MediaMetadataRetriever mediaMetadataRetriever;
    MediaController myMediaController;
    Context mContext;
    AlertDialog.Builder myCaptureDialog;
    AudioExtractorTask audioExtrator;
    MediaPlayer.OnCompletionListener myVideoViewCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                    Toast.makeText(mContext, "End of Video", Toast.LENGTH_SHORT).show();
                    myVideoView.pause();
                }
            };
    MediaPlayer.OnPreparedListener MyVideoViewPreparedListener =
            new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // long duration = myVideoView.getDuration(); //in millisecond
                    //  Toast.makeText(mContext,"Duration: " + duration + " (ms)",  Toast.LENGTH_SHORT).show();
                    Toast.makeText(mContext, "onPrepared", Toast.LENGTH_SHORT).show();

                }
            };
    MediaPlayer.OnErrorListener myVideoViewErrorListener =
            new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Toast.makeText(mContext, "Error!!!", Toast.LENGTH_SHORT).show();
                    return true;
                }
            };
    private View decorView;
    private int uiOption;
    private boolean runClassifier = false;
    private TextView textView;
    private ImageClassifier classifier;

    //
    private boolean inputEos;
    private ByteBuffer readBuffer, writeBuffer;
    private long presentationTimeUs;
    private TextureView textureView;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;
    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler;
    /**
     * Takes photos and classify them periodically.
     */
    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {
                            classifyFrame();
                        }
                    }
                    try {
                        backgroundHandler.post(periodicClassify);
                    } catch (Exception e) {
                        Log.e(TAG, "tag");
                    }
                }
            };

    /**
     * Shows a {@link Toast} on the UI thread for the classification results.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        this.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(text);
                    }
                });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(uiOption);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //hide status and navigation bar
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        uiOption = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        setContentView(R.layout.activity_player_local);

        textureView = findViewById(R.id.texture);
        textView = findViewById(R.id.text);
        myVideoView = findViewById(R.id.video_texture);
        //////////////////////
        // video start
        mContext = this;
        String video_url = "android.resource://" + getPackageName() + "/raw/pororo01";
        // String video_url = "android.resource://"+getPackageName()+"/raw/tensorflow_sample";
        Uri video_uri = Uri.parse(video_url);
        // METADATA_KEY_DURATION: 300744
        // METADATA_KEY_MIMETYPE: video/mp4
        // METADATA_KEY_NUM_TRACKS: 2

        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mContext, video_uri);
        myVideoView.setVideoURI(video_uri);

        Log.d(TAG, "KMI METADATA_KEY_ALBUM: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        Log.d(TAG, "KMI METADATA_KEY_ALBUMARTIST: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
        Log.d(TAG, "KMI METADATA_KEY_ARTIST: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        Log.d(TAG, "KMI METADATA_KEY_AUTHOR: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR));
        Log.d(TAG, "KMI METADATA_KEY_COMPILATION: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
        Log.d(TAG, "KMI METADATA_KEY_COMPILATION: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION));
        Log.d(TAG, "KMI METADATA_KEY_DATE: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
        Log.d(TAG, "KMI METADATA_KEY_DISC_NUMBER: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER));
        Log.d(TAG, "KMI METADATA_KEY_DURATION: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Log.d(TAG, "KMI METADATA_KEY_GENRE: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
        Log.d(TAG, "KMI METADATA_KEY_MIMETYPE: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));
        Log.d(TAG, "KMI METADATA_KEY_NUM_TRACKS: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
        Log.d(TAG, "KMI METADATA_KEY_TITLE: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        Log.d(TAG, "KMI METADATA_KEY_WRITER: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER));
        Log.d(TAG, "KMI METADATA_KEY_YEAR: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR));

        myMediaController = new MediaController(mContext);
        myVideoView.setMediaController(myMediaController);

        myVideoView.setOnCompletionListener(myVideoViewCompletionListener);
        myVideoView.setOnPreparedListener(MyVideoViewPreparedListener);
        myVideoView.setOnErrorListener(myVideoViewErrorListener);

        myVideoView.requestFocus();
        myVideoView.start();

        Button buttonCapture = findViewById(R.id.capture);
        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int currentPosition = myVideoView.getCurrentPosition(); //in millisecond
                // Toast.makeText(mContext,"Current Position: " + currentPosition + " (ms)", Toast.LENGTH_SHORT).show();

                Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(currentPosition * 1000); //unit in microsecond

                if (bmFrame == null) {
                    Toast.makeText(mContext, "bmFrame == null!", Toast.LENGTH_SHORT).show();
                } else {
                    myCaptureDialog = new AlertDialog.Builder(mContext);
                    ImageView capturedImageView = new ImageView(mContext);
                    capturedImageView.setImageBitmap(bmFrame);
                    ViewGroup.LayoutParams capturedImageViewLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    capturedImageView.setLayoutParams(capturedImageViewLayoutParams);

                    myCaptureDialog.setView(capturedImageView);
                    myCaptureDialog.show();
////////////

                    Log.e("TAG", "kmi audio extractor start");
                    try {
                        audioExtrator = new AudioExtractorTask(PlayActivity_with_tensorflow.this, R.raw.pororo01);
                        long start_time = 10 * 1000 * 1000; // start from 10 sec
                        long duration = 5 * 1000 * 1000;    // play duration : 5 sec
                        audioExtrator.setTime(start_time, duration);
                        audioExtrator.start();
                    } catch (Exception e) {
                        Log.e("TAG", "audioExtrator Error !! " + e);

                    }

                    Log.e("TAG", "kmi audio extractor ed");
                }

            }
        });
        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            classifier = new ImageClassifierQuantizedMobileNet(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }
        startBackgroundThread();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            // openCamera(textureView.getWidth(), textureView.getHeight());
            myVideoView.resume();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        stopBackgroundThread();
        myVideoView.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        classifier.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        backgroundHandler.post(periodicClassify);
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted when stopping background thread", e);
        }
    }

    /**
     * Classifies a frame from the preview stream.
     */
    private void classifyFrame() {
        Log.e(TAG, "classifyFrame start");
        if (classifier == null) {
            showToast("Uninitialized Classifier or invalid context.");
            return;
        }
        try {
            int currentPosition = myVideoView.getCurrentPosition(); //in millisecond
            Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(currentPosition * 1000); //unit in microsecond
            bitmap.setWidth(224);
            bitmap.setHeight(224);
            String textToShow = classifier.classifyFrame(bitmap);
            bitmap.recycle();
            showToast(textToShow);
        } catch (Exception e) {
            Log.e(TAG, "Error!!!!!!!!!!!!!!!!!", e);
        }
    }

    void releaseCodec() {
        mediaCodec.stop();
        mediaCodec.release();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }
}
