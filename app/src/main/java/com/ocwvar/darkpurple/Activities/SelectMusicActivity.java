package com.ocwvar.darkpurple.Activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.transition.Slide;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.ocwvar.darkpurple.Adapters.MainViewPagerAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.FragmentPages.AllMusicFragment;
import com.ocwvar.darkpurple.FragmentPages.PlaylistPageFragment;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.BaseActivity;

public class SelectMusicActivity extends BaseActivity {

    ViewPager viewPager;
    MainViewPagerAdapter viewPagerAdapter;
    ServiceConnection serviceConnection;

    AllMusicFragment allMusicFragment;
    PlaylistPageFragment playlistPageFragment;

    @Override
    protected boolean onPreSetup() {
        getWindow().setBackgroundDrawable(new ColorDrawable(AppConfigs.Color.WindowBackground_Color));
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

        if (isToolBarLoaded()){
            //getToolBar().setLogo(R.drawable.ic_logo_title);
            Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/cuyabra.ttf");
            ((TextView)findViewById(R.id.logo_title)).setTypeface(typeface);
            setTitle("");
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.Main_TabLayout);
        viewPager = (ViewPager) findViewById(R.id.Main_ViewPager);
        viewPagerAdapter = new MainViewPagerAdapter(getSupportFragmentManager(),
                new String[]{
                        getText(R.string.ViewPage_Tab_AllMusic).toString(),
                        getText(R.string.ViewPage_Tab_Playlist).toString(),
                        getText(R.string.ViewPage_Tab_RecentPlayed).toString()
                });
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        allMusicFragment = new AllMusicFragment();
        playlistPageFragment = new PlaylistPageFragment();
        viewPagerAdapter.addFragmentPageToEnd(allMusicFragment);
        viewPagerAdapter.addFragmentPageToEnd(playlistPageFragment);

        tabLayout.setBackgroundColor(AppConfigs.Color.TabLayout_color);
        tabLayout.setTabTextColors(AppConfigs.Color.TabLayout_title_color,AppConfigs.Color.TabLayout_title_color_selected);
        tabLayout.setSelectedTabIndicatorColor(AppConfigs.Color.TabLayout_Indicator_color);

        onSetupService();

    }

    private void onSetupService() {
        if (ServiceHolder.getInstance().getService() == null) {
            //如果当前没有获取到服务对象 , 则创建一个保存
            serviceConnection = new ServiceConnection() {
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

    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_base, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_base_setting:
                startActivityForResult(new Intent(SelectMusicActivity.this, SettingsActivity.class), 1);
                break;
            case R.id.menu_base_playing:
                startActivity(new Intent(SelectMusicActivity.this, PlayingActivity.class));
                break;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                AppConfigs.reInitOptionValues();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (viewPager.getCurrentItem()) {
            case 0:
                if (allMusicFragment == null) {
                    return super.onKeyDown(keyCode, event);
                } else {
                    return !allMusicFragment.onActivityKeyDown(keyCode, event) && super.onKeyDown(keyCode, event);
                }
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
}
