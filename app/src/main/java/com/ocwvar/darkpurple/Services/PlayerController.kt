package com.ocwvar.darkpurple.Services

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.PlaylistItem
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Services.AudioCore.EXO
import com.ocwvar.darkpurple.Services.AudioCore.ICore
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import java.util.*
import kotlin.collections.ArrayList

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午5:28
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   媒体播放控制器，负责调配当前使用的播放资源
 */
class PlayerController(val appContext: Context = AppConfigs.ApplicationContext) : IController {

    //当前使用的媒体库，默认是指向主媒体库
    private var usingLibrary: ArrayList<SongItem> = MediaLibrary.getMainLibrary()

    //当前使用的媒体库 TAG  主媒体库TAG = MAIN ，播放列表TAG = 列表名称
    private var currentLibraryTAG: String = ""

    //当前播放位置
    private var currentIndex: Int = 0

    //随机序列
    private var randomIndex: ArrayList<Int> = ArrayList()

    //播放器核心
    private val iCore: ICore = EXO()

    /**
     * 切换播放的媒体库
     *
     * 此操作将会终止当前正在播放的音频
     *
     * @param   libraryTAG  媒体库TAG
     * @return  执行结果
     */
    override fun changeLibrary(libraryTAG: String): Boolean {
        val library: ArrayList<SongItem>

        if (TextUtils.isEmpty(libraryTAG)) {
            //TAG字符串不合法
            return false
        } else if (this.currentLibraryTAG == libraryTAG) {
            //切换的媒体库与当前使用的相同
            return true
        } else if (libraryTAG == "MAIN") {
            //要切换为主媒体库
            library = MediaLibrary.getMainLibrary()
        } else {
            //要切换为播放列表库
            val playlist: PlaylistItem? = MediaLibrary.getPlaylistLibrary().find { it.name == libraryTAG }

            if (playlist == null || playlist.playlist.size == 0) {
                //无法使用此播放列表
                //1.无法找到对应的播放列表
                //2.播放列表为空
                //3.播放列表未初始化内容数据
                return false
            }
            library = playlist.playlist

        }

        //生成随机序列
        this.randomIndex.clear()
        for (index in 0..library.size - 1) {
            this.randomIndex.add(index)
        }
        Collections.shuffle(this.randomIndex)

        //指向新的媒体库
        this.usingLibrary = library

        //先释放当前的资源
        this.iCore.release()

        //设置、重置相关的属性
        this.currentLibraryTAG = libraryTAG
        this.currentIndex = 0

        //更新当前使用的数据
        MediaLibrary.updateUsingLibraryTAG(this.currentLibraryTAG)
        MediaLibrary.updateUsingIndex(this.currentIndex)

        return true
    }

    /**
     * @return  当前使用的媒体库
     */
    override fun usingLibrary(): ArrayList<SongItem> = this.usingLibrary

    /**
     * @return  当前的媒体位置
     */
    override fun currentIndex(): Int = this.currentIndex

    /**
     * 播放指定媒体数据
     *
     * @param   source  媒体数据
     * @param   isPlayWhenReady 是否缓冲好数据好就进行播放，默认=True
     */
    override fun play(source: SongItem?, isPlayWhenReady: Boolean) {
        source ?: return
        iCore.play(source, isPlayWhenReady)
    }

    /**
     * 播放媒体库指定的位置
     *
     * @param   index   要播放的位置，位置无效无法播放
     * @param   isPlayWhenReady 是否缓冲好数据好就进行播放，默认=True
     */
    override fun play(index: Int, isPlayWhenReady: Boolean) {
        if (this.usingLibrary.size > 0 && index >= 0 && index < this.usingLibrary.size) {
            //播放的位置有效
            this.currentIndex = index
            //更新使用位置
            MediaLibrary.updateUsingIndex(this.currentIndex)

            play(this.usingLibrary[index], isPlayWhenReady)
        }
    }

    /**
     * 1.播放已缓冲好的媒体数据
     * 2.恢复播放已暂停或停止的媒体数据
     */
    override fun resume() {
        this.iCore.resume()
    }

    /**
     * 暂停媒体
     */
    override fun pause() {
        this.iCore.pause()
    }

    /**
     * 停止媒体
     */
    override fun stop() {
        this.iCore.stop()
    }

    /**
     * 播放下一个媒体资源
     */
    override fun next() {
        if (this.usingLibrary.size <= 0) {
            //媒体库数据为空，无法进行播放
            return
        }

        //结果index，-1则代表不进行播放，默认为 -1
        var index: Int

        //根据 OPTIONS.LOOP_LIBRARY 来处理结果 index
        if ((this.currentIndex + 1) >= this.usingLibrary.size) {
            if (IController.OPTIONS.LOOP_LIBRARY) {
                //循环则置于列表头部
                index = 0
            } else {
                //不循环，则不进行播放，发送播放序列完成广播
                this.appContext.sendBroadcast(Intent(IController.ACTIONS.ACTION_QUEUE_FINISH))
                index = -1
            }
        } else {
            index = this.currentIndex + 1
        }

        //根据 OPTIONS.LOOP_LIBRARY 来处理结果 index
        if (index != -1 && IController.OPTIONS.RANDOM_LIBRARY) {
            index = this.randomIndex[Random(System.currentTimeMillis()).nextInt(this.randomIndex.size)]
        }

        //执行结果
        this.play(index)
    }

    /**
     * 播放上一个媒体资源
     */
    override fun previous() {
        if (this.usingLibrary.size <= 0) {
            //媒体库数据为空，无法进行播放
            return
        }

        //结果index，-1则代表不进行播放，默认为 -1
        var index: Int

        //根据 OPTIONS.LOOP_LIBRARY 来处理结果 index
        if ((this.currentIndex - 1) < 0) {
            if (IController.OPTIONS.LOOP_LIBRARY) {
                //循环则置于列表尾部
                index = this.usingLibrary.size - 1
            } else {
                //不循环，则不进行播放，发送播放序列完成广播
                this.appContext.sendBroadcast(Intent(IController.ACTIONS.ACTION_QUEUE_FINISH))
                index = -1
            }
        } else {
            index = this.currentIndex - 1
        }

        //根据 OPTIONS.LOOP_LIBRARY 来处理结果 index
        if (index != -1 && IController.OPTIONS.RANDOM_LIBRARY) {
            index = this.randomIndex[index]
        }

        //执行结果
        this.play(index)
    }

    /**
     * 释放媒体资源
     */
    override fun release() {
        this.iCore.release()
    }

    /**
     * 跳转位置
     *
     * @param   position    要跳转的位置
     */
    override fun seek2(position: Long) {
        this.iCore.seek2(position)
    }

    /**
     * 通知核心更新 AudioSession ID
     */
    override fun updateAudioSessionID() {
        iCore.updateAudioSessionID()
    }

    /**
     * @return  当前媒体的长度
     */
    override fun mediaDuration(): Long = this.iCore.mediaDuration()

    /**
     * @return  当前播放的位置
     */
    override fun currentPosition(): Long = this.iCore.currentPosition()

    /**
     * @return  当前播放核心状态
     */
    override fun currentState(): Int = this.iCore.currentState()
}