package com.ocwvar.darkpurple.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.ocwvar.darkpurple.Activities.SelectMusicActivity;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.ActivityManager;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.Units.Logger;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Services
 * Data: 2016/7/12 15:54
 * Project: DarkPurple
 * 音频播放服务
 */
public class AudioService extends Service {

    //音频处理类
    private  AudioCore core;
    //状态栏控制回调
    private NotificationControl notificationControl;
    //耳机断开监听回调
    private HeadsetReceiver headsetReceiver;
    //Notification  的管理器 , 用于更新界面数据
    private NotificationManager nm;
    //播放的状态提示
    private Notification notification;
    //Notification  拓展布局
    private RemoteViews remoteView;
    //Notification  普通布局
    private RemoteViews smallRemoteView;
    //音频服务
    private AudioManager audioManager;
    //连接接收器
    private ComponentName componentName;
    //耳机按钮次数延时器
    private MediaButtonPressCountingThread countingThread;
    //耳机插入广播接收器
    private HeadsetPlugInReceiver headsetPlugInReceiver;

    //操作的全局变量
    public static int mediaButtonPressCount = 0;
    private boolean isMediaButtonHandling = false;
    private boolean isRunningFreground = false;

    //参数变量
    private final int notificationID = 888;
    private final String TAG = "音频服务";

    //全局参数变量  -- 状态栏按钮广播Action
    //点击主按钮
    public static final String NOTIFICATION_MAIN = "ac1";
    //点击上一首按钮
    public static final String NOTIFICATION_BACK = "ac2";
    //点击下一首按钮
    public static final String NOTIFICATION_NEXT = "ac3";
    //点击封面
    public static final String NOTIFICATION_COVER = "ac4";
    //手动更新状态
    public static final String NOTIFICATION_REFRESH = "ac5";
    //点击关闭I按钮
    public static final String NOTIFICATION_CLOSE = "ac6";

    //全局参数变量  -- 音频变化广播Action
    //有音频被播放
    public static final String AUDIO_PLAY = "ac7";
    //有音频被暂停
    public static final String AUDIO_PAUSED = "ac8";
    //有音频从暂停中恢复
    public static final String AUDIO_RESUMED = "ac9";
    //当音频进行切换
    public static final String AUDIO_SWITCH = "ac10";

    /**
     * 当音频服务创建的时候
     *
     * 创建音频引擎对象 和 状态栏广播接收器对象 注册耳机拔出监听
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.warnning(TAG,"onCreate");
        core = new AudioCore(getApplicationContext());
        notificationControl = new NotificationControl();
        headsetReceiver = new HeadsetReceiver();
        headsetPlugInReceiver = new HeadsetPlugInReceiver();

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        componentName = new ComponentName(getPackageName(),HeadsetButtonReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(componentName);
        registerReceiver(headsetPlugInReceiver,headsetPlugInReceiver.filter);
        registerReceiver(headsetReceiver,headsetReceiver.filter);
        headsetPlugInReceiver.registered = true;
        headsetReceiver.registered = true;
    }

    /**
     * 当服务被绑定的时候
     *
     * 便开始后台常驻运行 , 同时显示状态栏布局
     * 返回音频服务的对象
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.warnning(TAG,"onBind");
        return new ServiceObject();
    }

    /**
     * 当服务面临销毁的时候
     *
     * 如果当前音频引擎内不为空 , 则释放引擎内的数据
     * 结束服务常驻后台
     * 反注册状态栏操作监听器
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.warnning(TAG,"onDestroy");
        if (core.getCurrectStatus() != AudioCore.AudioStatus.Empty){
            core.releaseAudio();
        }
        hideNotification();
        if (notificationControl != null && notificationControl.registered){
            notificationControl.registered = false;
            unregisterReceiver(notificationControl);
        }
        if (headsetReceiver != null && headsetReceiver.registered){
            headsetReceiver.registered = false;
            unregisterReceiver(headsetReceiver);
            audioManager.unregisterMediaButtonEventReceiver(componentName);
        }
        if (headsetPlugInReceiver != null && headsetPlugInReceiver.registered){
            headsetPlugInReceiver.registered = false;
            unregisterReceiver(headsetPlugInReceiver);
        }
    }

    /**
     * 服务对象传递类
     */
    public class ServiceObject extends Binder {

