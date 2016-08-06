package com.ocwvar.darkpurple.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import com.ocwvar.darkpurple.Activities.SelectMusicActivity;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.ImageLoader.OCImageLoader;
import com.ocwvar.darkpurple.Units.Logger;

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
    private NotificationControl control;
    //Notification  的管理器 , 用于更新界面数据
    private NotificationManager nm;
    //播放的状态提示
    private Notification notification;
    //Notification  布局
    private RemoteViews remoteView;

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

    //全局参数变量  -- 音频变化广播Action
    //有音频被播放
    public static final String AUDIO_PLAY = "ac6";
    //有音频被暂停
    public static final String AUDIO_PAUSED = "ac7";
    //有音频从暂停中恢复
    public static final String AUDIO_RESUMED = "ac8";
    //当音频进行切换
    public static final String AUDIO_SWITCH = "ac9";

    /**
     * 当音频服务创建的时候
     *
     * 创建音频引擎对象 和 状态栏广播接收器对象
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.warnning(TAG,"onCreate");
        core = new AudioCore(getApplicationContext());
        control = new NotificationControl();
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
        showNotification();
        updateNotification();
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
        if (control.registed){
            control.registed = false;
            unregisterReceiver(control);
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

        public IntentFilter filter;
        public boolean registed = false;

        public NotificationControl() {
            filter = new IntentFilter();
            filter.addAction(NOTIFICATION_BACK);
            filter.addAction(NOTIFICATION_COVER);
            filter.addAction(NOTIFICATION_MAIN);
            filter.addAction(NOTIFICATION_NEXT);
            filter.addAction(NOTIFICATION_REFRESH);
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
                    break;
            }

            if (result){
                updateNotification();
            }

        }

    }

    /**
     * 显示状态栏的   Notification       同时进入后台模式
     */
    private void showNotification(){
        if (nm == null || notification == null){
            //如果Notification 为空 或 NotificationManager为空 , 则重新创建对象
            initNotification();
        }
        startForeground(notificationID,notification);
        Logger.warnning(TAG,"音频服务正在后台运行");
    }

    /**
     * 更新当前播放的音频信息数据到   Notification
     */
    private void updateNotification(){
        SongItem songItem = core.getPlayingSong();
        if (remoteView != null && songItem != null){
            Logger.warnning(TAG,"已更新状态栏布局. 歌曲:"+songItem.getTitle());
            //如果有歌曲信息
            //更新标题
            remoteView.setTextViewText(R.id.notification_title,songItem.getTitle());
            //更新作者
            remoteView.setTextViewText(R.id.notification_artist,songItem.getArtist());
            //更新专辑
            remoteView.setTextViewText(R.id.notification_album,songItem.getAlbum());
            //更新封面
            if (songItem.getAlbumCoverUri() != null){
                //如果有封面图像Uri路径则设置图像
                remoteView.setImageViewUri(R.id.notification_cover,songItem.getAlbumCoverUri());
            }else if (songItem.getPath() != null){
                //如果有歌曲路径 , 则尝试获取预先存好的缓存 , 如果成功则设置图像 , 否则设置默认图像
                Bitmap coverImage = OCImageLoader.loader().getCache(songItem.getPath());
                if (coverImage != null){
                    remoteView.setImageViewBitmap(R.id.notification_cover,coverImage);
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
                    break;
                case Stopped:
                case Paused:
                    remoteView.setImageViewResource(R.id.notification_mainButton,R.drawable.ic_action_play);
                    break;

            }
        }else if (remoteView != null){
            Logger.warnning(TAG,"更新状态栏失败 , 使用默认文字资源. 原因:当前没有歌曲加载");
            //更新标题
            remoteView.setTextViewText(R.id.notification_title,getResources().getText(R.string.notification_string_empty));
            //更新作者
            remoteView.setTextViewText(R.id.notification_artist,getResources().getText(R.string.notification_string_empty));
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
        //创建打开主界面的Intent , 使得点击提示空白部分能直接返回app主界面
        Intent intent = new Intent(AudioService.this, SelectMusicActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        if (initNotificationClickCallback(remoteView,pendingIntent)){
            //如果创建回调成功 , 则开始创建Notification对象
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setTicker(getResources().getText(R.string.notification_ticker));
            builder.setSmallIcon(R.drawable.ic_action_small_icon);
            builder.setOngoing(true);
            builder.setCustomBigContentView(remoteView);
            builder.setContentIntent(pendingIntent);
            notification = builder.build();
            notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
            Logger.warnning(TAG,"创建状态栏布局对象完成");
        }else {
            //否则使用默认的样式 , 但不具有控制性
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setTicker(getResources().getText(R.string.notification_ticker));
            builder.setSmallIcon(R.drawable.ic_action_small_icon);
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
     * @param remoteView    远程View对象
     * @param pendingIntent    用于调回主界面的PendingIntent
     * @return 执行结果
     */
    private boolean initNotificationClickCallback(RemoteViews remoteView , PendingIntent pendingIntent){
        if (remoteView != null){
            remoteView.setOnClickPendingIntent(R.id.notification_mainButton,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_MAIN),0));
            remoteView.setOnClickPendingIntent(R.id.notification_next,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_NEXT),0));
            remoteView.setOnClickPendingIntent(R.id.notification_back,PendingIntent.getBroadcast(getApplicationContext(),0,new Intent(NOTIFICATION_BACK),0));
            remoteView.setOnClickPendingIntent(R.id.notification_cover,pendingIntent);
            //注册监听的广播接收器
            control.registed = true;
            registerReceiver(control,control.filter);
            return true;
        }else {
            return false;
        }
    }

    /**
     * 播放音频
     * @param songList  要播放的音频列表
     * @param playIndex  要播放的音频位置
     * @return  执行结果
     */
    public boolean play(ArrayList<SongItem> songList , int playIndex){
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

}
