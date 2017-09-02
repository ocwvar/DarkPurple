package com.ocwvar.darkpurple.Activities.MainFramework

import com.ocwvar.darkpurple.Services.MediaServiceConnector

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-30 下午10:14
 * File Location com.ocwvar.darkpurple.Activities.MainFramework
 * This file use to : 主界面控制层
 */
interface IFramePresenter : MediaServiceConnector.Callbacks {

    /**
     * 切换页面
     *
     * @param   pageName    要切换的页面名称
     */
    fun onSwitchPage(pageName: IFrameViews.PageName)

    /**
     * 界面暂停
     */
    fun onPause()

    /**
     * 界面恢复
     */
    fun onResume()

}