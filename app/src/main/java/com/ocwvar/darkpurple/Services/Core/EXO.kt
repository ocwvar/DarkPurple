package com.ocwvar.darkpurple.Services.Core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import java.io.IOException

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午7:22
 * File Location com.ocwvar.darkpurple.Services.Core
 * This file use to :   基于Google ExoPlayer2的播放核心
 */
class EXO(val appContext: Context = AppConfigs.ApplicationContext) : ICore {

    //播放器状态回调处理类
    private val exoCallbacks: ExoPlayerCallbacks = ExoPlayerCallbacks()

    //EXO实例
    private val exoPlayer: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(appContext, DefaultTrackSelector()).let {
        it.addListener(exoCallbacks)
        it
    }

    //当前播放器状态
    private var currentState: Int = PlaybackStateCompat.STATE_NONE

    //是否缓冲完成后播放媒体
    private var isPlayWhenReady: Boolean = false

    //当前是否已经缓冲完毕媒体资源
    private var isMediaReady: Boolean = false

    //当前媒体长度
    private var currentDuration: Long = -1


    /**
     * 播放指定媒体数据
     *
     * @param   source  媒体数据
     * @param   isPlayWhenReady 是否缓冲好数据好就进行播放，此状态标记仅对此次加载对象生效，默认=True
     */
    override fun play(source: SongItem?, isPlayWhenReady: Boolean) {
        source ?: return
        try {
            //释放当前正在使用的资源
            release()

            //获取媒体的URI地址
            val mediaURI: Uri = Uri.parse(source.mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))

            //生成播放对象
            val factory: DataSource.Factory = DefaultDataSourceFactory(this.appContext, Util.getUserAgent(this.appContext, this@EXO::class.java.simpleName))
            val mediaSource: MediaSource = ExtractorMediaSource(mediaURI, factory, DefaultExtractorsFactory(), null, exoCallbacks)

            //重置状态
            this.isPlayWhenReady = isPlayWhenReady

            //缓冲数据
            this.exoPlayer.prepare(mediaSource, true, true)
        } catch(e: Exception) {
            return
        }
    }

    /**
     * 1.播放已缓冲好的媒体数据
     * 2.恢复播放已暂停或停止的媒体数据
     */
    override fun resume() {
        when (this.currentState) {

            PlaybackStateCompat.STATE_PAUSED -> {
                this.exoPlayer.playWhenReady = true
                this.currentState = PlaybackStateCompat.STATE_PLAYING

                sendBroadcast(ICore.ACTIONS.CORE_ACTION_PLAYING)
            }

            PlaybackStateCompat.STATE_STOPPED -> {
                //停止状态恢复的时候需要重置播放位置
                seek2(0L)
                this.exoPlayer.playWhenReady = true
                this.currentState = PlaybackStateCompat.STATE_PLAYING

                sendBroadcast(ICore.ACTIONS.CORE_ACTION_PLAYING)
            }

        }
    }

    /**
     * 暂停媒体
     */
    override fun pause() {
        //暂停播放
        this.exoPlayer.playWhenReady = false
        //设置状态
        this.currentState = PlaybackStateCompat.STATE_PAUSED

        sendBroadcast(ICore.ACTIONS.CORE_ACTION_PAUSED)
    }

    /**
     * 停止媒体
     */
    override fun stop() {
        //暂停播放
        this.exoPlayer.playWhenReady = false
        //设置状态
        this.currentState = PlaybackStateCompat.STATE_STOPPED

        sendBroadcast(ICore.ACTIONS.CORE_ACTION_STOPPED)
    }

    /**
     * 释放媒体资源
     */
    override fun release() {
        this.exoPlayer.stop()
        this.isMediaReady = false
        this.currentDuration = -1L
        this.currentState = PlaybackStateCompat.STATE_NONE
    }

    /**
     * 跳转位置
     *
     * @param   position    要跳转的位置，位置无效则不跳转，单位ms
     */
    override fun seek2(position: Long) {
        if (this.currentState != PlaybackStateCompat.STATE_NONE) {
            this.exoPlayer.seekTo(position)
        }
    }

    /**
     * @param   volume 音频音量 0.0 ~ 1.0
     */
    override fun setVolume(volume: Float) {
        if (volume > 1.0f) {
            this.exoPlayer.volume = 1.0f
        } else if (volume < 0.0f) {
            this.exoPlayer.volume = 0.0f
        } else {
            this.exoPlayer.volume = volume
        }
    }

    /**
     * 更新 AudioSession ID
     */
    override fun updateAudioSessionID() {
        MediaLibrary.updateAudioSessionID(this.exoPlayer.audioSessionId)
    }

    /**
     * @return  当前媒体的长度，无效长度：-1，单位ms
     */
    override fun mediaDuration(): Long = this.currentDuration

    /**
     * @return  当前播放的位置，无效位置：-1，单位ms
     */
    override fun currentPosition(): Long {
        if (this.currentState == PlaybackStateCompat.STATE_NONE) {
            return -1L
        } else {
            return this.exoPlayer.currentPosition
        }
    }

    /**
     * @return  当前播放核心状态
     */
    override fun currentState(): Int = this.currentState

    /**
     * 发送广播Action
     *
     * @param   action  广播Action
     */
    private fun sendBroadcast(action: String?) {
        action ?: return
        this.appContext.sendBroadcast(Intent(action))
    }

    /**
     * 播放器状态回调处理类
     */
    private inner class ExoPlayerCallbacks : Player.EventListener, ExtractorMediaSource.EventListener {

        override fun onLoadError(error: IOException?) {
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (!isMediaReady && playbackState == Player.STATE_READY) {
                //媒体缓冲完成
                isMediaReady = true

                //更新媒体数据
                currentDuration = exoPlayer.duration

                sendBroadcast(ICore.ACTIONS.CORE_ACTION_READY)

                if (isPlayWhenReady) {
                    //需要在缓冲完成后进行播放
                    exoPlayer.playWhenReady = true
                    currentState = PlaybackStateCompat.STATE_PLAYING

                    sendBroadcast(ICore.ACTIONS.CORE_ACTION_PLAYING)
                } else {
                    //不需要马上播放的 置于暂停状态
                    exoPlayer.playWhenReady = false
                    currentState = PlaybackStateCompat.STATE_PAUSED
                }

            } else if (isMediaReady && ((playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE))) {
                //媒体播放完成

                currentState = PlaybackStateCompat.STATE_NONE
                MediaLibrary.updateAudioSessionID(0)
                sendBroadcast(ICore.ACTIONS.CORE_ACTION_COMPLETED)
            }
        }

        override fun onLoadingChanged(isLoading: Boolean) {
        }

        override fun onPositionDiscontinuity() {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
        }

    }

}