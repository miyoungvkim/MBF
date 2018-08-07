package creativeLab.samsung.mbf.mbf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.mbf.extractor.ImageClassifier;
import creativeLab.samsung.mbf.mbf.extractor.ImageClassifierQuantizedMobileNet;
import creativeLab.samsung.mbf.utils.MBFLog;

public class MBFController {
    private static final String TAG = MBFController.class.getSimpleName();
    private static final String HANDLE_THREAD_NAME = "VideoBackground";
    public Handler videoFrameHandler = null;
    private final Object lock = new Object();
    public String videoUrl = null;
    public Context context = null;
    public MediaMetadataRetriever mediaMetadataRetriever = null;
    public FullscreenVideoView videoView = null;
    private ImageClassifier classifier = null;
    private boolean runClassifier = false;
    UtteranceProgressListener ttsUtteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
        }

        @Override
        public void onDone(String utteranceId) {
            MBFLog.d("UtteranceProgressListener onDone " + utteranceId);
            videoView.start();
            setVideoKeyscreenVisibleState(View.INVISIBLE);
        }

        @Override
        public void onError(String utteranceId) {
            MBFLog.d("UtteranceProgressListener onError " + utteranceId);
            videoView.start();
            setVideoKeyscreenVisibleState(View.INVISIBLE);
        }
    };
    private MediaPlayer mediaPlayer = null;
    private boolean IsClassifyFrameDebuggingMode = false;
    private Timer timer;
    private long timer_called_sec = 0;
    private TimerTask timerTask;
    private MBFInfo mbfInfo;
    private TextToSpeechTask tts = null;

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
    MediaPlayer.OnCompletionListener mediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            String reactionMent = mbfInfo.getReactionMent();
            if (reactionMent != null && reactionMent.length() > 0) {
                tts.speak(reactionMent);
            } else {
                MBFLog.e("[error!!] no reaction Ment...." + reactionMent);
            }
        }
    };
    private boolean isFinalMentCalled = false;

    public MBFController(Context context, Handler videoFrameHandler, FullscreenVideoView videoView, String videoURL) {
        this.context = context;
        this.videoFrameHandler = videoFrameHandler;
        this.videoView = videoView;
        this.videoUrl = videoURL;

        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, Uri.parse(videoUrl));
    }

    public void start() {
        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            classifier = new ImageClassifierQuantizedMobileNet(((Activity) context));
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }

        mbfInfo = new MBFInfo(context, videoUrl);

        tts = new TextToSpeechTask(context, Locale.KOREAN, ttsUtteranceProgressListener);
        tts.setSpeechRate((float) 0.9);

        startBackgroundThread();
        startMBFProcess();
    }

    public void stop() {
        stopBackgroundThread();
        stopMBFProcess();
    }

    private void startMBFProcess() {

        isFinalMentCalled = false;

        /////////////////// 5초에 한번씩 mbf soulution 실행 ////////////////////////
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    int ret = MBFInfo.MBF_ERROR;
                    int finalDuration = videoView.getDuration() - 6000;
                    int currentVideoPosition = videoView.getCurrentPosition();

                    mbfInfo.Init();
                    mbfInfo.setVideoCurrentPosition(currentVideoPosition);

                    int video_sec = mbfInfo.getPlaySec();

                    if (currentVideoPosition > finalDuration && finalDuration > 0) {
                        if (isFinalMentCalled == false) {
                            video_pause_and_play_mbf("오늘은 여기까지 안녕~");
                            isFinalMentCalled = true;
                        }
                        return;
                    }
                    // 1. every 10 sec
                    else if (video_sec != 0 && video_sec % 10 == 0 && video_sec != timer_called_sec) {
                        timer_called_sec = video_sec;

                        ////////////////////////////////////////////////////////
                        // 1 extract audio data from video
                        int audioPreparedSeconds = 10; // before 10 sec
                        int audioRecordDuration = 5;  // while 5 sec
                        ret = mbfInfo.createExtractedAudioData(audioPreparedSeconds, audioRecordDuration);
                        if (ret == mbfInfo.MBF_ERROR || ret == mbfInfo.MBF_NO_DATA) {
                            MBFLog.e("Error, Fail to create Voice Keyword List" + mbfInfo.LogChecker(ret));
                        }

                        String tmpAudioFilePath = mbfInfo.getExtractedAudioFilePath();
                        MBFLog.d("getExtractedAudioFilePath = " + tmpAudioFilePath);

                        // 2 get STT String from extracted audio file
                        // Todo : add STT
                        String strSTT = mbfInfo.getSpeechToTextFromAudioData(tmpAudioFilePath);
                        if (strSTT.length() <= 0) {
                            MBFLog.e("Error, STT null" + mbfInfo.LogChecker(mbfInfo.MBF_ERROR));
                            return;
                        }

                        // 3 create voice keyword list
                        ret = mbfInfo.createVoiceKeywordList(strSTT);
                        if (ret == mbfInfo.MBF_ERROR || ret == mbfInfo.MBF_NO_DATA) {
                            MBFLog.e("Error, Fail to create Voice Keyword List" + mbfInfo.LogChecker(ret));
                        }

                        ArrayList<String> voiceKeywordList = mbfInfo.getVoiceKeywordList();
                        if (voiceKeywordList.size() <= 0) {
                            MBFLog.e("No Voice Keyword in " + strSTT);
                            return;
                        }

                        // tmp use 1st extracted voice keyword
                        strSTT = voiceKeywordList.get(0);

                        /// 4 image detector
                        // Todo : add OBD
                        long imageExtractStartTime = currentVideoPosition + (10 * 1000); // before 10 sec
                        ret = mbfInfo.createExtractedImageData(imageExtractStartTime);
                        if (ret == mbfInfo.MBF_ERROR || ret == mbfInfo.MBF_NO_DATA) {
                            MBFLog.e("Error, Fail to createExtractedImageData" + mbfInfo.LogChecker(ret));
                        }

                        /*
                        Bitmap bitmap = mbfInfo.getExtractedBitmapFile();
                        String characterID = mbfInfo.getCharaterID();
                        String emotionID = mbfInfo.getEmotionID();
                        if (characterID == null || emotionID == null) {
                            return;
                        }
                        */

                        ret = mbfInfo.createReactionMentList(strSTT);
                        if (ret == mbfInfo.MBF_ERROR || ret == mbfInfo.MBF_NO_DATA) {
                            MBFLog.e("ERROR, Create ReactionMent List" + mbfInfo.LogChecker(ret));
                            return;
                        }

                        String reactionMent = mbfInfo.getReactionMent();
                        if (reactionMent == null) {
                            MBFLog.e("ERROR, ReactionMent null");
                            return;
                        }

                        video_pause_and_play_mbf(reactionMent);
                        ////////////////////////////////////////////////////////
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in mbf Controller " + e);
                }
                ;
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0, 1000);
        //////////////////////////////////////////////////////
    }

    private void stopMBFProcess() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
        if (tts != null) {
            tts.shutdown();
        }
        tts = null;

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    // deprecated(?)
    public Bitmap getExtractedImage() {
        Bitmap bmFrame = mbfInfo.getExtractedImageAtTime(videoView.getCurrentPosition());
        return bmFrame;
    }

    void setVideoKeyscreenVisibleState(int visibleState) {
        Message visible_message = new Message();
        visible_message.what = visibleState;
        videoFrameHandler.sendMessage(visible_message);
    }

    void video_pause_and_play_mbf(String ment) {
        MBFLog.d("reactionMent start " + ment);
        if (ment != null && ment.length() > 0) {
            if (videoView.isPlaying() == true) {
                videoView.pause();

                setVideoKeyscreenVisibleState(View.VISIBLE);

                mbfInfo.setReactionMent(ment);

                ////////////////
                mediaPlayer = MediaPlayer.create(context, R.raw.mbf_start);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
            }
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
            String textToShow = classifier.classifyFrame(bitmap);
            if (IsClassifyFrameDebuggingMode) {
                Log.d(TAG, textToShow);
            }
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error!!!!!!!!!!!!!!!!!", e);
        }
    }
}
