package com.ocwvar.darkpurple.Services.Core

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-11 上午2:11
 * File Location com.ocwvar.darkpurple.Services.Core
 * This file use to :   EXO2特有的功能接口
 */
interface EXO_ONLY_Functions : CoreBaseFunctions {
    /**
     * EXO核心特有方法
     * 开始允许接收频谱数据，调用此方法后才可以从：getSpectrum()方法内获取到数据
     */
    fun EXO_ONLY_switch_on_visualizer()

    /**
     * EXO核心特有方法
     * 关闭接收频谱数据，调用此方法后将无法接收到频谱数据
     */
    fun EXO_ONLY_switch_off_visualizer()
}