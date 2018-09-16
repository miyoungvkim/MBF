package creativeLab.samsung.mbf.activity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.MBFLog;

public class WelcomeActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 4000;
    private View decorView;
    private int uiOption;

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

        setContentView(R.layout.activity_welcome);

        boolean isGrantStorage = grantExternalStoragePermission();
        if (isGrantStorage) {
            MBFLog.d(" Permission Granted Status");
            welcomeUIProcess_start();
        } else {
            MBFLog.e("Error!! need to check permission state");
        }
    }


    void welcomeUIProcess_start() {
        // We normally won't show the welcome slider again in real app
        // For testing how to show welcome slide
        final PrefManager prefManager = new PrefManager(getApplicationContext());
        prefManager.setFirstTimeLaunch(true);
        //startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        //finish();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (prefManager.isFirstTimeLaunch()) {
                    Intent nextIntent = new Intent(WelcomeActivity.this, InitialSettingActivity.class);
                    startActivity(nextIntent);
                    finish();
                } else {
                    Intent nextIntent = new Intent(WelcomeActivity.this, CategoryActivity.class);
                    startActivity(nextIntent);
                    finish();
                }
            }
        }, SPLASH_TIME_OUT);

    }

    private boolean grantExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                MBFLog.d("Permission is granted");
                return true;
            } else {
                MBFLog.d("Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            Toast.makeText(this, "External Storage Permission is Grant", Toast.LENGTH_SHORT).show();
            MBFLog.d("External Storage Permission is Grant ");
            return true;
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MBFLog.d("Permission: " + permissions[0] + "was " + grantResults[0]);
                //resume tasks needing this permission
                welcomeUIProcess_start();
            }
        }
    }

}