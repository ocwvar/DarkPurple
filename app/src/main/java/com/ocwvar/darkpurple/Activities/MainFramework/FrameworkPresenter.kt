package com.ocwvar.darkpurple.Activities.MainFramework

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.v4.app.FragmentTransaction
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.FragmentPages.MusicListFragment
import com.ocwvar.darkpurple.FragmentPages.PlaylistFragment
import com.ocwvar.darkpurple.FragmentPages.UserFragment
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.Core.ICore
import com.ocwvar.darkpurple.Services.MediaServiceConnector
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import java.lang.ref.WeakReference

@SuppressLint("NewApi")
/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-30 下午10:27
 * File Location com.ocwvar.darkpurple.Activities.MainFramework
 * This file use to :
 */
class FrameworkPresenter(private val iFrameViews: IFrameViews) : IFramePresenter {

    //当前显示的页面
    private var pageName: IFrameViews.PageName = IFrameViews.PageName.设置

    //媒体连接器
    private val mediaServiceConnector: MediaServiceConnector by lazy {
        MediaServiceConnector(iFrameViews.activity(), this@FrameworkPresenter)
    }

    //标准的接收封面模糊处理结果的广播接收器
    private val blurCoverUpdateReceiver: BlurCoverUpdateReceiver by lazy { BlurCoverUpdateReceiver() }

    //标准的接收封面模糊处理结果的广播接收器
    private val playingDataUpdateReceiver: PlayingDataUpdateReceiver by lazy { PlayingDataUpdateReceiver() }

    //默认顶部图像
    private val defaultBackground: Drawable by lazy {
        iFrameViews.activity().resources.getDrawable(R.drawable.blur, null)
    }

    //主页面弱容器
    private var musicPageContainer: WeakReference<MusicListFragment?> = WeakReference(null)

    //播放列表弱容器
    private var playlistContainer: WeakReference<PlaylistFragment?> = WeakReference(null)

    //用户设置弱容器
    private var userContainer: WeakReference<UserFragment?> = WeakReference(null)

    /**
     * 切换页面
     *
     * @param   pageName    要切换的页面名称
     */
    override fun onSwitchPage(pageName: IFrameViews.PageName) {
        if (this.pageName == pageName) {
            //相同页面，不进行操作
            return
        }

        //获取Fragment切换对象
        val fragmentTransaction: FragmentTransaction = iFrameViews.activity().supportFragmentManager.beginTransaction().let {
            it.setCustomAnimations(R.anim.fragment_anim_in, R.anim.fragment_anim_out)
        }

        //进行页面切换
        when (pageName) {

            IFrameViews.PageName.主界面 -> {
                var musicListFragment: MusicListFragment? = musicPageContainer.get()
                if (musicListFragment == null) {
                    musicListFragment = MusicListFragment()
                    musicPageContainer = WeakReference(musicListFragment)
                }

                fragmentTransaction.replace(R.id.FragmentContainer, musicListFragment)
            }

            IFrameViews.PageName.播放列表 -> {
                var playlistFragment: PlaylistFragment? = playlistContainer.get()
                if (playlistFragment == null) {
                    playlistFragment = PlaylistFragment()
                    playlistContainer = WeakReference(playlistFragment)
                }

                fragmentTransaction.replace(R.id.FragmentContainer, playlistFragment)
            }

            IFrameViews.PageName.设置 -> {
                var userFragment: UserFragment? = userContainer.get()
                if (userFragment == null) {
                    userFragment = UserFragment()
                    userContainer = WeakReference(userFragment)
                }

                fragmentTransaction.replace(R.id.FragmentContainer, userFragment)
            }

        }
        //记录当前的页面
        this.pageName = pageName

        //执行页面切换
        fragmentTransaction.commit()

        //切换按钮点击样式
        iFrameViews.onUpdateFragmentButtonState(pageName)
    }

    /**
     *  媒体服务连接成功：
     *  更新浮动按钮状态
     *  更新顶部文字
     *  更新顶部图像资源
     */
    override fun onServiceConnected() {
        updateFloatingButton()
        updateHeaderText(true)
        updateHeaderImage(null, CoverProcesser.getBlur() ?: defaultBackground, MediaLibrary.getUsingMedia()?.album)
    }

    /**
     *  媒体服务连接断开
     */
    override fun onServiceDisconnected() {
        iFrameViews.onSwitchPlayingButton(false)
    }

