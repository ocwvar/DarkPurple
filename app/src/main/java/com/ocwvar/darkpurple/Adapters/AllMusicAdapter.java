package com.ocwvar.darkpurple.Adapters;

import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.ImageLoader.OCImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/7/5 14:20
 * Project: DarkPurple
 * 扫描歌曲的适配器
 */
public class AllMusicAdapter extends RecyclerView.Adapter {

    private ArrayList<SongItem> arrayList;
    private OnClick onClick;
    private Drawable defaultCover;

    public AllMusicAdapter() {
        arrayList = new ArrayList<>();
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    /**
     * 重置并添加数据
     * @param songItems 数据源
     */
    public void setDatas(ArrayList<SongItem> songItems){
        this.arrayList.clear();
        this.arrayList.addAll(songItems);
    }

    /**
     * 移除单个项目
     * @param position  项目位置
     */
    public void removeItem(int position){
        arrayList.remove(position);
        notifyItemRemoved(position+1);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0){
            return new OptionItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option,parent,false));
        }else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card,parent,false);
            itemView.getLayoutParams().width = (parent.getWidth()/2);
            itemView.getLayoutParams().height = (parent.getWidth()/2);
            return new MusicItemViewHolder(itemView);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) return;

        SongItem songItem = arrayList.get(position - 1);
        MusicItemViewHolder viewHolder = (MusicItemViewHolder) holder;
        viewHolder.title.setText(songItem.getTitle());
        viewHolder.artist.setText(songItem.getArtist());
        viewHolder.panel.setBackgroundColor(ColorUtils.setAlphaComponent(songItem.getPaletteColor(),200));
        if (songItem.isHaveCover()){
            OCImageLoader.loader().loadImage(songItem.getPath(),viewHolder.cover);
        }else {
            if (defaultCover == null){
                defaultCover = AppConfigs.ApplicationContext.getResources().getDrawable(R.drawable.ic_cd);
            }
            viewHolder.cover.setImageDrawable(defaultCover);
        }

    }

    @Override
    public int getItemCount() {
        //多出一个文字用于放置首个选项按钮
        return arrayList.size() + 1;
    }

    class MusicItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView cover;
        TextView title;
        TextView artist;
        View panel;

        public MusicItemViewHolder(View itemView) {
            super(itemView);
            cover = (ImageView)itemView.findViewById(R.id.item_cover);
            title = (TextView)itemView.findViewById(R.id.item_title);
            artist = (TextView)itemView.findViewById(R.id.item_artist);
            panel = itemView.findViewById(R.id.item_message_panel);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null){
                onClick.onListClick(arrayList,getAdapterPosition()-1);
            }
        }

    }

    class OptionItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public OptionItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null){
                onClick.onOptionClick();
            }
        }

    }

    /**
     * 点击的回调接口
     */
    public interface OnClick{

        void onListClick(ArrayList<SongItem> songList , int position);

        void onOptionClick();
    }

}
