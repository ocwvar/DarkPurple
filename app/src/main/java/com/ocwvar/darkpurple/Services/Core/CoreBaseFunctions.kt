package com.ocwvar.darkpurple.Services.Core

import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Services.AudioStatus

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/12 12:49 PM
 * File Location com.ocwvar.darkpurple.Services.Core
 * This file use to :   音频Core基础功能需求接口
 */
interface CoreBaseFunctions {

    /**
     * 播放音频
     * @param songItem 音频信息对象
     * @param onlyInit 是否仅加载音频数据，而不进行播放
     * @return  执行结果
     */
    fun play(songItem: SongItem, onlyInit: Boolean): Boolean

    /**
     * 续播音频
     * @return 执行结果
     */
    fun resume(): Boolean

    /**
     * 暂停音频
     * @return 执行结果
     */
    fun pause(): Boolean

    /**
     * 释放音频资源
     * @return 执行结果
     */
    fun release(): Boolean

    /**
     * 获取音频当前播放的位置，即已播放的长度
     * @return 当前位置，异常返回 0
     */
    fun playingPosition(): Double

    /**
     * 跳转至指定音频长度位置
     * @return 执行结果
     */
    fun seekPosition(position: Double): Boolean

    /**
     * 获取音频长度
     * @return 音频长度，异常返回 0
     */
    fun getAudioLength(): Double

    /**
     * 获取当前音乐播放状态
     * @return  当前状态
     */
    fun getAudioStatus(): AudioStatus
}