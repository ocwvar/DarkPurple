package com.ocwvar.darkpurple.Activities.MusicPlaying

import com.ocwvar.darkpurple.Bean.SongItem

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-26 下午5:40
 * File Location com.ocwvar.darkpurple.Activities.MusicPlaying
 * This file use to :   播放界面播放数据层
 */
interface IPlayingModel {

    /**
     * @return  正在播放的列表数据
     */
    fun getSourceList(): ArrayList<SongItem>

    /**
     * @return  正在播放位置
     */
    fun getPlayingIndex(): Int

    /**
     * @return  正在播放的数据
     */
    fun getPlayingData(): SongItem?

    /**
     * @return  检查是否合法
     */
    fun isIndexValid(index: Int): Boolean

}