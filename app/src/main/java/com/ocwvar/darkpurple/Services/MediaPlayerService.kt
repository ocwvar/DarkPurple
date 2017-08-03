package com.ocwvar.darkpurple.Services

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.KeyEvent
import com.ocwvar.darkpurple.Services.AudioCore.ICore
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

    //服务是否正在运行标记
    private var isServiceStarted: Boolean = false
    //媒体播放调配控制器
    private val iController: IController = PlayerController()
    //Notification 生成器
    private val notificationHelper: MediaNotification = MediaNotification()
    //播放核心状态广播监听器
    private val coreStateBroadcastReceiver: CoreStateBroadcastReceiver = CoreStateBroadcastReceiver()
    //Notification按钮广播监听器
    private val notificationBroadcastReceiver: NotificationBroadcastReceiver = NotificationBroadcastReceiver()

    //MediaSession状态回调处理器
    private val mediaSessionCallback: MediaSessionCallback = MediaSessionCallback()
    //MediaSession对象
    private lateinit var mediaSession: MediaSessionCompat

    object COMMAND {

        /**
         * 播放一个媒体库
         */
        val COMMAND_PLAY_LIBRARY: String = "c1"

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
     * MediaSession 状态回调控制器
     */
    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        //////////////////播放控制////////////////////

        override fun onPlay() {
            iController.resume()
        }

        override fun onPause() {
            iController.pause()
        }

        override fun onStop() {
            iController.stop()
        }

        //////////////////顺序控制////////////////////

        override fun onSkipToPrevious() {
            iController.previous()
        }

        override fun onSkipToNext() {
            iController.next()
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
                            iController.resume()
                        }

                    //多媒体暂停按钮
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            iController.pause()
                        }

                    }

                }

            }

            return true
        }

        //自定义指令接收
        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            command ?: return
            extras ?: return

            when (command) {

            //播放媒体库指令
                COMMAND.COMMAND_PLAY_LIBRARY -> {
                    val isPlayWhenReady: Boolean = extras.getBoolean(COMMAND_EXTRA.ARG_BOOLEAN_PLAY_WHEN_READY, true)
                    val libraryTAG: String = extras.getString(COMMAND_EXTRA.ARG_STRING_LIBRARY_NAME, "")
                    val playIndex: Int = extras.getInt(COMMAND_EXTRA.ARG_INT_LIBRARY_INDEX, 0)

                    if (!TextUtils.isEmpty(libraryTAG)) {
                        //如果媒体库TAG不为空，则进行媒体库切换
                        if (iController.changeLibrary(libraryTAG)) {
                            //切换媒体库成功，进行播放
                            iController.play(playIndex, isPlayWhenReady)

                        }
                    }
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
            it
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {

            //播放完成
                ICore.ACTIONS.CORE_ACTION_COMPLETED -> {
                    updateMediaMetadata()
                    mediaSessionCallback.onSkipToNext()
                }

            //媒体资源被 停止
                ICore.ACTIONS.CORE_ACTION_STOPPED -> {
                    updateNotification()
                    dismissNotification(true)
                }

            //媒体资源被 播放
                ICore.ACTIONS.CORE_ACTION_PLAYING -> {
                    updateNotification()
                }

            //媒体资源被 暂停
                ICore.ACTIONS.CORE_ACTION_PAUSED -> {
                    updateNotification()
                    dismissNotification(false)
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
                }

            }

        }

    }

}