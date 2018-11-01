package creativeLab.samsung.mbf.mbf;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowObjectDetectionAPIModel;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.MBFLog;

public class MBFController {
    private static final String TAG = MBFController.class.getSimpleName();
    public Handler videoFrameHandler = null;
    public String videoUrl = null;
    public Context context = null;
    //public MediaMetadataRetriever mediaMetadataRetriever = null;
    public FullscreenVideoView videoView = null;

    private boolean runClassifier = false;

    /*geonhui83.lee*/
    /*tensorflow speech recognition*/
    /*tensorflow object detection declaraion*/
    private static final int TF_OD_API_INPUT_SIZE = 300;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    /*speech recognition*/
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 5000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    //objectDetectModelDir + "/all_animation_label_map.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/q_wavenet_mobile.pb";
    private static final String INPUT_DATA_NAME = "Placeholder:0";
    private static final String OUTPUT_SCORES_NAME = "output";
    private static String[] SCENE_CATEGORY_CLASSES;
    private final Logger logger = new Logger();
    File sdCard = Environment.getExternalStorageDirectory();
    String objectDetectModelDir = sdCard.getAbsolutePath() + "/tensorflow_mobile";
    Bitmap origBitmap = null;
    //File sdcard = Environment.getExternalStorageDirectory();
    String sceneCategoryModelDir = sdCard.getAbsolutePath() + "/caffe_mobile";
    //String modelDir = "/sdcard/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = sceneCategoryModelDir + "/deploy_googlenet_places365.prototxt";
    String modelBinary = sceneCategoryModelDir + "/googlenet_places365.caffemodel";
    String sceneLabel = "categories_places365.txt";
    MediaPlayer.OnCompletionListener mediaTTSVoiceCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            MBFLog.d("mediaTTSVoiceCompletionListener end");
            videoView.start();
            setMBFPlayState(MBFInfo.MBF_STATE_CONTENTS_PLAY);
        }
    };
    private String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph.pb";
    //objectDetectModelDir + "/frozen_inference_graph.pb";
    private String TF_OD_API_LABELS_FILE = "file:///android_asset/all_animation_label_map.txt";
    private int cropSize;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Bitmap cropCopyBitmap;
    private long detectionTimeStamp;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private int playWidth;
    private int playHeight;
    private Classifier detector;
    private MultiBoxTracker tracker;

    /*public String getSpeechRecogResult(short[] input){
        String
    }*/

    /*tensorflow speech recognition*/
    //private TensorFlowSpeechRecognitionAPIModel speechRecog;
    private CaffeMobile caffeMobile;
    private MediaPlayer mediaTTSVoicePlayer = null;
    UtteranceProgressListener ttsUtteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
        }

        @Override
        public void onDone(String utteranceId) {
            MBFLog.d("UtteranceProgressListener onDone " + utteranceId);

            mbfttsStart();
        }

        @Override
        public void onError(String utteranceId) {
            MBFLog.d("UtteranceProgressListener onError " + utteranceId);
            videoView.start();
            setMBFPlayState(MBFInfo.MBF_STATE_CONTENTS_PLAY);
        }
    };
    MediaPlayer.OnCompletionListener mediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            setMBFPlayState(MBFInfo.MBF_STATE_MBF_PLAY);

            String reactionMent = mbfInfo.getReactionMent();
            if (reactionMent != null && reactionMent.length() > 0) {
                // tts.speak(reactionMent);
                mbfttsStart();
            } else {
                MBFLog.e("[error!!] no reaction Ment...." + reactionMent);
            }
        }
    };

    private void initDetector(AssetManager assetManager) {
        rgbFrameBitmap = null;
        croppedBitmap = null;
        cropCopyBitmap = null;
        lastProcessingTimeMs = 0;

        cropSize = TF_OD_API_INPUT_SIZE;
        detectionTimeStamp = 0;

        playWidth = 0;
        playHeight = 0;

        try {
            detector = TensorFlowObjectDetectionAPIModel.create(assetManager, TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!", e);
        }
        tracker = new MultiBoxTracker(context);
    }

    public String getObjectDetectorResult(Bitmap origBitmap) {
        MBFLog.d("KMI getObjectDetectorResult start");
        String ret = " ";
        String debugStr = "";
        int chracterMaxNum = 3;
        String characterObject[] = new String[chracterMaxNum];

        rgbFrameBitmap = Bitmap.createBitmap(origBitmap.getWidth(), origBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(origBitmap.getWidth(), origBitmap.getHeight(), cropSize, cropSize, 0, true);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(origBitmap, frameToCropTransform, null);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        final long stopTime = SystemClock.uptimeMillis();
        final long dur = stopTime - startTime;

        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();
        int numObject = 0;

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= 0.3) {
                //logger.i(TAG, "geonhui83.lee %s : %d, %d, %d, %d" + result.getTitle(),location.left, location.right, location.bottom, location.top);
                MBFLog.d("geonhui83.lee " + result.getTitle(), location.left, location.right, location.bottom, location.top);

                // kmi add for 1st demo (2018.09.06)
                characterObject[numObject] = result.getTitle();
                if (numObject > chracterMaxNum)
                    break;
                //
                numObject++;
                debugStr += result.getTitle() + "    " + result.getConfidence() + "\n";
                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
            //tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
        }

        // kmi add for 1st demo (2018.09.06)
        if (numObject == 1) {
            ret = "우와~" + characterObject[0] + "다~!";
        } else if (numObject > 1) {
            ret = "우와~" + characterObject[0] + "와" + characterObject[1] + "다~!";
        } else if (numObject > 2) {
            ret = "우와~" + characterObject[0] + "와 친구들이다~!";
        }
        //

        return ret;
    }

    /*tensorflow object detection declaraion*/
    /*tensorflow speech recognition
    private void initSpeechRecognition(AssetManager assetManager) {
        speechRecog = new TensorFlowSpeechRecognitionAPIModel();
        speechRecog.create(assetManager, MODEL_FILENAME, INPUT_DATA_NAME, SAMPLE_RATE, SAMPLE_DURATION_MS);
    }*/
    /*caffe scene categization declaraion*/



    /*geonhui83.lee*/
/*
    public String getSpeechRecognize(short[] inputAudio, int sampleRate, int duration) {
        //int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //int bufferSizeInput = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int inputAudioDataLen = SAMPLE_RATE * SAMPLE_DURATION_MS / 1000;
        int inputAudioLen = sampleRate * duration / 1000;
        short[] inpputAudioData = new short[inputAudioDataLen];
        for (int i = 0; i < inputAudioDataLen; i++) {
            float conversionF = (float) inputAudioDataLen / (float) inputAudioDataLen;
            int conversion = (int) (conversionF * 1000.0) * i;
            int jj = conversion / 1000;
            //int con = i*(int)(conversionF);
            if (jj > inputAudioLen || jj < 0) {
                for (int m = i; m < inputAudioDataLen; m++) {
                    inpputAudioData[i] = 0;
                }
                break;
            }
            inpputAudioData[i] = inputAudio[jj];
        }
        //speechRecog.setSampleData(sampleRate, duration);
        return speechRecog.recognize(inpputAudioData);
    }

    public synchronized void startRecognition(short[] inputAudio) {
        if (speechRecog.recognitionThread != null) {
            return;
        }
        speechRecog.shouldContinueRecognition = true;
        speechRecog.recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                speechRecog.recognize(inputAudio);
                            }
                        });
        speechRecog.recognitionThread.start();
    }
*/
    private MediaPlayer mediaPlayer = null;

    private void initSceneCategorization(AssetManager assetManager) {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");

        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(1);
        caffeMobile.loadModel(modelProto, modelBinary);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        AssetManager am = assetManager;
        try {
            InputStream is = am.open("categories_places365.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            SCENE_CATEGORY_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Timer timer;
    private long timer_called_sec = 0;
    private TimerTask timerTask;
    private MBFInfo mbfInfo;
    private TextToSpeechTask tts = null;

    public String getSceneCategory(Bitmap origBitmap) {
        String ret = "";
        String sceneID = "";
        String tempJPEGFileName = sceneCategoryModelDir + "/tempJPG.jpg";
        try {
            FileOutputStream outStream = new FileOutputStream(tempJPEGFileName);
            origBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
            //img.compressToJpeg(rect_rsz, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int sceneClass = caffeMobile.predictImage(tempJPEGFileName)[0];
        sceneID = SCENE_CATEGORY_CLASSES[sceneClass];
        //
        // need to mention for scean explain
        ret = mbfInfo.getSceneTextFromDB(sceneID);
        ret += "이다~!";
        //
        return ret;
    }

    public void mbfttsStart() {
        String reactionMent2 = mbfInfo.getReactionMent2();
        MBFLog.d("kmi reactionMent2" + reactionMent2);

        int[] voice_id = {R.raw.tts_voice_32_1, R.raw.tts_voice_32_2, R.raw.tts_voice_32_3, R.raw.tts_voice_32_4, R.raw.tts_voice_32_5,
                R.raw.tts_voice_67_1, R.raw.tts_voice_67_2, R.raw.tts_voice_67_3, R.raw.tts_voice_74_1, R.raw.tts_voice_74_2, R.raw.tts_voice_74_3, R.raw.tts_voice_74_4, R.raw.tts_voice_170_1,
                R.raw.tts_voice_170_2, R.raw.tts_voice_170_3, R.raw.tts_voice_170_4, R.raw.tts_voice_195_1, R.raw.tts_voice_195_2, R.raw.tts_voice_195_3, R.raw.tts_voice_195_4, R.raw.tts_voice_217_1,
                R.raw.tts_voice_217_2, R.raw.tts_voice_217_3, R.raw.tts_voice_217_4, R.raw.tts_voice_217_5, R.raw.tts_voice_217_6,
                R.raw.tts_voice_320_1, R.raw.tts_voice_320_2, R.raw.tts_voice_320_3, R.raw.tts_voice_320_4, R.raw.tts_voice_487_1,
                R.raw.tts_voice_487_2, R.raw.tts_voice_487_3, R.raw.tts_voice_487_4, R.raw.tts_voice_487_5, R.raw.tts_voice_487_6};
        Random rand = new Random();
        int voice_index = rand.nextInt() % 35;
        if (voice_index < 0)
            voice_index = -voice_index;
        MBFLog.d("kmi  voice_index " + voice_index + "mediaTTSVoicePlayer start");
        mediaTTSVoicePlayer = MediaPlayer.create(context, voice_id[voice_index]);
        mediaTTSVoicePlayer.start();
        mediaTTSVoicePlayer.setOnCompletionListener(mediaTTSVoiceCompletionListener);
    }


    private boolean isFinalMentCalled = false;

    public MBFController(Context context, Handler videoFrameHandler, FullscreenVideoView videoView, String videoURL) {
        this.context = context;
        this.videoFrameHandler = videoFrameHandler;
        this.videoView = videoView;
        this.videoUrl = videoURL;

        //mediaMetadataRetriever = new MediaMetadataRetriever();
        //mediaMetadataRetriever.setDataSource(context, Uri.parse(videoUrl));
        //mediaMetadataRetriever. setDataSource(videoUrl, new HashMap<String, String>());
    }

    public void start(AssetManager assetManager) {
        mbfInfo = new MBFInfo(context, videoUrl);
        /*geonhui83.lee*/
        /*Object Detector Initialize*/

        initDetector(assetManager);
        initSceneCategorization(assetManager);
        //initSpeechRecognition(assetManager);

        /*geonhui83.lee*/

        tts = new TextToSpeechTask(context, Locale.KOREAN, ttsUtteranceProgressListener);
        tts.setSpeechRate((float) 0.9);

        startMBFProcess();
    }

    public void stop() {
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
                        mbf_final_mention_start();
                        return;
                    }
                    // 1. every 10 sec
                    else if (video_sec != 0 && video_sec % 10 == 0 && video_sec != timer_called_sec) {
                        timer_called_sec = video_sec;
                        // kmi add for 1st demo (2018.09.06)
                        // mbf_mention_start();
                        //
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

    public void mbf_final_mention_start() {
        if (isFinalMentCalled == false) {
            MBFLog.d("mbf_final_mention_start");
            String finalMention = "오늘은 여기까지! 우리 또 만나요 안녕! ";
            video_pause_and_play_mbf(finalMention);
            isFinalMentCalled = true;
        }
    }

    public void mbf_mention_start() {
        MBFLog.d("mbf_mention_start");
        int ret = MBFInfo.MBF_ERROR;
        int currentVideoPosition = mbfInfo.getVideoCurrentPosition();

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

    public String getReactionWithKeyword(String Keyword) {
        MBFLog.d("getReactionWithKeyword start");
        int errCode = mbfInfo.MBF_ERROR;
        String ret = "";

        // Todo : add STT
        String strSTT = Keyword;
        if (strSTT.length() <= 0) {
            MBFLog.e("Error, STT null" + mbfInfo.LogChecker(mbfInfo.MBF_ERROR));
            return ret;
        }

        // 3 create voice keyword list
        errCode = mbfInfo.createVoiceKeywordList(strSTT);
        if (errCode == mbfInfo.MBF_ERROR || errCode == mbfInfo.MBF_NO_DATA) {
            MBFLog.e("Error, Fail to create Voice Keyword List" + mbfInfo.LogChecker(errCode));
        }

        ArrayList<String> voiceKeywordList = mbfInfo.getVoiceKeywordList();
        if (voiceKeywordList.size() <= 0) {
            MBFLog.e("No Voice Keyword in " + strSTT);
            return ret;
        }

        // tmp use 1st extracted voice keyword
        strSTT = voiceKeywordList.get(0);

        errCode = mbfInfo.createReactionMentList(strSTT);
        if (errCode == mbfInfo.MBF_ERROR || errCode == mbfInfo.MBF_NO_DATA) {
            MBFLog.e("ERROR, Create ReactionMent List" + mbfInfo.LogChecker(errCode));
            return ret;
        }

        String reactionMent = mbfInfo.getReactionMent();
        if (reactionMent == null) {
            MBFLog.e("ERROR, ReactionMent null");
            return ret;
        }

        ret = reactionMent;

        return ret;
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

    void setMBFPlayState(int playState) {
        Message visible_message = new Message();
        visible_message.what = playState;
        videoFrameHandler.sendMessage(visible_message);
    }

    public void video_pause_and_play_mbf(String ment) {
        MBFLog.d("reactionMent start " + ment);
        if (ment != null && ment.length() > 0) {
            if (videoView.isPlaying() == true) {
                videoView.pause();

                setMBFPlayState(MBFInfo.MBF_STATE_MBF_READY);

                mbfInfo.setReactionMent(ment);

                ////////////////
                mediaPlayer = MediaPlayer.create(context, R.raw.mbf_start);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
            }
        }
    }

    public void video_pause_and_play_mbf_for_demo_sound_play(String ment, String reactionMention) {
        MBFLog.d("video_pause_and_play_mbf_for_demo_sound_play start " + ment);

        if (ment != null && ment.length() > 0) {
            if (videoView.isPlaying() == true) {
                videoView.pause();

                setMBFPlayState(MBFInfo.MBF_STATE_MBF_READY);

                mbfInfo.setReactionMent(ment, reactionMention);

                ////////////////
                mediaPlayer = MediaPlayer.create(context, R.raw.mbf_start);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
            }
        }
    }

    public void video_pause_and_play_mbf(String ment, String reactionMention) {
        MBFLog.d("video_pause_and_play_mbf start " + ment);

        if (ment != null && ment.length() > 0) {
            if (videoView.isPlaying() == true) {
                videoView.pause();

                setMBFPlayState(MBFInfo.MBF_STATE_MBF_READY);

                mbfInfo.setReactionMent(ment, reactionMention);

                ////////////////
                mediaPlayer = MediaPlayer.create(context, R.raw.mbf_start);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
            }
        }
    }
}
