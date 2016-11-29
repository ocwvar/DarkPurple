package com.ocwvar.darkpurple.Adapters;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.Units.PlaylistUnits;
import com.ocwvar.darkpurple.widgets.SquareHightImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/8/14 19:41
 * Project: DarkPurple
 * 播放列表项目的列表适配器
 */
public class PlaylistItemAdapter extends RecyclerView.Adapter {

    ArrayList<PlaylistItem> playlistItems;
    OnButtonClickCallback callback;

    public PlaylistItemAdapter() {
        playlistItems = PlaylistUnits.getInstance().getPlaylists();
    }

    public void setOnButtonClickCallback(OnButtonClickCallback callback) {
        this.callback = callback;
    }

    public void removePlaylist(int position) {
        if (playlistItems != null && position >= 0 && position < playlistItems.size()) {
            PlaylistUnits.getInstance().removePlaylist(playlistItems.remove(position));
            notifyItemRemoved(position);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlaylistItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        PlaylistItemViewHolder itemViewHolder = (PlaylistItemViewHolder) holder;
        PlaylistItem playlistItem = playlistItems.get(position);

        itemViewHolder.title.setText(playlistItem.getName());
        itemViewHolder.count.setText(Integer.toString(playlistItem.getCounts()) + AppConfigs.ApplicationContext.getString(R.string.text_playlist_countEND));
        itemViewHolder.backGround.setBackgroundColor(playlistItem.getColor());

        Picasso.with(AppConfigs.ApplicationContext)
                .load(CoverImage2File.getInstance().getAbsoluteCachePath(playlistItem.getFirstAudioPath()))
                .error(R.drawable.ic_music_mid)
                .into(itemViewHolder.cover);

    }

    @Override
    public int getItemCount() {
        return playlistItems.size();
    }

    public interface OnButtonClickCallback {

        void onPlayButtonClick(PlaylistItem playlistItem, int position);

        void onMoreButtonClick(PlaylistItem playlistItem, int position);

    }

    private class PlaylistItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View backGround;
        SquareHightImageView cover;
        ImageButton more, play;
        TextView title, count;

        PlaylistItemViewHolder(View itemView) {
            super(itemView);
            backGround = itemView.findViewById(R.id.bg);
            cover = (SquareHightImageView) itemView.findViewById(R.id.imageView_cover);

            more = (ImageButton) itemView.findViewById(R.id.imageButton_more);
            play = (ImageButton) itemView.findViewById(R.id.imageButton_play);

            title = (TextView) itemView.findViewById(R.id.textView_title);
            count = (TextView) itemView.findViewById(R.id.textView_count);

            more.setOnClickListener(this);
            play.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (callback != null) {
                switch (view.getId()) {
                    case R.id.imageButton_more:
                        callback.onMoreButtonClick(playlistItems.get(getAdapterPosition()), getAdapterPosition());
                        break;
                    case R.id.imageButton_play:
                        callback.onPlayButtonClick(playlistItems.get(getAdapterPosition()), getAdapterPosition());
                        break;
                }
            }
        }

    }

}
