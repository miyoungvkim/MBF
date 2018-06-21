package creativeLab.samsung.mbf.Activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.Utils.Animation;

import static creativeLab.samsung.mbf.Utils.json.AssetJSONFile;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AnimationAdapter adapter;
    private List<Animation> animationList;
    private View decorView;
    private int uiOption;
    private Button btnSetting;

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

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        btnSetting = findViewById(R.id.btn_setting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFingerPrintScreen();
            }
        });

        animationList = new ArrayList<>();
        adapter = new AnimationAdapter(this, animationList);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        prepareAnimation();
        // We normally won't show the welcome slider again in real app
        // For testing how to show welcome slide
        //final PrefManager prefManager = new PrefManager(getApplicationContext());
        //prefManager.setFirstTimeLaunch(true);
        //startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        //finish();

    }

    private void launchFingerPrintScreen() {
        startActivity(new Intent(MainActivity.this, FingerprintActivity.class));
        //finish();
    }

    /**
     * Adding few albums for testing
     */

    private void prepareAnimation() {
        int[] covers = new int[100];
        String[] categoryname = new String[1024];
        int category_num = 0;
        Animation category_card_ui;

        try {
            String jsonFileLocation = AssetJSONFile("json/category_list.json", this);
            JSONObject jsonObject = new JSONObject(jsonFileLocation);
            JSONArray jsonArray = jsonObject.getJSONArray("category_lists");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonSubObject = (JSONObject) jsonArray.get(i);
                String category_name = jsonSubObject.getString("category");
                String category_image = jsonSubObject.getString("category_image");
                covers[i] = this.getResources().getIdentifier(category_image, "drawable", this.getPackageName());
                categoryname[i] = category_name;
                category_card_ui = new Animation(categoryname[i], i, covers[i]);
                category_num++;
                animationList.add(category_card_ui);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
/*
        category_card_ui = new Animation("Maroon 5", 11, covers[category_num]);
        category_num++;
        animationList.add(category_card_ui);
*/
        adapter.notifyDataSetChanged();
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }
}
