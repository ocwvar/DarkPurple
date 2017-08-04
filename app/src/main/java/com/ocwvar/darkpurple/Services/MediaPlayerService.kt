package com.ocwvar.darkpurple.Services

import android.annotation.SuppressLint
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.KeyEvent
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Services.AudioCore.ICore
import com.ocwvar.darkpurple.Units.ActivityManager
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午11:07
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   播放器媒体服务
 */
class MediaPlayerService : MediaBrowserServiceCompat() {

    private val ROOT_ID_OK: String = "_1"
    private val ROOT_ID_DENIED: String = "no"

    //媒体播放调配控制器
    private val iController: IController = PlayerController()
    //Notification 生成器
    private val notificationHelper: MediaNotification = MediaNotification()
    //音频焦点变化回调
    private val audioFocusCallback: AudioFocusCallback = AudioFocusCallback()
    //媒体播放设备 断开连接 广播接收器
    private val deviceDisconnectReceiver: MediaDeviceDisconnectedReceiver = MediaDeviceDisconnectedReceiver()
    //媒体播放设备 连接 广播接收器
    private val deviceConnectReceiver: MediaDeviceConnectedReceiver = MediaDeviceConnectedReceiver()
    //播放核心状态广播监听器
    private val coreStateBroadcastReceiver: CoreStateBroadcastReceiver = CoreStateBroadcastReceiver()
    //Notification按钮广播监听器
    private val notificationBroadcastReceiver: NotificationBroadcastReceiver = NotificationBroadcastReceiver()

    //MediaSession状态回调处理器
    private val mediaSessionCallback: MediaSessionCallback = MediaSessionCallback()
    //MediaSession对象
    private lateinit var mediaSession: MediaSessionCompat
    //系统音频服务
    private lateinit var audioManager: AudioManager
    //音频焦点请求任务
    private lateinit var audioFocusRequest: AudioFocusRequest

    //服务是否正在运行标记
    private var isServiceStarted: Boolean = false
    //当前是否有媒体设备连接
    private var isDeviceConnected: Boolean = false
    /**
     * 耳机状态记录标识。 当耳机处于插入状态的时候，如果先后发生了振铃和耳机拔出
     * 会导致无法监听到耳机拔出的广播 ACTION_AUDIO_BECOMING_NOISY
     * 所以需要先了解焦点变化前耳机是否处于插入状态，如果在焦点重新获取到后耳机处于已经拔出
     * 则不重新开始播放。
     */
    private var isDeviceConnectedBeforeFocusLoss: Boolean = false


    object COMMAND {

        /**
         * 播放一个媒体库
         */
        val COMMAND_PLAY_LIBRARY: String = "c1"

        /**
         * 更新 Audio Session ID
         */
        val COMMAND_UPDATE_AUDIO_SESSION_ID: String = "c2"

    }

    object COMMAND_EXTRA {

        /**
         * String类型
         *
         * 播放的媒体库名称。主媒体库 = MAIN，播放列表 = 播放列表名称
         */
        val ARG_STRING_LIBRARY_NAME: String = "a1"

        /**
         * Integer类型
         *
         * 播放媒体在媒体库中的位置，默认=0
         */
        val ARG_INT_LIBRARY_INDEX: String = "a2"

