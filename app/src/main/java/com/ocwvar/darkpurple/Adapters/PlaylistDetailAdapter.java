package com.ocwvar.darkpurple.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Callbacks.OnDragChangedCallback;
import com.ocwvar.darkpurple.R;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/8/16 1:22
 * Project: DarkPurple
 * 播放列表详情里列表的适配器
 */
public class PlaylistDetailAdapter extends RecyclerView.Adapter implements OnDragChangedCallback {

    private ArrayList<SongItem> songItems;

    public PlaylistDetailAdapter(ArrayList<SongItem> songItems) {
        this.songItems = songItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SimpleDetailViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_song, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SimpleDetailViewHolder viewHolder = (SimpleDetailViewHolder) holder;
        SongItem songItem = songItems.get(position);
        viewHolder.title.setText(songItem.getTitle());
        viewHolder.artist.setText(songItem.getArtist());
    }

    @Override
    public int getItemCount() {
        if (songItems == null) {
            return 0;
        } else {
            return songItems.size();
        }
    }

    @Override
    public void onItemPositionChange(RecyclerView.ViewHolder viewHolder, int originalPosition, int targetPosition) {
        Collections.swap(songItems, originalPosition, targetPosition);
        notifyItemMoved(originalPosition, targetPosition);
    }

    @Override
    public void onItemDelete(int position) {
        songItems.remove(position);
        notifyItemRemoved(position);
    }

    private class SimpleDetailViewHolder extends RecyclerView.ViewHolder {

        TextView title, artist;
        ImageButton playButton;

        SimpleDetailViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textView_title);
            artist = itemView.findViewById(R.id.textView_artist);
            playButton = itemView.findViewById(R.id.imageButton_play);

        }
    }
}
