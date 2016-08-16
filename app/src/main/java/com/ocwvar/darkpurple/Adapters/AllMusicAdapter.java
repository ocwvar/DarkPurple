package com.ocwvar.darkpurple.Adapters;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.Units.SquareWidthImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/7/5 14:20
 * Project: DarkPurple
 * 扫描歌曲的适配器
 */
public class AllMusicAdapter extends RecyclerView.Adapter {

    private ArrayList<SongItem> checkedItems;
    private ArrayList<SongItem> arrayList;
    private Drawable defaultCover;
    private OnClick onClick;

    boolean isMuiltSelecting = false;

    public AllMusicAdapter() {
        checkedItems = new ArrayList<>();
        arrayList = new ArrayList<>();
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    /**
     * 开启多选模式
     */
    public void startMuiltMode(){
        checkedItems.clear();
        isMuiltSelecting = true;
        notifyDataSetChanged();
    }

    /**
     * 关闭多选模式
     * @return  返回已选择的项目
     */
    public ArrayList<SongItem> stopMuiltMode(){
        isMuiltSelecting = false;
        notifyDataSetChanged();
        return checkedItems;
    }

    /**
     * 当前是否是多选模式
     */
    public boolean isMuiltSelecting() {
        return isMuiltSelecting;
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
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option,parent,false);
            itemView.getLayoutParams().width = (parent.getWidth()/2);
            itemView.getLayoutParams().height = (parent.getWidth()/2);
            return new OptionItemViewHolder(itemView);
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

        viewHolder.title.setBackgroundColor(songItem.getPaletteColor());
        viewHolder.artist.setBackgroundColor(songItem.getPaletteColor());

        viewHolder.marker.setVisibility(View.GONE);
        if (isMuiltSelecting){
            //如果当前是多选模式 , 则先检查是否已经被选择了
            if (checkedItems.contains(songItem)) {
                //如果已经被选择了
                viewHolder.marker.setVisibility(View.VISIBLE);
            }else {
                viewHolder.marker.setVisibility(View.GONE);
            }
        }

        if (songItem.isHaveCover()){
            Picasso
                    .with(AppConfigs.ApplicationContext)
                    .load(CoverImage2File.getInstance().getCacheFile(songItem.getPath()))
                    .error(R.drawable.ic_cd)
                    .into(viewHolder.cover);
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

    class MusicItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        SquareWidthImageView cover;
        ImageView marker;
        TextView title;
        TextView artist;

        public MusicItemViewHolder(View itemView) {
            super(itemView);
            cover = (SquareWidthImageView)itemView.findViewById(R.id.item_cover);
            marker = (ImageView)itemView.findViewById(R.id.item_selector_marker);
            title = (TextView)itemView.findViewById(R.id.item_title);
            artist = (TextView)itemView.findViewById(R.id.item_artist);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null){
                if (isMuiltSelecting){
                    SongItem songItem = arrayList.get( getAdapterPosition()-1 );
                    if (checkedItems.contains(songItem)){
                        //如果点击的时候 , 这个项目已经是被选中了 , 则应该执行取消选中动作
                        checkedItems.remove(songItem);
                        marker.setVisibility(View.GONE);
                    }else {
                        //如果是没选中状态 , 则应该被标记上
                        checkedItems.add(songItem);
                        marker.setVisibility(View.VISIBLE);
                    }
                    notifyItemChanged(getAdapterPosition());

                }else {
                    onClick.onListClick( arrayList , getAdapterPosition()-1 );
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (onClick != null && !isMuiltSelecting){
                onClick.onListItemLongClick(arrayList.get(getAdapterPosition()) , getAdapterPosition()-1);
            }
            return false;
        }

    }

    class OptionItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public OptionItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null && !isMuiltSelecting){
                onClick.onOptionClick();
            }
        }

    }

    /**
     * 点击的回调接口
     */
    public interface OnClick{

        void onListClick(ArrayList<SongItem> songList , int position);

        void onListItemLongClick(SongItem songItem , int position);

        void onOptionClick();
    }

}
