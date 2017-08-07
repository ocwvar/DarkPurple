package com.ocwvar.darkpurple.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.FragmentPages.MusicListFragment
import com.ocwvar.darkpurple.FragmentPages.PlaylistFragment
import com.ocwvar.darkpurple.FragmentPages.UserFragment
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.Core.ICore
import com.ocwvar.darkpurple.Services.MediaPlayerService
import com.ocwvar.darkpurple.Services.MediaServiceConnector
import com.ocwvar.darkpurple.Units.BaseActivity
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import java.lang.ref.WeakReference

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/29 10:08 PM
 * File Location com.ocwvar.darkpurple.Activities
 * This file use to :   主界面框架，负责承载页面的切换操作
 */
@SuppressLint("NewApi")
class MainFrameworkActivity : BaseActivity() {

    private lateinit var songButton: View
    private lateinit var playlistButton: View
    private lateinit var userButton: View
    private lateinit var headShower: ImageView
    private lateinit var headCoverShower: ImageView
    private lateinit var headMusicTitle: TextView
    private lateinit var headMusicArtist: TextView
    private lateinit var headMusicAlbum: TextView
    private lateinit var floatingActionButton: FloatingActionButton

    private var musicPageKeeper: WeakReference<MusicListFragment?> = WeakReference(null)
    private var playlistPageKeeper: WeakReference<PlaylistFragment?> = WeakReference(null)
    private var userPagekeeper: WeakReference<UserFragment?> = WeakReference(null)

    private var currentPageTAG: Any? = null
    private val serviceCallbacks: ServiceCallbacks = ServiceCallbacks()
    private var blurCoverUpdateReceiver: BlurCoverUpdateReceiver = BlurCoverUpdateReceiver()
    private var playingDataUpdateReceiver: PlayingDataUpdateReceiver = PlayingDataUpdateReceiver()

    //请求权限用的Snackbar
    private lateinit var requestPermission: Snackbar

    //服务连接器
    private lateinit var serviceConnector: MediaServiceConnector

    override fun onPreSetup(): Boolean {
        if (AppConfigs.OS_5_UP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = AppConfigs.Color.WindowBackground_Color
        }
        return true
    }

    override fun setActivityView(): Int {
        return R.layout.activity_framework
    }

    override fun onSetToolBar(): Int {
        return 0
    }

