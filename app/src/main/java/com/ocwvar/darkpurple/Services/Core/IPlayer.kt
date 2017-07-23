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
interface IPlayer {

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
    fun seekPosition(position: Long): Boolean

    /**
     * 获取音频长度
     * @return 音频长度，异常返回 0
     */
    fun getAudioLength(): Long

    /**
     * 获取当前音乐播放状态
     * @return  当前状态
     */
    fun getAudioStatus(): AudioStatus

    /**
     * @see EXOCORE.VisualizerLoader.switchOn
     */
    fun switchOnVisualizer()

    /**
     * @see EXOCORE.VisualizerLoader.switchOff
     */
    fun switchOffVisualizer()

    /**
     * 获取均衡器各个频段参数
     * @return  均衡器参数
     */
    fun getEQParameters(): IntArray

    /**
     * 更改均衡器频段参数
     * @param eqParameter 均衡器参数 -10 ~ 10
     * @param eqIndex     调节位置
     * @return 执行结果
     */
    fun setEQParameters(eqParameter: Int, eqIndex: Int): Boolean

    /**
     * 重置均衡器
     */
    fun resetEQ()

    /**
     * 获取当前频谱数据
     * @return  频谱数据，异常返回 NULL
     */
    fun getSpectrum(): FloatArray?

}