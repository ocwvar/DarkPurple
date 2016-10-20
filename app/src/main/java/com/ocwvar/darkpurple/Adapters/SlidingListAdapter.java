package com.ocwvar.darkpurple.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/8/3 1:12
 * Project: DarkPurple
 * 侧滑菜单适配器
 */
public class SlidingListAdapter extends RecyclerView.Adapter {

    private ArrayList<SongItem> songItems;
    private OnSlidingMenuClickCallback callback;

    public void setCallback(OnSlidingMenuClickCallback callback) {
        this.callback = callback;
    }

    public void setSongItems(ArrayList<SongItem> songItems) {
        this.songItems = songItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SlideMusicViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slide, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SlideMusicViewHolder slideMusicViewHolder = (SlideMusicViewHolder) holder;
        SongItem songItem = songItems.get(position);
        slideMusicViewHolder.title.setText(songItem.getTitle());
    }

    @Override
    public int getItemCount() {
        if (songItems != null) {
            return songItems.size();
        } else {
            return 0;
        }
    }

    public interface OnSlidingMenuClickCallback {

        void onSlidingMenuClick(SongItem songItem, int position);

    }

    private class SlideMusicViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title;

        SlideMusicViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.textView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            callback.onSlidingMenuClick(songItems.get(getAdapterPosition()), getAdapterPosition());
        }

    }

}
