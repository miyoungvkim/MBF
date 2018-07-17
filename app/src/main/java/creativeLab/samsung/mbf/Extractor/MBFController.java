package creativeLab.samsung.mbf.Extractor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import creativeLab.samsung.mbf.R;

public class MBFController {
    private static final String TAG = "MBF|MBFController";
    private static final String HANDLE_THREAD_NAME = "VideoBackground";
    public final Handler handler;
    private final Object lock = new Object();
    public String videoUrl;
    public Context context;
    public MediaMetadataRetriever mediaMetadataRetriever;
    public FullscreenVideoView videoView;
    private ImageClassifier classifier;
    private boolean runClassifier = false;
    private boolean IsClassifyFrameDebuggingMode = true;
    private MediaPlayer mediaPlayer;
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
                        Log.e(TAG, "error to run classify " + e);
                    }
                }
            };

    public MBFController(Context context, Handler handler, FullscreenVideoView videoView, String url) {
        this.context = context;
        this.handler = handler;
        this.videoView = videoView;
        this.videoUrl = url;

        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, Uri.parse(url));
    }

    public void start() {
        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            classifier = new ImageClassifierQuantizedMobileNet(((Activity) context));
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }
        startBackgroundThread();

        /////////////////// 10초에 한번씩 mbf player 실행 및 추출한 sound 저장 ////////////////////////
        SimpleDateFormat time = new SimpleDateFormat("mm:ss:SS");
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                String sTime = time.format(videoView.getCurrentPosition());
                String[] parsedTime = sTime.split(":");
                for (int i = 0; i < parsedTime.length; i++) {
                    if (i == 1 && Integer.parseInt(parsedTime[i]) % 10 == 0 && Integer.parseInt(parsedTime[i]) != 0) {
                        try {
                            video_pause_and_play_mbf();
                            // 10초 앞선 audio 추출
                            int preparedSec = 10;
                            int startSec = Integer.parseInt(parsedTime[i]) + preparedSec;
                            int durationSec = 5;
                            String extractedAudioPath = getExtractedAudioDataPath(startSec, durationSec);
                        } catch (Exception e) {
                            Log.e(TAG, "error!!!!!!!! " + e);
                        }
                    }
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(tt, 0, 1000);
        //////////////////////////////////////////////////////
    }

    public void stop() {
        stopBackgroundThread();
    }

    public Bitmap getExtractedImage() {
        Bitmap resultbitmap = null;
        int currentPosition = videoView.getCurrentPosition(); //in millisecond

        Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(currentPosition * 1000); //unit in microsecond
        if (bmFrame == null) {
            resultbitmap = null;
        } else {
            resultbitmap = bmFrame;
        }
        return bmFrame;
    }

    ///// add for tensorflow

    // deprecated
    public String getExtractedAudioDataPath() {
        // "/storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4"
        String outputFilename = "pororo01";
        String resultAudioPath = null;

        AudioExtractor mAudioExtractor = new AudioExtractor(context);
        mAudioExtractor.setUrlString(videoUrl);

        long startTime = 10 * 1000 * 1000;   // start from 10 sec
        long duration = 5 * 1000 * 1000;    // play duration : 5 sec
        mAudioExtractor.setTime(startTime, duration);
        resultAudioPath = mAudioExtractor.getExtractedAudioDataPath(outputFilename);

        // example log : extractedAudioPath = /storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4
        Log.e(TAG, "extractedAudioPath = " + resultAudioPath);
        return resultAudioPath;
    }

    public String getExtractedAudioDataPath(int startTime, int duration) {
        // "/storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4"
        String outputFilename = "pororo01_" + startTime;
        String resultAudioPath = null;

        AudioExtractor mAudioExtractor = new AudioExtractor(context);
        mAudioExtractor.setUrlString(videoUrl);

        long mStartTime = startTime * 1000 * 1000;
        long mDuration = duration * 1000 * 1000;

        mAudioExtractor.setTime(mStartTime, mDuration);
        resultAudioPath = mAudioExtractor.getExtractedAudioDataPath(outputFilename);

        // example log : extractedAudioPath = /storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4
        Log.e(TAG, "extractedAudioPath = " + resultAudioPath);
        return resultAudioPath;
    }

    void video_pause_and_play_mbf() {
        if (videoView.isPlaying() == true) {
            videoView.pause();

            Message visible_message = new Message();
            visible_message.what = View.VISIBLE;
            handler.sendMessage(visible_message);

            mediaPlayer = MediaPlayer.create(context, R.raw.mbf_start);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if (videoView.isPlaying() == false) {
                        mediaPlayer.stop();
                        videoView.start();
                        Message invisible_message = new Message();
                        invisible_message.what = View.INVISIBLE;
                        handler.sendMessage(invisible_message);
                    }
                }
            });
        }
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
        if (backgroundThread != null) {
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
    }

    /**
     * Classifies a frame from the preview stream.
     */
    private void classifyFrame() {
        if (classifier == null) {
            Log.e(TAG, "Uninitialized Classifier or invalid context.");
            return;
        }
        try {
            Bitmap bitmap = getExtractedImage();
            bitmap.setWidth(224);
            bitmap.setHeight(224);
            if (IsClassifyFrameDebuggingMode) {
                String textToShow = classifier.classifyFrame(bitmap);
                Log.d(TAG, textToShow);
            }
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error!!!!!!!!!!!!!!!!!", e);
        }
    }
}
