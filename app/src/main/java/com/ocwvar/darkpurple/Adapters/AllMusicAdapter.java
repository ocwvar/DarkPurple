package com.ocwvar.darkpurple.Adapters;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.widgets.SquareWidthImageView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/7/5 14:20
 * Project: DarkPurple
 * 扫描歌曲的适配器
 */
public class AllMusicAdapter extends RecyclerView.Adapter {

    private final Object TAGOBJECT = new Object();
    private final ArrayList<SongItem> checkedItems;
    private final ArrayList<SongItem> arrayList;
    private final RecycleViewScrollController scrollController;
    //用于时间转换的类
    private SimpleDateFormat dateFormat;
    private Date date;
    private LayoutStyle layoutStyle = LayoutStyle.Grid;
    private boolean isMuiltSelecting = false;
    private Drawable defaultCover;
    private OnClick onClick;
    private int imageSize;

    public AllMusicAdapter(@Nullable LayoutStyle layoutStyle) {
        scrollController = new RecycleViewScrollController();
        checkedItems = new ArrayList<>();
        arrayList = new ArrayList<>();
        date = new Date();
        dateFormat = new SimpleDateFormat("hh:mm:ss", Locale.US);
        if (layoutStyle == null || layoutStyle == LayoutStyle.Grid) {
            this.layoutStyle = LayoutStyle.Grid;
        } else {
            this.layoutStyle = LayoutStyle.Linear;
        }
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setOnRecycleViewScrollController(RecyclerView recyclerView) {
        if (recyclerView != null) {
            recyclerView.addOnScrollListener(scrollController);
        }
    }

    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }

    /**
     * 开启多选模式
     */
    public void startMuiltMode() {
        checkedItems.clear();
        isMuiltSelecting = true;
        notifyDataSetChanged();
    }

    /**
     * 关闭多选模式
     *
     * @return 返回已选择的项目列表副本
     */
    public ArrayList<SongItem> stopMuiltMode() {
        isMuiltSelecting = false;
        notifyDataSetChanged();
        return new ArrayList<>(checkedItems);
    }

    /**
     * 当前是否是多选模式
     */
    public boolean isMuiltSelecting() {
        return isMuiltSelecting;
    }

    /**
     * 重置并添加数据
     *
     * @param songItems 数据源
     */
    public void setDatas(ArrayList<SongItem> songItems) {
        this.arrayList.clear();
        this.arrayList.addAll(songItems);
    }

    /**
     * @return 歌曲列表数据
     */
    public ArrayList<SongItem> getSongList() {
        return arrayList;
    }

    /**
     * 替换列表中的其中一个数据
     *
     * @param songItem 要替换进列表的数据
     */
    public void replaceSongItem(SongItem songItem) {
        int position = arrayList.indexOf(songItem);
        arrayList.set(position, songItem);
    }

