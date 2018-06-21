package creativeLab.samsung.mbf.Activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import creativeLab.samsung.mbf.R;

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
                    Intent nextIntent = new Intent(WelcomeActivity.this, SettingActivity.class);
                    startActivity(nextIntent);
                    finish();
                } else {
                    Intent nextIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(nextIntent);
                    finish();
                }
            }
        }, SPLASH_TIME_OUT);
    }

}