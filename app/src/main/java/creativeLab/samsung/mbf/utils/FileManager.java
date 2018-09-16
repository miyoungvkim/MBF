package creativeLab.samsung.mbf.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileManager {
    public static String getExternalCacheFilePath(Context context, String directory_name, String fileName, String fileType) {
        String fileDir = context.getExternalCacheDir().getAbsolutePath() + "/" + directory_name;
        String fileDirPath = fileDir + "/" + fileName + "." + fileType;
        File dir = new File(fileDir);
        if (!dir.exists())
            dir.mkdirs();

        return fileDirPath;
    }

    public static boolean isMovieFile(Context context, String fileName) {
        boolean res = false;
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        String fileDirPath = dir.getAbsolutePath() + "/" + fileName + ".mp4";
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            String[] listFiles = dir.list();
            for (int i = 0; i < listFiles.length; i++)
                if (listFiles[i] == fileName) {
                    MBFLog.d("fileName");
                    res = true;
                }
        }
        MBFLog.d("kmi fileDirPath " + fileDirPath);
        return res;
    }


    public static String getMovieFilePath(Context context, String fileName) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        String fileDirPath = "";
        boolean isExistFile = false;

        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            String[] listFiles = dir.list();
            for (int i = 0; i < listFiles.length; i++) {
                if (listFiles[i].contains(fileName)) {
                    fileDirPath = "file://" + dir.getAbsolutePath() + "/" + fileName + ".mp4";
                    isExistFile = true;
                    break;
                }
            }
            if (isExistFile == false) {
                fileDirPath = "android.resource://" + context.getPackageName() + "/raw/" + "pororo_01_01";
                MBFLog.w("There is no file " + fileName + "So, I will use default movie file");
            }
        }
        return fileDirPath;
    }
}