    /**
     * 移除单个项目
     *
     * @param position 项目位置
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeItem(int position) {
        String filePath = arrayList.remove(position).getPath();
        //删除歌曲文件
        new File(filePath).delete();
        notifyItemRemoved(position + 1);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        imageSize = parent.getWidth() / 2;
        if (viewType == 0) {
            //第一个项目永远为 Option
            switch (layoutStyle) {
                //根据风格进行加载不同的布局文件
                case Grid:
                    //表格型布局
                    View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_grid, parent, false);
                    itemView.getLayoutParams().width = parent.getWidth() / 2;
                    itemView.getLayoutParams().height = parent.getWidth() / 2;
                    return new OptionItemViewHolder(itemView);
                case Linear:
                    //线性布局
                    return new OptionItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_linear, parent, false));
            }
        } else {
            //除了第一个项目外的都是歌曲项目
            switch (layoutStyle) {
                case Grid:
                    return new MusicItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card_grid, parent, false));
                case Linear:
                    return new MusicItemLinearViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card_linear, parent, false));
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0 || holder == null) return;

        SongItem songItem = arrayList.get(position - 1);

        switch (layoutStyle) {
            case Grid:
                MusicItemViewHolder viewHolder = (MusicItemViewHolder) holder;

                viewHolder.title.setText(songItem.getTitle());
                viewHolder.artist.setText(songItem.getArtist());

                viewHolder.marker.setVisibility(View.GONE);
                if (isMuiltSelecting) {
                    //如果当前是多选模式 , 则先检查是否已经被选择了
                    if (checkedItems.contains(songItem)) {
                        //如果已经被选择了
                        viewHolder.marker.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.marker.setVisibility(View.GONE);
                    }
                }

                if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
                    //如果有用户自定义的封面和混合颜色,则优先使用
                    Picasso
                            .with(AppConfigs.ApplicationContext)
                            .load(songItem.getCustomCoverPath())
                            .config(Bitmap.Config.RGB_565)
                            .tag(TAGOBJECT)
                            .error(R.drawable.ic_cd)
                            .resize(imageSize, imageSize)
                            .into(viewHolder.cover);
                    viewHolder.cardView.setCardBackgroundColor(songItem.getCustomPaletteColor());
                } else if (songItem.isHaveCover()) {
                    //没有下载的封面,则使用读取到的封面文件和混合颜色
                    Picasso
                            .with(AppConfigs.ApplicationContext)
                            .load(CoverImage2File.getInstance().getCacheFile(songItem.getPath()))
                            .config(Bitmap.Config.RGB_565)
                            .tag(TAGOBJECT)
                            .error(R.drawable.ic_cd)
                            .resize(imageSize, imageSize)
                            .into(viewHolder.cover);
                    viewHolder.cardView.setCardBackgroundColor(songItem.getPaletteColor());
                } else {
                    if (defaultCover == null) {
                        defaultCover = AppConfigs.ApplicationContext.getResources().getDrawable(R.drawable.ic_cd);
                    }
                    viewHolder.cardView.setCardBackgroundColor(songItem.getPaletteColor());
                    viewHolder.cover.setImageDrawable(defaultCover);
                }
                break;
            case Linear:
                MusicItemLinearViewHolder viewHolderLinear = (MusicItemLinearViewHolder) holder;
                viewHolderLinear.title.setText(songItem.getTitle());
                viewHolderLinear.title.setTextColor(AppConfigs.Color.Linear_Title_Color);
                viewHolderLinear.artist.setText(songItem.getArtist());
                viewHolderLinear.artist.setTextColor(AppConfigs.Color.Linear_Artist_Color);
                viewHolderLinear.icon.setImageResource(R.drawable.ic_action_music_small);
                viewHolderLinear.time.setText(time2String(songItem.getLength()));
                viewHolderLinear.time.setTextColor(AppConfigs.Color.Linear_Time_Color);
                if (isMuiltSelecting) {
                    //如果当前是多选模式 , 则先检查是否已经被选择了
                    if (checkedItems.contains(songItem)) {
                        //如果已经被选择了
                        viewHolderLinear.icon.setImageResource(R.drawable.ic_action_marker_small);
                    } else {
                        viewHolderLinear.icon.setImageResource(R.drawable.ic_action_music_small);
                    }
                }
                if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
                    viewHolderLinear.icon.setBackgroundColor(ColorUtils.setAlphaComponent(songItem.getCustomPaletteColor(), 100));
                    viewHolderLinear.colorBar.setBackgroundColor(songItem.getCustomPaletteColor());
                } else if (songItem.isHaveCover()) {
                    viewHolderLinear.icon.setBackgroundColor(ColorUtils.setAlphaComponent(songItem.getPaletteColor(), 100));
                    viewHolderLinear.colorBar.setBackgroundColor(songItem.getPaletteColor());
                } else {
                    viewHolderLinear.icon.setBackgroundColor(ColorUtils.setAlphaComponent(songItem.getPaletteColor(), 100));
                    viewHolderLinear.colorBar.setBackgroundColor(songItem.getPaletteColor());
                }
                break;
        }

    }

    @Override
    public int getItemCount() {
        //多出一个文字用于放置首个选项按钮
        return arrayList.size() + 1;
    }

    /**
     * 时间长度转换为文本类型
     *
     * @param time 时间长度
     * @return 文本
     */
    private String time2String(long time) {
        if (time < 0d) {
            return "00:00";
        } else {
            date.setTime(time);
            if (time >= 3600000) {
                dateFormat.applyPattern("hh:mm:ss");
            } else {
                dateFormat.applyPattern("mm:ss");
            }
            return dateFormat.format(date);
        }
    }

