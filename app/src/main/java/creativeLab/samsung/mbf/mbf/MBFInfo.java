package creativeLab.samsung.mbf.mbf;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import creativeLab.samsung.mbf.mbf.extractor.AudioExtractor;
import creativeLab.samsung.mbf.utils.DBManager;
import creativeLab.samsung.mbf.utils.FileManager;
import creativeLab.samsung.mbf.utils.MBFLog;
import creativeLab.samsung.mbf.utils.UserInfo;

public class MBFInfo {
    public static final int MBF_SUCCESS = 0;
    public static final int MBF_ERROR = -1;
    public static final int MBF_NO_DATA = 1;

    public static final int MBF_STATE_CONTENTS_PLAY = 0;
    public static final int MBF_STATE_MBF_READY = 1;
    public static final int MBF_STATE_MBF_PLAY = 2;

    private static final String DATABASE_FILE_NAME = "mbfDB.db";
    private static final String DATABASE_TABLE_VOICE_KEYWORD = "voiceKeywordTable";
    private static final String DATABASE_TABLE_REACTION = "reactionTable";
    private static final String DATABASE_TABLE_SCENE = "sceneTable";
    private static final String DATABASE_PREDEFINED_STRING_NAME = "[Name_id]";

    private String videoUrl;
    private Bitmap extractedBitmapFile;
    private String extractedAudioFilePath;
    private String animationID;
    private String charaterID;
    private String emotionID;

    private ArrayList<String> voiceKeywordList;
    private ArrayList<String> reactionMentList;
    private String reactionMent;
    private String reactionMent2 = "";

    private int catchCount;
    private boolean isReactionUsed;
    private int videoCurrentPosition;
    private boolean isObjectDetected;

    private DBManager mbfDB;
    private Context context;
    private int playMin;
    private int playSec;

    private MediaMetadataRetriever mediaMetadataRetriever;

    public MBFInfo(Context context, String videoUrl) {
        this.context = context;
        this.videoUrl = videoUrl;

        mbfDB = new DBManager(context, DATABASE_FILE_NAME);
        boolean ret = mbfDB.dbFileCheck(DATABASE_FILE_NAME);
        if (ret == true) {
            mbfDB.dbRemove(DATABASE_FILE_NAME); // remove DB for reinstall
        }
        mbfDB.dbOpen();

        mediaMetadataRetriever = new MediaMetadataRetriever();
        //mediaMetadataRetriever.setDataSource(context, Uri.parse(videoUrl));
        mediaMetadataRetriever. setDataSource(videoUrl, new HashMap<String, String>());
    }

    public static String LogChecker(int num) {
        String ret = null;
        switch (num) {
            case MBF_ERROR: {
                ret = " error";
                break;
            }
            case MBF_NO_DATA:
                ret = " no data";
                break;
            case MBF_SUCCESS:
                ret = " success";
                break;
            default:
                ret = " error in Log checker input value";
                break;
        }
        return ret;
    }

    public void Init() {
        extractedAudioFilePath = null;
        animationID = null;
        charaterID = null;
        emotionID = null;
        reactionMent2 = "";
//        if(voiceKeywordList != null)
//            voiceKeywordList.clear();

        catchCount = 0;
        isReactionUsed = false;
        videoCurrentPosition = 0;
        isObjectDetected = false;
//        if(reactionMentList != null)
//            reactionMentList.clear();

        playMin = 0;
        playSec = 0;
    }

    public String getExtractedAudioFilePath() {
        return extractedAudioFilePath;
    }

    public String getAnimationID() {
        return animationID;
    }

    public void setAnimationID(String animationID) {
        this.animationID = animationID;
    }

    public String getCharaterID() {
        return charaterID;
    }

    public void setCharaterID(String charaterID) {
        this.charaterID = charaterID;
    }

    public String getEmotionID() {
        return emotionID;
    }

    public void setEmotionID(String emotionID) {
        this.emotionID = emotionID;
    }

    public ArrayList<String> getVoiceKeywordList() {
        if (voiceKeywordList != null && voiceKeywordList.size() > 0) {
            for (int i = 0; i < voiceKeywordList.size(); i++) {
                MBFLog.d(" [getVoiceKeywordList] voiceKeyword[" + i + "] = " + voiceKeywordList.get(i));
            }
        }
        return voiceKeywordList;
    }

    public void setVoiceKeywordList(ArrayList<String> voiceKeywordList) {
        this.voiceKeywordList = voiceKeywordList;
    }