    /**
     *  无法连接媒体服务
     */
    override fun onServiceConnectionError() {
        iFrameViews.onSwitchPlayingButton(false)
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
     * 界面暂停：
     * 断开媒体服务连接
     * 反注册所有广播接收器
     * 恢复空状态
     */
    override fun onPause() {
        this.mediaServiceConnector.disConnect()

        if (this.blurCoverUpdateReceiver.isRegistered) {
            iFrameViews.activity().unregisterReceiver(this.blurCoverUpdateReceiver)
            this.blurCoverUpdateReceiver.isRegistered = false
        }

        if (this.playingDataUpdateReceiver.isRegistered) {
            iFrameViews.activity().unregisterReceiver(this.playingDataUpdateReceiver)
            this.playingDataUpdateReceiver.isRegistered = false
        }

        updateHeaderText(false)
        updateHeaderImage(null)
    }

    /**
     * 界面恢复：
     * 检查权限
     * 连接媒体服务
     * 注册所有广播接收器
     */
    override fun onResume() {
        if (iFrameViews.activity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            iFrameViews.onLeakPermission()
        }

        this.mediaServiceConnector.connect()

        if (!this.blurCoverUpdateReceiver.isRegistered) {
            iFrameViews.activity().registerReceiver(this.blurCoverUpdateReceiver, this.blurCoverUpdateReceiver.intentFilter)
            this.blurCoverUpdateReceiver.isRegistered = true
        }

        if (!this.playingDataUpdateReceiver.isRegistered) {
            iFrameViews.activity().registerReceiver(this.playingDataUpdateReceiver, this.playingDataUpdateReceiver.intentFilter)
            this.playingDataUpdateReceiver.isRegistered = true
        }
    }

    /**
     * 更新浮动按钮状态
     */
    private fun updateFloatingButton() {
        //只有在可以获取服务状态的时候，并且播放状态是正在播放时，才能显示浮动按钮
        iFrameViews.onSwitchPlayingButton(getMediaController()?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING)
    }

    /**
     * 更新顶部显示的文字数据
     *
     * @param   fromMedia   是否从当前正在使用的媒体获取数据，否则仅显示默认文字，默认 = False
     */
    private fun updateHeaderText(fromMedia: Boolean = false) {
        if (!fromMedia) {
            iFrameViews.onUpdateAlbum(null)
            iFrameViews.onUpdateArtist(null)
            iFrameViews.onUpdateTitle("#DarkPurple Project")
            return
        }

        val data: SongItem? = MediaLibrary.getUsingMedia()
        if (data == null) {
            //如果当前没有正在使用的数据，则使用默认样式
            updateHeaderText()
            return
        }

        iFrameViews.onUpdateAlbum(data.album)
        iFrameViews.onUpdateArtist(data.artist)
        iFrameViews.onUpdateTitle(data.title)
    }

    /**
     * 更新顶部图像样式
     *
     * @param   coverDrawable   封面图像，NULL = 不显示，默认 = NULL
     * @param   backgroundDrawable   背景图像，默认 = 默认背景图像
     * @param imageTAG  图像TAG，如果是相同的TAG，则不更新图像，默认为default
     */
    private fun updateHeaderImage(coverDrawable: Drawable? = null, backgroundDrawable: Drawable = this.defaultBackground, imageTAG: String? = null) {
        var tag: String = imageTAG ?: "default"

        if (backgroundDrawable == this.defaultBackground) {
            //如果是默认图样，则设置默认TAG
            tag = "default"
        }

        iFrameViews.onUpdateCover(coverDrawable, tag)
        iFrameViews.onUpdateBackground(backgroundDrawable, tag)
    }

    /**
     * @return  媒体服务控制器，如果没有初始化则返回NULL
     */
    private fun getMediaController(): MediaControllerCompat? = MediaControllerCompat.getMediaController(iFrameViews.activity())

    /**
     * 标准的接收封面模糊处理结果的广播接收器
     */
    private inner class BlurCoverUpdateReceiver : BroadcastReceiver() {

        val intentFilter: IntentFilter = IntentFilter().let {
            it.addAction(CoverProcesser.ACTION_BLUR_UPDATED)
            it.addAction(CoverProcesser.ACTION_BLUR_UPDATE_FAILED)
            it
        }

        var isRegistered: Boolean = false


        override fun onReceive(context: Context?, intent: Intent?) {
            //无效Intent，不做任何处理
            intent ?: return

            when (intent.action) {

            //有新的封面模糊图像产生
                CoverProcesser.ACTION_BLUR_UPDATED -> {
                    updateHeaderImage(null, CoverProcesser.getBlur() ?: defaultBackground, MediaLibrary.getUsingMedia()?.album)
                }

            //有新的封面模糊图像发生失败
                CoverProcesser.ACTION_BLUR_UPDATE_FAILED -> {
                    updateHeaderImage(null)
                }

            }
        }

    }

    /**
     * 接收当前播放音频数据
     */
    private inner class PlayingDataUpdateReceiver : BroadcastReceiver() {

        val intentFilter: IntentFilter = IntentFilter().let {
            it.addAction(ICore.ACTIONS.CORE_ACTION_READY)
            it.addAction(ICore.ACTIONS.CORE_ACTION_PLAYING)
            it.addAction(ICore.ACTIONS.CORE_ACTION_PAUSED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_STOPPED)
            it.addAction(ICore.ACTIONS.CORE_ACTION_RELEASE)
            it
        }

        var isRegistered: Boolean = false

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {

                ICore.ACTIONS.CORE_ACTION_STOPPED, ICore.ACTIONS.CORE_ACTION_PAUSED -> {
                    iFrameViews.onSwitchPlayingButton(false)
                }

                ICore.ACTIONS.CORE_ACTION_READY -> {
                    updateHeaderText(true)
                    iFrameViews.onSwitchPlayingButton(false)
                }

                ICore.ACTIONS.CORE_ACTION_PLAYING -> {
                    updateHeaderText(true)
                    iFrameViews.onSwitchPlayingButton(true)
                }

                ICore.ACTIONS.CORE_ACTION_RELEASE -> {
                    updateHeaderText()
                    iFrameViews.onSwitchPlayingButton(false)
                }

            }
        }

    }

}