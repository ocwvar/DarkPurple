package com.ocwvar.darkpurple.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.Adapters.MainViewPagerAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.FragmentPages.ui.AllMusicFragment;
import com.ocwvar.darkpurple.FragmentPages.ui.PlaylistPageFragment;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.BaseActivity;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.squareup.picasso.Picasso;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Activities
 * Data: 2016/7/5 13:31
 * Project: DarkPurple
 * 显示的主界面
 */
public class SelectMusicActivity extends BaseActivity {

    //ViewPager的Adapter
    protected MainViewPagerAdapter viewPagerAdapter;
    //用于显示所有功能板块的ViewPager
    private ViewPager viewPager;
    //主界面上方的歌曲文字信息
    private TextView nowPlayingTV;
    //主界面上方的歌曲封面信息
    private ImageView headerCover;
    //更新主界面当前播放信息的广播接收器
    private UpdateHeaderPlayingText headerTextUpdater;

    @Override
    protected boolean onPreSetup() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setBackgroundDrawable(new ColorDrawable(AppConfigs.Color.WindowBackground_Color));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.argb(160, 0, 0, 0));
        }
        headerTextUpdater = new UpdateHeaderPlayingText();
        return true;
    }

    @Override
    protected int onSetToolBar() {
        return R.id.toolbar;
    }

    @Override
    protected int setActivityView() {
        return R.layout.activity_main;
    }

    @Override
    protected void onSetupViews() {

        if (isToolBarLoaded()) {
            getToolBar().setBackground(null);
            setTitle(null);
            ((ImageView) findViewById(R.id.header_image_shadow)).setColorFilter(AppConfigs.Color.TabLayout_color);
        }

        nowPlayingTV = (TextView) findViewById(R.id.header_nowPlaying);
        headerCover = (ImageView) findViewById(R.id.header_small_cover);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.Main_TabLayout);
        viewPager = (ViewPager) findViewById(R.id.Main_ViewPager);
        viewPagerAdapter = new MainViewPagerAdapter(getSupportFragmentManager(),
                new String[]{
                        getText(R.string.ViewPage_Tab_AllMusic).toString(),
                        getText(R.string.ViewPage_Tab_Playlist).toString(),
                });
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        viewPagerAdapter.addFragmentPageToEnd(new AllMusicFragment());
        viewPagerAdapter.addFragmentPageToEnd(new PlaylistPageFragment());

        tabLayout.setBackgroundColor(AppConfigs.Color.TabLayout_color);
        tabLayout.setTabTextColors(AppConfigs.Color.TabLayout_title_color, AppConfigs.Color.TabLayout_title_color_selected);
        tabLayout.setSelectedTabIndicatorColor(AppConfigs.Color.TabLayout_Indicator_color);

        findViewById(R.id.action_playing).setOnClickListener(this);
        findViewById(R.id.action_setting).setOnClickListener(this);
        findViewById(R.id.action_sort).setOnClickListener(this);

        onSetupService();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                WelcomeActivity.startBlurActivity(5, Color.TRANSPARENT, false, SelectMusicActivity.this, WelcomeActivity.class, null);
            }
        }, 3000);

    }

    private void onSetupService() {
        if (ServiceHolder.getInstance().getService() == null) {
            //如果当前没有获取到服务对象 , 则创建一个保存
            final ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    //当服务连接上的时候

                    if (iBinder != null) {
                        //获取服务对象
                        AudioService service = ((AudioService.ServiceObject) iBinder).getService();
                        if (service != null) {
                            //如果获取服务成功 , 则保存到全局储存器中 , 然后解除绑定
                            ServiceHolder.getInstance().setService(service);
                            Snackbar.make(findViewById(android.R.id.content), R.string.service_ok, Snackbar.LENGTH_LONG).show();
                        } else {
                            //否则提示用户
                            Snackbar.make(findViewById(android.R.id.content), R.string.service_error, Snackbar.LENGTH_LONG).show();
                        }
                        unbindService(this);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    //当服务断开连接的时候 , 将全局储存器中的对象置为 NULL
                    ServiceHolder.getInstance().setService(null);
                }
            };

            //开始连接服务
            Intent intent = new Intent(SelectMusicActivity.this, AudioService.class);
            startService(intent);
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //如果当前系统版本大于 23 则先检测是否有文件读写权限
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                    Snackbar.make(findViewById(android.R.id.content), R.string.musicFolder_noPermission, Snackbar.LENGTH_SHORT).show();
                }
            }

        }
    }

    @Override
    protected void onViewClick(View clickedView) {
        switch (clickedView.getId()) {
            case R.id.action_playing:
                startActivity(new Intent(SelectMusicActivity.this, PlayingActivity.class));
                break;
            case R.id.action_setting:
                startActivityForResult(new Intent(SelectMusicActivity.this, SettingsActivity.class), 1);
                break;
            case R.id.action_sort:
                showMusicSortList();
                break;
        }
    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }

    /**
     * 当用户从设置界面返回出来的时候，重新载入所有设置
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                AppConfigs.reInitOptionValues();
                break;
        }
    }

    /**
     * 当界面恢复的时候 , 重新注册广播接收器
     * 并重新拉取一次播放歌曲名字
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (headerTextUpdater == null) {
            this.headerTextUpdater = new UpdateHeaderPlayingText();
        }
        registerReceiver(headerTextUpdater, headerTextUpdater.intentFilter);
        updateNowPlaying();
    }

    /**
     * 当界面被暂停的时候 , 注销广播接收器
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (headerTextUpdater != null) {
            unregisterReceiver(headerTextUpdater);
        }
    }

    /**
     * 根据当前ViewPager显示的位置不同，将Activity得到的按键事件传递到对应的Fragment中
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (viewPager.getCurrentItem()) {
            case 0:
                //第一个界面的
                final AllMusicFragment fragment = (AllMusicFragment) viewPagerAdapter.getItem(0);
                if (fragment == null) {
                    return super.onKeyDown(keyCode, event);
                } else {
                    return !fragment.onActivityKeyDown(keyCode, event) && super.onKeyDown(keyCode, event);
                }
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * 更新正在播放的歌曲文字
     */
    private void updateNowPlaying() {
        final AudioService service = ServiceHolder.getInstance().getService();
        if (service != null && service.getPlayingSong() != null) {
            final SongItem songItem = service.getPlayingSong();
            nowPlayingTV.setText(songItem.getTitle() + "\n" + songItem.getArtist());
            if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
                //如果有用户自定义的封面和混合颜色,则优先使用
                Picasso
                        .with(AppConfigs.ApplicationContext)
                        .load(songItem.getCustomCoverPath())
                        .config(Bitmap.Config.RGB_565)
                        .error(R.drawable.ic_music_mid)
                        .resize(120, 120)
                        .into(headerCover);
            } else if (songItem.isHaveCover()) {
                //没有下载的封面,则使用读取到的封面文件和混合颜色
                Picasso
                        .with(AppConfigs.ApplicationContext)
                        .load(CoverImage2File.getInstance().getCacheFile(songItem.getPath()))
                        .config(Bitmap.Config.RGB_565)
                        .error(R.drawable.ic_music_mid)
                        .resize(120, 120)
                        .into(headerCover);
            } else {
                headerCover.setImageResource(R.drawable.ic_music_mid);
            }
        } else {
            nowPlayingTV.setText(R.string.header_noMusic);
        }
    }

    /**
     * 显示歌曲排序方式对话框
     * 这里不能使用弱引用来做缓存处理，因为每次都需要重新读取当前选择的项来确定当前选择的位置
     */
    private void showMusicSortList() {

        //从SP中读取当前的排序类型
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext);
        final int position = Integer.valueOf(preferences.getString("scanner_sort_type", "0"));
        final AlertDialog.Builder builder = new AlertDialog.Builder(SelectMusicActivity.this, R.style.Setting_AlertTheme);

        builder.setSingleChoiceItems(R.array.sort_types_name, position, new DialogInterface.OnClickListener() {
            @Override
            @SuppressLint("CommitPrefEdits")
            public void onClick(DialogInterface dialog, int which) {
                //当用户作出了选择之后

                //更新SP文件数据
                final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext).edit();
                editor.putString("scanner_sort_type", String.valueOf(which));
                editor.commit();

                //重新读取所有项目数据
                AppConfigs.reInitOptionValues();

                //隐藏对话框
                dialog.dismiss();

                //更新列表，注意数据异常！
                final AllMusicFragment fragment = (AllMusicFragment) viewPagerAdapter.getItem(0);
                if (fragment != null) {
                    fragment.refreshListData();
                }

            }
        });

        builder.show();
    }

    /**
     * 主界面上方播放数据更新
     */
    private class UpdateHeaderPlayingText extends BroadcastReceiver {

        private IntentFilter intentFilter;

        public UpdateHeaderPlayingText() {
            this.intentFilter = new IntentFilter();
            this.intentFilter.addAction(AudioService.AUDIO_PLAY);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case AudioService.AUDIO_PLAY:
                    updateNowPlaying();
                    break;
            }
        }

    }

}
