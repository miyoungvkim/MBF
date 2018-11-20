package creativeLab.samsung.mbf.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.BounceInterpolator;
import creativeLab.samsung.mbf.utils.MBFLog;
import creativeLab.samsung.mbf.utils.UserInfo;

public class InitialSettingActivity extends AppCompatActivity {

    private CustomViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private int[] layouts;
    private Button[] button_skip, button_accept;
    private EditText txtName;
    private RadioGroup radioAge;
    private RadioGroup radioLimitTime;

    private PrefManager prefManager;
    private View decorView;
    private int uiOption;

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

        // Checking for first time launch - before calling setContentView()
        prefManager = new PrefManager(this);
        if (!prefManager.isFirstTimeLaunch()) {
            launchHomeScreen();
            finish();
        }

        setContentView(R.layout.activity_initialsetting);

        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);

        // layouts of all welcome sliders
        // add few more layouts if you want
        layouts = new int[]{
                R.layout.initialsetting_greeting_slide,
                R.layout.initialsetting_name_slide,
                R.layout.initialsetting_age_slide,
                R.layout.initialsetting_time_limit_slide,
                R.layout.initialsetting_parents_slide};

        button_accept = new Button[layouts.length];
        button_skip = new Button[layouts.length];

        // making notification bar transparent
        changeStatusBarColor();

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
    }

    public void didTapButton(View view) {

        int rb_id = view.getId();
        /*
        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
        // Use bounce interpolator with amplitude 0.2 and frequency 20
        BounceInterpolator interpolator = new BounceInterpolator(0.05, 13);
        myAnim.setInterpolator(interpolator);
        Button button = findViewById(rb_id);
        button.startAnimation(myAnim);
        */
        // Write what kind of setting value
        switch (rb_id) {
            // TO DO
            case R.id.age_btn1:
                break;
            case R.id.age_btn2:
                break;
            case R.id.age_btn3:
                break;
            case R.id.age_btn4:
                break;
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(uiOption);
        }
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    private void launchHomeScreen() {
        prefManager.setFirstTimeLaunch(false);
        startActivity(new Intent(InitialSettingActivity.this, CategoryActivity.class));
        //startActivity(new Intent(WelcomeActivity.this, FingerprintActivity.class));
        finish();
    }

    private void onClickNextPage() {
        int current = getItem(+1);
        viewPager.setCurrentItem(current);
    }


    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);
            switch(layouts[position]) {
                case R.layout.initialsetting_greeting_slide : {
                    button_accept[position] = view.findViewById(R.id.button_next);
                    button_accept[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClickNextPage();
                        }
                    });
                    break;
                }
                case R.layout.initialsetting_name_slide : {
                    txtName = view.findViewById(R.id.edittxt_initial_setting_name_baby_name);
                    txtName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            switch(actionId) {
                                case EditorInfo.IME_ACTION_DONE:
                                    break;
                                default :
                                    break;
                            }
                            return false;
                        }
                    });

                    button_skip[position] = view.findViewById(R.id.button_skip);
                    button_skip[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // launchHomeScreen()
                            onClickNextPage();
                        }
                    });
                    button_accept[position] = view.findViewById(R.id.button_accept);
                    button_accept[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (txtName.getText().toString().length() == 0) {
                                Toast.makeText(InitialSettingActivity.this, R.string.initial_setting_name_input_wrong_message, Toast.LENGTH_SHORT).show();
                            } else {
                                String babyNickName = txtName.getText().toString();
                                UserInfo.setUserName(babyNickName);
                            }
                            onClickNextPage();
                        }
                    });
                    break;
                }
                case R.layout.initialsetting_age_slide : {
                    radioAge = view.findViewById(R.id.radioGroup_age);
                    button_skip[position] = view.findViewById(R.id.button_skip);
                    button_skip[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // launchHomeScreen()
                            onClickNextPage();
                        }
                    });
                    button_accept[position] = view.findViewById(R.id.button_accept);
                    button_accept[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            /*
                            int selectedAge = radioAge.getCheckedRadioButtonId();
                            UserInfo.setUserAge(selectedAge);
                            */
                            onClickNextPage();
                        }
                    });
                    break;
                }
                case R.layout.initialsetting_time_limit_slide : {
                    button_accept[position] = view.findViewById(R.id.button_next);
                    button_accept[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClickNextPage();
                        }
                    });
                    break;
                }
                case R.layout.initialsetting_parents_slide : {
                    button_accept[position] = view.findViewById(R.id.button_start);
                    button_accept[position].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            launchHomeScreen();
                        }
                    });
                    break;
                }
                default :
                    break;
            }
            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}