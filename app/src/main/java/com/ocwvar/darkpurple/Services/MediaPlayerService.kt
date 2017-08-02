package com.ocwvar.darkpurple.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.view.KeyEvent
import com.ocwvar.darkpurple.Services.AudioCore.ICore

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
    //播放核心状态广播监听器
    private val coreStateBroadcastReceiver: CoreStateBroadcastReceiver = CoreStateBroadcastReceiver()

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
            registerReceiver(this.coreStateBroadcastReceiver, this.coreStateBroadcastReceiver.intentFilter)
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (parentId == ROOT_ID_OK) {
            //这里发送使用的媒体数据。但是我们并不使用这个数据，而是直接使用MediaLibrary中储存的数据
            val usingMedias: ArrayList<MediaBrowserCompat.MediaItem> = ArrayList()

            iController.usingLibrary().forEach {
                //组装数据
                usingMedias.add(MediaBrowserCompat.MediaItem(it.mediaMetadata.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            }

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

    }

    /**
     * 更新当前媒体状态并显示或更新 Notification
     *
     * 进入保持前台模式
     * @see updateMediaMetadata
     */
    private fun updateNotification() {

    }

    /**
     * 取消显示当前的 Notification
     *
     * 退出保持前台模式
     */
    private fun dismissNotification() {
        stopForeground(false)
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

                ICore.ACTIONS.CORE_ACTION_COMPLETED -> {
                    updateMediaMetadata()
                }

                ICore.ACTIONS.CORE_ACTION_STOPPED -> {
                    updateNotification()
                }

                ICore.ACTIONS.CORE_ACTION_PLAYING -> {
                    updateNotification()
                }

                ICore.ACTIONS.CORE_ACTION_PAUSED -> {
                    dismissNotification()
                }

                ICore.ACTIONS.CORE_ACTION_READY -> {
                    updateMediaMetadata()
                }

            }

        }

    }

}