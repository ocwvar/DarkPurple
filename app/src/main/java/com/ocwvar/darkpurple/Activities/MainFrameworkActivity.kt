package com.ocwvar.darkpurple.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.FragmentPages.MusicListFragment
import com.ocwvar.darkpurple.FragmentPages.ui.PlaylistPageFragment
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.AudioService
import com.ocwvar.darkpurple.Services.ServiceHolder
import com.ocwvar.darkpurple.Units.BaseActivity
import com.ocwvar.darkpurple.Units.CoverProcesser
import com.ocwvar.darkpurple.Units.ToastMaker

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

    private var musicPage: MusicListFragment? = null
    private var playlistPage: PlaylistPageFragment? = null
    private var currentPageTAG: Any? = null
    private var blurCoverUpdateReceiver: BlurCoverUpdateReceiver = BlurCoverUpdateReceiver()
    private var playingDataUpdateReceiver: PlayingDataUpdateReceiver = PlayingDataUpdateReceiver()

    //请求权限用的Snackbar
    private lateinit var requestPermission: Snackbar

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

    override fun onSetupViews() {
        requestPermission = Snackbar.make(findViewById(android.R.id.content), R.string.ERROR_Permission, Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(AppConfigs.Color.getColor(R.color.colorSecond))
                .setAction(R.string.request_permission_button, {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //如果应用还可以请求权限,则弹出请求对话框
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 9)
                    } else {
                        //如果用户选择了不再提醒,则不弹出请求对话框,直接跳转到设置界面
                        val packageURI = Uri.parse("package:" + AppConfigs.ApplicationContext.packageName)
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI)
                        startActivity(intent)
                    }
                })
        headShower = findViewById(R.id.MainMusic_headShower) as ImageView
        headCoverShower = findViewById(R.id.MainMusic_coverShower) as ImageView
        headMusicTitle = findViewById(R.id.MainMusic_title) as TextView
        headMusicArtist = findViewById(R.id.MainMusic_artist) as TextView
        headMusicAlbum = findViewById(R.id.MainMusic_album) as TextView
        songButton = findViewById(R.id.button_song)
        playlistButton = findViewById(R.id.button_playlist)
        userButton = findViewById(R.id.button_user)
        songButton.setOnClickListener(this@MainFrameworkActivity)
        playlistButton.setOnClickListener(this@MainFrameworkActivity)
        userButton.setOnClickListener(this@MainFrameworkActivity)

        //初始化音频服务
        onSetupService()

        //默认切换到音频列表
        onViewClick(songButton)
    }

    /**
     * 启动音频服务
     */
    private fun onSetupService() {
        if (ServiceHolder.getInstance().service == null) {
            //如果当前没有获取到服务对象 , 则创建一个保存
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder?) {
                    //当服务连接上的时候
                    if (iBinder != null) {
                        //获取服务对象
                        val service = (iBinder as AudioService.ServiceObject).service
                        if (service != null) {
                            //如果获取服务成功 , 则保存到全局储存器中 , 然后解除绑定
                            ServiceHolder.getInstance().service = service
                            ToastMaker.show(R.string.service_ok)
                        } else {
                            //否则提示用户
                            ToastMaker.show(R.string.ERROR_SERVICE_CREATE)
                        }
                        unbindService(this)
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName) {
                    //当服务断开连接的时候 , 将全局储存器中的对象置为 NULL
                    ServiceHolder.getInstance().service = null
                }
            }

            //开始连接服务
            val intent = Intent(this@MainFrameworkActivity, AudioService::class.java)
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        //检查文件读写权限
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermission.show()
        } else if (requestPermission.isShown) {
            requestPermission.dismiss()
        }
        //更新当前的头部图像
        updateHeaderDrawable(CoverProcesser.getBlur(), CoverProcesser.getOriginal())
        //检查封面处理广播接收器
        if (!blurCoverUpdateReceiver.isRegistered) {
            blurCoverUpdateReceiver.isRegistered = true
            registerReceiver(blurCoverUpdateReceiver, blurCoverUpdateReceiver.intentFilter)
        }
        //更新当前的播放数据
        updateHeaderMessage(ServiceHolder.getInstance().service?.playingSong)
        //检查数据广播接收器
        if (!playingDataUpdateReceiver.isRegistered) {
            playingDataUpdateReceiver.isRegistered = true
            registerReceiver(playingDataUpdateReceiver, playingDataUpdateReceiver.intentFilter)
        }
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

    override fun onViewLongClick(holdedView: View): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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
                if (musicPage == null) {
                    musicPage = MusicListFragment()
                }
                fragmentTransaction.replace(R.id.fragmentWindow, musicPage)
            }
            playlistButton.tag -> {
                if (playlistPage == null) {
                    playlistPage = PlaylistPageFragment()
                }
                fragmentTransaction.replace(R.id.fragmentWindow, playlistPage)
            }
            userButton.tag -> {
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
            animeDrawable = TransitionDrawable(arrayOf(headShower.drawable, ColorDrawable(AppConfigs.Color.DefaultCoverColor)))
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
    private fun updateHeaderMessage(songData: SongItem?) {
        if (songData == null) {
            //显示默认数据
            headMusicAlbum.text = ""
            headMusicArtist.text = ""
            headMusicTitle.text = "#DarkPurple"
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

        val intentFilter: IntentFilter = IntentFilter(AudioService.NOTIFICATION_UPDATE)
        var isRegistered: Boolean = false

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                AudioService.NOTIFICATION_UPDATE -> {
                    updateHeaderMessage(ServiceHolder.getInstance().service?.playingSong)
                }
            }
        }

    }

}