        public AudioService getService(){
            return AudioService.this;
        }

    }

    /**
     * Notification操作的广播接收器 , 在这里处理     Notification    的操作
     */
    private class NotificationControl extends BroadcastReceiver{

        final IntentFilter filter;

        public boolean registered = false;

        public NotificationControl() {
            filter = new IntentFilter();
            filter.addAction(NOTIFICATION_BACK);
            filter.addAction(NOTIFICATION_COVER);
            filter.addAction(NOTIFICATION_MAIN);
            filter.addAction(NOTIFICATION_NEXT);
            filter.addAction(NOTIFICATION_REFRESH);
            filter.addAction(NOTIFICATION_CLOSE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            boolean result = false;

            switch (action){
                case NOTIFICATION_BACK:
                    result = core.playPrevious();
                    break;
                case NOTIFICATION_COVER:
                    break;
                case NOTIFICATION_NEXT:
                    result = core.playNext();
                    break;
                case NOTIFICATION_MAIN:
                    if (core.getCurrectStatus() == AudioCore.AudioStatus.Playing){
                        //如果当前是播放状态 , 则执行暂停操作 , 图片变为播放
                        result = core.pauseAudio();
                    }else if (core.getCurrectStatus() == AudioCore.AudioStatus.Paused){
                        //如果当前是暂停状态 , 则执行播放操作 , 图片变为暂停
                        result = core.resumeAudio();
                    }
                    break;
                case NOTIFICATION_REFRESH:
                    updateNotification();
                    result = false; //不重复更新状态栏布局
                    break;
                case NOTIFICATION_CLOSE:
                     closeApp();
                    break;
            }

            if (result){
                updateNotification();
            }

        }

    }

    /**
     * 耳机拔出广播接收器
     */
    private class HeadsetReceiver extends BroadcastReceiver{

        final IntentFilter filter;
        final BluetoothAdapter bluetoothAdapter;

        boolean registered = false;

