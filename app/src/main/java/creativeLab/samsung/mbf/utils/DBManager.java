package creativeLab.samsung.mbf.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.File;

public class DBManager extends SQLiteAssetHelper {
    private static final String TAG = DBManager.class.getSimpleName();

    private static final int DATABASE_VERSION = 1;
    private Context context;
    private SQLiteDatabase db;
    private String DBFileName = null;

    public DBManager(Context context, String DBFileName) {
        super(context, DBFileName, null, DATABASE_VERSION);
        this.context = context;
    }

    public Boolean dbFileCheck(String DBFileName) {
        boolean checkResult = false;
        String DB_PATH = context.getDatabasePath(DBFileName).getPath();
        File DbFile = new File(DB_PATH);
        if (DbFile.exists()) {
            checkResult = true;
        }
        return checkResult;
    }

    public void dbRemove(String DBFileName) {
        boolean checkResult = false;
        String DB_PATH = context.getDatabasePath(DBFileName).getPath();
        File DbFile = new File(DB_PATH);
        if (DbFile.exists()) {
            DbFile.delete();
        }
    }

    public void dbOpen() {
        db = getReadableDatabase();
    }

    public Cursor getDBCusor(String query) {
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }
}