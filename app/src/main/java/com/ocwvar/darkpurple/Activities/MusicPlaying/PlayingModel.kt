package com.ocwvar.darkpurple.Activities.MusicPlaying

import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-26 下午6:01
 * File Location com.ocwvar.darkpurple.Activities.MusicPlaying
 * This file use to :   播放数据控制层
 */
class PlayingModel : IPlayingModel {

    /**
     * 当前正在使用的媒体库
     */
    private val playingList: ArrayList<SongItem> = MediaLibrary.getUsingLibrary() ?: ArrayList()

    /**
     * @return  正在播放的列表数据
     */
    override fun getSourceList(): ArrayList<SongItem> = this.playingList

    /**
     * @return  正在播放位置
     */
    override fun getPlayingIndex(): Int = MediaLibrary.getUsingIndex()

    /**
     * @return  检查是否合法
     */
    override fun isIndexValid(index: Int): Boolean = this.playingList.size > 0 && index in playingList.indices

    /**
     * @return  正在播放的数据
     */
    override fun getPlayingData(): SongItem? =
            if (isIndexValid(MediaLibrary.getUsingIndex())) {
                this.playingList[MediaLibrary.getUsingIndex()]
            } else {
                null
            }

}