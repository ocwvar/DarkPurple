package com.ocwvar.darkpurple.Services

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-3 下午5:21
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   媒体服务连接器
 */
class MediaServiceConnector(val activity: Activity, val callback: Callbacks? = null) {

    //媒体服务连接器
    private val serviceConnection: ServiceConnection = ServiceConnection()

    //媒体服务订阅器，可以获取当前的媒体数据列表
    private val serviceSubscription: ServiceSubscription = ServiceSubscription()

    //媒体状态回调处理器
    private val mediaControllerCallback: MediaControllerCallback = MediaControllerCallback()

    //媒体服务对象
    private val mediaService: MediaBrowserCompat = MediaBrowserCompat(activity, ComponentName(activity, MediaPlayerService::class.java), this.serviceConnection, null)

    //媒体服务的ROOT ID
    private var ROOT_ID: String = ""

    interface Callbacks {

        /**
         *  媒体服务连接成功
         */
        fun onServiceConnected()

        /**
         *  媒体服务连接断开
         */
        fun onServiceDisconnected()

        /**
         *  无法连接媒体服务
         */
        fun onServiceConnectionError()

        /**
         *  媒体数据发生更改
         *  @param  metadata
         */
        fun onMediaChanged(metadata: MediaMetadataCompat)

        /**
         *  媒体播放状态发生改变
         *  @param  state
         */
        fun onMediaStateChanged(state: PlaybackStateCompat)

        /**
         * 媒体服务返回当前正在使用的媒体数据列表回调
         * @param   data    数据列表
         */
        fun onGotUsingLibraryData(data: MutableList<MediaBrowserCompat.MediaItem>)

        /**
         *  无法获取媒体服务返回的媒体数据
         */
        fun onGetUsingLibraryDataError()

    }

    /**
     * 连接媒体服务
     *
     * @return  是否在 500ms 内连接成功
     */
    fun connect(): Boolean {
        if (!this.mediaService.isConnected) {
            this.mediaService.connect()
            var timeoutCount: Int = 5
            while (!this.mediaService.isConnected && --timeoutCount > 0) {
                //等待服务连接结果
                Thread.sleep(100)
            }

            return mediaService.isConnected
        }
        return true
    }

    /**
     * 断开媒体服务连接
     */
    fun disConnect() {
        if (this.mediaService.isConnected) {
            this.mediaService.disconnect()
        }
    }

    /**
     * @return  当前媒体服务是否已经处于连接状态
     */
    fun isServiceConnected(): Boolean = this.mediaService.isConnected

    /**
     * @return 当前音频播放状态，无效为：PlaybackStateCompat.STATE_ERROR
     */
    fun currentState(): Int = MediaControllerCompat.getMediaController(activity)?.playbackState?.state ?: PlaybackStateCompat.STATE_ERROR

    /**
     * 发送指令到媒体服务
     * @param   commandAction   服务的Action
     * @param   extra   附带的数据
     */
    fun sendCommand(commandAction: String, extra: Bundle?) {
        MediaControllerCompat.getMediaController(this.activity)?.sendCommand(commandAction, extra, null)
    }

    /**
     * 媒体状态回调处理器
     */
    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            state?.let {
                callback?.onMediaStateChanged(it)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                callback?.onMediaChanged(it)
            }
        }

    }

    /**
     * 媒体服务订阅器，可以获取当前的媒体数据列表
     */
    private inner class ServiceSubscription : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>, options: Bundle) {
            super.onChildrenLoaded(parentId, children, options)
            callback?.onGotUsingLibraryData(children)
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            callback?.onGetUsingLibraryDataError()
        }

    }

    /**
     * 媒体服务连接器
     */
    private inner class ServiceConnection : MediaBrowserCompat.ConnectionCallback() {

        //当前服务连接成功
        override fun onConnected() {
            super.onConnected()
            if (TextUtils.isEmpty(ROOT_ID)) {
                ROOT_ID = mediaService.root
            }

            //设置当前 Activity 可用的 MediaControllerCompat
            MediaControllerCompat.setMediaController(activity, MediaControllerCompat(activity, mediaService.sessionToken).let {
                //注册媒体控制的回调接口
                it.registerCallback(mediaControllerCallback)
                it
            })

            //开始订阅服务
            mediaService.subscribe(ROOT_ID, serviceSubscription)

            callback?.let { Handler(Looper.getMainLooper()).postDelayed({ callback.onServiceConnected() }, 1000) }
        }

        //当服务断开连接
        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            //注销相关的回调接口
            MediaControllerCompat.getMediaController(activity)?.let {
                it.unregisterCallback(mediaControllerCallback)
                MediaControllerCompat.setMediaController(activity, null)
            }

            callback?.onServiceDisconnected()
        }

        //当服务连接失败
        override fun onConnectionFailed() {
            super.onConnectionFailed()
            callback?.onServiceConnectionError()
        }
    }

}