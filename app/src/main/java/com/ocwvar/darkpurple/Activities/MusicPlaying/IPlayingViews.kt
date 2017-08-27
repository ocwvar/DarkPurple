package com.ocwvar.darkpurple.Activities.MusicPlaying

import android.app.Activity
import android.graphics.drawable.TransitionDrawable
import android.support.v7.widget.AppCompatImageView

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-26 下午5:31
 * File Location com.ocwvar.darkpurple.Activities.MusicPlaying
 * This file use to :   播放界面视图控制层
 */
interface IPlayingViews {

    /**
     * @return  实现了显示层的界面
     */
    fun getActivity(): Activity

    /**
     * @return  负责背景显示的控件
     */
    fun getBackgroundContainer(): AppCompatImageView

    /**
     * 显示音乐恢复暂停动画
     */
    fun onMusicPause()

    /**
     * 显示音乐恢复播放动画
     */
    fun onMusicResume()

    /**
     * 更新标题文字
     *
     * @param   title   文字
     */
    fun onUpdateTitle(title: String)

    /**
     * 更新歌曲名称显示
     *
     * @param   musicName   歌曲名称
     */
    fun onUpdateMusicName(musicName: String)

    /**
     * 更新歌曲专辑名称显示
     *
     * @param   musicAlbum   歌曲专辑
     */
    fun onUpdateMusicAlbum(musicAlbum: String)

    /**
     * 更新进度文字显示
     *
     * @param   timerText   进度文字
     */
    fun onUpdateTimer(timerText: String)

    /**
     * 更新进度条
     *
     * @param   progress    进度值
     * @param   max    最大值
     */
    fun onUpdateSeekBar(progress: Int, max: Int)

    /**
     * 更新当前封面轮播显示的位置
     *
     * @param   position 歌曲的位置
     */
    fun onUpdateShowingCover(position: Int)

    /**
     * 更新循环播放按钮状态
     *
     *  @param  enable  是否启用
     */
    fun onUpdateLoopState(enable: Boolean)

    /**
     * 显示频谱动画
     */
    fun onShowSpectrum()

    /**
     * 不显示频谱动画
     */
    fun onHideSpectrum()

    /**
     * 更新随机播放按钮状态
     *
     *  @param  enable  是否启用
     */
    fun onUpdateRandomState(enable: Boolean)

    /**
     * 更新背景图像
     *
     * @param   imageDrawable   背景图像
     */
    fun onUpdateBackground(imageDrawable: TransitionDrawable)

    /**
     * 发生异常
     */
    fun onError()

    /**
     * 服务连接失败、断开时 显示画面
     */
    fun onServiceDisconnect()

    /**
     * 服务连接成功时 显示画面
     */
    fun onServiceConnected()

    /**
     * 关闭页面
     */
    fun onRequireFinishActivity()

}