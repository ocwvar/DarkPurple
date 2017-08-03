package com.ocwvar.darkpurple.Units.MediaLibrary

import android.text.TextUtils
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

    //当前使用的媒体库TAG，主媒体库=MAIN，播放列表媒体库=播放列表名称
    private var usingLibraryTAG: String = ""

    //当前正在使用的媒体索引编号
    private var usingIndex: Int = -1

    /**
     * 更新当前正在使用的媒体库TAG
     *
     * @param   libraryTAG  要更新的媒体库TAG
     */
    fun updateUsingLibraryTAG(libraryTAG: String) {
        if (this.usingLibraryTAG != libraryTAG) {
            this.usingLibraryTAG = libraryTAG
        }
    }

    /**
     * 更新正在使用的媒体索引编号
     *
     * @param   usingIndex  正在使用的媒体索引编号
     */
    fun updateUsingIndex(usingIndex: Int) {
        if (this.usingIndex != usingIndex) {
            this.usingIndex = usingIndex
        }
    }

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
     * 获取当前正在使用的媒体数据
     * @return  媒体数据，不可用时返回 NULL
     */
    fun getUsingMedia(): SongItem? {
        if (TextUtils.isEmpty(this.usingLibraryTAG) || this.usingIndex < 0) {
            //没有有效的查询数据
            return null
        }
        //结果数组声明
        val usingLibrary: ArrayList<SongItem>?

        if (this.usingLibraryTAG == "MAIN") {
            //主媒体库
            usingLibrary = mainLibrary
        } else {
            //查找对应的播放列表
            usingLibrary = playlistLibrary.find { it.name == this.usingLibraryTAG }?.playlist
        }

        usingLibrary?.let {
            if (it.size > 0 && this.usingIndex >= 0 && this.usingIndex < it.size) {
                //在有效范围内
                return it[this.usingIndex]
            }
        }

        return null
    }

    /**
     * @return  当前正在使用的媒体库
     */
    fun getUsingLibrary(): ArrayList<SongItem>? {
        if (TextUtils.isEmpty(this.usingLibraryTAG)) {
            return null
        } else if (this.usingLibraryTAG == "MAIN") {
            return this.mainLibrary
        } else {
            return this.playlistLibrary.find { it.name == this.usingLibraryTAG }?.playlist
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

    /**
     * @return  当前正在使用的媒体库TAG
     */
    fun getUsingLibraryTAG(): String = this.usingLibraryTAG

    /**
     * @return  当前正在使用的媒体索引编号
     */
    fun getUsingIndex(): Int = this.usingIndex

}