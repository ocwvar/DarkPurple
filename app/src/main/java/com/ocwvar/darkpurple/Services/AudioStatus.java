package com.ocwvar.darkpurple.Services;

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/11 10:16 PM
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   播放状态枚举类
 */

public enum AudioStatus {
    /**
     * 正在播放
     */
    Playing,
    /**
     * 暂停
     */
    Paused,
    /**
     * 未加载音频数据
     */
    Empty,
    /**
     * 错误或未知状态
     */
    Error
}
