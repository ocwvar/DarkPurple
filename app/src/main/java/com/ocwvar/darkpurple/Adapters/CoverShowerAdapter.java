package com.ocwvar.darkpurple.Adapters;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.ImageLoader.HandleOnLoaded;
import com.ocwvar.darkpurple.Units.ImageLoader.OCImageLoader;
import com.ocwvar.darkpurple.Units.ImageLoader.OnImageLoad;

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
        if (Build.VERSION.SDK_INT >= 21){
            this.defaultCover = AppConfigs.ApplicationContext.getDrawable(R.drawable.ic_cd);
        }else {
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
    public Object instantiateItem(ViewGroup container, int position) {

        final ImageView cover = new ImageView(container.getContext());
        cover.setScaleType(ImageView.ScaleType.CENTER);

        SongItem songItem = playingList.get(position);

        final int viewPagerWidth = container.getMeasuredWidth();

        final float imageWidth = viewPagerWidth / 1.5f;

        if (songItem.isHaveCover()){
            //如果先前缓存有图像 , 则开始读取
            OCImageLoader.loader().loadImage(
                    songItem.getPath(), null, cover, new OnImageLoad() {

                @Override
                public void onLoadCompleted(Bitmap image, String tag) {}

                @Override
                public void onLoadFailed() {
                    cover.setImageDrawable(defaultCover);
                }

            }, new HandleOnLoaded() {

                @Override
                public Bitmap reduce(Bitmap bitmap, String tag) {
                    Matrix matrix = new Matrix();
                    float scaleTimes = imageWidth / (float)bitmap.getWidth();
                    matrix.postScale( scaleTimes , scaleTimes );
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

            });
        }else {

            cover.setImageDrawable(defaultCover);

        }


        /*if (songItem != null){
            Bitmap coverImage = OCImageLoader.loader().getCache(songItem.getPath());
            if (coverImage != null){
                cover.setImageBitmap(coverImage);
            }else {
                cover.setImageDrawable(defaultCover);
            }
        }*/

        container.addView(cover);

        return cover;
    }

    /**
     * 将超出缓存范围的 View 从VP中移除 , 同时消除他们内的图像资源
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ImageView imageView = (ImageView)object;
        imageView.setImageBitmap(null);
        imageView.setImageDrawable(null);
        container.removeView(imageView);
    }

}