    public int createVoiceKeywordList(String strSTT) {
        int ret = MBF_ERROR;
        ArrayList<String> mVoiceKeywordList = new ArrayList<String>();
        String[] parsedString = strSTT.split(" ");
        for (int i = 0; i < parsedString.length; i++) {
            String resultStr = getVoiceKeywordFromDB(parsedString[i]);
            if (resultStr != null) {
                mVoiceKeywordList.add(resultStr);
            }
        }

        if (mVoiceKeywordList != null && mVoiceKeywordList.size() > 0) {
            MBFLog.d("original STT = " + strSTT);
            for (int i = 0; i < mVoiceKeywordList.size(); i++) {
                MBFLog.d("voiceKeyword[" + i + "] = " + mVoiceKeywordList.get(i));
            }
            this.voiceKeywordList = mVoiceKeywordList;
            ret = MBF_SUCCESS;
        }
        return ret;
    }

    public String getReactionMent() {
        if (reactionMentList != null && reactionMentList.size() > 0) {
            this.reactionMent = reactionMentList.get(0);
        }
        return reactionMent;
    }

    public String getReactionMent2() {
        return reactionMent2;
    }

    public void setReactionMent(String ment, String reactionMent2) {
        if (reactionMentList != null) {
            reactionMentList.clear();
            reactionMentList = null;
        }
        this.reactionMent = ment;
        this.reactionMent2 = reactionMent2;
        MBFLog.d("set reaction Ment = " + ment);
    }

    public void setReactionMent(String reactionMent) {
        if (reactionMentList != null) {
            reactionMentList.clear();
            reactionMentList = null;
        }
        this.reactionMent = reactionMent;
        MBFLog.d("set reaction Ment = " + reactionMent);
    }


    public ArrayList<String> getReactionMentList() {
        return reactionMentList;
    }

    public void setReactionMentList(ArrayList<String> reactionMentList) {
        this.reactionMentList = reactionMentList;
    }

    public int createReactionMentList(String strSTT) {
        int ret = MBF_ERROR;
        if (strSTT.length() <= 0) {
            ret = MBF_NO_DATA;
            return ret;
        }

        ArrayList<String> mReactionMentList = getReactionMentListFromDB(strSTT);
        if (mReactionMentList != null && mReactionMentList.size() > 0) {
            for (int i = 0; i < mReactionMentList.size(); i++) {
                String UserInfo_name = UserInfo.getUserName();
                if (UserInfo_name == null)
                    UserInfo_name = UserInfo.USER_DEFAULT_NAME;
                String replacedStr = mReactionMentList.get(i).replace(DATABASE_PREDEFINED_STRING_NAME, UserInfo_name);
                mReactionMentList.set(i, replacedStr);
                MBFLog.d("[Replaced str] reactionMentList[" + i + "] = " + mReactionMentList.get(i));
            }
            this.reactionMentList = mReactionMentList;
            ret = MBF_SUCCESS;
        } else {
            String defaultRecationStr = "우리  " + strSTT + "  라고 함께 말해 볼까?";
            mReactionMentList.add(defaultRecationStr);
            this.reactionMentList = mReactionMentList;
            ret = MBF_SUCCESS;
        }
        return ret;
    }

