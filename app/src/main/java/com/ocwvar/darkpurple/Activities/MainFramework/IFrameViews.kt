package com.ocwvar.darkpurple.Activities.MainFramework

import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.widget.FrameLayout

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-30 下午10:14
 * File Location com.ocwvar.darkpurple.Activities.MainFramework
 * This file use to :   主界面视图控制层
 */
interface IFrameViews {

    enum class PageName { 主界面, 播放列表, 设置 }

    /**
     * 更新标题文字
     *
     * @param text  标题，NULL = 留空
     */
    fun onUpdateTitle(text: String?)

    /**
     * 更新专辑文字
     *
     * @param text  标题，NULL = 留空
     */
    fun onUpdateAlbum(text: String?)

    /**
     * 更新作家文字
     *
     * @param text  标题，NULL = 留空
     */
    fun onUpdateArtist(text: String?)

    /**
     * 更新顶部背景图像
     *
     * @param drawable  图像Drawable
     * @param tag  图像TAG
     */
    fun onUpdateBackground(drawable: Drawable, tag: String)

    /**
     * 更新顶部封面
     *
     * @param drawable 封面Drawable，NULL = 不显示
     * @param tag  图像TAG
     */
    fun onUpdateCover(drawable: Drawable?, tag: String)

    /**
     * 更新底部按钮状态
     *
     * @param pageName  激活的按钮名称
     */
    fun onUpdateFragmentButtonState(pageName: PageName)

    /**
     * 缺失权限
     */
    fun onLeakPermission()

    /**
     * 切换是否显示正在播放按钮
     *
     * @param   enable  是否显示
     */
    fun onSwitchPlayingButton(enable: Boolean)

    /**
     * @return  负责显示的Activity
     */
    fun activity(): AppCompatActivity

    /**
     * @return  负责加载Fragment的FrameLayout容器
     */
    fun fragmentContainer(): FrameLayout

}