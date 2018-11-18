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

public class EpisodeListAdapter extends RecyclerView.Adapter<EpisodeListAdapter.MyViewHolder> {
    private static final String TAG = EpisodeListAdapter.class.getSimpleName();

    private Context mContext;
    private List<AnimationInfo> episodeList;

    public EpisodeListAdapter(Context mContext, List<AnimationInfo> episodeList) {
        this.mContext = mContext;
        this.episodeList = episodeList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.episode_card,parent,false);
        //.inflate(R.layout.episode_card_card, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        AnimationInfo animationInfo = episodeList.get(position);

        holder.title.setText(animationInfo.getTitle());
        holder.number = position;
        String selectedCategoryId = animationInfo.getSelected_categoryID();
        if(selectedCategoryId.equals("frienzoo") || selectedCategoryId.equals("poli") || selectedCategoryId.equals("banji") || selectedCategoryId.equals("woodang") ){
            int white = mContext.getResources().getColor(R.color.white);
            holder.title.setTextColor(white);
        }
        // loading album cover using Glide library
        Glide.with(mContext).load(animationInfo.getThumbnail()).into(holder.thumbnail);
/*
        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(holder.overflow);
            }
        });
*/
        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toast.makeText(mContext, "click " + holder.getNumber() + " " + holder.title.getText(), Toast.LENGTH_SHORT).show();

                if(animationInfo.getContentsAddress().equals("null"))
                {
                    Toast toast = Toast.makeText(mContext, "죄송합니다. 다른 컨텐츠를 선택해 주세요.", Toast.LENGTH_SHORT);
                    toast.show();
                }

                try {
                    //Intent intent = new Intent(mContext, PlayActivity_with_tensorflow.class);
                    Intent intent = new Intent(mContext, PlayActivity_exoplayer.class);
                    intent.putExtra("FILE_NAME", animationInfo.getFileName());
                    intent.putExtra("CONTENTS_ADDRESS", animationInfo.getContentsAddress());
                    intent.putExtra("SUBTITLE_ADDRESS", animationInfo.getSubtitleAddress());
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error! intent " + e);
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
        return episodeList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public ImageView thumbnail;
        public int number;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            thumbnail = view.findViewById(R.id.thumbnail);
            number = -1;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public int getNumber(){
            return number;
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