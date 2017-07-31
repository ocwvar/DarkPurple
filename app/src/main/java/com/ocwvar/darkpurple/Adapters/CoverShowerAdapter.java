package com.ocwvar.darkpurple.Adapters;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.Cover.CoverManager;
import com.ocwvar.darkpurple.widgets.CImageView;
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

    @SuppressWarnings("deprecation")
    public CoverShowerAdapter(ArrayList<SongItem> playingList) {
        this.playingList = playingList;
        if (Build.VERSION.SDK_INT >= 21) {
            this.defaultCover = AppConfigs.ApplicationContext.getDrawable(R.drawable.ic_music_big);
        } else {
            this.defaultCover = AppConfigs.ApplicationContext.getResources().getDrawable(R.drawable.ic_music_big);
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

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public Object instantiateItem(final ViewGroup container, int position) {

        final SongItem songItem = playingList.get(position);
        //封面绘制中心点  X轴
        final float centerX = container.getMeasuredWidth() / 2.0f;
        //封面绘制中心点  Y轴
        final float centerY = centerX;
        //封面绘制的半径长度
        final int coverR = (int) (centerX);

        final CImageView imageView;

        //获取有效封面路径，自定义封面优先
        final String coverPath = CoverManager.INSTANCE.getValidSource(songItem.getCoverID());

        //如果存在有效封面数据，则进行加载
        if (!TextUtils.isEmpty(coverPath)) {

            //创建圆形ImageView用于显示封面图像
            imageView = new CImageView(container.getContext(), coverR, centerX, centerY, CoverManager.INSTANCE.getValidColor(songItem.getCoverID()));
            Picasso
                    .with(imageView.getContext())
                    .load(CoverManager.INSTANCE.getAbsoluteSource(coverPath))
                    .error(R.drawable.ic_music_big)
                    .placeholder(R.drawable.ic_music_big)
                    .config(Bitmap.Config.RGB_565)
                    .into(imageView);
        } else {
            //默认图像
            imageView = new CImageView(container.getContext(), coverR, centerX, centerY, AppConfigs.Color.DefaultCoverColor);
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
        container.removeView((View) object);
    }

}