    public String getVoiceKeywordFromDB(String str) {
        if (str.length() <= 0) {
            return null;
        }

        String query = "SELECT * FROM " + DATABASE_TABLE_VOICE_KEYWORD + " WHERE keyword LIKE \'" + str + "%\'";
        Cursor cursor = mbfDB.getDBCusor(query);
        String res = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                res = cursor.getString(cursor.getColumnIndex("keyword"));
                break;
            } while (cursor.moveToNext());
        }
        cursor.close();

        MBFLog.d("[getVoiceKeywordFromDBgetKeyword Result " + res);
        return res;
    }

    public ArrayList<String> getReactionMentListFromDB(String voiceKeyword) {
        if (voiceKeyword.length() <= 0) {
            return null;
        }

        String query = "SELECT * FROM " + DATABASE_TABLE_REACTION + " WHERE voiceKeyword =\'" + voiceKeyword + "\'";
        Cursor cursor = mbfDB.getDBCusor(query);
        ArrayList<String> list = new ArrayList<String>();
        String res = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                res = cursor.getString(cursor.getColumnIndex("ment"));
                if (res.length() > 0) {
                    list.add(res);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public String getSceneTextFromDB(String str) {
        if (str.length() <= 0) {
            return null;
        }

        String query = "SELECT * FROM " + DATABASE_TABLE_SCENE + " WHERE sceneID LIKE \'" + str + "\'";
        Cursor cursor = mbfDB.getDBCusor(query);
        String res = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                res = cursor.getString(cursor.getColumnIndex("sceneNameKR"));
                break;
            } while (cursor.moveToNext());
        }
        cursor.close();

        MBFLog.d("[getSceneTextFromDB Result] " + res);
        return res;
    }

    public int getCatchCount() {
        return catchCount;
    }

    public void setCatchCount(int catchCount) {
        this.catchCount = catchCount;
    }

    public boolean getIsReactionUsed() {
        return isReactionUsed;
    }

    public void setIsReactionUsed(boolean isReactionUsed) {
        this.isReactionUsed = isReactionUsed;
    }

    public int getVideoCurrentPosition() {
        return videoCurrentPosition;
    }

    public void setVideoCurrentPosition(int videoCurrentPosition) {
        SimpleDateFormat time = new SimpleDateFormat("mm:ss:SS");
        String strVideoCurrentPosition = time.format(videoCurrentPosition);

        String[] parsedTimeInfo = strVideoCurrentPosition.split(":");

        this.videoCurrentPosition = videoCurrentPosition;
        this.playMin = Integer.parseInt(parsedTimeInfo[0]);
        this.playSec = Integer.parseInt(parsedTimeInfo[1]);
    }

    public int getPlayMin() {
        return playMin;
    }

    public int getPlaySec() {
        return playSec;
    }

    public boolean getIsObjectDetected() {
        return isObjectDetected;
    }

    public void setIsObjectDetected(boolean isObjectDetected) {
        this.isObjectDetected = isObjectDetected;
    }

    public int createExtractedAudioData(int audioPreparedSeconds, int audioRecordDuration) {
        int ret = MBF_NO_DATA;

        int audioStartSeconds = playMin * 60 + playSec + audioPreparedSeconds;
        long startTime = audioStartSeconds * 1000 * 1000;
        long durationTime = audioRecordDuration * 1000 * 1000;

        String outputFileName = "mbf_tmp_" + audioStartSeconds / 60 + "_" + audioStartSeconds % 60;

        AudioExtractor mAudioExtractor = new AudioExtractor(context);
        mAudioExtractor.setUrlString(videoUrl);
        ret = mAudioExtractor.startExtractedAudioData(startTime, durationTime, outputFileName);
        if (ret == MBF_ERROR || ret == MBF_NO_DATA) {
            MBFLog.e("ERROR, startExtractedAudioData " + LogChecker(ret));
            return ret;
        }

        String AudioFilePath = mAudioExtractor.getExtractedAudioFile();
        if (AudioFilePath != null) {
            File FileChecker = new File(AudioFilePath);
            if (FileChecker.exists()) {
                this.extractedAudioFilePath = AudioFilePath;
                ret = MBF_SUCCESS;
            } else {
                ret = MBF_ERROR;
            }
        }
        return ret;
    }

    public Bitmap getExtractedImageAtTime(int currentPosition) {
        Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(currentPosition * 1000); //unit in microsecond
        return bmFrame;
    }

    public int createExtractedImageData(long currentPosition) {
        int ret = MBF_NO_DATA;

        Bitmap resultbitmap = null;

        // int currentPosition = videoView.getCurrentPosition(); //in millisecond
        Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(currentPosition * 1000); //unit in microsecond
        if (bmFrame == null) {
            resultbitmap = null;
        } else {
            resultbitmap = bmFrame;
            String outputFileName = "mbf_tmp_" + playMin + "_" + playSec;
            saveBitmapToFileCache(bmFrame, outputFileName);
            this.extractedBitmapFile = resultbitmap;
            ret = MBF_SUCCESS;
        }
        return ret;
    }

    private void saveBitmapToFileCache(Bitmap bitmap, String outputFilename) {

        String dstMediaPath = FileManager.getExternalCacheFilePath(context, "image", outputFilename, "jpg");

        File fileCacheItem = new File(dstMediaPath);
        OutputStream out = null;
        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            MBFLog.e("error!!, faile to save Bitmap data as a File");
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                MBFLog.e("error!!, faile to save Bitmap data as a File" + e);
            }
        }
    }

    public Bitmap getExtractedBitmapFile() {
        return extractedBitmapFile;
    }

    public Bitmap getExtractedCurrentPositionFrame() {
        Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(videoCurrentPosition * 1000); //unit in microsecond
        return bmFrame;
    }

    // Todo : add STT
    String getSpeechToTextFromAudioData(String ouputAudioPath) {
        String[] tmp_str = {"우리는 친구", "하지만 뽀로로는 아기 공룡을", "공룡에게 사과", "새내기 버스의 하루",
                "미안해 이제 조용히 할게", "내 이름은 타요야", "만나서 반가워"
                , "무서운 괴물이라고 생각합니다", "반가워", "함께 준비 해보자",
                "이 버스 타자", "뽀로로 같이 준비 해보자", "안녕 난 뽀로로야", "뭐하는 거야", "같이 가", "아이쿠"};
        String str = tmp_str[videoCurrentPosition % 10];
        return str;
    }
}

