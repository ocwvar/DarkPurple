package com.ocwvar.darkpurple.Services

import com.ocwvar.darkpurple.Bean.SongItem

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午7:05
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   媒体库控制器接口
 */
interface IController {

    /**
     * 播放参数
     */
    object OPTIONS {

        /**
         * 循环播放媒体库
         */
        val LOOP_LIBRARY: Boolean = false

        /**
         * 随机播放媒体库
         */
        val RANDOM_LIBRARY: Boolean = false
    }

    /**
     * 广播 Action
     */
    object ACTIONS {

        /**
         * 播放序列完成
         */
        val ACTION_QUEUE_FINISH: String = "aqf"

    }

    /**
     * 切换播放的媒体库
     *
     * 此操作将会终止当前正在播放的音频
     *
     * @param   libraryTAG  媒体库TAG
     * @return  执行结果
     */
    fun changeLibrary(libraryTAG: String): Boolean

    /**
     * @return  当前使用的媒体库
     */
    fun usingLibrary(): ArrayList<SongItem>

    /**
     * @return  当前的媒体位置
     */
    fun currentIndex(): Int

    /**
     * 播放指定媒体数据
     *
     * @param   source  媒体数据
     * @param   isPlayWhenReady 是否缓冲好数据好就进行播放，默认=True
     */
    fun play(source: SongItem?, isPlayWhenReady: Boolean = true)

    /**
     * 播放媒体库指定的位置
     *
     * @param   index   要播放的位置，位置无效无法播放
     * @param   isPlayWhenReady 是否缓冲好数据好就进行播放，默认=True
     */
    fun play(index: Int, isPlayWhenReady: Boolean = true)

    /**
     * 1.播放已缓冲好的媒体数据
     * 2.恢复播放已暂停或停止的媒体数据
     */
    fun resume()

    /**
     * 播放下一个媒体资源
     */
    fun next()

    /**
     * 播放上一个媒体资源
     */
    fun previous()

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
     * @param   position    要跳转的位置
     */
    fun seek2(position: Long)

    /**
     * 通知核心更新 AudioSession ID
     */
    fun updateAudioSessionID()

    /**
     * @return  当前媒体的长度
     */
    fun mediaDuration(): Long

    /**
     * @return  当前播放的位置
     */
    fun currentPosition(): Long

    /**
     * @return  当前播放核心状态
     */
    fun currentState(): Int

}