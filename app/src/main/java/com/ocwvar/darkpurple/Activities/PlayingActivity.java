package com.ocwvar.darkpurple.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ocwvar.darkpurple.Adapters.CoverShowerAdapter;
import com.ocwvar.darkpurple.Adapters.SlidingListAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.Core.ICore;
import com.ocwvar.darkpurple.Services.MediaPlayerService;
import com.ocwvar.darkpurple.Services.MediaServiceConnector;
import com.ocwvar.darkpurple.Units.Cover.CoverManager;
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser;
import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary;
import com.ocwvar.darkpurple.Units.SpectrumAnimDisplay;
import com.ocwvar.darkpurple.Units.ToastMaker;
import com.ocwvar.darkpurple.widgets.CoverShowerViewPager;
import com.ocwvar.darkpurple.widgets.CoverSpectrum;
import com.ocwvar.darkpurple.widgets.LineSlider;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        SlidingListAdapter.OnSlidingMenuClickCallback {

    //轮播滚动等待线程
    PendingStartThread pendingStartThread;
    //刷新界面播放位置线程
    UpdatingTimerThread updatingTimerThread;
    //音频变化广播接收器
    AudioChangeReceiver audioChangeReceiver;
    //滚动条控制器
    SeekBarController seekBarController;
    //侧滑父容器
    DrawerLayout drawerLayout;
    //封面轮播控件
    CoverShowerViewPager coverShower;
    //歌曲信息文字显示
    TextView title, album, artist;
    //当前播放时间
    TextView currentTime, restTime;
    //歌曲进度条
    LineSlider musicSeekBar;
    //频谱开关
    ImageButton spectrumSwitch;
    //均衡器设置
    ImageButton equalizerPage;
    //随机 和 循环 按钮
    ImageView randomButton, loopButton;
    //侧滑快捷切歌列表
    RecyclerView recyclerView;
    //用于显示频谱的SurfaceView
    CoverSpectrum coverSpectrum;
    SpectrumAnimDisplay spectrumAnimDisplay;
    //动画Drawable显示View
    View backGround, darkAnime, mainButton, waitForService;
    //侧滑菜单适配器
    SlidingListAdapter slidingListAdapter;
    //封面轮播适配器
    CoverShowerAdapter showerAdapter;
    //用于时间转换的类
    SimpleDateFormat dateFormat;
    Date date;
    //当前播放的歌曲信息列表
    ArrayList<SongItem> playingList;
    //媒体服务连接器
    private MediaServiceConnector serviceConnector;
    //背景模糊图片弱引用
    private WeakReference<Drawable> blurBG = new WeakReference<>(null);
    //背景模糊图片处理线程弱引用
    private WeakReference<BlurCoverThread> blurCoverThreadObject = new WeakReference<>(null);
    //动画Drawable弱引用
    private WeakReference<TransitionDrawable> weakAnimDrawable = new WeakReference<>(null);
    //主按钮动画Drawable弱引用
    private WeakReference<TransitionDrawable> mainButtonAnimDrawable = new WeakReference<>(null);

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Window window = getWindow();

        if (!AppConfigs.useCompatMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //只有在不使用兼容模式 同时 系统版本大于 Android 5.0 才能使用透明样式

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

        } else if (AppConfigs.useCompatMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.DKGRAY);
            window.setNavigationBarColor(Color.DKGRAY);
        }

        setContentView(R.layout.activity_playing);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        //初始化对象
        seekBarController = new SeekBarController();
        serviceConnector = new MediaServiceConnector(PlayingActivity.this, new ServiceCallback());
        date = new Date();
        dateFormat = new SimpleDateFormat("hh:mm:ss", Locale.US);
        audioChangeReceiver = new AudioChangeReceiver();
        playingList = new ArrayList<>();
        spectrumAnimDisplay = new SpectrumAnimDisplay();
        showerAdapter = new CoverShowerAdapter(playingList);

        //加载View对象
        backGround = findViewById(R.id.contener);
        darkAnime = findViewById(R.id.darkBG);
        waitForService = findViewById(R.id.waitForService);
        spectrumSwitch = (ImageButton) findViewById(R.id.spectrum);
        equalizerPage = (ImageButton) findViewById(R.id.equalizer);
        randomButton = (ImageView) findViewById(R.id.random);
        loopButton = (ImageView) findViewById(R.id.loop);
        coverSpectrum = (CoverSpectrum) findViewById(R.id.surfaceView);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        coverShower = (CoverShowerViewPager) findViewById(R.id.coverShower);
        title = (TextView) findViewById(R.id.shower_title);
        album = (TextView) findViewById(R.id.shower_album);
        artist = (TextView) findViewById(R.id.shower_artist);
        currentTime = (TextView) findViewById(R.id.shower_playing_position);
        restTime = (TextView) findViewById(R.id.shower_rest_position);
        mainButton = findViewById(R.id.shower_mainButton);
        musicSeekBar = (LineSlider) findViewById(R.id.seekBar);

        //随机、循环按钮
        randomButton.setOnClickListener(this);
        loopButton.setOnClickListener(this);

        //封面轮播默认预加载数量
        coverShower.setOffscreenPageLimit(1);

        //设置均衡器界面
        equalizerPage.setOnClickListener(this);

        //设置频谱的控制器
        spectrumSwitch.setOnClickListener(this);
        coverSpectrum.setZOrderOnTop(true);
        coverSpectrum.getHolder().addCallback(spectrumAnimDisplay);

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

        //设置动画显示View的默认TAG
        backGround.setTag(-1L);    //-1为无背景ID
        darkAnime.setTag(false);    //false为不显示，true为已显示
        mainButton.setTag(false);
        waitForService.setTag(false);
        spectrumSwitch.setTag("off");   //off表示关闭状态

        //设置主按钮的按键回调
        mainButton.setOnClickListener(this);

        //设置滚动条的相关操作
        musicSeekBar.setOnSlidingCallback(seekBarController);

        //添加正在使用的媒体库数据到侧滑菜单中
        final ArrayList<SongItem> usingLibrary = MediaLibrary.INSTANCE.getUsingLibrary();
        if (usingLibrary != null) {
            this.playingList.clear();
            this.playingList.addAll(usingLibrary);
            showerAdapter.notifyDataSetChanged();

            slidingListAdapter.setSongItems(this.playingList);
        }

        if (slidingListAdapter.getItemCount() == 0) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }

        if (!AppConfigs.useCompatMode && AppConfigs.StatusBarHeight > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

        //当前没有连接媒体服务
        if (!serviceConnector.isServiceConnected()) {
            //显示等待服务连接动画
            switchWaitForService(true);

            //开始连接服务
            serviceConnector.connect();
        }

        //注册音频变化广播接收器
        registerReceiver(audioChangeReceiver, audioChangeReceiver.filter);

        //同步随机、循环 按钮状态
        if (AppConfigs.playMode_Random) {
            randomButton.setImageResource(R.drawable.ic_action_random_on);
        } else {
            randomButton.setImageResource(R.drawable.ic_action_random_off);
        }

        if (AppConfigs.playMode_Loop) {
            loopButton.setImageResource(R.drawable.ic_action_loop_on);
        } else {
            loopButton.setImageResource(R.drawable.ic_action_loop_off);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        serviceConnector.disConnect();
    }

    /**
     * Activity 被暂停的时候
     * <p>
     * 停止接收音频变化广播
     * 停止更新音频时间变化
     * 停止显示频谱动画
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(audioChangeReceiver);

        if (updatingTimerThread != null) {
            updatingTimerThread.interrupt();
            updatingTimerThread = null;
        }

        if (spectrumSwitch.getTag().toString().equals("on")) {
            switchSpectrumEffect();
        }

    }

    /**
     * 在获取权限后，在这个回调方法中重新连接媒体服务
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!serviceConnector.isServiceConnected()) {
            serviceConnector.connect();
        }
    }

    /**
     * Activity 被销毁的时候手动置空部分资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.warning("播放界面", "开始释放内存");
        blurBG.clear();
        blurCoverThreadObject.clear();
        weakAnimDrawable.clear();
        mainButtonAnimDrawable.clear();

        coverShower.addOnPageChangeListener(null);
        slidingListAdapter.setCallback(null);
        recyclerView.setAdapter(null);
        coverShower.setAdapter(null);

        if (pendingStartThread != null && pendingStartThread.getStatus() != AsyncTask.Status.FINISHED) {
            pendingStartThread.cancel(true);
        }
        if (updatingTimerThread != null) {
            updatingTimerThread.interrupt();
        }

        musicSeekBar = null;
        drawerLayout = null;
        seekBarController = null;
        dateFormat = null;
        date = null;
        recyclerView = null;
        coverShower = null;
        showerAdapter = null;
        slidingListAdapter = null;
        updatingTimerThread = null;
        spectrumSwitch = null;
        equalizerPage = null;
        title = null;
        album = null;
        artist = null;
        currentTime = null;
        restTime = null;
        System.gc();
    }

    /**
     * 更新界面数据
     *
     * @param onlyShowDefault 强制只显示默认数据
     */
    @SuppressWarnings("deprecation")
    private void updateInformation(boolean onlyShowDefault) {
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

            //获取当前正在显示的媒体位置索引
            final int usingIndex = MediaLibrary.INSTANCE.getUsingIndex();

            if (usingIndex >= 0 && serviceConnector.isServiceConnected()) {
                //如果当前有播放数据，并且服务已经连接

                //设置Toolbar上的数据
                setTitle("当前播放序列: " + (usingIndex + 1) + " / " + this.playingList.size());

                //设置当前封面
                coverShower.setCurrentItem(usingIndex);

                //从本地缓存的媒体库中获取当前要显示的媒体数据
                final SongItem playingSong = this.playingList.get(usingIndex);
                //当前播放状态
                final int currentState = serviceConnector.currentState();
                //当前的音频状态，有可能为 NULL
                final PlaybackStateCompat playbackState = MediaControllerCompat.getMediaController(PlayingActivity.this).getPlaybackState();
                //当前的播放位置
                final long currentPosition = (playbackState == null) ? 0L : playbackState.getPosition();
                //媒体长度
                final long mediaDuration = playingSong.getDuration();


                //滑动条是否可用
                musicSeekBar.setEnabled(currentState != PlaybackStateCompat.STATE_NONE);
                //设置歌曲名称显示
                title.setText(String.format("%s %s", getString(R.string.main_header_title), playingSong.getTitle()));
                //设置歌手名称显示
                artist.setText(String.format("%s %s", getString(R.string.main_header_artist), playingSong.getArtist()));
                //设置专辑名称显示
                album.setText(String.format("%s %s", getString(R.string.main_header_album), playingSong.getAlbum()));
                //重置滚动控制条数据
                musicSeekBar.setProgress(0);
                //设置需要重置歌曲长度标记
                musicSeekBar.setTag(true);
                //设置当前播放的时间
                currentTime.setText(time2String(currentPosition));
                //设置当前剩余时间
                restTime.setText(time2String(mediaDuration - currentPosition));
                //执行封面模糊风格处理
                if (!AppConfigs.isUseSimplePlayingScreen) {
                    generateBlurBackGround();
                }

                //根据音频服务当前的状态来更新UI
                switch (currentState) {

                    case PlaybackStateCompat.STATE_PLAYING:
                        switchMainButtonAnim(false);
                        switchDarkAnime(false);

                        if (updatingTimerThread != null) {
                            updatingTimerThread.interrupt();
                            updatingTimerThread = null;
                        }
                        updatingTimerThread = new UpdatingTimerThread();
                        updatingTimerThread.start();
                        break;

                    case PlaybackStateCompat.STATE_NONE:
                    case PlaybackStateCompat.STATE_PAUSED:
                    case PlaybackStateCompat.STATE_STOPPED:
                        switchMainButtonAnim(true);
                        switchDarkAnime(true);
                        if (updatingTimerThread != null) {
                            updatingTimerThread.interrupt();
                            updatingTimerThread = null;
                        }
                        break;
                }
            } else {
                //当前没有播放数据，则显示空界面
                updateInformation(true);
            }
        }
    }

    /**
     * 更新模糊背景效果
     */
    @SuppressWarnings("deprecation")
    private void generateBlurBackGround() {
        if (serviceConnector.isServiceConnected()) {
            //先获取当前播放的数据
            final SongItem playingSong = playingList.get(MediaLibrary.INSTANCE.getUsingIndex());

            //此歌曲没有封面，显示默认背景
            if (TextUtils.isEmpty(CoverManager.INSTANCE.getValidSource(playingSong.getCoverID()))) {

                //获取当前背景图像
                final Drawable prevDrawable = (backGround.getBackground() != null) ? backGround.getBackground() : new ColorDrawable(Color.TRANSPARENT);
                //默认图像，作为切换
                final Drawable nextDrawable = (Build.VERSION.SDK_INT >= 21) ? getResources().getDrawable(R.drawable.blur) : getResources().getDrawable(R.drawable.blur);

                //设置空的TAG
                backGround.setTag(-1L);

                //切换背景
                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{prevDrawable, nextDrawable});
                backGround.setBackground(transitionDrawable);
                transitionDrawable.startTransition(1500);
                return;
            }

            //此歌曲存在可用封面，并且与当前的背景不相同，则进行加载
            if (!backGround.getTag().equals(CoverManager.INSTANCE.getValidSource(playingSong.getCoverID()))) {

                if (blurCoverThreadObject != null && blurCoverThreadObject.get() != null && blurCoverThreadObject.get().getStatus() != AsyncTask.Status.FINISHED) {
                    //如果当前还在处理上一个封面模糊效果 , 则先中断了
                    blurCoverThreadObject.get().cancel(true);
                    blurCoverThreadObject.clear();
                    blurCoverThreadObject = null;
                }
                //设置当前背景图片ID
                backGround.setTag(CoverManager.INSTANCE.getValidSource(playingSong.getCoverID()));

                //开始执行模糊背景处理
                blurCoverThreadObject = new WeakReference<>(new BlurCoverThread(playingSong));
                blurCoverThreadObject.get().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
    }

    /**
     * 滚动封面轮播控件 切换媒体数据
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        if (serviceConnector.isServiceConnected() && state == ViewPager.SCROLL_STATE_IDLE && coverShower.getCurrentItem() != MediaLibrary.INSTANCE.getUsingIndex()) {
            //如果当前音频服务已启动 , 同时滚动动作已完成并且位置发生了变动

            if (pendingStartThread != null && pendingStartThread.getStatus() != AsyncTask.Status.FINISHED) {
                pendingStartThread.cancel(true);
                pendingStartThread = null;
            }

            pendingStartThread = new PendingStartThread();
            pendingStartThread.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        }
    }

    /**
     * 界面点击事件处理
     * 1.切换频谱动画
     * 2.打开EQ界面
     * 3.切换暂停播放主按钮
     * 4.随机、循环 按钮
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.random:
                if (AppConfigs.playMode_Random) {
                    AppConfigs.playMode_Random = false;
                    randomButton.setImageResource(R.drawable.ic_action_random_off);
                } else {
                    AppConfigs.playMode_Random = true;
                    randomButton.setImageResource(R.drawable.ic_action_random_on);
                }
                PreferenceManager.getDefaultSharedPreferences(PlayingActivity.this).edit().putBoolean("playMode_Random", AppConfigs.playMode_Loop).apply();

                break;
            case R.id.loop:
                if (AppConfigs.playMode_Loop) {
                    AppConfigs.playMode_Loop = false;
                    loopButton.setImageResource(R.drawable.ic_action_loop_off);
                } else {
                    AppConfigs.playMode_Loop = true;
                    loopButton.setImageResource(R.drawable.ic_action_loop_on);
                }
                PreferenceManager.getDefaultSharedPreferences(PlayingActivity.this).edit().putBoolean("playMode_Loop", AppConfigs.playMode_Loop).apply();
                break;
            case R.id.spectrum:
                switchSpectrumEffect();
                break;
            case R.id.equalizer:
                //EqualizerActivity.startBlurActivity(5, Color.argb(50, 0, 0, 0), false, PlayingActivity.this, EqualizerActivity.class, null);
                ToastMaker.INSTANCE.show(R.string.coreNotSupported);
                break;
            case R.id.shower_mainButton:
                //主按钮点击事件
                if (serviceConnector.isServiceConnected()) {
                    final int currentState = serviceConnector.currentState();
                    switch (currentState) {
                        case PlaybackStateCompat.STATE_PLAYING:
                            //当前是播放状态 , 则执行暂停操作
                            MediaControllerCompat.getMediaController(PlayingActivity.this).getTransportControls().pause();
                            if (coverSpectrum.isShown()) {
                                //如果当前正在显示频谱 , 则停止刷新
                                if (spectrumAnimDisplay.isDrawing()) {
                                    spectrumAnimDisplay.stop();
                                }
                            }
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            //当前是暂停/停止状态 , 则执行继续播放操作
                            MediaControllerCompat.getMediaController(PlayingActivity.this).getTransportControls().play();
                            if (coverSpectrum.isShown()) {
                                //如果当前正在显示频谱 , 则开始
                                if (!spectrumAnimDisplay.isDrawing()) {
                                    spectrumAnimDisplay.start();
                                }
                            }
                            break;
                        case PlaybackStateCompat.STATE_NONE:
                            //当前服务没有加载资源，直接使用当前已缓存的数据
                            final Bundle bundle = new Bundle();
                            bundle.putInt(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_INT_LIBRARY_INDEX(), coverShower.getCurrentItem());
                            bundle.putString(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_STRING_LIBRARY_NAME(), MediaLibrary.INSTANCE.getUsingLibraryTAG());
                            serviceConnector.sendCommand(MediaPlayerService.COMMAND.INSTANCE.getCOMMAND_PLAY_LIBRARY(), bundle);
                            break;
                    }
                }
                break;
        }
    }

    /**
     * 点击左上角 home 的事件 -> 展开侧滑菜单
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Logo点击事件 , 打开和关闭侧滑菜单
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (slidingListAdapter.getItemCount() > 0) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                break;
        }
        return true;
    }

    /**
     * 处理 BACK 事件
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return false;
                } else {
                    finish();
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 时间长度转换为文本类型
     *
     * @param time 时间长度，单位：ms
     * @return 文本
     */
    private String time2String(long time) {
        if (date == null || time < 0d) {
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

    /**
     * 切换暗色调背景动画
     *
     * @param show True：显示，False：隐藏
     */
    private void switchDarkAnime(boolean show) {
        TransitionDrawable drawable = weakAnimDrawable.get();
        if (drawable == null) {
            final Drawable[] layers = new Drawable[]{
                    new ColorDrawable(Color.TRANSPARENT),
                    new ColorDrawable(Color.argb(100, 0, 0, 0))
            };
            drawable = new TransitionDrawable(layers);
            drawable.setCrossFadeEnabled(true);
            weakAnimDrawable = new WeakReference<>(drawable);
        }
        darkAnime.setBackground(drawable);
        if (show && !((boolean) darkAnime.getTag())) {
            //需要切换为显示状态，同时当前为隐藏状态
            drawable.startTransition(200);
            darkAnime.setTag(true);
        } else if (!show && ((boolean) darkAnime.getTag())) {
            drawable.reverseTransition(200);
            darkAnime.setTag(false);
        }
    }

    /**
     * 切换主按钮动画
     *
     * @param show True：显示，False：隐藏
     */
    private void switchMainButtonAnim(boolean show) {
        TransitionDrawable drawable = mainButtonAnimDrawable.get();
        if (drawable == null) {
            final Drawable[] layers = new Drawable[]{
                    new ColorDrawable(Color.TRANSPARENT),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.ic_main_big_play))
            };
            drawable = new TransitionDrawable(layers);
            drawable.setCrossFadeEnabled(true);
            mainButtonAnimDrawable = new WeakReference<>(drawable);
        }
        mainButton.setBackground(drawable);
        if (show && !((boolean) mainButton.getTag())) {
            //需要切换为显示状态，同时当前为隐藏状态
            drawable.startTransition(200);
            mainButton.setTag(true);
        } else if (!show && ((boolean) mainButton.getTag())) {
            drawable.reverseTransition(200);
            mainButton.setTag(false);
        }
    }

    /**
     * 切换显示等待服务动画
     *
     * @param show True：显示，False：隐藏
     */
    private void switchWaitForService(boolean show) {
        if (show && !(boolean) waitForService.getTag()) {
            //显示等待动画
            //设置View可见性
            waitForService.setVisibility(View.VISIBLE);

            //加载动画
            final Animation animation = AnimationUtils.loadAnimation(PlayingActivity.this, R.anim.wait_for_service_show);
            animation.setDuration(500L);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    waitForService.setAnimation(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            //执行动画
            waitForService.setAnimation(animation);
            waitForService.startAnimation(animation);
            waitForService.setTag(true);
        } else if (!show && (boolean) waitForService.getTag()) {
            //隐藏等待动画
            waitForService.setVisibility(View.GONE);

            final Animation animation = AnimationUtils.loadAnimation(PlayingActivity.this, R.anim.wait_for_service_hide);
            animation.setDuration(500L);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    waitForService.setAnimation(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            waitForService.setAnimation(animation);
            waitForService.startAnimation(animation);
            waitForService.setTag(false);
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
        final Bundle bundle = new Bundle();
        bundle.putInt(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_INT_LIBRARY_INDEX(), position);
        bundle.putString(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_STRING_LIBRARY_NAME(), MediaLibrary.INSTANCE.getUsingLibraryTAG());
        serviceConnector.sendCommand(MediaPlayerService.COMMAND.INSTANCE.getCOMMAND_PLAY_LIBRARY(), bundle);

        updateInformation(!serviceConnector.isServiceConnected());

        if (spectrumSwitch.getTag().toString().equals("on") && !spectrumAnimDisplay.isDrawing()) {
            spectrumAnimDisplay.start();
        }
    }

    /**
     * 切换频谱效果开关
     */
    @SuppressLint("NewApi")
    private void switchSpectrumEffect() {

        if (!AppConfigs.OS_5_UP) {
            ToastMaker.INSTANCE.show(R.string.coreNotSupported);
            return;
            // FIXME: 17-8-11 Android KK 在加载频谱解析器时会产生 error code: -1 (未知异常)，暂时停止 KK 下的使用
        }

        //先让控件不可用
        spectrumSwitch.setEnabled(false);
        spectrumSwitch.setAlpha(0.5f);

        //判断用户是要显示还是隐藏动画
        if (spectrumSwitch.getTag().toString().equals("off")) {
            //显示频谱动画的时候 , 先执行背景淡入动画 , 动画结束后显示SurfaceView , 然后开始绘制频谱动画

            if (AppConfigs.OS_6_UP && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                //判断是否有音频录制权限
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 110);
                spectrumSwitch.setEnabled(true);
                return;
            }

            final int currentState = serviceConnector.currentState();
            if (currentState == PlaybackStateCompat.STATE_PLAYING) {
                //更换按钮图标
                spectrumSwitch.setImageResource(R.drawable.ic_action_sp_on);
                //设置状态到TAG中
                spectrumSwitch.setTag("on");
                //显示频谱SurfaceView
                coverSpectrum.setVisibility(View.VISIBLE);
                spectrumAnimDisplay.start();
            }

            spectrumSwitch.setAlpha(1f);
            spectrumSwitch.setEnabled(true);

        } else {
            //不显示频谱动画的时候 , 隐藏SurfaceView 和 SurfaceView BG , 然后停止动画刷新
            spectrumSwitch.setImageResource(R.drawable.ic_action_sp_off);

            //设置状态到TAG中
            spectrumSwitch.setTag("off");

            //先关闭频谱输出动画
            spectrumAnimDisplay.stop();

            coverSpectrum.setVisibility(View.GONE);

            // 恢复按钮的可点击状态 , 设置按钮的样式
            spectrumSwitch.setEnabled(true);
        }

    }

    /**
     * 媒体服务连接状态回调处理器
     */
    private class ServiceCallback implements MediaServiceConnector.Callbacks {

        /**
         * 媒体服务连接成功
         */
        @Override
        public void onServiceConnected() {

            switchWaitForService(false);

            updateInformation(!serviceConnector.isServiceConnected());
        }

        /**
         * 媒体服务连接断开
         */
        @Override
        public void onServiceDisconnected() {
            finish();
        }

        /**
         * 无法连接媒体服务
         */
        @Override
        public void onServiceConnectionError() {
            finish();
        }

        /**
         * 媒体数据发生更改
         *
         * @param metadata
         */
        @Override
        public void onMediaChanged(MediaMetadataCompat metadata) {

        }

        /**
         * 媒体播放状态发生改变
         *
         * @param state
         */
        @Override
        public void onMediaStateChanged(PlaybackStateCompat state) {

        }

        /**
         * 媒体服务返回当前正在使用的媒体数据列表回调
         *
         * @param data 数据列表
         */
        @Override
        public void onGotUsingLibraryData(List<MediaBrowserCompat.MediaItem> data) {

        }

        /**
         * 无法获取媒体服务返回的媒体数据
         */
        @Override
        public void onGetUsingLibraryDataError() {

        }
    }

    /**
     * 处理封面图片背景效果线程
     */
    private class BlurCoverThread extends AsyncTask<Integer, Void, Drawable> {

        private String TAG = "播放界面Blur等待线程";

        private SongItem songItem;

        BlurCoverThread(SongItem songItem) {
            this.songItem = songItem;
        }

        @Override
        protected Drawable doInBackground(Integer... integers) {
            final String coverID = songItem.getCoverID();

            Logger.normal(TAG, "开始等待：" + coverID);

            int timeoutCount = 10;
            while (!CoverProcesser.INSTANCE.getLastCompletedCoverID().equals(coverID)) {
                try {
                    Thread.sleep(100);
                    timeoutCount--;
                } catch (InterruptedException ignore) {
                }

                if (timeoutCount <= 0) {
                    //等待超时
                    Logger.normal(TAG, "等待超时：" + coverID);
                    return null;
                }
            }

            Logger.normal(TAG, "得到图像：" + coverID);
            return CoverProcesser.INSTANCE.getBlur();
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(final Drawable result) {
            super.onPostExecute(result);
            if (blurBG != null) {
                blurBG.clear();
            }
            blurBG = new WeakReference<>(result);

            //获取当前背景图像
            final Drawable prevDrawable = (backGround.getBackground() != null) ? backGround.getBackground() : new ColorDrawable(Color.TRANSPARENT);

            //生成下一个背景内容Drawable
            final Drawable nextDrawable = (result == null) ? ((Build.VERSION.SDK_INT >= 21) ? getResources().getDrawable(R.drawable.blur) : getResources().getDrawable(R.drawable.blur)) : result;

            //生成动画Drawable
            TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{prevDrawable, nextDrawable});

            //执行动画切换背景
            backGround.setBackground(transitionDrawable);
            transitionDrawable.startTransition(1500);
        }

    }

    /**
     * 轮播切换等待线程
     */
    private class PendingStartThread extends AsyncTask<Integer, Void, Boolean> {

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

            final int currentState = serviceConnector.currentState();
            if (currentState == PlaybackStateCompat.STATE_PLAYING) {

                //如果当前是正在播放状态, 则直接播放
                final Bundle bundle = new Bundle();
                bundle.putInt(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_INT_LIBRARY_INDEX(), coverShower.getCurrentItem());
                bundle.putString(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_STRING_LIBRARY_NAME(), MediaLibrary.INSTANCE.getUsingLibraryTAG());
                serviceConnector.sendCommand(MediaPlayerService.COMMAND.INSTANCE.getCOMMAND_PLAY_LIBRARY(), bundle);
            } else {

                //否则仅加载媒体数据
                final Bundle bundle = new Bundle();
                bundle.putInt(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_INT_LIBRARY_INDEX(), coverShower.getCurrentItem());
                bundle.putString(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_STRING_LIBRARY_NAME(), MediaLibrary.INSTANCE.getUsingLibraryTAG());
                bundle.putBoolean(MediaPlayerService.COMMAND_EXTRA.INSTANCE.getARG_BOOLEAN_PLAY_WHEN_READY(), false);
                serviceConnector.sendCommand(MediaPlayerService.COMMAND.INSTANCE.getCOMMAND_PLAY_LIBRARY(), bundle);
            }

        }
    }

    /**
     * 更新当前播放长度
     */
    private class UpdatingTimerThread extends Thread {

        private String TAG = "更新位置线程";

        @Override
        public void run() {
            Logger.warning(TAG, "开始更新");

            while (!isInterrupted() && seekBarController != null && serviceConnector.isServiceConnected() && musicSeekBar != null && serviceConnector.currentState() == PlaybackStateCompat.STATE_PLAYING) {
                if (serviceConnector.currentState() == PlaybackStateCompat.STATE_PLAYING) {
                    //如果当前不为正在缓冲，则可以读取正确的歌曲长度和当前位置

                    //获取媒体状态数据合集
                    final PlaybackStateCompat playbackState = MediaControllerCompat.getMediaController(PlayingActivity.this).getPlaybackState();

                    //获取媒体长度
                    final long duration = playingList.get(MediaLibrary.INSTANCE.getUsingIndex()).getDuration();

                    //获取最后更新时的位置
                    final long lastPosition = playbackState.getPosition();

                    //当前的时间为：最后更新的位置 + （（当前时间 - 最后更新时间 = 间隔时间）× 播放速度）
                    final long currentPosition = lastPosition + (long) ((SystemClock.elapsedRealtime() - playbackState.getLastPositionUpdateTime()) * playbackState.getPlaybackSpeed());

                    if (duration > 0L && currentPosition >= 0L) {
                        //如果当前时间数据都合法，则进行更新
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //更新进度文字数据
                                currentTime.setText(time2String(currentPosition));
                                restTime.setText(time2String(duration - currentPosition));

                                //如果进度条需要重置歌曲长度标记，则更新歌曲长度
                                if (musicSeekBar.getTag() != null && (boolean) musicSeekBar.getTag()) {
                                    musicSeekBar.setMax((int) (duration / 1000L));
                                    musicSeekBar.setTag(false);
                                }

                                //如果当前没有用户在调整进度条 , 则更新当前播放位置
                                if (!seekBarController.isUserTorching) {
                                    musicSeekBar.setProgress((int) (currentPosition / 1000L));
                                }
                            }
                        });
                    }
                } else {
                    Logger.warning(TAG, "缓冲中...");
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }

            }

            Logger.warning(TAG, "更新中断");
        }

    }

    /**
     * 滚动条的控制器
     */
    private class SeekBarController implements LineSlider.OnSlidingCallback {

        /**
         * 用户当前是否正在滑动的标志，用于阻止歌曲进度在用户滑动进度条的时候更新，而导致
         * 滑动条进度异常
         */
        boolean isUserTorching = false;

        @Override
        public void onSliding(int progress, int max) {
            isUserTorching = true;
        }

        @Override
        public void onStopSliding(int progress, int max) {
            isUserTorching = false;
            if (serviceConnector.isServiceConnected()) {
                MediaControllerCompat.getMediaController(PlayingActivity.this).getTransportControls().seekTo(progress * 1000);
            }
        }

    }

    /**
     * 音频变化接收器
     */
    private class AudioChangeReceiver extends BroadcastReceiver {

        private IntentFilter filter;

        public AudioChangeReceiver() {
            filter = new IntentFilter();
            filter.addAction(ICore.ACTIONS.INSTANCE.getCORE_ACTION_PAUSED());
            filter.addAction(ICore.ACTIONS.INSTANCE.getCORE_ACTION_PLAYING());
            filter.addAction(ICore.ACTIONS.INSTANCE.getCORE_ACTION_STOPPED());
            filter.addAction(ICore.ACTIONS.INSTANCE.getCORE_ACTION_READY());
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            //由于Java不能在 case 中使用 Kotlin 的 Object 中的字符串，所以这里直接使用值
            Logger.normal("音频变化接收器", "Action：" + intent.getAction());
            switch (intent.getAction()) {

                //播放
                //ICore.ACTIONS.INSTANCE.getCORE_ACTION_PLAYING
                case "ca_3":
                    updateInformation(!serviceConnector.isServiceConnected());
                    break;

                //暂停 和 停止
                //ICore.ACTIONS.INSTANCE.getCORE_ACTION_PAUSED 和 ICore.ACTIONS.INSTANCE.getCORE_ACTION_STOPPED
                case "ca_2":
                case "ca_1":
                    updateInformation(!serviceConnector.isServiceConnected());
                    break;

                //新的媒体数据
                //ICore.ACTIONS.INSTANCE.getCORE_ACTION_READY
                case "ca_5":
                    updateInformation(!serviceConnector.isServiceConnected());
                    break;
            }

        }

    }

}

