package creativeLab.samsung.mbf.activity;

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
import android.widget.ImageButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.AnimationInfo;

import static creativeLab.samsung.mbf.utils.json.AssetJSONFile;


public class CategoryActivity extends AppCompatActivity {
    private static final String TAG = CategoryActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private CategoryListAdapter adapter;
    private List<AnimationInfo> categoryInfoList;
    private View decorView;
    private int uiOption;
    private ImageButton btnSetting;
    private ImageButton btnSearching;
    private ImageButton btnFavorite;

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

        setContentView(R.layout.activity_category);

        recyclerView = findViewById(R.id.recycler_view);
        btnSetting = findViewById(R.id.btn_setting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFingerPrintScreen();
            }
        });

        btnSearching = findViewById(R.id.btn_search);
        btnSearching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { launchSearchScreen();

            }
        });

        btnFavorite = findViewById(R.id.btn_favorite);
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { launchFavoriteScreen();

            }
        });
        categoryInfoList = setCategoryList();
        adapter = new CategoryListAdapter(this, categoryInfoList);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
        // We normally won't show the welcome slider again in real app
        // For testing how to show welcome slide
        //final PrefManager prefManager = new PrefManager(getApplicationContext());
        //prefManager.setFirstTimeLaunch(true);
        //startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        //finish();
    }

    private void launchFavoriteScreen() {
        startActivity(new Intent(CategoryActivity.this, FavoriteActivity.class));
    }

    private void launchSearchScreen() {
        startActivity(new Intent(CategoryActivity.this, SearchActivity.class));
    }

    private void launchFingerPrintScreen() {
        startActivity(new Intent(CategoryActivity.this, SettingsActivity.class));// FingerprintActivity.class));
        //finish();
    }

    private List<AnimationInfo> setCategoryList(){
        List<AnimationInfo> InfoList = new ArrayList<>();

        // String category_id, String title, int thumbnail
        try{
            String jsonFileLocation = AssetJSONFile("data/category_lists.json", this);
            JSONObject jsonObject = new JSONObject(jsonFileLocation);
            JSONArray jsonArray = jsonObject.getJSONArray("category_list");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonSubObject = (JSONObject) jsonArray.get(i);

                String id = jsonSubObject.getString("id");
                String title = jsonSubObject.getString("title");
                String image = jsonSubObject.getString("thumbnail");
                int episode_num = jsonSubObject.getInt("episode_num");
                int thumbnail_image = getResources().getIdentifier(image, "drawable", getPackageName());

                AnimationInfo categoryInfo = new AnimationInfo();
                categoryInfo.setID(id);
                categoryInfo.setTitle(title);
                categoryInfo.setThumbnail(thumbnail_image);
                categoryInfo.setEpisodeNum(episode_num);

                InfoList.add(categoryInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return InfoList;
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
