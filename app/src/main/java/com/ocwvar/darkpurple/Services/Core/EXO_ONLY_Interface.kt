package com.ocwvar.darkpurple.Services.Core

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-12 下午7:56
 * File Location com.ocwvar.darkpurple.Services.Core
 * This file use to :   EXO2特有的接口
 */
interface EXO_ONLY_Interface : CoreAdvFunctions {

    /**
     * @see EXOCORE.VisualizerLoader.switchOn
     */
    fun switchOnVisualizer()

    /**
     * @see EXOCORE.VisualizerLoader.switchOff
     */
    fun switchOffVisualizer()

}