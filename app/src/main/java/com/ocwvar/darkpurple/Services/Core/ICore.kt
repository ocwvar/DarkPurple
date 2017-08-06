package com.ocwvar.darkpurple.Services.Core

import com.ocwvar.darkpurple.Bean.SongItem

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午7:16
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   播放核心接口
 */
interface ICore {

    /**
     * 播放核心的广播 action 定义
     */
    object ACTIONS {

        /**
         *  切换到：暂停
         */
        val CORE_ACTION_PAUSED: String = "ca_1"

        /**
         *  切换到：停止
         */
        val CORE_ACTION_STOPPED: String = "ca_2"

        /**
         *  切换到：播放
         */
        val CORE_ACTION_PLAYING: String = "ca_3"

        /**
         *  媒体播放完成
         */
        val CORE_ACTION_COMPLETED: String = "ca_4"

        /**
         *  新媒体缓冲完成
         */
        val CORE_ACTION_READY: String = "ca_5"

        /**
         *  释放数据
         */
        val CORE_ACTION_RELEASE: String = "ca_6"

    }

    /**
     * 播放指定媒体数据
     *
     * @param   source  媒体数据
     * @param   isPlayWhenReady 是否缓冲好数据好就进行播放
     */
    fun play(source: SongItem?, isPlayWhenReady: Boolean = true)

    /**
     * 1.播放已缓冲好的媒体数据
     * 2.恢复播放已暂停或停止的媒体数据
     */
    fun resume()

    /**
     * 暂停媒体
     */
    fun pause()

    /**
     * 停止媒体
     */
    fun stop()

    /**
     * 释放媒体资源
     */
    fun release()

    /**
     * 跳转位置
     *
     * @param   position    要跳转的位置，位置无效则不跳转
     */
    fun seek2(position: Long)

    /**
     * @param   volume 音频音量 0.0 ~ 1.0
     */
    fun setVolume(volume: Float)

    /**
     * 更新 AudioSession ID
     */
    fun updateAudioSessionID()

    /**
     * @return  当前媒体的长度，无效长度：-1
     */
    fun mediaDuration(): Long

    /**
     * @return  当前播放的位置，无效位置：-1
     */
    fun currentPosition(): Long

    /**
     * @return  当前播放核心状态
     */
    fun currentState(): Int

}