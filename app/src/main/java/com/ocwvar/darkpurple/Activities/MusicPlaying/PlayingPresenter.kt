package com.ocwvar.darkpurple.Activities.MusicPlaying

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.Core.ICore
import com.ocwvar.darkpurple.Services.MediaPlayerService
import com.ocwvar.darkpurple.Services.MediaServiceConnector
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import com.ocwvar.darkpurple.Units.OCThreadExecutor
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-26 下午6:08
 * File Location com.ocwvar.darkpurple.Activities.MusicPlaying
 * This file use to :
 */
class PlayingPresenter(private val iPlayingViews: IPlayingViews) : IPlayingPresenter {

    //Model数据控制层
    private val iPlayingModel: IPlayingModel = PlayingModel()

    //线程池
    private val threadPool: OCThreadExecutor = OCThreadExecutor(3, "PlayingThreadPool")

    //音频变化接收器
    private val audioChangeReceiver: AudioChangeReceiver = AudioChangeReceiver()

    //媒体连接器
    private var mediaServiceConnector: MediaServiceConnector? = null

    //切换等待任务线程
    private val switchPendingHandler: SwitchPendingHandler = SwitchPendingHandler()

    //背景图像切换任务线程
    private val backgroundUpdater: BackgroundUpdater = BackgroundUpdater()

    //进度条以及时间更新任务线程
    private val timer: Timer = Timer()

    /**
     * Activity 恢复状态
     *
     * 进行广播监听器的注册
     * 连接媒体服务
     */
    override fun onActivityResume() {
        if (!audioChangeReceiver.isRegistered) {
            iPlayingViews.getActivity().registerReceiver(audioChangeReceiver, audioChangeReceiver.filter)
        }

        connectService()
    }

    /**
     * Activity 暂停状态
     *
     * 断开媒体服务连接
     * 进行广播监听器的反注册
     */
    override fun onActivityPause() {
        disConnectService()

        if (audioChangeReceiver.isRegistered) {
            iPlayingViews.getActivity().unregisterReceiver(audioChangeReceiver)
        }
    }

    /**
     * Activity 销毁状态
     */
    override fun onActivityDestroy() {
        threadPool.shutdownNow()
    }

    /**
     * 请求连接媒体服务
     */
    override fun connectService() {
        //开始进行服务连接
        if (mediaServiceConnector?.isServiceConnected() == true) {
            //如果已经连接则不进行操作
            return
        }

        //显示等待动画
        iPlayingViews.onServiceDisconnect()

        //开始连接媒体服务
        if (mediaServiceConnector == null) {
            mediaServiceConnector = MediaServiceConnector(iPlayingViews.getActivity(), this@PlayingPresenter)
            mediaServiceConnector?.connect()
        } else {
            mediaServiceConnector?.connect()
        }

    }

    /**
     * 请求断开媒体服务
     */
    override fun disConnectService() {
        mediaServiceConnector?.disConnect()
    }

    /**
     * 切换当前播放状态 播放↔暂停/停止
     */
    override fun switchPlaybackState() {

        //获取当前状态
        val state: Int = getMediaController()?.playbackState?.state ?: PlaybackStateCompat.STATE_ERROR

        when (state) {

            PlaybackStateCompat.STATE_PLAYING -> {
                getMediaController()?.transportControls?.pause()
                iPlayingViews.onMusicPause()
            }

            PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> {
                getMediaController()?.transportControls?.play()
                iPlayingViews.onMusicResume()
            }

        }

    }

    /**
     * 切换频谱播放
     */
    @SuppressLint("NewApi")
    override fun switchSpectrum(enable: Boolean) {

        //先检查是否有音频录制权限
        if (AppConfigs.OS_6_UP && iPlayingViews.getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //进行请求权限
            iPlayingViews.getActivity().requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 99)
            return
        }

