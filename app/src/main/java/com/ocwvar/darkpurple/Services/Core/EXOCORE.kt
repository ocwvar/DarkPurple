package com.ocwvar.darkpurple.Services.Core

import android.content.Context
import android.content.Intent
import android.media.audiofx.Visualizer
import android.net.Uri
import android.support.v4.content.FileProvider
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Services.AudioService
import com.ocwvar.darkpurple.Services.AudioStatus
import com.ocwvar.darkpurple.Services.ServiceHolder
import com.ocwvar.darkpurple.Units.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/12 12:56 PM
 * File Location com.ocwvar.darkpurple.Services.Core
 * This file use to :   Google ExoPlayer2 播放方案
 */
class EXOCORE(val applicationContext: Context) : CoreAdvFunctions, EXO_ONLY_Interface {

    private val TAG: String = "EXO_CORE"
    private val exoPlayer: SimpleExoPlayer
    private val exoPlayerCallback: ExoPlayerCallback
    //当前音频状态枚举
    private var currentAudioStatus = AudioStatus.Empty
    //曲目长度变量
    private var isMusicLoaded: Boolean = false
    private var loadedSourceDuration: Double = 100000.0
    //频谱加载对象
    private var visualizerLoader: VisualizerLoader? = null

    init {
        exoPlayerCallback = ExoPlayerCallback()
        exoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext, DefaultTrackSelector())
        exoPlayer.addListener(exoPlayerCallback)
    }

    /**
     * 播放音频
     * @param songItem 音频信息对象
     * @param onlyInit 是否仅加载音频数据，而不进行播放
     * @return  执行结果
     */
    override fun play(songItem: SongItem, onlyInit: Boolean): Boolean {
        //从歌曲路径获取Uri路径
        val songUri: Uri? = FILE2URI(songItem.path)
        songUri?.let {
            release()
            //成功获取到路径，则开始加载音频数据
            val factory: DataSource.Factory = DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, TAG))
            val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()
            val mediaSource: MediaSource = ExtractorMediaSource(it, factory, extractorsFactory, null, exoPlayerCallback)
            //重置音频长度
            isMusicLoaded = false
            loadedSourceDuration = 0.0
            //开始准备音频数据
            exoPlayer.prepare(mediaSource, true, true)
            if (onlyInit) {
                //仅加载不播放状态，通知刷新UI
                applicationContext.sendBroadcast(Intent(AudioService.NOTIFICATION_UPDATE))
            }
            exoPlayer.playWhenReady = !onlyInit
            return true
        }
        //无法获取到Uri路径
        return false
    }

    /**
     * 续播音频
     * @return 执行结果
     */
    override fun resume(): Boolean {
        if (exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            //如果当前core已经装载有音频数据，则开始播放
            exoPlayer.playWhenReady = true
            applicationContext.sendBroadcast(Intent(AudioService.AUDIO_RESUMED))
            return true
        } else {
            return false
        }
    }

    /**
     * 暂停音频
     * @return 执行结果
     */
    override fun pause(): Boolean {
        if (exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            //如果当前core已经装载有音频数据，则可以执行暂停操作
            exoPlayer.playWhenReady = false
            applicationContext.sendBroadcast(Intent(AudioService.AUDIO_PAUSED))
            return true
        } else {
            return false
        }
    }

    /**
     * 释放音频资源
     * @return 执行结果
     */
    override fun release(): Boolean {
        exoPlayer.stop()
        visualizerLoader?.switchOff()
        //执行完成后如果状态为静止，则表明释放成功
        return exoPlayer.playbackState == ExoPlayer.STATE_IDLE
    }

    /**
     * 获取音频当前播放的位置，即已播放的长度
     * @return 当前位置，异常返回 0
     */
    override fun playingPosition(): Double {
        if (exoPlayer.playbackState != ExoPlayer.STATE_IDLE) {
            //当前音频资源不为空，则可以返回数据
            return exoPlayer.currentPosition / 1000.0
        } else {
            return 0.0
        }
    }

    /**
     * 跳转至指定音频长度位置
     * @return 执行结果
     */
    override fun seekPosition(position: Double): Boolean {
        if (exoPlayer.playbackState != ExoPlayer.STATE_IDLE) {
            //当前音频资源不为空，可以进行播放位置调整
            exoPlayer.seekTo((position * 1000).toLong())
            return true
        } else {
            return false
        }
    }

    /**
     * 获取音频长度
     * @return 音频长度，异常返回 0
     */
    override fun getAudioLength(): Double {
        return loadedSourceDuration
    }

    /**
     * 获取当前音乐播放状态
     * @return  当前状态
     */
    override fun getAudioStatus(): AudioStatus {
        return currentAudioStatus
    }

    /**
     * @see EXOCORE.VisualizerLoader.switchOn
     */
    override fun switchOnVisualizer() {
        visualizerLoader?.switchOn()
    }

    /**
     * @see EXOCORE.VisualizerLoader.switchOff
     */
    override fun switchOffVisualizer() {
        visualizerLoader?.switchOff()
    }

    /**
     * 获取当前频谱数据
     * @return  频谱数据，异常返回 NULL
     */
    override fun getSpectrum(): FloatArray? {
        if (visualizerLoader == null || visualizerLoader!!.id != exoPlayer.audioSessionId) {
            //如果当前频谱加载器为空、频谱加载器中的音频ID不是当前的播放ID
            //如果存在旧的频谱加载器则先关闭
            visualizerLoader?.switchOff()
            //获取音频ID
            val id: Int = exoPlayer.audioSessionId
            if (id == 0) {
                //如果当前播放的ID为0，则是无效状态，直接返回NULL，并不创建对象
                return null
            } else {
                //创建频谱加载器对象，并自动打开
                visualizerLoader = VisualizerLoader(id)
                visualizerLoader?.switchOn()
            }
        }
        return visualizerLoader?.get()
    }

    /**
     * 获取均衡器各个频段参数
     * @return  均衡器参数
     */
    override fun getEQParameters(): IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /**
     * 更改均衡器频段参数
     * @param eqParameter 均衡器参数 -10 ~ 10
     * @param eqIndex     调节位置
     * @return 执行结果
     */
    override fun setEQParameters(eqParameter: Int, eqIndex: Int): Boolean = false

    /**
     * 重置均衡器
     */
    override fun resetEQ() {
    }

    /**
     * 将文件路径转换为Uri
     * @return  文件的URI路径，如果文件无法找到或解析失败，返回NULL结果
     */
    private fun FILE2URI(filePath: String): Uri? {
        try {
            val fileObject = File(filePath)
            if (fileObject.exists() && fileObject.canRead()) {
                return FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", fileObject)
            }
            throw FileNotFoundException("没有找到文件：$filePath")
        } catch(e: Exception) {
            Logger.error(TAG, "解析文件Uri路径失败！  $e")
            return null
        }
    }

    /**
     * 频谱数据加载器
     */
    private class VisualizerLoader(val id: Int) {

        private val TAG: String = "ID$id 频谱数据"

        private val visualizer: Visualizer = Visualizer(id)

        init {
            visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
        }

        /**
         * 获取频谱前需要调用 switchOn 来开始接收数据
         * @see switchOn
         * @return  频谱数据，无数据返回NULL
         */
        fun get(): FloatArray? {
            val byteArray: ByteArray = ByteArray(1024)
            visualizer.getFft(byteArray)
            return handleIntArray2PositionArray(byteArray2intArray(byteArray), 0f, 128)
        }

        /**
         * 开始接收数据，在不需要频谱数据的时候必须要调用 switchOff 来停止接收数据
         * @see switchOff
         */
        fun switchOn() {
            Logger.warnning(TAG, "开始接收数据")
            visualizer.enabled = true
        }

        /**
         * 停止接收频谱数据
         */
        fun switchOff() {
            Logger.warnning(TAG, "停止接收数据，并释放资源")
            visualizer.enabled = false
            visualizer.release()
        }

        /**
         * 将原生FFT数据转换为IntArray的格式
         *
         * @param byteArray 原生FFT数据
         * @return 转换得到的数据，如果转换失败则返回NULL
         */
        private fun byteArray2intArray(byteArray: ByteArray?): IntArray? {
            if (byteArray == null) return null
            val byteBuffer = ByteBuffer.wrap(byteArray)
            val numberArray = IntArray(byteBuffer.asIntBuffer().limit())
            try {
                //得到FloatArray数据
                byteBuffer.asIntBuffer().get(numberArray)
            } catch (e: Exception) {
                Logger.error(TAG, "ByteArray → IntArray 发生异常：" + e)
                return null
            }

            return numberArray
        }

        /**
         * 将原生FFT IntArray转换为 0 ~ 限制大小 区间内的FloatArray
         * @param inArray           要用于转换的IntArray
         * @param sizeLimit         限制每个Float数值的最大值 <=0 则不限制
         * @param arrayLengthLimit 输出数组的长度 <=0 则不限制
         * @return 转换得到的数据，如果转换失败则返回NULL
         */
        private fun handleIntArray2PositionArray(inArray: IntArray?, sizeLimit: Float, arrayLengthLimit: Int): FloatArray? {
            if (inArray == null || arrayLengthLimit >= inArray.size) return null
            //根据限制创建工作数组长度
            val workArray = if (arrayLengthLimit <= 0) FloatArray(inArray.size) else FloatArray(arrayLengthLimit)
            for (i in workArray.indices) {
                var number = inArray[i].toFloat()
                if (number == 0.0f) {
                    //原本数据就是 0f 不需要重新设置
                    continue
                }
                if (number < 0f) {
                    //数据为负数，转为正数
                    number *= -1f
                }
                //移动小数点
                number *= 0.0000000001f

                if (sizeLimit > 0 && sizeLimit < number) {
                    //如果大于限制数，则将数字设为最大数值
                    number = sizeLimit
                }
                workArray[i] = number
            }
            return workArray
        }

    }

    /**
     * 播放器状态回调
     */
    private inner class ExoPlayerCallback : ExoPlayer.EventListener, ExtractorMediaSource.EventListener {

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            //音频播放失败，则直接播放下一首
            Logger.warnning(TAG, "音频播放失败！ 原因：$error")
            applicationContext.sendBroadcast(Intent(AudioService.NOTIFICATION_NEXT))
        }

        /**
         * 播放状态变化回调接口
         * @param   playWhenReady   是否当音频准备好后会自动播放
         * @param   playbackState   变更为的状态
         */
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            //是否需要更新Notification的标记变量
            val need2UpdateNotification: Boolean

            when (playbackState) {
                ExoPlayer.STATE_ENDED -> {
                    need2UpdateNotification = false
                    currentAudioStatus = AudioStatus.Buffering
                    if (playWhenReady) {
                        //播放完成后，需要播放下一首歌曲
                        Logger.warnning(TAG, "状态变更：播放结束")
                        applicationContext.sendBroadcast(Intent(AudioService.NOTIFICATION_NEXT))
                    }
                }

                ExoPlayer.STATE_READY -> {
                    need2UpdateNotification = true
                    Logger.warnning(TAG, "状态变更：音频加载完成")
                    //更新状态
                    if (playWhenReady) {
                        currentAudioStatus = AudioStatus.Playing
                    } else {
                        currentAudioStatus = AudioStatus.Paused
                    }
                    //设置标记
                    isMusicLoaded = true
                    //当读取完成后，设置音频的长度
                    loadedSourceDuration = exoPlayer.duration / 1000.0
                    Logger.warnning(TAG, "音频长度：$loadedSourceDuration")
                }

                ExoPlayer.STATE_BUFFERING -> {
                    need2UpdateNotification = false
                    Logger.warnning(TAG, "状态变更：音频正在缓冲")
                    //更新状态
                    currentAudioStatus = AudioStatus.Buffering
                    //设置标记
                    isMusicLoaded = false
                    //当读取完成后，设置音频的长度
                    loadedSourceDuration = 0.0
                }

                ExoPlayer.STATE_IDLE -> {
                    need2UpdateNotification = false
                    //更新状态
                    currentAudioStatus = AudioStatus.Empty
                }

                else -> {
                    need2UpdateNotification = false
                    currentAudioStatus = AudioStatus.Error
                }
            }
            if (!need2UpdateNotification) {
                //不需要更新Notification，直接跳出
                return
            }
            ServiceHolder.getInstance().service?.let {
                if (it.isRunningForeground) {
                    //通知Notification刷新
                    applicationContext.sendBroadcast(Intent(AudioService.NOTIFICATION_UPDATE))
                }
            }
        }

        override fun onLoadingChanged(isLoading: Boolean) {
        }

        override fun onPositionDiscontinuity() {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
        }

        override fun onLoadError(error: IOException?) {
            //音频加载失败，则直接播放下一首
            Logger.warnning(TAG, "音频加载失败！ 原因：$error")
        }
    }

}