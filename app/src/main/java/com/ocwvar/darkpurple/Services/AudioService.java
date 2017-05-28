package com.ocwvar.darkpurple.Services;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Units.ActivityManager;
import com.ocwvar.darkpurple.Units.Logger;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Services
 * Data: 2016/7/12 15:54
 * Project: DarkPurple
 * 音频播放服务
 */
public class AudioService extends Service implements AudioManager.OnAudioFocusChangeListener {

    //===========Notification 按钮点击广播Action===========
    //点击播放
    public static final String NOTIFICATION_PLAY = "NOTIFICATION_PLAY";
    //点击暂停
    public static final String NOTIFICATION_PAUSE = "NOTIFICATION_PAUSE";
    //点击上一首
    public static final String NOTIFICATION_PREV = "NOTIFICATION_PREV";
    //点击下一首
    public static final String NOTIFICATION_NEXT = "NOTIFICATION_NEXT";
    //点击关闭
    public static final String NOTIFICATION_CLOSE = "NOTIFICATION_CLOSE";
    //更新状态
    public static final String NOTIFICATION_UPDATE = "NOTIFICATION_UPDATE";
    //===========音频变化广播Action===========
    //有音频被播放
    public static final String AUDIO_PLAY = "AUDIO_PLAY";
    //有音频被暂停
    public static final String AUDIO_PAUSED = "AUDIO_PAUSED";
    //有音频从暂停中恢复
    public static final String AUDIO_RESUMED = "AUDIO_RESUMED";
    //当音频进行切换
    public static final String AUDIO_SWITCH = "AUDIO_SWITCH";
    private final String TAG = "音频服务";
    //===========变量===========
    //多媒体案件（耳机按钮）点击次数统计变量
    public int mediaButtonPressCount = 0;
    //===========使用到的对象===========
    //音频处理核心
    private AudioNextCore core;
    //音频服务
    private AudioManager audioManager;
    //Notification按钮点击广播监听器
    private NotificationControl notificationControl;
    /**
     * 系统自带的MediaStyle Notification：
     * private MediaNotification mediaNotification;
     * <p>
     * 自定义Notification：
     * private MediaNotificationCompact mediaNotification;
     */
    private MediaNotificationCompact mediaNotification;
    //耳机拔出监听回调
    private HeadsetDisconnectedReceiver headsetDisconnectedReceiver;
    //耳机插入广播接收器
    private HeadsetPlugInReceiver headsetPlugInReceiver;
    //耳机按钮次数延时统计线程
    private MediaButtonPressCountingThread countingThread;
    //===========标记变量===========
    //是否正在处理上一个耳机按钮事件
    private boolean isMediaButtonHandling = false;
    //是否正在后台运行
    private boolean isRunningForeground = false;
    //是否需要主动改变音频状态，当该标记为False的时候，则忽略焦点变化
    private boolean needAutoChanged = true;
    //当前是否已经在监听音频焦点
    private boolean listeningAudioFocus = false;
    /**
     * 耳机状态记录标识。 当耳机处于插入状态的时候，如果先后发生了振铃和耳机拔出
     * 会导致无法监听到耳机拔出的广播 ACTION_AUDIO_BECOMING_NOISY
     * 所以需要先了解焦点变化前耳机是否处于插入状态，如果在焦点重新获取到后耳机处于已经拔出
     * 则不重新开始播放。
     */
    private boolean isPluginBeforeFocusChanged = false;

