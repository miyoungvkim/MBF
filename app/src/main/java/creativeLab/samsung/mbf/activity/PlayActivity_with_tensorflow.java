package creativeLab.samsung.mbf.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.mbf.AudioTask;
import creativeLab.samsung.mbf.mbf.FullscreenVideoView;
import creativeLab.samsung.mbf.mbf.MBFController;
import creativeLab.samsung.mbf.mbf.extractor.AudioExtractor;
import creativeLab.samsung.mbf.utils.MBFLog;

public class PlayActivity_with_tensorflow extends AppCompatActivity {
    private static final String TAG = PlayActivity_with_tensorflow.class.getSimpleName();

    private FullscreenVideoView myVideoView;
    private MediaController myMediaController;
    private Context context;
    MediaPlayer.OnCompletionListener myVideoViewCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                    Toast.makeText(context, "End of Video", Toast.LENGTH_SHORT).show();
                    myVideoView.pause();
                    finish();
                }
            };
    MediaPlayer.OnPreparedListener MyVideoViewPreparedListener =
            new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // long duration = myVideoView.getDuration(); //in millisecond
                    //  Toast.makeText(context,"Duration: " + duration + " (ms)",  Toast.LENGTH_SHORT).show();
                    Toast.makeText(context, "onPrepared", Toast.LENGTH_SHORT).show();
                }
            };
    MediaPlayer.OnErrorListener myVideoViewErrorListener =
            new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Toast.makeText(context, "Error!!!", Toast.LENGTH_SHORT).show();
                    return true;
                }
            };
    private AlertDialog.Builder myCaptureDialog;
    private ImageView videoFrame;
    private String selectedFileName = null;
    private MBFController mbfController;
    private View decorView;
    private int uiOption;

    public final Handler videoFrameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int visibleState = msg.what;

            videoFrame = findViewById(R.id.videoFrame);
            videoFrame.setVisibility(View.GONE);

            if (visibleState == View.INVISIBLE)
                videoFrame.setVisibility(View.INVISIBLE);
            else if (visibleState == View.VISIBLE)
                videoFrame.setVisibility(View.VISIBLE);

            super.handleMessage(msg);
        }
    };

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

        myVideoView = findViewById(R.id.video_texture);
        videoFrame = findViewById(R.id.videoFrame);

        context = this;

        selectedFileName = this.getIntent().getStringExtra("FILE_NAME");
        if(selectedFileName == null) {
            MBFLog.e("selectedFileName == null ");
            selectedFileName = "pororo_01_01"; // default animation File name
        }

        String video_url = "android.resource://" + getPackageName() + "/raw/" + selectedFileName;

        mbfController = new MBFController(context, videoFrameHandler, myVideoView, video_url);
        mbfController.start();

        myMediaController = new MediaController(context);
        myVideoView.setMediaController(myMediaController);
        myVideoView.setOnCompletionListener(myVideoViewCompletionListener);
        myVideoView.setOnPreparedListener(MyVideoViewPreparedListener);
        myVideoView.setOnErrorListener(myVideoViewErrorListener);

        myVideoView.setVideoURI(Uri.parse(video_url));
        myVideoView.requestFocus();
        myVideoView.start();

        Button buttonCapture = findViewById(R.id.capture);
        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Bitmap bmFrame = mbfController.getExtractedImage();
                if (bmFrame == null) {
                    Toast.makeText(context, "bmFrame == null!", Toast.LENGTH_SHORT).show();
                } else {
                    myCaptureDialog = new AlertDialog.Builder(context);
                    ImageView capturedImageView = new ImageView(context);
                    capturedImageView.setImageBitmap(bmFrame);
                    ViewGroup.LayoutParams capturedImageViewLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    capturedImageView.setLayoutParams(capturedImageViewLayoutParams);

                    myCaptureDialog.setView(capturedImageView);
                    myCaptureDialog.show();
////////////
                    try {
                        // 1. extract audio data
                        // example log : extractedAudioPath = /storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4
                        String outputFilename = "pororo01";
                        String resultAudioPath = null;
                        AudioExtractor mAudioExtractor = new AudioExtractor(context);
                        mAudioExtractor.setUrlString(video_url);
                        long startTime = 20 * 1000 * 1000; // start from 20 sec
                        long duration = 5 * 1000 * 1000;    // play duration : 5 sec
                        mAudioExtractor.startExtractedAudioData(startTime, duration, outputFilename);
                        String extractedAudioPath = mAudioExtractor.getExtractedAudioFile();

                        // 2 : Play extracted audio data using mp4 file. it will be use as a debugging.
                        AudioTask mAudioTask = new AudioTask(PlayActivity_with_tensorflow.this);
                        mAudioTask.setUrlString(extractedAudioPath);
                        mAudioTask.start();
                    } catch (Exception e) {
                        Log.e(TAG, "audioExtrator Error !! " + e);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        mbfController.start();
    }

    @Override
    public void onPause() {
        myVideoView.pause();
        mbfController.stop();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mbfController.stop();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
