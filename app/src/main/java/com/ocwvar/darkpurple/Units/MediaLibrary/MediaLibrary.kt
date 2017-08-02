package com.ocwvar.darkpurple.Units.MediaLibrary

import com.ocwvar.darkpurple.Bean.PlaylistItem
import com.ocwvar.darkpurple.Bean.SongItem

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午5:54
 * File Location com.ocwvar.darkpurple.Units
 * This file use to :   媒体库
 */
object MediaLibrary {

    //主媒体库
    private val mainLibrary: ArrayList<SongItem> = ArrayList()

    //播放列表媒体库
    private val playlistLibrary: ArrayList<PlaylistItem> = ArrayList()

    /**
     * 更新主媒体库内容
     *
     * 此操作将会清空已有数据
     * @param   source  数据源
     */
    fun updateMainSource(source: ArrayList<SongItem>) {
        this.mainLibrary.clear()
        source.let {
            this.mainLibrary.addAll(it)
        }
    }

    /**
     * 更新播放列表媒体库内容
     *
     * 此操作将会清空已有数据
     * @param   source  数据源
     */
    fun updatePlaylistSource(source: ArrayList<PlaylistItem>) {
        this.playlistLibrary.clear()
        source.let {
            this.playlistLibrary.addAll(it)
        }
    }

    /**
     * @return  主媒体库
     */
    fun getMainLibrary(): ArrayList<SongItem> = this.mainLibrary

    /**
     * @return  播放列表媒体库
     */
    fun getPlaylistLibrary(): ArrayList<PlaylistItem> = this.playlistLibrary

}