    /**
     * 当音频服务创建的时候
     * <p>
     * 创建音频引擎对象 和 状态栏广播接收器对象 注册耳机拔出监听
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.warnning(TAG, "onCreate");

        //创建对象
        core = new AudioNextCore(getApplicationContext(), AudioNextCore.CoreType.EXO2);
        notificationControl = new NotificationControl();
        headsetDisconnectedReceiver = new HeadsetDisconnectedReceiver();
        headsetPlugInReceiver = new HeadsetPlugInReceiver();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaNotification = new MediaNotificationCompact(getApplicationContext());

        //注册广播监听器
        registerReceiver(notificationControl, notificationControl.filter);
        registerReceiver(headsetPlugInReceiver, headsetPlugInReceiver.filter);
        registerReceiver(headsetDisconnectedReceiver, headsetDisconnectedReceiver.filter);

        headsetPlugInReceiver.registered = true;
        headsetDisconnectedReceiver.registered = true;
        notificationControl.registered = true;
    }

    /**
     * 当服务被绑定的时候
     * <p>
     * 便开始后台常驻运行 , 同时显示状态栏布局
     * 返回音频服务的对象
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.warnning(TAG, "onBind");
        return new ServiceObject();
    }

    /**
     * 当服务面临销毁的时候
     * <p>
     * 如果当前音频引擎内不为空 , 则释放引擎内的数据
     * 结束服务常驻后台
     * 反注册监听器
     * 停止监听焦点变化
     * 清空服务储存的单例对象
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.warnning(TAG, "onDestroy");
        if (core.getCurrentStatus() != AudioStatus.Empty) {
            core.releaseAudio();
        }
        hideNotification();
        if (notificationControl != null && notificationControl.registered) {
            notificationControl.registered = false;
            unregisterReceiver(notificationControl);
        }
        if (headsetDisconnectedReceiver != null && headsetDisconnectedReceiver.registered) {
            headsetDisconnectedReceiver.registered = false;
            unregisterReceiver(headsetDisconnectedReceiver);
        }
        if (headsetPlugInReceiver != null && headsetPlugInReceiver.registered) {
            headsetPlugInReceiver.registered = false;
            unregisterReceiver(headsetPlugInReceiver);
        }
        if (mediaNotification != null) {
            mediaNotification.close();
        }
        disableAudioFocus();
        ServiceHolder.getInstance().setService(null);
    }

    /**
     * 当媒体按键触发的时候调用此方法
     *
     * @param keyIntent 广播得到的 Intent
     */
    public synchronized void onMediaButtonPress(Intent keyIntent) {
        if (isRunningForeground && !isMediaButtonHandling) {
            //如果当前没有正在处理上一次的媒体按钮事件 , 即可开始响应接下来的事件
            switch (keyIntent.getAction()) {
                case HeadsetButtonReceiver.fromMediaSession:
                case Intent.ACTION_MEDIA_BUTTON:
                    if (keyIntent.getExtras() != null && keyIntent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
                        KeyEvent event = keyIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK && event.getAction() == KeyEvent.ACTION_DOWN) {
                            //当用户按下耳机按钮的时候
                            Logger.warnning(TAG, "耳机按钮事件");
                            //增加按键次数计数
                            mediaButtonPressCount += 1;
                            if (countingThread != null && countingThread.getState() != Thread.State.TERMINATED) {
                                //如果当前存在延时器线程 , 则不做处理
                                break;
                            } else {
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
     * 更新当前播放的音频信息数据到   Notification
     */
    public void updateNotification() {

        final SongItem songItem = core.getPlayingSong();

        final Notification notification = mediaNotification.updateNotification(songItem, getAudioStatus());

        //更新状态
        isRunningForeground = true;
        startForeground(MediaNotification.notificationID, notification);
    }

    /**
     * 隐藏状态栏的   Notification       同时退出后台模式
     */
    private void hideNotification() {
        stopForeground(true);
        isRunningForeground = false;
        Logger.warnning(TAG, "音频服务已停止后台运行");
    }

    /**
     * 关闭App的所有页面 , 但不退出服务
     */
    private void closeApp() {
        //结束掉所有Activity页面
        ActivityManager.getInstance().release();

        //停止音频焦点监听
        disableAudioFocus();

        //暂停当前的播放
        if (core.currentCoreType() == AudioNextCore.CoreType.EXO2) {
            this.isRunningForeground = false;   //此标记是给EXO2內核使用的
            System.out.println();
        }
        core.pauseAudio();

        //不显示当前的状态栏
        hideNotification();

    }

    /**
     * 预加载音频
     *
     * @param songList  要播放的音频列表
     * @param playIndex 要播放的音频位置
     * @return 执行结果
     */
    public boolean initAudio(ArrayList<SongItem> songList, int playIndex) {
        return core.onlyInitAudio(songList, playIndex);
    }

    /**
     * 播放音频
     *
     * @param songList  要播放的音频列表
     * @param playIndex 要播放的音频位置
     * @return 执行结果
     */
    public boolean play(ArrayList<SongItem> songList, int playIndex) {

        //这个方法只会由用户调用，所以此方法被调用的时候，则代表需要恢复焦点的影响
        needAutoChanged = true;
        isPluginBeforeFocusChanged = false;

        //请求焦点监听（不会重复请求）
        requestAudioFocus();

        boolean result = core.play(songList, playIndex);
        if (result) {
            switch (core.currentCoreType()) {
                case EXO2:
                    /**
                     * 如果是使用 EXO2 播放方案的时候，由于在调用play()方法后內核仍处于加载状态，此时更新
                     * Notification会导致状态不准确，所以Notification的状态更新由內核通过发送 NOTIFICATION_UPDATE
                     * 广播来进行状态的更新，在这里就只需要把：isRunningForeground变量设为真，內核即可进行更新操作。
                     *
                     * 注意：如果变量  isRunningForeground  为假，则EXO音频內核不会因为音频状态发生改变而更新Notification
                     * （调用  initAudio()  方法除外）
                     */
                    this.isRunningForeground = true;
                    break;
                case BASS_Library:
                    updateNotification();
                    break;
                case COMPAT:
                    break;
            }
        }
        return result;
    }

    /**
     * @return 当前使用的播放核心是否支持高级功能
     */
    public boolean isCoreSupportedAdvFunction() {
        return core.isCoreSupportedAdvFunction();
    }

    /**
     * @return 当前服务是否处于后台运行状态
     */
    public boolean isRunningForeground() {
        return this.isRunningForeground;
    }

    /**
     * 继续播放音频 , 如果音频是被暂停则继续播放  如果音频是被停止则从头播放
     *
     * @return 执行结果
     */
    public boolean resume() {

        Logger.warnning(TAG, "恢复焦点变化影响");
        needAutoChanged = true;
        requestAudioFocus();

        final boolean result = core.resumeAudio();
        if (result) {
            switch (core.currentCoreType()) {
                case EXO2:
                    this.isRunningForeground = true;
                    break;
                case BASS_Library:
                    updateNotification();
                    break;
                case COMPAT:
                    break;
            }
        }
        return result;
    }

    /**
     * 暂停播放音频
     *
     * @return 执行结果
     */
    public boolean pause(boolean isPauseByUser) {

        if (isPauseByUser) {
            //如果是由用户主动暂停的音频，则之后忽略焦点变化
            needAutoChanged = false;
            Logger.warnning(TAG, "忽略焦点变化影响");
        }

        final boolean result = core.pauseAudio();
        if (result) {
            switch (core.currentCoreType()) {
                case EXO2:
                    this.isRunningForeground = true;
                    break;
                case BASS_Library:
                    updateNotification();
                    break;
                case COMPAT:
                    break;
            }
        }
        return result;
    }

    /**
     * 播放上一首音频
     *
     * @return 执行结果
     */
    public boolean playPrevious() {
        //此方法仅有用户才会调用，所以此方法被调用的时候恢复焦点影响
        needAutoChanged = true;
        requestAudioFocus();

        return core.playPrevious();
    }

    /**
     * 播放下一首音频
     *
     * @return 执行结果
     */
    public boolean playNext() {
        //此方法仅有用户才会调用，所以此方法被调用的时候恢复焦点影响
        needAutoChanged = true;
        requestAudioFocus();

        return core.playNext();
    }

    /**
     * 音频焦点变化监听
     *
     * @param focusChange 焦点变化
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                //音频获得焦点
                if (needAutoChanged) {
                    if (isPluginBeforeFocusChanged && !audioManager.isWiredHeadsetOn()) {
                        //如果耳机已经拔出，则不做重新播放操作。
                        //重置状态标识
                        needAutoChanged = false;
                        isPluginBeforeFocusChanged = false;
                        return;
                    }
                    resume();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //音频焦点暂时丢失
                if (needAutoChanged) {
                    //记录下当前耳机状态
                    isPluginBeforeFocusChanged = audioManager.isWiredHeadsetOn();
                    pause(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //音频焦点丢失
                pause(true);
                disableAudioFocus();
                break;
        }
    }

    /**
     * 获取音频焦点监听
     *
     * @return 获取状态
     */
    private boolean requestAudioFocus() {
        if (!listeningAudioFocus) {
            final int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            //音频焦点监听获取结果
            listeningAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        return listeningAudioFocus;
    }

    /**
     * 取消监听音频焦点变化
     */
    private void disableAudioFocus() {
        audioManager.abandonAudioFocus(this);
        listeningAudioFocus = false;
        isPluginBeforeFocusChanged = false;
    }

    /**
     * 获取频谱数据
     *
     * @return 当前声音的频谱数据
     */
    public float[] getSpectrum() {
        return core.getSpectrum();
    }

    /**
     * 获取均衡器频段设置
     *
     * @return 频段参数
     */
    public int[] getEqParameters() {
        return core.getEqParameters();
    }

    /**
     * 更改均衡器频段参数
     *
     * @param eqParameter 均衡器参数 -10 ~ 10
     * @param eqIndex     调节位置
     * @return 执行结果
     */
    public boolean updateEqParameter(int eqParameter, int eqIndex) {
        return core.updateEqParameter(eqParameter, eqIndex);
    }

    /**
     * 重置均衡器设置
     */
    public void resetEqualizer() {
        core.resetEqualizer();
    }

    /**
     * 释放歌曲占用的资源
     *
     * @return 执行结果
     */
    public boolean release() {
        return core.releaseAudio();
    }

    /**
     * @return 当前播放的音频列表
     */
    public ArrayList<SongItem> getPlayingList() {
        return core.getPlayingList();
    }

    /**
     * @return 获取当前播放的位置 , 无的时候为 -1
     */
    public int getPlayingIndex() {
        return core.getPlayingIndex();
    }

    /**
     * 得到当前已激活的歌曲
     *
     * @return 有则返回歌曲集合 , 没有则返回 NULL
     */
    public
    @Nullable
    SongItem getPlayingSong() {
        return core.getPlayingSong();
    }

    /**
     * @return 当前音频播放状态
     */
    public AudioStatus getAudioStatus() {
        return core.getCurrentStatus();
    }

    /**
     * 获取当前播放的位置
     *
     * @return 当前音频播放位置
     */
    public double getPlayingPosition() {
        return core.getPlayingPosition();
    }

    /**
     * 获取当前播放的音频长度
     *
     * @return 音频长度
     */
    public double getAudioLength() {
        return core.getAudioLength();
    }

    /**
     * 歌曲播放位置设置
     *
     * @param position 位置长度
     * @return 执行结果
     */
    public boolean seek2Position(double position) {
        return core.seek2Position(position);
    }

    /**
     * 服务对象传递类
     */
    public class ServiceObject extends Binder {

        public AudioService getService() {
            return AudioService.this;
        }

    }

    /**
     * Notification操作的广播接收器 , 在这里处理     Notification    的操作
     */
    private class NotificationControl extends BroadcastReceiver {

        final IntentFilter filter;

        public boolean registered = false;

        public NotificationControl() {
            filter = new IntentFilter();
            filter.addAction(NOTIFICATION_NEXT);
            filter.addAction(NOTIFICATION_CLOSE);
            filter.addAction(NOTIFICATION_PAUSE);
            filter.addAction(NOTIFICATION_PLAY);
            filter.addAction(NOTIFICATION_PREV);
            filter.addAction(NOTIFICATION_UPDATE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case NOTIFICATION_NEXT:
                    playNext();
                    break;
                case NOTIFICATION_PREV:
                    playPrevious();
                    break;
                case NOTIFICATION_PLAY:
                    resume();
                    break;
                case NOTIFICATION_PAUSE:
                    pause(true);
                    break;
                case NOTIFICATION_CLOSE:
                    closeApp();
                    break;
                case NOTIFICATION_UPDATE:
                    updateNotification();
                    break;
            }
        }

    }

    /**
     * 耳机拔出广播接收器
     */
    private class HeadsetDisconnectedReceiver extends BroadcastReceiver {

        final IntentFilter filter;
        final BluetoothAdapter bluetoothAdapter;

        boolean registered = false;

        public HeadsetDisconnectedReceiver() {
            filter = new IntentFilter();
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY); //有线耳机拔出变化
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); //蓝牙耳机连接变化

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isRunningForeground) {
                //当前是正在运行的时候才能通过媒体按键来操作音频
                switch (intent.getAction()) {
                    case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                        if (bluetoothAdapter != null && BluetoothProfile.STATE_DISCONNECTED == bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) && core.getCurrentStatus() == AudioStatus.Playing) {
                            //蓝牙耳机断开连接 同时当前音乐正在播放
                            pause(true);
                        }
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        if (core.getCurrentStatus() == AudioStatus.Playing) {
                            //有线耳机断开连接 同时当前音乐正在播放
                            pause(true);
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
            if (Build.VERSION.SDK_INT >= 21) {
                filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
            } else {
                filter.addAction(Intent.ACTION_HEADSET_PLUG);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("state") && AppConfigs.isResumeAudioWhenPlugin) {
                final boolean isPlugIn = intent.getExtras().getInt("state") == 1;
                if (isPlugIn) {

                    if (getAudioStatus() == AudioStatus.Paused) {
                        //如果插入耳机的时候 , 当前有暂停的音频 , 则会继续播放 , 同时显示状态栏数据
                        resume();
                        updateNotification();
                    }

                }
            }
        }

    }

    /**
     * 耳机媒体按钮次数统计线程
     */
    private class MediaButtonPressCountingThread extends Thread {

        private final String TAG = "按键计数延时器";

        @Override
        public void run() {
            super.run();
            isMediaButtonHandling = false;
            try {
                //线程等待 800 毫秒 , 用户在此期间触发媒体按键的次数会被记录下
                //当超过时间之后 在执行完对应次数的事件期间 , 用户的按键将不会被响应
                Thread.sleep(800);
            } catch (InterruptedException ignore) {
            }

            isMediaButtonHandling = true;

            Logger.warnning(TAG, "此次按键次数为: " + mediaButtonPressCount);

            switch (mediaButtonPressCount) {
                case 1:
                    //点击一次暂停播放
                    if (getAudioStatus() == AudioStatus.Paused) {
                        sendBroadcast(new Intent(NOTIFICATION_PLAY));
                    } else if (getAudioStatus() == AudioStatus.Playing) {
                        sendBroadcast(new Intent(NOTIFICATION_PAUSE));
                    }
                    break;
                case 2:
                    //点击两次播放下一首
                    sendBroadcast(new Intent(NOTIFICATION_NEXT));
                    break;
                case 3:
                    //点击三次播放上一首
                    sendBroadcast(new Intent(NOTIFICATION_PREV));
                    break;
                default:
                    break;
            }

            //响应变量复位
            try {
                //允许下一次多媒体响应时间前间隔 1 秒
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            mediaButtonPressCount = 0;
            isMediaButtonHandling = false;

        }

    }

}