    public enum LayoutStyle {Linear, Grid}

    /**
     * 点击的回调接口
     */
    public interface OnClick {

        void onListClick(ArrayList<SongItem> songList, int position, View itemView);

        void onListItemLongClick(SongItem songItem, int position);

        void onOptionClick();
    }

    private class MusicItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        SquareWidthImageView cover;
        CardView cardView;
        ImageView marker;
        TextView title;
        TextView artist;

        MusicItemViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cardView_BG);
            cover = (SquareWidthImageView) itemView.findViewById(R.id.item_cover);
            marker = (ImageView) itemView.findViewById(R.id.item_selector_marker);
            title = (TextView) itemView.findViewById(R.id.item_title);
            artist = (TextView) itemView.findViewById(R.id.item_artist);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null) {
                if (isMuiltSelecting) {
                    SongItem songItem = arrayList.get(getAdapterPosition() - 1);
                    if (checkedItems.contains(songItem)) {
                        //如果点击的时候 , 这个项目已经是被选中了 , 则应该执行取消选中动作
                        checkedItems.remove(songItem);
                        marker.setVisibility(View.GONE);
                    } else {
                        //如果是没选中状态 , 则应该被标记上
                        checkedItems.add(songItem);
                        marker.setVisibility(View.VISIBLE);
                    }
                    notifyItemChanged(getAdapterPosition());

                } else {
                    onClick.onListClick(arrayList, getAdapterPosition() - 1, itemView);
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (onClick != null && !isMuiltSelecting) {
                onClick.onListItemLongClick(arrayList.get(getAdapterPosition() - 1), getAdapterPosition() - 1);
            }
            return false;
        }

    }

    private class MusicItemLinearViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        View colorBar;
        TextView title, artist, time;
        ImageView icon;

        MusicItemLinearViewHolder(View itemView) {
            super(itemView);
            colorBar = itemView.findViewById(R.id.item_cover_color_bar);
            title = (TextView) itemView.findViewById(R.id.item_title);
            artist = (TextView) itemView.findViewById(R.id.item_artist);
            time = (TextView) itemView.findViewById(R.id.item_time);
            icon = (ImageView) itemView.findViewById(R.id.item_icon);
            itemView.setOnTouchListener(this);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null) {
                if (isMuiltSelecting) {
                    SongItem songItem = arrayList.get(getAdapterPosition() - 1);
                    if (checkedItems.contains(songItem)) {
                        //如果点击的时候 , 这个项目已经是被选中了 , 则应该执行取消选中动作
                        checkedItems.remove(songItem);
                        icon.setImageResource(R.drawable.ic_action_music_small);
                    } else {
                        //如果是没选中状态 , 则应该被标记上
                        checkedItems.add(songItem);
                        icon.setImageResource(R.drawable.ic_action_marker_small);
                    }
                    notifyItemChanged(getAdapterPosition());

                } else {
                    onClick.onListClick(arrayList, getAdapterPosition() - 1, itemView);
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (onClick != null && !isMuiltSelecting) {
                onClick.onListItemLongClick(arrayList.get(getAdapterPosition() - 1), getAdapterPosition() - 1);
            }
            return false;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    title.setAlpha(0.5f);
                    break;
                default:
                    title.setAlpha(1.0f);
            }
            return false;
        }
    }

    private class OptionItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        OptionItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onClick != null && !isMuiltSelecting) {
                onClick.onOptionClick();
            }
        }

    }

    /**
     * 监听RecycleView的滚动情况 , 来控制图像的加载
     */
    private class RecycleViewScrollController extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                Picasso.with(recyclerView.getContext()).resumeTag(TAGOBJECT);
            } else {
                Picasso.with(recyclerView.getContext()).pauseTag(TAGOBJECT);
            }
        }

    }

}
