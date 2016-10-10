package com.ocwvar.darkpurple.Adapters;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.CImageView;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;



/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/7/22 16:17
 * Project: DarkPurple
 * 封面轮播适配器
 */
public class CoverShowerAdapter extends PagerAdapter {

    private ArrayList<SongItem> playingList;
    private Drawable defaultCover;

    public CoverShowerAdapter(ArrayList<SongItem> playingList) {
        this.playingList = playingList;
        if (Build.VERSION.SDK_INT >= 21) {
            this.defaultCover = AppConfigs.ApplicationContext.getDrawable(R.drawable.ic_cd);
        } else {
            this.defaultCover = AppConfigs.ApplicationContext.getResources().getDrawable(R.drawable.ic_cd);
        }
    }

    @Override
    public int getCount() {
        return playingList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public float getPageWidth(int position) {
        return 1f;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {

        final SongItem songItem = playingList.get(position);
        final float centerX = container.getMeasuredWidth()/2;
        final float centerY = container.getMeasuredHeight()/2;
        final int coverWidth = (int) (container.getMeasuredWidth() / 1.5f);

        final CImageView imageView;

        if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
            //如果有用户手动下载的封面,则优先使用
            imageView = new CImageView(container.getContext(),coverWidth/2,centerX,centerY,songItem.getCustomPaletteColor());
            Picasso
                    .with(AppConfigs.ApplicationContext)
                    .load(songItem.getCustomCoverPath())
                    .error(R.drawable.ic_cd)
                    .resize(coverWidth,coverWidth)
                    .into(imageView);

        } else if (songItem.isHaveCover()) {
            //如果先前缓存有图像 , 则开始读取
            imageView = new CImageView(container.getContext(),coverWidth/2,centerX,centerY,songItem.getPaletteColor());
            Picasso.with(AppConfigs.ApplicationContext)
                    .load(CoverImage2File.getInstance().getAbsoluteCachePath(songItem.getPath()))
                    .error(R.drawable.ic_cd)
                    .resize(coverWidth,coverWidth)
                    .into(imageView);
        } else {
            imageView = new CImageView(container.getContext(),coverWidth/2,centerX,centerY,songItem.getPaletteColor());
            imageView.setImageDrawable(defaultCover);

        }

        container.addView(imageView);

        return imageView;
    }

    /**
     * 将超出缓存范围的 View 从VP中移除 , 同时消除他们内的图像资源
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

}