        /**
         * Boolean类型
         *
         * 是否在媒体资源缓存完成后进行播放，默认=True
         */
        val ARG_BOOLEAN_PLAY_WHEN_READY: String = "a3"

    }


    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            //Android O 使用的音频焦点请求
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(audioFocusCallback).build()
        }

        //获取音频服务
        this.audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager


        //创建MediaSession对象，以及它自身的属性
        this.mediaSession = MediaSessionCompat(this@MediaPlayerService, this::class.java.simpleName).let {
            it.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS and MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            this@MediaPlayerService.sessionToken = it.sessionToken
            it.setCallback(this.mediaSessionCallback)
            it
        }

        //设置核心广播监听器
        if (!this.coreStateBroadcastReceiver.registered) {
            this.coreStateBroadcastReceiver.registered = true
            registerReceiver(this.coreStateBroadcastReceiver, this.coreStateBroadcastReceiver.intentFilter)
        }

        //设置Notification按钮广播监听器
        if (!this.notificationBroadcastReceiver.registered) {
            this.notificationBroadcastReceiver.registered = true
            registerReceiver(this.notificationBroadcastReceiver, this.notificationBroadcastReceiver.intentFilter)
        }

        //设置媒体播放设备 连接 广播接收器
        if (!this.deviceConnectReceiver.registered) {
            this.deviceConnectReceiver.registered = true
            registerReceiver(this.deviceConnectReceiver, this.deviceConnectReceiver.intentFilter)
        }

        //设置媒体播放设备 断开连接 广播接收器
        if (!this.deviceDisconnectReceiver.registered) {
            this.deviceDisconnectReceiver.registered = true
            registerReceiver(this.deviceDisconnectReceiver, this.deviceDisconnectReceiver.intentFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //注销核心广播监听器
        if (this.coreStateBroadcastReceiver.registered) {
            this.coreStateBroadcastReceiver.registered = false
            unregisterReceiver(this.coreStateBroadcastReceiver)
        }

        //注销Notification按钮广播监听器
        if (this.notificationBroadcastReceiver.registered) {
            this.notificationBroadcastReceiver.registered = false
            unregisterReceiver(this.notificationBroadcastReceiver)
        }

        //注销媒体播放设备 连接 广播接收器
        if (this.deviceConnectReceiver.registered) {
            this.deviceConnectReceiver.registered = false
            unregisterReceiver(this.deviceConnectReceiver)
        }

        //注销媒体播放设备 断开连接 广播接收器
        if (this.deviceDisconnectReceiver.registered) {
            this.deviceDisconnectReceiver.registered = false
            unregisterReceiver(this.deviceDisconnectReceiver)
        }

        //销毁所有媒体活动
        this.iController.release()
        this.mediaSession.isActive = false
        this.mediaSession.release()
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (parentId == ROOT_ID_OK) {
            //这里发送使用的媒体数据。但是我们并不使用这个数据，而是直接使用MediaLibrary中储存的数据
            val usingMedias: ArrayList<MediaBrowserCompat.MediaItem> = ArrayList()

            iController.usingLibrary().forEach {
                //组装数据
                usingMedias.add(MediaBrowserCompat.MediaItem(it.mediaMetadata.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            }

            //发送结果
            result.sendResult(usingMedias)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if (clientPackageName == this.packageName) {
            return BrowserRoot(this.ROOT_ID_OK, null)
        } else {
            return BrowserRoot(this.ROOT_ID_DENIED, null)
        }
    }

    /**
     * 请求获取音频焦点
     *
     * @return  请求结果
     */
    private fun requireAudioFocus(): Boolean {
        if (this.audioFocusCallback.currentAudioFocusState != AudioManager.AUDIOFOCUS_GAIN) {
            val result: Int
            if (Build.VERSION.SDK_INT >= 26) {
                result = this.audioManager.requestAudioFocus(audioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                result = this.audioManager.requestAudioFocus(audioFocusCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        return true
    }

    /**
     * 放弃音频焦点
     */
    private fun giveAwayAudioFocus() {
        if (this.audioFocusCallback.currentAudioFocusState == AudioManager.AUDIOFOCUS_LOSS) {
            return
        }

        if (Build.VERSION.SDK_INT >= 26) {
            this.audioManager.abandonAudioFocusRequest(this.audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            this.audioManager.abandonAudioFocus(this.audioFocusCallback)
        }
    }

    /**
     * 从 IController 中更新当前的媒体数据以及对应的状态
     */
    private fun updateMediaMetadata() {
        //设置当前MediaSession使用中的媒体数据
        this.mediaSession.setMetadata(this.iController.usingLibrary()[this.iController.currentIndex()].mediaMetadata)

        //更新封面效果
        CoverProcesser.handleThis(this.iController.usingLibrary()[this.iController.currentIndex()].coverID)

        //根据当前状态设置MediaSession是否已激活
        val state: Int = this.iController.currentState()
        this.mediaSession.isActive = state != PlaybackStateCompat.STATE_NONE

        //创建状态构造器
        val playbackStateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()

        //创建当前Session可用的Action
        val playbackActions = PlaybackStateCompat.ACTION_PLAY and PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID and
                if (this.iController.currentState() == PlaybackStateCompat.STATE_PLAYING) {
                    PlaybackStateCompat.ACTION_PAUSE and PlaybackStateCompat.ACTION_STOP
                } else {
                    0
                }

        //设置当前的状态和播放位置
        playbackStateBuilder.setState(this.iController.currentState(), this.iController.currentPosition(), 1.0f, SystemClock.elapsedRealtime())

        //设置当前的可用Action
        playbackStateBuilder.setActions(playbackActions)

        //设置MediaSession的状态
        this.mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    /**
     * 更新当前媒体状态并显示或更新 Notification
     *
     * 进入保持前台模式
     * @see updateMediaMetadata
     */
    private fun updateNotification() {
        updateMediaMetadata()
        val notification: Notification? = this.notificationHelper.createNotification(this.mediaSession, this@MediaPlayerService)
        notification?.let {
            if (!this.isServiceStarted) {
                startService(Intent(this@MediaPlayerService, MediaPlayerService::class.java))
                this.isServiceStarted = true
            }
            startForeground(this.notificationHelper.NOTIFICATION_ID, it)
        }
    }

    /**
     * 取消显示当前的 Notification
     *
     * 退出保持前台模式
     * @param   removeNotification  是否移除正在显示的 Notification。同时，在APP返回桌面时，服务将会中断
     */
    private fun dismissNotification(removeNotification: Boolean = false) {
        stopForeground(removeNotification)

        if (removeNotification && this.isServiceStarted) {
            this.isServiceStarted = false
            stopSelf()
        }
    }


    /**
     * 音频焦点变化回调控制器
     */
    private inner class AudioFocusCallback : AudioManager.OnAudioFocusChangeListener {

        //当前是否有音频的焦点
        var currentAudioFocusState: Int = AudioManager.AUDIOFOCUS_LOSS

        override fun onAudioFocusChange(focusChange: Int) {

            when (focusChange) {

            //成功获取到音频焦点
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (this.currentAudioFocusState == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && !AppConfigs.autoAdjustVolumeWhenTemperatelyLoss) {
                        //如果用户不允许 低音量播放，则需要进行恢复播放处理
                        this.currentAudioFocusState = AudioManager.AUDIOFOCUS_GAIN
                        mediaSessionCallback.onPlay()
                    } else if (this.currentAudioFocusState == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT && isDeviceConnectedBeforeFocusLoss && !isDeviceConnected) {
                        //此状态，查看 isDeviceConnectedBeforeFocusLoss 的变量说明
                        mediaSessionCallback.onPause()
                    }

                    this.currentAudioFocusState = AudioManager.AUDIOFOCUS_GAIN
                    iController.setVolume(1.0f)
                }

            //暂时丢失音频焦点，但是可以以低音量播放（短信提示音、Notification 提示音）
            //如果用户不允许 低音量播放，则需要进行恢复播放处理
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    this.currentAudioFocusState = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK

                    //允许暂时丢失焦点时将音频降低音量
                    if (AppConfigs.autoAdjustVolumeWhenTemperatelyLoss) {
                        iController.setVolume(0.2f)
                    } else {
                        //不允许则暂停播放
                        mediaSessionCallback.onPause()
                    }
                }

            //暂时丢失音频焦点，不可以进行播放打扰（拨打电话、QQ语音录制播放 等）
            //需要进行恢复播放处理
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    this.currentAudioFocusState = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT

                    //这里需要记录下是否连接着耳机
                    isDeviceConnectedBeforeFocusLoss = isDeviceConnected

                    mediaSessionCallback.onPause()
                }

            //音频焦点永久丢失（其他播放器开始播放）
            //不做恢复播放处理
                AudioManager.AUDIOFOCUS_LOSS -> {
                    this.currentAudioFocusState = AudioManager.AUDIOFOCUS_LOSS
                    //丢失时记录是否连接着媒体
                    isDeviceConnectedBeforeFocusLoss = isDeviceConnected

                    mediaSessionCallback.onStop()
                }
            }

        }

    }

    /**
     * MediaSession 状态回调控制器
     */
    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        //////////////////播放控制////////////////////

        override fun onPlay() {
            if (requireAudioFocus()) {
                //只有请求音频焦点成功才执行操作
                iController.resume()
            }
        }

        override fun onPause() {
            iController.pause()
        }

        override fun onStop() {
            iController.stop()
        }

        //////////////////顺序控制////////////////////

        override fun onSkipToPrevious() {
            if (requireAudioFocus()) {
                //只有请求音频焦点成功才执行操作
                iController.previous()
            }
        }

        override fun onSkipToNext() {
            if (requireAudioFocus()) {
                //只有请求音频焦点成功才执行操作
                iController.next()
            }
        }

        //////////////////数据控制////////////////////

        override fun onSeekTo(pos: Long) {
            iController.seek2(pos)
            updateMediaMetadata()
        }

        //////////////////其他指令////////////////////

        //多媒体按键事件接收
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            mediaButtonEvent ?: return false

            val action: String = mediaButtonEvent.action
            val keyEvent: KeyEvent? = mediaButtonEvent.extras.getParcelable("EXTRA_KEY_EVENT")
            when (action) {

                Intent.ACTION_MEDIA_BUTTON -> {

                    keyEvent ?: return false
                    when (keyEvent.action) {

                    //播放暂停二合一按钮
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {

                        }

                    //多媒体播放按钮
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            this.onPlay()
                        }

                    //多媒体暂停按钮
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            this.onPause()
                        }

                    }

                }

            }

            return true
        }

        //自定义指令接收
        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            command ?: return

            when (command) {

            //播放媒体库指令
                COMMAND.COMMAND_PLAY_LIBRARY -> {
                    extras ?: return

                    val isPlayWhenReady: Boolean = extras.getBoolean(COMMAND_EXTRA.ARG_BOOLEAN_PLAY_WHEN_READY, true)
                    val libraryTAG: String = extras.getString(COMMAND_EXTRA.ARG_STRING_LIBRARY_NAME, "")
                    val playIndex: Int = extras.getInt(COMMAND_EXTRA.ARG_INT_LIBRARY_INDEX, 0)

                    if (!TextUtils.isEmpty(libraryTAG)) {
                        //如果媒体库TAG不为空，则进行媒体库切换
                        if (iController.changeLibrary(libraryTAG)) {
                            //切换媒体库成功，进行播放
                            if (requireAudioFocus()) {
                                //只有请求音频焦点成功才执行操作
                                iController.play(playIndex, isPlayWhenReady)
                            }

                        }
                    }
                }

            //更新AudioSession ID
                COMMAND.COMMAND_UPDATE_AUDIO_SESSION_ID -> {
                    iController.updateAudioSessionID()
                }

            }

        }

    }

    /**
     * 播放核心状态变更广播监听器
     */
    private inner class CoreStateBroadcastReceiver : BroadcastReceiver() {

        var registered: Boolean = false

        val intentFilter: IntentFilter = IntentFilter().let {
            it.addAction(ICore.ACTIONS.CORE_ACTION_COMPLETED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_STOPPED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_PLAYING)
            it.addAction(ICore.ACTIONS.CORE_ACTION_PAUSED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_READY)
            it.addAction(IController.ACTIONS.ACTION_QUEUE_FINISH)
            it
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {

            //播放序列完成
                IController.ACTIONS.ACTION_QUEUE_FINISH -> {
                    updateMediaMetadata()
                    dismissNotification(true)
                    giveAwayAudioFocus()
                    iController.release()
                }

            //播放完成
                ICore.ACTIONS.CORE_ACTION_COMPLETED -> {
                    updateMediaMetadata()
                    mediaSessionCallback.onSkipToNext()
                }

            //媒体资源被 停止
                ICore.ACTIONS.CORE_ACTION_STOPPED -> {
                    updateNotification()
                    dismissNotification(true)
                    giveAwayAudioFocus()
                }

            //媒体资源被 播放
                ICore.ACTIONS.CORE_ACTION_PLAYING -> {
                    updateNotification()
                }

            //媒体资源被 暂停
                ICore.ACTIONS.CORE_ACTION_PAUSED -> {
                    updateNotification()
                    dismissNotification(false)
                    giveAwayAudioFocus()
                }

            //媒体资源 缓冲完成
                ICore.ACTIONS.CORE_ACTION_READY -> {
                    updateMediaMetadata()
                }

            }

        }

    }

    /**
     * Notification按钮广播监听器
     */
    private inner class NotificationBroadcastReceiver : BroadcastReceiver() {

        var registered: Boolean = false

        val intentFilter: IntentFilter = IntentFilter().let {
            it.addAction(MediaNotification.ACTIONS.NOTIFICATION_ACTION_PREVIOUS)
            it.addAction(MediaNotification.ACTIONS.NOTIFICATION_ACTION_PAUSE)
            it.addAction(MediaNotification.ACTIONS.NOTIFICATION_ACTION_PLAY)
            it.addAction(MediaNotification.ACTIONS.NOTIFICATION_ACTION_NEXT)
            it.addAction(MediaNotification.ACTIONS.NOTIFICATION_ACTION_CLOSE)
            it
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {

            //上一个媒体资源
                MediaNotification.ACTIONS.NOTIFICATION_ACTION_PREVIOUS -> {
                    mediaSessionCallback.onSkipToPrevious()
                }

            //暂停媒体资源
                MediaNotification.ACTIONS.NOTIFICATION_ACTION_PAUSE -> {
                    mediaSessionCallback.onPause()
                }

            //播放媒体资源
                MediaNotification.ACTIONS.NOTIFICATION_ACTION_PLAY -> {
                    mediaSessionCallback.onPlay()
                }

            //下一个媒体资源
                MediaNotification.ACTIONS.NOTIFICATION_ACTION_NEXT -> {
                    mediaSessionCallback.onSkipToNext()
                }

            //终止服务
                MediaNotification.ACTIONS.NOTIFICATION_ACTION_CLOSE -> {
                    iController.stop()
                    dismissNotification(true)
                    ActivityManager.getInstance().release()
                }

            }

        }

    }

    /**
     * 媒体播放设备 断开连接 广播接收器
     */
    private inner class MediaDeviceDisconnectedReceiver : BroadcastReceiver() {

        var registered: Boolean = false

        val intentFilter: IntentFilter = IntentFilter().let {
            it.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            it
        }

        override fun onReceive(p0: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {

            //播放设备断开连接广播通知
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    isDeviceConnected = false
                    mediaSessionCallback.onPause()
                }

            }
        }

    }

    /**
     * 媒体播放设备 连接 广播接收器
     */
    private inner class MediaDeviceConnectedReceiver : BroadcastReceiver() {

        var registered: Boolean = false

        @SuppressLint("InlinedApi")
        val intentFilter: IntentFilter = IntentFilter().let {
            if (AppConfigs.OS_5_UP) {
                it.addAction(AudioManager.ACTION_HEADSET_PLUG)
            } else {
                it.addAction(Intent.ACTION_HEADSET_PLUG)
            }
            it
        }

        override fun onReceive(p0: Context?, intent: Intent?) {
            intent ?: return
            intent.extras ?: return

            if (intent.action == Intent.ACTION_HEADSET_PLUG || intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                when (intent.extras.getInt("state", -1)) {
                //断开连接
                    0 -> {
                        //这里不做处理，由优先度更高的 AudioManager.ACTION_AUDIO_BECOMING_NOISY 来处理
                        isDeviceConnected = false
                    }

                //连接
                    1 -> {
                        isDeviceConnected = true
                        if (AppConfigs.isResumeAudioWhenPlugin) {
                            mediaSessionCallback.onPlay()
                        }
                    }
                }
            }

        }

    }

}