        public HeadsetReceiver() {
            filter = new IntentFilter();
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY); //有线耳机拔出变化
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); //蓝牙耳机连接变化

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
           if (isRunningFreground){
               //当前是正在运行的时候才能通过媒体按键来操作音频
               switch (intent.getAction()){
                   case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                       if (bluetoothAdapter != null && BluetoothProfile.STATE_DISCONNECTED == bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) && core.getCurrectStatus() == AudioCore.AudioStatus.Playing){
                           //蓝牙耳机断开连接 同时当前音乐正在播放
                           core.pauseAudio();
                       }
                       break;
                   case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                       if (core.getCurrectStatus() == AudioCore.AudioStatus.Playing){
                           //有线耳机断开连接 同时当前音乐正在播放
                           core.pauseAudio();
                       }
                       break;
               }
           }
        }

    }

    /**
     * 耳机插入广播接收器
     */
    public class HeadsetPlugInReceiver extends BroadcastReceiver {

        final IntentFilter filter;
        boolean registered = false;

        public HeadsetPlugInReceiver() {
            filter = new IntentFilter();
           if (Build.VERSION.SDK_INT >= 21){
                filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
            }else {
                filter.addAction(Intent.ACTION_HEADSET_PLUG);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("state")){
                final boolean isPlugIn = intent.getExtras().getInt("state") == 1;
                if (isPlugIn){

                    if (getAudioStatus() == AudioCore.AudioStatus.Paused){
                        //如果插入耳机的时候 , 当前有暂停的音频 , 则会继续播放 , 同时显示状态栏数据
                        showNotification();
                        resume();
                    }

                }
            }
        }

    }

    /**
     * 耳机媒体按钮次数统计线程
     */
    private class MediaButtonPressCountingThread extends Thread{

        private final String TAG = "按键计数延时器";

        @Override
        public void run() {
            super.run();
            isMediaButtonHandling = false;
            try {
                //线程等待 800 毫秒 , 用户在此期间触发媒体按键的次数会被记录下
                //当超过时间之后 在执行完对应次数的事件期间 , 用户的按键将不会被响应
                Thread.sleep(800);
            } catch (InterruptedException ignore) {}

            isMediaButtonHandling = true;

            Logger.warnning( TAG , "此次按键次数为: "+mediaButtonPressCount );

            switch (mediaButtonPressCount){
                case 1:
                    //点击一次暂停播放
                    sendBroadcast(new Intent(NOTIFICATION_MAIN));
                    break;
                case 2:
                    //点击两次播放下一首
                    sendBroadcast(new Intent(NOTIFICATION_NEXT));
                    break;
                case 3:
                    //点击三次播放上一首
                    sendBroadcast(new Intent(NOTIFICATION_BACK));
                    break;
                default:
                    break;
            }

            //响应变量复位
            try {
                //允许下一次多媒体响应时间前间隔 1 秒
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            mediaButtonPressCount = 0;
            isMediaButtonHandling = false;

        }

    }

    /**
     * 当媒体按键触发的时候调用此方法
     * @param keyIntent 广播得到的 Intent
     */
    public synchronized void onMediaButtonPress(Intent keyIntent){
        if ( isRunningFreground && ! isMediaButtonHandling ){
            //如果当前没有正在处理上一次的媒体按钮事件 , 即可开始响应接下来的事件
            switch (keyIntent.getAction()){
                case Intent.ACTION_MEDIA_BUTTON:
                    if (keyIntent.getExtras() != null && keyIntent.hasExtra(Intent.EXTRA_KEY_EVENT)){
                        KeyEvent event = keyIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK && event.getAction() == KeyEvent.ACTION_DOWN){
                            //当用户按下耳机按钮的时候
                            Logger.warnning(TAG,"耳机按钮事件");
                            //增加按键次数计数
                            mediaButtonPressCount += 1;
                            if (countingThread != null && countingThread.getState() != Thread.State.TERMINATED){
                                //如果当前存在延时器线程 , 则不做处理
                                break;
                            }else {
                                //如果延时器线程不存在 , 则开始执行线程
                                countingThread = null;
                                countingThread = new MediaButtonPressCountingThread();
                                countingThread.start();
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 显示状态栏的   Notification       同时进入后台模式
     */
    public void showNotification(){
        if (nm == null || notification == null){
            //如果Notification 为空 或 NotificationManager为空 , 则重新创建对象
            initNotification();
        }
        if (!isRunningFreground){
            startForeground(notificationID,notification);
            isRunningFreground = true;
        }
        Logger.warnning(TAG,"音频服务正在后台运行");
    }

    /**
     * 更新当前播放的音频信息数据到   Notification
     */
    public void updateNotification(){
        SongItem songItem = core.getPlayingSong();
        System.out.println("updateNotification");
        if (isRunningFreground && smallRemoteView != null && remoteView != null && songItem != null){
            Logger.warnning(TAG,"已更新状态栏布局. 歌曲:"+songItem.getTitle());
            //如果有歌曲信息
            //更新标题
            remoteView.setTextViewText(R.id.notification_title,songItem.getTitle());
            smallRemoteView.setTextViewText(R.id.notification_title,songItem.getTitle());
            //更新作者
            remoteView.setTextViewText(R.id.notification_artist,songItem.getArtist());
            smallRemoteView.setTextViewText(R.id.notification_artist,songItem.getArtist());
            //更新专辑
            remoteView.setTextViewText(R.id.notification_album,songItem.getAlbum());
            //更新封面
            if (songItem.getAlbumCoverUri() != null){
                //如果有封面图像Uri路径则设置图像
                Picasso.with(AppConfigs.ApplicationContext)
                        .load(songItem.getAlbumCoverUri())
                        .into(remoteView , R.id.notification_cover , notificationID ,notification);
            }else if (songItem.getPath() != null){
                //如果有歌曲路径 , 则尝试获取预先存好的缓存 , 如果成功则设置图像 , 否则设置默认图像
                if (CoverImage2File.getInstance().isAlreadyCached(songItem.getPath())){
                    Picasso.with(AppConfigs.ApplicationContext)
                            .load(CoverImage2File.getInstance().getCacheFile(songItem.getPath()))
                            .into(remoteView , R.id.notification_cover , notificationID ,notification);
                }else {
                    remoteView.setImageViewResource(R.id.notification_cover,R.drawable.ic_cd);
                }
            }else {
                //设置默认图像
                remoteView.setImageViewResource(R.id.notification_cover,R.drawable.ic_cd);
            }
            //更新主按钮状态
            switch (getAudioStatus()){
                case Playing:
                    remoteView.setImageViewResource(R.id.notification_mainButton,R.drawable.ic_action_pause);
                    smallRemoteView.setImageViewResource(R.id.notification_mainButton,R.drawable.ic_action_pause);
                    break;
                case Stopped:
                case Paused:
                    remoteView.setImageViewResource(R.id.notification_mainButton,R.drawable.ic_action_play);
                    smallRemoteView.setImageViewResource(R.id.notification_mainButton,R.drawable.ic_action_play);
                    break;

            }
        }else if (isRunningFreground && smallRemoteView != null && remoteView != null){
            Logger.warnning(TAG,"更新状态栏失败 , 使用默认文字资源. 原因:当前没有歌曲加载");
            //更新标题
            remoteView.setTextViewText(R.id.notification_title,getResources().getText(R.string.notification_string_empty));
            smallRemoteView.setTextViewText(R.id.notification_title,getResources().getText(R.string.notification_string_empty));
            //更新作者
            remoteView.setTextViewText(R.id.notification_artist,getResources().getText(R.string.notification_string_empty));
            smallRemoteView.setTextViewText(R.id.notification_artist,getResources().getText(R.string.notification_string_empty));
            //更新专辑
            remoteView.setTextViewText(R.id.notification_album,getResources().getText(R.string.notification_string_empty));
            //更新封面
            remoteView.setImageViewResource(R.id.notification_cover,R.drawable.ic_cd);
        }else {
            Logger.error(TAG,"更新状态栏失败. 原因:没有创建状态栏布局对象");
            return;
        }

        //更新状态
        nm.notify(notificationID,notification);
    }

    /**
     * 隐藏状态栏的   Notification       同时退出后台模式
     */
    private void hideNotification(){
        stopForeground(true);
        isRunningFreground = false;
        Logger.warnning(TAG,"音频服务已停止后台运行");
    }

    /**
     * 开始创建 Notification
     */
    private void initNotification(){
        //获取提示管理器
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建远程View
        remoteView = new RemoteViews(getPackageName(),R.layout.notification_layout);
        smallRemoteView = new RemoteViews(getPackageName(),R.layout.notification_small_layout);
        //创建打开主界面的Intent , 使得点击提示空白部分能直接返回app主界面
        Intent intent = new Intent(AudioService.this, SelectMusicActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        if (initNotificationClickCallback( remoteView , smallRemoteView , pendingIntent)){
            //如果创建回调成功 , 则开始创建Notification对象
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setTicker(getResources().getText(R.string.notification_ticker));
            if (Build.VERSION.SDK_INT  >= 23){
                builder.setColor(getColor(R.color.backgroundColor_Dark));
            }else {
                builder.setColor(getResources().getColor(R.color.backgroundColor_Dark));
            }
            builder.setSmallIcon(R.drawable.ic_action_small_icon);
            builder.setOngoing(true);
            builder.setCustomBigContentView(remoteView);
            builder.setCustomContentView(smallRemoteView);
            builder.setContentIntent(pendingIntent);
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notification = builder.build();
            notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
            Logger.warnning(TAG,"创建状态栏布局对象完成");
        }else {
            //否则使用默认的样式 , 但不具有控制性
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setTicker(getResources().getText(R.string.notification_ticker));
            builder.setSmallIcon(R.drawable.ic_action_small_icon);
            if (Build.VERSION.SDK_INT  >= 23){
                builder.setColor(getColor(R.color.backgroundColor_Dark));
            }else {
                builder.setColor(getResources().getColor(R.color.backgroundColor_Dark));
            }
            builder.setOngoing(true);
            builder.setContentText("点击返回主界面");
            builder.setContentIntent(pendingIntent);
            notification = builder.build();
            notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
            Logger.error(TAG,"无法创建状态栏回调. 使用默认样式");
        }

    }

    /**
     * 创建   Notification    的按钮回调
     * @param remoteView    拓展的远程View对象
     * @param smallRemoteView    普通的远程View对象
     * @param pendingIntent    用于调回主界面的PendingIntent
     * @return 执行结果
     */
    private boolean initNotificationClickCallback(RemoteViews remoteView , RemoteViews smallRemoteView , PendingIntent pendingIntent){
        if (remoteView != null){
            //主按钮的点击广播
            remoteView.setOnClickPendingIntent(R.id.notification_mainButton,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_MAIN),0));
            smallRemoteView.setOnClickPendingIntent(R.id.notification_mainButton,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_MAIN),0));
            //下一首按钮的点击广播
            remoteView.setOnClickPendingIntent(R.id.notification_next,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_NEXT),0));
            smallRemoteView.setOnClickPendingIntent(R.id.notification_next,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_NEXT),0));
            //上一首按钮的点击广播
            remoteView.setOnClickPendingIntent(R.id.notification_back,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_BACK),0));
            smallRemoteView.setOnClickPendingIntent(R.id.notification_back,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_BACK),0));
            //封面点击广播
            remoteView.setOnClickPendingIntent(R.id.notification_cover,pendingIntent);
            //关闭按钮点击广播
            remoteView.setOnClickPendingIntent(R.id.notification_close,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_CLOSE),0));
            smallRemoteView.setOnClickPendingIntent(R.id.notification_close,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_CLOSE),0));
            //注册监听的广播接收器
            registerReceiver(notificationControl,notificationControl.filter);
            notificationControl.registered = true;
            return true;
        }else {
            return false;
        }
    }

    /**
     * 关闭App的所有页面 , 但不退出服务
     */
    private void closeApp(){
        //结束掉所有Activity页面
        ActivityManager.getInstance().release();
        //不显示当前的状态栏
        hideNotification();
        //暂停当前的播放
        pause();
    }

    /**
     * 播放音频
     * @param songList  要播放的音频列表
     * @param playIndex  要播放的音频位置
     * @return  执行结果
     */
    public boolean play(ArrayList<SongItem> songList , int playIndex){
        if(!isRunningFreground){
            showNotification();
        }
        boolean result = core.play(songList, playIndex);
        if (remoteView != null){
            updateNotification();
        }
        return result;
    }

    /**
     * 预加载音频
     * @param songList  要播放的音频列表
     * @param playIndex  要播放的音频位置
     * @return  执行结果
     */
    public boolean initAudio(ArrayList<SongItem> songList , int playIndex){
        return core.onlyInitAudio(songList, playIndex);
    }

    /**
     * 继续播放音频 , 如果音频是被暂停则继续播放  如果音频是被停止则从头播放
     * @return  执行结果
     */
    public boolean resume(){
        return core.resumeAudio();
    }

    /**
     * 暂停播放音频
     * @return  执行结果
     */
    public boolean pause(){
        return core.pauseAudio();
    }

    /**
     * 停止播放音频
     * @return  执行结果
     */
    public boolean stop(){
        return core.stopAudio();
    }

    /**
     * 释放歌曲占用的资源
     * @return  执行结果
     */
    public boolean release(){
        return core.releaseAudio();
    }

    /**
     * @return  当前播放的音频列表
     */
    public ArrayList<SongItem> getPlayingList(){
        return core.getPlayingList();
    }

    /**
     * @return  获取当前播放的位置 , 无的时候为 -1
     */
    public int getPlayingIndex(){
        return core.getPlayingIndex();
    }

    /**
     * 得到当前已激活的歌曲
     * @return  有则返回歌曲集合 , 没有则返回 NULL
     */
    public SongItem getPlayingSong(){
        return core.getPlayingSong();
    }

    /**
     * @return  当前音频播放状态
     */
    public AudioCore.AudioStatus getAudioStatus(){
        return core.getCurrectStatus();
    }

    /**
     * 获取当前播放的位置
     * @return  当前音频播放位置
     */
    public double getPlayingPosition(){
        return core.getPlayingPosition();
    }

    /**
     * 获取当前播放的音频长度
     * @return  音频长度
     */
    public double getAudioLength(){
        return core.getAudioLength();
    }

    /**
     * 歌曲播放位置设置
     * @param position  位置长度
     * @return  执行结果
     */
    public boolean seek2Position(double position){
        return core.seek2Position(position);
    }

    /**
     * 播放上一首音频
     * @return  执行结果
     */
    public boolean playPrevious(){
        return core.playPrevious();
    }

    /**
     * 播放下一首音频
     * @return  执行结果
     */
    public boolean playNext(){
        return core.playNext();
    }

}