        if (enable) {
            iPlayingViews.onShowSpectrum()
        } else {
            iPlayingViews.onHideSpectrum()
        }

    }

    /**
     * 切换循环播放开关
     *
     * @param   enable  开关
     */
    override fun switchLoopState(enable: Boolean) {
        AppConfigs.playMode_Loop = enable
        PreferenceManager.getDefaultSharedPreferences(iPlayingViews.getActivity()).edit().putBoolean("playMode_Loop", AppConfigs.playMode_Loop).apply()

        iPlayingViews.onUpdateLoopState(enable)
    }

    /**
     * 切换随机播放开关
     *
     * @param   enable  开关
     */
    override fun switchRandomState(enable: Boolean) {
        AppConfigs.playMode_Random = enable
        PreferenceManager.getDefaultSharedPreferences(iPlayingViews.getActivity()).edit().putBoolean("playMode_Random", AppConfigs.playMode_Random).apply()

        iPlayingViews.onUpdateRandomState(enable)
    }

    /**
     * 请求播放指定位置
     *
     * @param   index   歌曲序列
     * @param   needPending 需要等待
     */
    override fun playIndex(index: Int, needPending: Boolean) {

        if (index == iPlayingModel.getPlayingIndex()) {
            //如果是重新切换回了当前正在播放的位置，则取消执行
            switchPendingHandler.cancel()

        } else if (iPlayingModel.isIndexValid(index)) {
            //在开始执行播放前 检查请求序列的合法性
            if (needPending) {
                //进行等待
                switchPendingHandler.start(index)
            } else {
                //发送请求指令
                mediaServiceConnector?.sendCommand(MediaPlayerService.COMMAND.COMMAND_PLAY_LIBRARY, Bundle().let {
                    it.putInt(MediaPlayerService.COMMAND_EXTRA.ARG_INT_LIBRARY_INDEX, index)
                    it.putString(MediaPlayerService.COMMAND_EXTRA.ARG_STRING_LIBRARY_NAME, MediaLibrary.getUsingLibraryTAG())
                    it
                })
            }

        }
    }

    /**
     * 显示频谱动画
     */
    @SuppressLint("NewApi")
    override fun onEnableSpectrum() {
        if (!AppConfigs.OS_6_UP) {
            iPlayingViews.onShowSpectrum()
        }

        when (iPlayingViews.getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_DENIED -> {
                iPlayingViews.onHideSpectrum()
            }

            PackageManager.PERMISSION_GRANTED -> {
                iPlayingViews.onShowSpectrum()
            }
        }
    }

    /**
     * 不显示频谱动画
     */
    override fun onDisableSpectrum() {
        iPlayingViews.onHideSpectrum()
    }

    /**
     *  媒体服务连接成功
     */
    override fun onServiceConnected() {
        iPlayingViews.onServiceConnected()
        updatePlayingScreen()
    }

    /**
     *  媒体服务连接断开
     */
    override fun onServiceDisconnected() {
        iPlayingViews.onServiceDisconnect()
        updatePlayingScreen()
    }

    /**
     *  无法连接媒体服务
     */
    override fun onServiceConnectionError() {
        iPlayingViews.onServiceDisconnect()
        updatePlayingScreen()
    }

    /**
     *  媒体数据发生更改
     *  @param  metadata
     */
    override fun onMediaChanged(metadata: MediaMetadataCompat) {
    }

    /**
     *  媒体播放状态发生改变
     *  @param  state
     */
    override fun onMediaStateChanged(state: PlaybackStateCompat) {
    }

    /**
     * 媒体服务返回当前正在使用的媒体数据列表回调
     * @param   data    数据列表
     */
    override fun onGotUsingLibraryData(data: MutableList<MediaBrowserCompat.MediaItem>) {
    }

    /**
     *  无法获取媒体服务返回的媒体数据
     */
    override fun onGetUsingLibraryDataError() {
    }

    /**
     * 进度发生变化回调接口
     *
     * @param max            最大值
     * @param progress       进度值
     * @param changedByTouch 是否由用户触摸发生变化
     */
    override fun onValueChanged(max: Int, progress: Int, changedByTouch: Boolean) {
        //这里不需要进行额外操作，已经由Timer处理
    }

    /**
     * 触摸调节开始时回调接口
     * 此回调仅会在触摸刚开始时触发
     *
     * @param max      最大值
     * @param progress 进度值
     */
    override fun onTouchBegin(max: Int, progress: Int) {
        timer.isControllingByUser = true
    }

    /**
     * 触摸调节结束时回调接口
     *
     * @param max      最大值
     * @param progress 进度值
     */
    override fun onTouchFinish(max: Int, progress: Int) {
        timer.isControllingByUser = false
        getMediaController()?.transportControls?.seekTo(progress * 1000L)
    }

    /**
     * 更新播放界面
     *
     * 包括：
     * 更新歌曲名称
     * 更新歌曲专辑
     * 更新播放序列文字
     * 更新循环、随机按钮状态
     * 更新封面轮播位置
     * 更新背景图像
     * **在状态无效时显示无效界面
     *
     */
    private fun updatePlayingScreen() {

        //获取基础数据
        val data: SongItem? = iPlayingModel.getPlayingData()
        val state: Int = getMediaController()?.playbackState?.state ?: PlaybackStateCompat.STATE_NONE
        val index: Int = iPlayingModel.getPlayingIndex()

        //确定是否需要显示无数据页面
        //1.服务未连接
        //2.无播放数据
        //3.播放序列不合法
        //4.播放状态不正确
        val needShowEmptyScreen: Boolean = mediaServiceConnector?.isServiceConnected() == false || data == null || !iPlayingModel.isIndexValid(index) || state == PlaybackStateCompat.STATE_NONE

        if (needShowEmptyScreen) {
            //发生异常
            iPlayingViews.onError()

        } else {
            //更新title
            iPlayingViews.onUpdateTitle(String.format("%s: %d / %d", AppConfigs.ApplicationContext.getString(R.string.text_toolbar_header), index + 1, iPlayingModel.getSourceList().size))
            //更新歌曲名称
            iPlayingViews.onUpdateMusicName(data!!.title)
            //更新歌曲专辑
            iPlayingViews.onUpdateMusicAlbum(data.album)
            //更新轮播封面
            iPlayingViews.onUpdateShowingCover(index)

            //根据状态不同，决定是否显示暂停界面以及Timer是否启动，同时需要更新背景图像
            when (state) {

                PlaybackStateCompat.STATE_PLAYING -> {
                    timer.start()
                    backgroundUpdater.start()
                    iPlayingViews.onMusicResume()
                }

                PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> {
                    timer.stop()
                    backgroundUpdater.start()
                    iPlayingViews.onMusicPause()
                }

                else -> {
                    timer.stop()
                    iPlayingViews.onError()
                }

            }

        }

    }

    /**
     * @return  媒体服务控制器，如果没有初始化则返回NULL
     */
    private fun getMediaController(): MediaControllerCompat? = MediaControllerCompat.getMediaController(iPlayingViews.getActivity())

    /**
     * 计数器控制器
     */
    private inner class Timer {

        /**
         * 更新方法
         */
        private val task: Callable<String?> = Callable {
            while (isAvailable2Update()) {

                //播放状态数据
                val state: PlaybackStateCompat? = getMediaController()?.playbackState

                val duration: Long = iPlayingModel.getPlayingData()?.duration ?: -1L
                //最后更新位置
                val lastPosition: Long = state?.position ?: -1L

                if (state == null || duration < 0 || lastPosition < 0) {
                    //数据无效，中断更新
                    break
                }

                //当前的时间为：最后更新位置 + （（当前时间 - 最后更新时间 = 间隔时间）× 播放速度）
                val currentPosition = lastPosition + ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
                if (currentPosition < 0 || currentPosition > duration) {
                    //如果得到的时间数据错误，中断更新
                    break
                }

                handler.post {
                    //更新Timer文字
                    iPlayingViews.onUpdateTimer(time2String(currentPosition))

                    //只有在用户没有触摸的状态下才更新进度条
                    if (!isControllingByUser) {
                        //更新进度条
                        iPlayingViews.onUpdateSeekBar((currentPosition / 1000).toInt(), (duration / 1000).toInt())
                    }
                }

                //等待1000ms
                Thread.sleep(1000)
            }

            threadPool.removeTag(this@Timer::class.java.simpleName)
            null
        }

        //用于在UI线程刷新的Handler
        private val handler: Handler = Handler(Looper.getMainLooper())

        //当前是否正在被用户调整进度条
        var isControllingByUser: Boolean = false

        //用于时间转换的类
        private var dateFormat: SimpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale.US)
        private var date: Date = Date()

        /**
         * 开始更新
         */
        fun start() {
            threadPool.cancelTask(this@Timer::class.java.simpleName)
            threadPool.submit(FutureTask(task), this@Timer::class.java.simpleName)
        }

        /**
         * 结束更新
         */
        fun stop() {
            threadPool.cancelTask(this@Timer::class.java.simpleName)
        }

        /**
         * 时间长度转换为文本类型
         *
         * @param time 时间长度，单位：ms
         * @return 文本
         */
        private fun time2String(time: Long): String {
            return if (time < 0.0) {
                "00:00"
            } else {
                date.time = time
                if (time >= 3600000) {
                    dateFormat.applyPattern("hh:mm:ss")
                } else {
                    dateFormat.applyPattern("mm:ss")
                }
                dateFormat.format(date)
            }
        }

        /**
         * @return  是否可以进行更新
         */
        private fun isAvailable2Update(): Boolean = getMediaController()?.playbackState?.state ?: PlaybackStateCompat.STATE_NONE == PlaybackStateCompat.STATE_PLAYING && mediaServiceConnector?.isServiceConnected() == true
    }

    /**
     * 歌曲切换等待线程
     */
    private inner class SwitchPendingHandler {

        //等待时间(ms)
        private val pendingTime = 1500L

        //是否还在等待中，当为False时，将不会接受新的指令
        private var isPending: Boolean = true

        //最终切换的索引
        private var targetIndex: Int = 0

        /**
         * 执行方法
         */
        private val task: Callable<String?> = Callable {

            //等待指定时间
            Thread.sleep(pendingTime)
            isPending = false

            //获取当前的状态
            val currentState: Int = getMediaController()?.playbackState?.state ?: PlaybackStateCompat.STATE_ERROR

            if (currentState != PlaybackStateCompat.STATE_ERROR) {
                //发送指令
                mediaServiceConnector?.sendCommand(MediaPlayerService.COMMAND.COMMAND_PLAY_LIBRARY, Bundle().let {
                    it.putInt(MediaPlayerService.COMMAND_EXTRA.ARG_INT_LIBRARY_INDEX, targetIndex)
                    it.putString(MediaPlayerService.COMMAND_EXTRA.ARG_STRING_LIBRARY_NAME, MediaLibrary.getUsingLibraryTAG())
                    it.putBoolean(MediaPlayerService.COMMAND_EXTRA.ARG_BOOLEAN_PLAY_WHEN_READY, currentState == PlaybackStateCompat.STATE_PLAYING)
                    it
                })
            }

            isPending = true

            threadPool.removeTag(this@SwitchPendingHandler::class.java.simpleName)
            null
        }

        /**
         * 开始更新
         */
        fun start(targetIndex: Int) {
            if (isPending) {
                this.targetIndex = targetIndex
                threadPool.cancelTask(this@SwitchPendingHandler::class.java.simpleName)
                threadPool.submit(FutureTask(task), this@SwitchPendingHandler::class.java.simpleName)
            }
        }

        /**
         * 终止
         */
        fun cancel() {
            threadPool.cancelTask(this@SwitchPendingHandler::class.java.simpleName)
            threadPool.removeTag(this@SwitchPendingHandler::class.java.simpleName)
        }

    }

    /**
     * 背景更新器
     */
    private inner class BackgroundUpdater {

        /**
         * 执行方法
         */
        private val task: Callable<String?> = Callable {

            //当前显示的背景图像
            val currentDrawable: Drawable = iPlayingViews.getBackgroundContainer().drawable ?: ColorDrawable(Color.TRANSPARENT)
            //要进行显示的图像
            val blurDrawable: Drawable
            //可以进行切换的背景图像
            val resultDrawable: TransitionDrawable

            //封面ID
            val coverID: String = iPlayingModel.getPlayingData()?.coverID ?: ""
            //等待处理次数
            var counter = 5

            //如果当前要更新的图像TAG，与当前正在显示的TAG不相同，则进行加载显示
            if (coverID != iPlayingViews.getBackgroundContainer().tag) {

                while (!TextUtils.isEmpty(coverID) && CoverProcesser.getLastCompletedCoverID() != coverID && counter > 0) {
                    //等待模糊处理结束
                    Thread.sleep(200)
                    counter -= 1
                }

                blurDrawable = if (CoverProcesser.getLastCompletedCoverID() == coverID) {
                    //如果有处理后的数据
                    CoverProcesser.getBlur() ?: getDefaultDrawable()
                } else {
                    getDefaultDrawable()
                }

                //生成切换Drawable对象
                resultDrawable = TransitionDrawable(arrayOf(currentDrawable, blurDrawable))
                resultDrawable.isCrossFadeEnabled = true

                //更新TAG
                iPlayingViews.getBackgroundContainer().tag = coverID

                //通知更新
                handler.post { iPlayingViews.onUpdateBackground(resultDrawable) }

            }

            threadPool.removeTag(this@BackgroundUpdater::class.java.simpleName)
            null
        }

        //用于在UI线程刷新的Handler
        private val handler: Handler = Handler(Looper.getMainLooper())

        /**
         * 开始更新
         */
        fun start() {
            threadPool.cancelTask(this@BackgroundUpdater::class.java.simpleName)
            threadPool.submit(FutureTask(task), this@BackgroundUpdater::class.java.simpleName)
        }

        /**
         * @return  默认图像
         */
        private fun getDefaultDrawable(): Drawable = AppConfigs.ApplicationContext.getDrawable(R.drawable.blur)

    }

    /**
     * 播放状态变化广播监听器
     */
    private inner class AudioChangeReceiver : BroadcastReceiver() {

        var isRegistered: Boolean = false

        val filter: IntentFilter = IntentFilter().let {
            it.addAction(ICore.ACTIONS.CORE_ACTION_PAUSED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_PLAYING)
            it.addAction(ICore.ACTIONS.CORE_ACTION_STOPPED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_READY)
            it
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            val action: String = intent.action

            when (action) {

                ICore.ACTIONS.CORE_ACTION_PAUSED, ICore.ACTIONS.CORE_ACTION_STOPPED -> {
                    iPlayingViews.onMusicPause()
                    timer.stop()
                }

                ICore.ACTIONS.CORE_ACTION_PLAYING -> {
                    iPlayingViews.onMusicResume()
                    timer.start()
                }

                ICore.ACTIONS.CORE_ACTION_READY -> {
                    updatePlayingScreen()
                }

            }

        }

    }

}