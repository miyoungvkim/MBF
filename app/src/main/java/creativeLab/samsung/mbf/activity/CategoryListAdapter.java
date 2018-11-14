package creativeLab.samsung.mbf.activity;


import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.AnimationInfo;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.MyViewHolder> {
    private static final String TAG = CategoryListAdapter.class.getSimpleName();

    private Context mContext;
    private List<AnimationInfo> categoryList;

    public CategoryListAdapter(Context mContext, List<AnimationInfo> categoryList) {
        this.mContext = mContext;
        this.categoryList = categoryList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.animation_card, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        AnimationInfo categoryInfo = categoryList.get(position);

        holder.title.setText(categoryInfo.getTitle());

        // loading album cover using Glide library
        Glide.with(mContext).load(categoryInfo.getThumbnail()).into(holder.thumbnail);

        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(holder.overflow);
            }
        });

        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(categoryInfo.getEpisodeNum() == 0) {
                    Toast.makeText(mContext, "No Episode now ....." + holder.title.getText(), Toast.LENGTH_SHORT).show();
                } else {
                    //   Toast.makeText(mContext, "click " + holder.title.getText(), Toast.LENGTH_SHORT).show();

                    try {

                        if(categoryInfo.getID().equals("pororo") == true || categoryInfo.getID().equals("poli") == true || categoryInfo.getID().equals("frienzoo") == true)
                        {
                            Intent intent = new Intent(mContext, EpisodeCharacterActivity.class);
                            intent.putExtra("CATEGORY_ID", categoryInfo.getID());
                            mContext.startActivity(intent);
                        }else{
                            Intent intent = new Intent(mContext, EpisodeActivity.class);
                            intent.putExtra("CATEGORY_ID", categoryInfo.getID());
                            mContext.startActivity(intent);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error! intent " + e);
                    }
                }
            }
        });

    }

    /**
     * Showing popup menu when tapping on 3 dots
     */
    private void showPopupMenu(View view) {
        // inflate menu
        PopupMenu popup = new PopupMenu(mContext, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_album, popup.getMenu());
        popup.setOnMenuItemClickListener(new MyMenuItemClickListener());
        popup.show();
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public ImageView thumbnail, overflow;
        public int number = -1;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            thumbnail = view.findViewById(R.id.thumbnail);
            overflow = view.findViewById(R.id.overflow);
        }
    }

    /**
     * Click listener for popup menu items
     */
    class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        public MyMenuItemClickListener() {
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_add_favourite:
                    Toast.makeText(mContext, "Add to favourite", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_play_next:
                    Toast.makeText(mContext, "Play next", Toast.LENGTH_SHORT).show();
                    return true;
                default:
            }
            return false;
        }
    }
}
