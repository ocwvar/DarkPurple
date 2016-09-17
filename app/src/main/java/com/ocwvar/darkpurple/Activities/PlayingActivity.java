package com.ocwvar.darkpurple.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ocwvar.darkpurple.Adapters.CoverShowerAdapter;
import com.ocwvar.darkpurple.Adapters.SlidingListAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioCore;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.Units.FastBlur;
import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.Units.SurfaceViewControler;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Activities
 * Data: 2016/7/20 18:08
 * Project: DarkPurple
 * 正在播放页面
 */
public class PlayingActivity
        extends AppCompatActivity
        implements ViewPager.OnPageChangeListener,
        View.OnClickListener,
        SlidingListAdapter.OnSlidingMenuClickCallback,
        CompoundButton.OnCheckedChangeListener {

    //音频服务
    AudioService audioService;
    //轮播滚动等待线程
    PendingStartThread pendingStartThread;
    //刷新界面播放位置线程
    UpdatingTimerThread updatingTimerThread;
    //音频变化广播接收器
    AudioChangeReceiver audioChangeReceiver;
    //滚动条控制器
    SeekBarControler seekBarControler;
    //侧滑父容器
    DrawerLayout drawerLayout;
    //封面轮播控件
    ViewPager coverShower;
    //歌曲信息文字显示
    TextView title, album, artist;
    //当前播放时间
    TextView currectTime, restTime;
    //主按钮
    ImageButton mainButton;
    //歌曲进度条
    SeekBar musicSeekBar;
    //动画切换器
    SwitchCompat switchCompat;
    //侧滑快捷切歌列表
    RecyclerView recyclerView;
    //用于显示频谱的SurfaceView
    SurfaceView surfaceView;
    SurfaceViewControler surfaceViewControler;
    //模糊画面显示位置
    View backGround, surfaceViewBG;

    //侧滑菜单适配器
    SlidingListAdapter slidingListAdapter;
    //封面轮播适配器
    CoverShowerAdapter showerAdapter;
    //背景模糊图片弱引用
    private WeakReference<Bitmap> blurBG = new WeakReference<>(null);
    //背景模糊图片处理线程弱引用
    private WeakReference<BlurCoverThread> blurCoverThreadObject = new WeakReference<>(null);

    //用于时间转换的类
    SimpleDateFormat dateFormat;
    Date date;
    //当前播放的歌曲信息列表
    ArrayList<SongItem> playingList;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.argb(160, 0, 0, 0));
            }

        }

        setContentView(R.layout.activity_playing);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        seekBarControler = new SeekBarControler();
        date = new Date();
        dateFormat = new SimpleDateFormat("hh:mm:ss", Locale.US);
        audioChangeReceiver = new AudioChangeReceiver();
        playingList = new ArrayList<>();
        surfaceViewControler = new SurfaceViewControler();
        showerAdapter = new CoverShowerAdapter(playingList);

        surfaceViewBG = findViewById(R.id.surfaceViewBG);
        backGround = findViewById(R.id.contener);
        switchCompat = (SwitchCompat) findViewById(R.id.switcher);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        coverShower = (ViewPager) findViewById(R.id.coverShower);
        title = (TextView) findViewById(R.id.shower_title);
        album = (TextView) findViewById(R.id.shower_album);
        artist = (TextView) findViewById(R.id.shower_artist);
        currectTime = (TextView) findViewById(R.id.shower_playing_position);
        restTime = (TextView) findViewById(R.id.shower_rest_position);
        mainButton = (ImageButton) findViewById(R.id.shower_mainButton);
        musicSeekBar = (SeekBar) findViewById(R.id.seekBar);

        //设置频谱的控制器
        switchCompat.setOnCheckedChangeListener(this);
        surfaceView.getHolder().addCallback(surfaceViewControler);

        //Toolbar属性设置
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_drawer);

        //主界面滚动控件设置
        coverShower.addOnPageChangeListener(this);
        coverShower.setAdapter(showerAdapter);
        coverShower.setClipChildren(false);

        //侧滑菜单适配器
        slidingListAdapter = new SlidingListAdapter();
        slidingListAdapter.setCallback(this);

        //设置侧滑菜单属性
        recyclerView.setLayoutManager(new LinearLayoutManager(PlayingActivity.this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(slidingListAdapter);

        //设置背景的默认TAG
        backGround.setTag(-1L);

        //设置主按钮的按键回调
        mainButton.setOnClickListener(this);

        //设置滚动条的相关操作
        musicSeekBar.setOnTouchListener(seekBarControler);
        musicSeekBar.setOnSeekBarChangeListener(seekBarControler);

        //获取服务对象
        audioService = ServiceHolder.getInstance().getService();

        if (audioService != null && audioService.getPlayingList() != null) {
            this.playingList.clear();
            this.playingList.addAll(audioService.getPlayingList());
            showerAdapter.notifyDataSetChanged();

            slidingListAdapter.setSongItems(this.playingList);

        }

        if (AppConfigs.NevBarHeight > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //设置底部间距高度
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.pendingLayout_NEV);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(1, AppConfigs.NevBarHeight);
            View emptyView = new View(PlayingActivity.this);
            emptyView.setLayoutParams(layoutParams);
            linearLayout.addView(emptyView);
        }

        if (AppConfigs.StatusBarHeight > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //设置顶部间距高度
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.pendingLayout_STATUSBAR);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(1, AppConfigs.StatusBarHeight);
            View emptyView = new View(PlayingActivity.this);
            emptyView.setLayoutParams(layoutParams);
            linearLayout.addView(emptyView);
        }

    }

    /**
     * 当主界面得到显示的时候 , 更新当前的播放信息数据
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateInfomation(audioService == null);
        registerReceiver(audioChangeReceiver, audioChangeReceiver.filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(audioChangeReceiver);
        if (updatingTimerThread != null) {
            updatingTimerThread.interrupt();
            updatingTimerThread = null;
        }
        if (switchCompat.isChecked()) {
            switchCompat.toggle();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.warnning("播放界面","开始释放内存");
        blurBG.clear();
        blurCoverThreadObject.clear();
        dateFormat = null;
        date = null;
        recyclerView.setAdapter(null);
        recyclerView = null;
        coverShower.setAdapter(null);
        coverShower.addOnPageChangeListener(null);
        coverShower = null;
        showerAdapter = null;
        slidingListAdapter.setCallback(null);
        slidingListAdapter = null;
        updatingTimerThread = null;
        switchCompat = null;
        title = null;
        album = null;
        artist = null;
        currectTime = null;
        restTime = null;
    }

    /**
     * 更新界面数据
     *
     * @param onlyShowDefault 强制只显示默认数据
     */
    @SuppressWarnings("deprecation")
    private void updateInfomation(boolean onlyShowDefault) {
        if (playingList.size() == 0 || onlyShowDefault) {
            //如果当前没有播放数据 , 则显示默认文字
            title.setText(R.string.main_default_title);
            album.setText(R.string.main_default_text);
            artist.setText(R.string.main_default_text);
            setTitle("");

            //如果当前没有播放数据 , 则背景显示默认颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                backGround.setBackgroundColor(getColor(R.color.backgroundColor_Dark));
            } else {
                backGround.setBackgroundColor(getResources().getColor(R.color.backgroundColor_Dark));
            }

        } else {
            //否则显示歌曲信息
            if (audioService != null && audioService.getPlayingIndex() >= 0) {
                //如果当前有播放数据

                //设置Toolbar上的数据
                setTitle("当前播放序列: " + (audioService.getPlayingIndex() + 1) + " / " + audioService.getPlayingList().size());

                //设置当前封面
                coverShower.setCurrentItem(audioService.getPlayingIndex());

                SongItem playingSong = playingList.get(audioService.getPlayingIndex());
                //设置歌曲名称显示
                title.setText(playingSong.getTitle());
                //设置歌手名称显示
                artist.setText(playingSong.getArtist());
                //设置专辑名称显示
                album.setText(playingSong.getAlbum());
                //重置滚动控制条数据
                musicSeekBar.setProgress(0);
                musicSeekBar.setMax((int) audioService.getAudioLength());
                //设置当前播放的时间
                currectTime.setText(time2String(audioService.getPlayingPosition()));
                //设置当前剩余时间
                restTime.setText(time2String(audioService.getAudioLength() - audioService.getPlayingPosition()));
                //设置主按钮的状态
                switch (audioService.getAudioStatus()) {
                    case Playing:
                        mainButton.setImageResource(R.drawable.ic_action_pause);
                        break;
                    case Stopped:
                    case Paused:
                        mainButton.setImageResource(R.drawable.ic_action_play);
                        break;
                }
                //执行封面模糊风格处理
                generateBlurBackGround();
                //如果歌曲播放了 , 就开始更新界面, 更新之前中断旧的更新线程
                if (audioService.getAudioStatus() == AudioCore.AudioStatus.Playing) {
                    if (updatingTimerThread != null) {
                        updatingTimerThread.interrupt();
                        updatingTimerThread = null;
                    }
                    updatingTimerThread = new UpdatingTimerThread();
                    updatingTimerThread.start();
                }
            } else {
                updateInfomation(true);
            }
        }
    }

    /**
     * 更新模糊背景效果
     */
    @SuppressWarnings("deprecation")
    private void generateBlurBackGround() {
        if (audioService != null) {
            //先获取当前播放的数据
            final SongItem playingSong = playingList.get(audioService.getPlayingIndex());

            //判断当前播放的音频是否有封面  同时  当前背景是否已经有相同的图像显示了
            if (playingSong.isHaveCover() && !backGround.getTag().equals(playingSong.getPath())) {
                if (blurCoverThreadObject != null && blurCoverThreadObject.get() != null && blurCoverThreadObject.get().getStatus() != AsyncTask.Status.FINISHED) {
                    //如果当前还在处理上一个封面模糊效果 , 则先中断了
                    blurCoverThreadObject.get().cancel(true);
                    blurCoverThreadObject.clear();
                    blurCoverThreadObject = null;
                }
                //设置当前背景图片ID
                backGround.setTag(playingSong.getPath());

                //开始执行模糊背景处理
                blurCoverThreadObject = new WeakReference<>(new BlurCoverThread(playingSong));
                blurCoverThreadObject.get().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

            } else if (!playingSong.isHaveCover()) {
                //封面不存在的时候 , 显示默认的背景
                Drawable prevDrawable;
                Drawable nextDrawable;

                backGround.setTag(-1L);

                //先设置淡出图像
                if (backGround.getBackground() != null) {
                    //如果能从当前背景中获取到图像
                    prevDrawable = backGround.getBackground();
                } else {
                    //如果获取不到 , 则直接使用透明色
                    prevDrawable = new ColorDrawable(Color.TRANSPARENT);
                }

                //设置淡入图像
                if (Build.VERSION.SDK_INT >= 23) {
                    nextDrawable = new ColorDrawable(getColor(R.color.backgroundColor_Dark));
                } else {
                    nextDrawable = new ColorDrawable(getResources().getColor(R.color.backgroundColor_Dark));
                }

                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{prevDrawable, nextDrawable});
                backGround.setBackground(transitionDrawable);
                transitionDrawable.startTransition(1500);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (audioService != null && state == ViewPager.SCROLL_STATE_IDLE && coverShower.getCurrentItem() != audioService.getPlayingIndex()) {
            //如果当前音频服务已启动 , 同时滚动动作已完成并且位置发生了变动

            if (pendingStartThread != null && pendingStartThread.getStatus() != AsyncTask.Status.FINISHED) {
                pendingStartThread.cancel(true);
                pendingStartThread = null;
            }

            pendingStartThread = new PendingStartThread();
            pendingStartThread.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shower_mainButton:
                //主按钮点击事件
                if (audioService != null) {
                    switch (audioService.getAudioStatus()) {
                        case Playing:
                            //当前是播放状态 , 则执行暂停操作
                            audioService.pause();
                            mainButton.setImageResource(R.drawable.ic_action_play);
                            if (surfaceView.isShown()) {
                                //如果当前正在显示频谱 , 则停止刷新
                                if (surfaceViewControler.isDrawing()) {
                                    surfaceViewControler.stop();
                                }
                            }
                            break;
                        case Stopped:
                        case Paused:
                            //当前是暂停/停止状态 , 则执行继续播放操作
                            audioService.resume();
                            mainButton.setImageResource(R.drawable.ic_action_pause);
                            if (surfaceView.isShown()) {
                                //如果当前正在显示频谱 , 则开始
                                if (!surfaceViewControler.isDrawing()) {
                                    surfaceViewControler.start();
                                }
                            }
                            break;
                    }
                    //更新播放按钮状态
                    sendBroadcast(new Intent(AudioService.NOTIFICATION_REFRESH));
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Logo点击事件 , 打开和关闭侧滑菜单
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return false;
                }else {
                    finish();
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 时间长度转换为文本类型
     *
     * @param time 时间长度
     * @return 文本
     */
    private String time2String(double time) {
        if (time < 0d) {
            return "00:00";
        } else {
            long timeL = (long) time * 1000;
            date.setTime(timeL);
            if (timeL >= 3600000) {
                dateFormat.applyPattern("hh:mm:ss");
            } else {
                dateFormat.applyPattern("mm:ss");
            }
            return dateFormat.format(date);
        }
    }

    /**
     * 侧滑菜单点击事件回调
     *
     * @param songItem 选择中的歌曲数据集合
     * @param position 列表中的位置
     */
    @Override
    public void onSlidingMenuClick(SongItem songItem, int position) {
        audioService.play(playingList, position);
        updateInfomation(false);
    }

    /**
     * 切换是否显示频谱效果
     *
     * @param isSwitchOn 当前是否被打开了
     */
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isSwitchOn) {
        //先让控件不可用
        switchCompat.setEnabled(false);
        switchCompat.setAlpha(0.5f);

        //判断用户是要显示还是隐藏动画
        if (isSwitchOn) {
            //显示频谱动画的时候 , 先执行背景淡入动画 , 动画结束后显示SurfaceView , 然后开始绘制频谱动画
            final Animation anim = new AlphaAnimation(0f, 1f);
            anim.setDuration(500);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //当动画演示完成
                    switchCompat.setEnabled(true);
                    switchCompat.setAlpha(1f);
                    surfaceView.setVisibility(View.VISIBLE);
                    if (audioService.getAudioStatus() == AudioCore.AudioStatus.Playing) {
                        //如果当前是正在播放 , 才执行动画
                        surfaceViewControler.start();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            surfaceViewBG.startAnimation(anim);
            surfaceViewBG.setVisibility(View.VISIBLE);
        } else {
            //不显示频谱动画的时候 , 隐藏SurfaceView 和 SurfaceView BG , 然后停止动画刷新
            final Animation anim = new AlphaAnimation(1f, 0f);
            anim.setDuration(500);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //当动画演示完成
                    switchCompat.setEnabled(true);
                    switchCompat.setAlpha(1f);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            surfaceViewControler.stop();
            surfaceView.setVisibility(View.GONE);
            surfaceViewBG.startAnimation(anim);
            surfaceViewBG.setVisibility(View.GONE);
        }
    }

    /**
     * 处理封面图片背景效果线程
     */
    class BlurCoverThread extends AsyncTask<Integer, Void, Bitmap> {

        private SongItem songItem;

        public BlurCoverThread(SongItem songItem) {
            this.songItem = songItem;
        }

        @Override
        protected Bitmap doInBackground(Integer... integers) {
            //先获取已缓存好的图像
            Bitmap coverImage = null;
            try {
                coverImage = Picasso.with(AppConfigs.ApplicationContext).load(CoverImage2File.getInstance().getCacheFile(songItem.getPath())).get();
            } catch (IOException ignored) {
            }

            if (coverImage != null) {

                //将图像进行一个缩放 , 以得到一个模糊程度很高的图像
                final Matrix matrix = new Matrix();
                matrix.postScale(0.2f, 0.2f);
                coverImage = Bitmap.createBitmap(coverImage, 0, 0, coverImage.getWidth(), coverImage.getHeight(), matrix, true);

                //创建一个空的对象 , 用于存放模糊图像
                Bitmap bluredImage = Bitmap.createBitmap(coverImage.getWidth(), coverImage.getHeight(), Bitmap.Config.ARGB_8888);

                if (Build.VERSION.SDK_INT >= 17) {
                    RenderScript renderScript = RenderScript.create(getApplicationContext());
                    Allocation in = Allocation.createFromBitmap(renderScript, coverImage);
                    Allocation out = Allocation.createFromBitmap(renderScript, bluredImage);
                    ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, in.getElement());
                    scriptIntrinsicBlur.setInput(in);
                    scriptIntrinsicBlur.setRadius(25);
                    scriptIntrinsicBlur.forEach(out);
                    out.copyTo(bluredImage);

                    scriptIntrinsicBlur.destroy();
                    renderScript.destroy();
                } else {
                    try {
                        bluredImage = FastBlur.doBlur(coverImage, 25, false);
                    } catch (Exception e) {
                        return null;
                    }
                }

                return bluredImage;
            } else {
                return null;
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            blurBG = new WeakReference<>(bitmap);
            if (bitmap != null) {
                Drawable prevDrawable;

                if (backGround.getBackground() == null) {

                    if (Build.VERSION.SDK_INT >= 23) {
                        prevDrawable = new ColorDrawable(getColor(R.color.backgroundColor_Dark));
                    } else {
                        prevDrawable = new ColorDrawable(getResources().getColor(R.color.backgroundColor_Dark));
                    }

                } else {
                    prevDrawable = backGround.getBackground();
                }

                Drawable nextDrawable = new BitmapDrawable(getResources(), bitmap);
                TransitionDrawable transitionDrawable = new TransitionDrawable(
                        new Drawable[]{prevDrawable, nextDrawable});
                backGround.setBackground(transitionDrawable);
                transitionDrawable.startTransition(1500);
            }
        }

    }

    /**
     * 轮播切换等待线程
     */
    class PendingStartThread extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... integers) {
            try {
                Thread.sleep(AppConfigs.switchPending);
            } catch (InterruptedException ignored) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            //如果当前是正在播放状态 , 则直接播放
            if (audioService.getAudioStatus() == AudioCore.AudioStatus.Playing) {
                audioService.play(playingList, coverShower.getCurrentItem());
            } else {
                //否则仅仅加载音频数据 , 同时通知状态栏数据更新
                audioService.initAudio(playingList, coverShower.getCurrentItem());
                sendBroadcast(new Intent(AudioService.NOTIFICATION_REFRESH));
            }

            updateInfomation(false);
        }
    }

    /**
     * 更新当前播放长度
     */
    class UpdatingTimerThread extends Thread {

        private String TAG = "更新位置线程";

        @Override
        public void run() {
            Logger.warnning(TAG, "开始更新");
            while (!isInterrupted() && audioService != null && audioService.getAudioStatus() == AudioCore.AudioStatus.Playing) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //更新进度文字数据
                        currectTime.setText(time2String(audioService.getPlayingPosition()));
                        restTime.setText(time2String(audioService.getAudioLength() - audioService.getPlayingPosition()));
                        //如果当前没有用户在调整进度条 , 则更新
                        if (!seekBarControler.isUserTorching) {
                            musicSeekBar.setProgress((int) (audioService.getPlayingPosition()));
                        }
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }

            }

            Logger.warnning(TAG, "更新中断");
        }

    }

    /**
     * 滚动条的控制器
     */
    class SeekBarControler implements View.OnTouchListener, SeekBar.OnSeekBarChangeListener {

        public boolean isUserTorching = false;

        /**
         * 当用户按下的时候将标记设为TRUE , 抬起触摸的时候表为FALSE
         */
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    this.isUserTorching = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    this.isUserTorching = false;
                    break;
            }

            return false;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (audioService != null) {
                audioService.seek2Position(seekBar.getProgress());
            }
        }

    }

    /**
     * 音频变化接收器
     */
    class AudioChangeReceiver extends BroadcastReceiver {

        private IntentFilter filter;

        public AudioChangeReceiver() {
            filter = new IntentFilter();
            filter.addAction(AudioService.AUDIO_PLAY);
            filter.addAction(AudioService.AUDIO_PAUSED);
            filter.addAction(AudioService.AUDIO_RESUMED);
            filter.addAction(AudioService.AUDIO_SWITCH);
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case AudioService.AUDIO_PLAY:
                    mainButton.setImageResource(R.drawable.ic_action_pause);
                    if (updatingTimerThread != null) {
                        updatingTimerThread.interrupt();
                        updatingTimerThread = null;
                    }
                    updatingTimerThread = new UpdatingTimerThread();
                    updatingTimerThread.start();
                    break;
                case AudioService.AUDIO_PAUSED:
                    mainButton.setImageResource(R.drawable.ic_action_play);
                    if (updatingTimerThread != null) {
                        updatingTimerThread.interrupt();
                        updatingTimerThread = null;
                    }
                    break;
                case AudioService.AUDIO_RESUMED:
                    mainButton.setImageResource(R.drawable.ic_action_pause);
                    if (updatingTimerThread != null) {
                        updatingTimerThread.interrupt();
                        updatingTimerThread = null;
                    }
                    updatingTimerThread = new UpdatingTimerThread();
                    updatingTimerThread.start();
                    break;
                case AudioService.AUDIO_SWITCH:
                    updateInfomation(false);
                    break;
            }

        }

    }

}

