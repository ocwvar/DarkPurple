package com.ocwvar.darkpurple.Adapters;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Callbacks.OnDragChangedCallback;
import com.ocwvar.darkpurple.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/8/16 1:22
 * Project: DarkPurple
 */
public class PlaylistDetailAdapter extends RecyclerView.Adapter  implements OnDragChangedCallback {

    private ArrayList<SongItem> songItems;
    private OnPlayButtonClickCallback clickCallback;

    public interface OnPlayButtonClickCallback{

        void onPlayButtonClick(int position);

    }

    public PlaylistDetailAdapter(ArrayList<SongItem> songItems , OnPlayButtonClickCallback clickCallback) {
        this.songItems = songItems;
        this.clickCallback = clickCallback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SimpleDetailViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_song,parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SimpleDetailViewHolder viewHolder = (SimpleDetailViewHolder) holder;
        SongItem songItem = songItems.get(position);
        viewHolder.title.setText(songItem.getTitle());
        viewHolder.artist.setText(songItem.getArtist());
        if (new File((songItem.getPath())).exists()){
            //如果文件的确存在 , 则显示播放按钮
            viewHolder.playButton.setVisibility(View.VISIBLE);
        }else {
            viewHolder.playButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (songItems == null){
            return 0;
        }else {
            return songItems.size();
        }
    }

    @Override
    public void onItemPositionChange(RecyclerView.ViewHolder viewHolder , int originalPosition, int targetPosition) {
        Collections.swap(songItems , originalPosition , targetPosition);
        notifyItemMoved(originalPosition , targetPosition);
    }

    @Override
    public void onItemDelete(int position) {
        songItems.remove(position);
        notifyItemRemoved(position);
    }

    class SimpleDetailViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title , artist;
        ImageButton playButton;

        public SimpleDetailViewHolder(View itemView) {
            super(itemView);
            title = (TextView)itemView.findViewById(R.id.textView_title);
            artist = (TextView)itemView.findViewById(R.id.textView_artist);
            playButton = (ImageButton)itemView.findViewById(R.id.imageButton_play);

            playButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (clickCallback != null){
                clickCallback.onPlayButtonClick(getAdapterPosition());
            }
        }

    }
}
