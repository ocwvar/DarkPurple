package com.ocwvar.darkpurple.Activities.MusicPlaying

import com.ocwvar.darkpurple.Services.MediaServiceConnector
import com.ocwvar.darkpurple.widgets.CircleSeekBar

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-26 下午5:24
 * File Location com.ocwvar.darkpurple.Activities.MusicPlaying
 * This file use to :   播放界面控制层
 */
interface IPlayingPresenter : MediaServiceConnector.Callbacks, CircleSeekBar.Callback {

    /**
     * Activity 恢复状态
     */
    fun onActivityResume()

    /**
     * Activity 暂停状态
     */
    fun onActivityPause()

    /**
     * Activity 销毁状态
     */
    fun onActivityDestroy()

    /**
     * 切换当前播放状态 播放↔暂停/停止
     */
    fun switchPlaybackState()

    /**
     * 切换循环播放开关
     *
     * @param   enable  开关
     */
    fun switchLoopState(enable: Boolean)

    /**
     * 切换随机播放开关
     *
     * @param   enable  开关
     */
    fun switchRandomState(enable: Boolean)

    /**
     * 切换频谱播放
     */
    fun switchSpectrum(enable: Boolean)

    /**
     * 请求连接媒体服务
     */
    fun connectService()

    /**
     * 请求断开媒体服务
     */
    fun disConnectService()

    /**
     * 请求播放指定位置
     *
     * @param   index   歌曲序列
     * @param   needPending 需要等待
     */
    fun playIndex(index: Int, needPending: Boolean)

    /**
     * 显示频谱动画
     */
    fun onEnableSpectrum()

    /**
     * 不显示频谱动画
     */
    fun onDisableSpectrum()

}