    override fun onSetupViews(savedInstanceState: Bundle?) {

        this.requestPermission = Snackbar.make(findViewById(android.R.id.content), R.string.ERROR_Permission, Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(AppConfigs.Color.getColor(R.color.colorSecond))
                .setAction(R.string.text_snackbar_request_permission_button, {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //如果应用还可以请求权限,则弹出请求对话框
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 9)
                    } else {
                        //如果用户选择了不再提醒,则不弹出请求对话框,直接跳转到设置界面
                        val packageURI = Uri.parse("package:" + AppConfigs.ApplicationContext.packageName)
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI)
                        startActivity(intent)
                    }
                })

        this.serviceConnector = MediaServiceConnector(this@MainFrameworkActivity, serviceCallbacks)

        this.headShower = findViewById(R.id.MainMusic_headShower) as ImageView
        this.headCoverShower = findViewById(R.id.MainMusic_coverShower) as ImageView
        this.headMusicTitle = findViewById(R.id.MainMusic_title) as TextView
        this.headMusicArtist = findViewById(R.id.MainMusic_artist) as TextView
        this.headMusicAlbum = findViewById(R.id.MainMusic_album) as TextView
        this.floatingActionButton = findViewById(R.id.fab) as FloatingActionButton
        this.songButton = findViewById(R.id.button_song)
        this.playlistButton = findViewById(R.id.button_playlist)
        this.userButton = findViewById(R.id.button_user)

        this.songButton.setOnClickListener(this@MainFrameworkActivity)
        this.playlistButton.setOnClickListener(this@MainFrameworkActivity)
        this.userButton.setOnClickListener(this@MainFrameworkActivity)

        this.floatingActionButton.setOnClickListener {
            //点击主界面上的Floating Action Button事件
            val currentState: Int = serviceConnector.currentState()
            if (currentState != PlaybackStateCompat.STATE_NONE && currentState != PlaybackStateCompat.STATE_ERROR) {
                //转跳到播放界面
                startActivity(Intent(this@MainFrameworkActivity, PlayingActivity::class.java))
            }
        }

        //尝试获取上一次的页面位置
        val lastPage: String? = savedInstanceState?.getString("LastPage", null)
        lastPage?.let {
            when (it) {
                songButton.tag as String -> {
                    onViewClick(songButton)
                }
                playlistButton.tag as String -> {
                    onViewClick(playlistButton)
                }
                userButton.tag as String -> {
                    onViewClick(userButton)
                }
            }
        }
        //默认启动页面
        if (lastPage == null) {
            onViewClick(songButton)
        }
    }

    override fun onResume() {
        super.onResume()
        //检查文件读写权限
        if (AppConfigs.OS_6_UP && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermission.show()
        } else if (AppConfigs.OS_6_UP && requestPermission.isShown) {
            requestPermission.dismiss()
        }

        //如果服务未连接，不显示播放界面按钮，等待服务连接后，在进行检查播放状态。
        if (!serviceConnector.isServiceConnected()) {
            floatingActionButton.visibility = View.GONE
        }

        //连接媒体服务
        this.serviceConnector.connect()

        //获取当前正在使用的媒体数据
        val playingSong: SongItem? = MediaLibrary.getUsingMedia()

        //更新当前的头部图像
        if (CoverProcesser.getLastCompletedCoverID() == playingSong?.coverID) {
            //当前生成的图像与当前相同
            updateHeaderDrawable(CoverProcesser.getBlur(), CoverProcesser.getOriginal())
        } else {
            updateHeaderDrawable(null, null)
        }

        //更新当前的播放曲目文字
        updateHeaderMessage(playingSong)

        //检查封面处理广播接收器
        if (!blurCoverUpdateReceiver.isRegistered) {
            blurCoverUpdateReceiver.isRegistered = true
            registerReceiver(blurCoverUpdateReceiver, blurCoverUpdateReceiver.intentFilter)
        }

        //检查数据广播接收器
        if (!playingDataUpdateReceiver.isRegistered) {
            playingDataUpdateReceiver.isRegistered = true
            registerReceiver(playingDataUpdateReceiver, playingDataUpdateReceiver.intentFilter)
        }


    }

    override fun onStop() {
        super.onStop()
        //断开媒体服务
        serviceConnector.disConnect()
    }

    override fun onPause() {
        super.onPause()
        //页面暂停时停止接收模糊处理的广播
        if (blurCoverUpdateReceiver.isRegistered) {
            blurCoverUpdateReceiver.isRegistered = false
            unregisterReceiver(blurCoverUpdateReceiver)
        }
        //页面暂停时停止接收歌曲信息切换广播
        if (playingDataUpdateReceiver.isRegistered) {
            playingDataUpdateReceiver.isRegistered = false
            unregisterReceiver(playingDataUpdateReceiver)
        }
    }

    /**
     * 保存上一次的页面位置
     */
    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (outState != null && currentPageTAG != null) {
            try {
                outState.putString("LastPage", currentPageTAG as String)
            } catch(e: Exception) {
                outState.putString("LastPage", null)
            }
        }
    }

    override fun onViewClick(clickedView: View) {
        if (clickedView.tag == currentPageTAG) {
            return
        }
        when (clickedView.tag) {
            songButton.tag -> {
                songButton.alpha = 1.0f
                playlistButton.alpha = 0.3f
                userButton.alpha = 0.3f
            }

            playlistButton.tag -> {
                songButton.alpha = 0.3f
                playlistButton.alpha = 1.0f
                userButton.alpha = 0.3f
            }

            userButton.tag -> {
                songButton.alpha = 0.3f
                playlistButton.alpha = 0.3f
                userButton.alpha = 1.0f
            }
        }
        currentPageTAG = clickedView.tag
        switchPage(currentPageTAG)
    }

    override fun onViewLongClick(holdedView: View): Boolean = false

    /**
     * 切换页面
     * @param   pageTAG 要切换的页面TAG
     */
    private fun switchPage(pageTAG: Any?) {
        if (pageTAG == null) {
            return
        }
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(R.anim.fragment_anim_in, R.anim.fragment_anim_out)
        when (pageTAG) {
            songButton.tag -> {
                var page: MusicListFragment? = musicPageKeeper.get()
                if (page == null) {
                    page = MusicListFragment()
                    musicPageKeeper = WeakReference(page)
                }
                fragmentTransaction.replace(R.id.fragmentWindow, page)
            }
            playlistButton.tag -> {
                var page: PlaylistFragment? = playlistPageKeeper.get()
                if (page == null) {
                    page = PlaylistFragment()
                    playlistPageKeeper = WeakReference(page)
                }
                fragmentTransaction.replace(R.id.fragmentWindow, page)
            }
            userButton.tag -> {
                var page: UserFragment? = userPagekeeper.get()
                if (page == null) {
                    page = UserFragment()
                    userPagekeeper = WeakReference(page)
                }
                fragmentTransaction.replace(R.id.fragmentWindow, page)
            }
        }
        fragmentTransaction.commit()
    }

    /**
     * 更新首页头部图像
     * @param   blurDrawable    要更新图像的模糊Drawable对象，如果没有封面图像则传入NULL
     * @param   originalDrawable    要更新图像的清晰Drawable对象，如果没有封面图像则传入NULL
     */
    private fun updateHeaderDrawable(blurDrawable: Drawable?, originalDrawable: Drawable?) {
        //处理头部模糊背景部分的图像加载
        val animeDrawable: TransitionDrawable
        if (blurDrawable == null) {
            if (Build.VERSION.SDK_INT >= 22) {
                animeDrawable = TransitionDrawable(arrayOf(headShower.drawable, this@MainFrameworkActivity.getDrawable(R.drawable.blur)))
            } else {
                @Suppress("DEPRECATION")
                animeDrawable = TransitionDrawable(arrayOf(headShower.drawable, this@MainFrameworkActivity.resources.getDrawable(R.drawable.blur)))
            }
        } else {
            animeDrawable = TransitionDrawable(arrayOf(headShower.drawable, blurDrawable))
        }
        animeDrawable.isCrossFadeEnabled = true
        headShower.setImageDrawable(animeDrawable)

        //处理头部封面部分的图像加载和动画
        headCoverShower.setImageDrawable(originalDrawable)

        //开始处理封面切换动画
        if (originalDrawable != null) {
            //只有当存在封面图像的时候才进行动画设置
            val anim: Animation = AnimationUtils.loadAnimation(AppConfigs.ApplicationContext, R.anim.fragment_anim_in)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    //当封面动画执行完后再执行背景切换动画
                    animeDrawable.startTransition(300)
                    headCoverShower.clearAnimation()
                }

                override fun onAnimationStart(animation: Animation?) {
                }
            })
            //先开始执行封面动画，避免UI卡顿
            headCoverShower.startAnimation(anim)
        } else {
            //不存在封面图像，则直接开始背景切换动画
            animeDrawable.startTransition(300)
        }
    }

    /**
     * 更新首页头部歌曲信息
     * @param   songData    要更新的音频数据，如果没有数据则传入NULL
     */
    @SuppressLint("SetTextI18n")
    private fun updateHeaderMessage(songData: SongItem?) {
        if (songData == null) {
            //显示默认数据
            headMusicAlbum.text = ""
            headMusicArtist.text = ""
            headMusicTitle.text = "#Project DarkPurple"
        } else {
            headMusicAlbum.text = songData.album
            headMusicArtist.text = songData.artist
            headMusicTitle.text = songData.title
        }
    }

    /**
     * 标准的接收封面模糊处理结果的广播接收器
     */
    private inner class BlurCoverUpdateReceiver : BroadcastReceiver() {

        val intentFilter: IntentFilter = IntentFilter()
        var isRegistered: Boolean = false

        init {
            intentFilter.addAction(CoverProcesser.ACTION_BLUR_UPDATED)
            intentFilter.addAction(CoverProcesser.ACTION_BLUR_UPDATE_FAILED)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            //无效Intent，不做任何处理
            intent ?: return

            when (intent.action) {
            //有新的封面模糊图像产生
                CoverProcesser.ACTION_BLUR_UPDATED -> {
                    updateHeaderDrawable(CoverProcesser.getBlur(), CoverProcesser.getOriginal())
                }
            //有新的封面模糊图像发生失败
                CoverProcesser.ACTION_BLUR_UPDATE_FAILED -> {
                    updateHeaderDrawable(null, null)
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
                    floatingActionButton.visibility = View.GONE
                }

                ICore.ACTIONS.CORE_ACTION_READY -> {
                    updateHeaderMessage(MediaLibrary.getUsingMedia())
                    floatingActionButton.visibility = View.GONE
                }

                ICore.ACTIONS.CORE_ACTION_PLAYING -> {
                    //通知更新AudioSession ID
                    serviceConnector.sendCommand(MediaPlayerService.COMMAND.COMMAND_UPDATE_AUDIO_SESSION_ID, null)
                    updateHeaderMessage(MediaLibrary.getUsingMedia())
                    floatingActionButton.show()
                }

                ICore.ACTIONS.CORE_ACTION_RELEASE -> {
                    updateHeaderMessage(null)
                    updateHeaderDrawable(null, null)
                    floatingActionButton.visibility = View.GONE
                }

            }
        }

    }

    /**
     * 媒体服务连接状态回调处理器
     */
    private inner class ServiceCallbacks : MediaServiceConnector.Callbacks {

        /**
         *  媒体服务连接成功
         */
        override fun onServiceConnected() {
            //服务连接后检查当前播放的状态
            if (serviceConnector.currentState() == PlaybackStateCompat.STATE_PLAYING) {
                floatingActionButton.show()
            } else {
                floatingActionButton.visibility = View.GONE
            }
        }

        /**
         *  媒体服务连接断开
         */
        override fun onServiceDisconnected() {
            floatingActionButton.visibility = View.GONE
        }

        /**
         *  无法连接媒体服务
         */
        override fun onServiceConnectionError() {
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
    }

}