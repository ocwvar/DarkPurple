package com.ocwvar.darkpurple.Activities.MusicPlaying

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.ocwvar.darkpurple.Activities.EqualizerActivity
import com.ocwvar.darkpurple.Adapters.CoverShowerAdapter
import com.ocwvar.darkpurple.Adapters.SlidingListAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import com.ocwvar.darkpurple.Units.SpectrumAnimDisplay
import com.ocwvar.darkpurple.Units.ToastMaker
import com.ocwvar.darkpurple.widgets.CircleSeekBar
import java.lang.ref.WeakReference

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-26 下午5:22
 * File Location com.ocwvar.darkpurple.Activities.MusicPlaying
 * This file use to :   歌曲播放界面
 */
class MusicPlayingActivity : AppCompatActivity(), IPlayingViews, SlidingListAdapter.OnSlidingMenuClickCallback, ViewPager.OnPageChangeListener, View.OnClickListener, CoverShowerAdapter.OnCoverClickCallback {

    //数据控制层
    private val iPlayingPresenter: IPlayingPresenter = PlayingPresenter(this@MusicPlayingActivity)

    //频谱控制器
    private val spectrumDisplay: SpectrumAnimDisplay = SpectrumAnimDisplay()

    private lateinit var backgroundContainer: AppCompatImageView
    private lateinit var serviceDisconnectBackground: View
    private lateinit var randomButton: AppCompatImageView
    private lateinit var loopButton: AppCompatImageView
    private lateinit var spectrumSwitcher: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var seeker: CircleSeekBar
    private lateinit var coverShower: ViewPager
    private lateinit var spectrum: SurfaceView
    private lateinit var musicAlbum: TextView
    private lateinit var musicName: TextView
    private lateinit var pausedButton: View
    private lateinit var timer: TextView


    private var fadeInAnim: WeakReference<Animation?> = WeakReference(null)
    private var fadeOutAnim: WeakReference<Animation?> = WeakReference(null)

    override fun onCreate(savedInstanceState: Bundle?) {

        if (!AppConfigs.useCompatMode) {
            //不使用兼容模式

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

        } else {
            window.statusBarColor = Color.DKGRAY
            window.navigationBarColor = Color.DKGRAY
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playing_page)

        if (!AppConfigs.useCompatMode && AppConfigs.StatusBarHeight > 0) {
            //设置顶部间距高度
            val linearLayout = findViewById(R.id.pendingLayout_STATUSBAR) as LinearLayout
            val layoutParams = LinearLayout.LayoutParams(1, AppConfigs.StatusBarHeight)
            val emptyView = View(this@MusicPlayingActivity)
            emptyView.layoutParams = layoutParams
            linearLayout.addView(emptyView)
        }

        //ToolBar配置
        (findViewById(R.id.toolbar) as Toolbar).let {

            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_action_drawer)

        }

        //侧滑菜单配置
        (findViewById(R.id.recycleView) as RecyclerView).let {
            it.layoutManager = LinearLayoutManager(this@MusicPlayingActivity, LinearLayoutManager.VERTICAL, false)
            it.setHasFixedSize(true)
            it.adapter = SlidingListAdapter().let {
                it.setSongItems(MediaLibrary.getUsingLibrary())
                it.setCallback(this@MusicPlayingActivity)
                it
            }
        }

        //均衡器按钮配置
        findViewById(R.id.equalizer).setOnClickListener(this@MusicPlayingActivity)

        //频谱按钮配置
        this.spectrumSwitcher = (findViewById(R.id.spectrumSwitcher) as ImageButton).let {
            it.setOnClickListener(this@MusicPlayingActivity)
            it.tag = false
            it
        }

        //滑动条配置
        this.seeker = (findViewById(R.id.circleSeekBar) as CircleSeekBar).let {
            it.setTextType(CircleSeekBar.TEXT_TYPE_HIDE)
            it.setCallback(iPlayingPresenter)
            it
        }

        //侧滑抽屉配置
        this.drawerLayout = (findViewById(R.id.drawerLayout) as DrawerLayout).let {
            it.setScrimColor(Color.argb(220, 0, 0, 0))
            it
        }

        //封面轮播配置
        this.coverShower = (findViewById(R.id.coverShower) as ViewPager).let {
            it.addOnPageChangeListener(this@MusicPlayingActivity)
            it.adapter = CoverShowerAdapter(MediaLibrary.getUsingLibrary(), this@MusicPlayingActivity)
            it
        }

        //循环播放按钮配置
        this.loopButton = (findViewById(R.id.loop) as AppCompatImageView).let {
            it.setOnClickListener(this@MusicPlayingActivity)
            it.tag = AppConfigs.playMode_Loop
            if (it.tag as Boolean) {
                it.setImageResource(R.drawable.ic_action_loop_on)
            } else {
                it.setImageResource(R.drawable.ic_action_loop_off)
            }
            it
        }

        //随机播放按钮配置
        this.randomButton = (findViewById(R.id.random) as AppCompatImageView).let {
            it.setOnClickListener(this@MusicPlayingActivity)
            it.tag = AppConfigs.playMode_Random
            if (it.tag as Boolean) {
                it.setImageResource(R.drawable.ic_action_random_on)
            } else {
                it.setImageResource(R.drawable.ic_action_random_off)
            }
            it
        }

        //暂停按钮配置
        this.pausedButton = findViewById(R.id.shower_mainButton).let {
            it.tag = false
            it
        }

        //等待服务界面配置
        this.serviceDisconnectBackground = findViewById(R.id.waitForService).let {
            it.tag = false
            it
        }

        //配置频谱显示控件
        this.spectrum = (findViewById(R.id.surfaceView) as SurfaceView).let {
            it.holder.addCallback(spectrumDisplay)
            it.setZOrderOnTop(true)
            it
        }

        this.backgroundContainer = findViewById(R.id.contener) as AppCompatImageView
        this.musicName = findViewById(R.id.shower_title) as TextView
        this.musicAlbum = findViewById(R.id.shower_album) as TextView
        this.timer = findViewById(R.id.shower_playing_position) as TextView
    }

    /**
     * 侧滑菜单点击事件
     *
     * @param   songItem    歌曲对象
     * @param   position    歌曲索引
     */
    override fun onSlidingMenuClick(songItem: SongItem, position: Int) {
        this.iPlayingPresenter.playIndex(position, false)
        this.drawerLayout.closeDrawer(Gravity.START)
    }

    /**
     * 封面点击事件回调
     */
    override fun onCoverClick() {
        iPlayingPresenter.switchPlaybackState()
    }

    /**
     * 滚动封面轮播处理
     */
    override fun onPageScrollStateChanged(state: Int) {

        when (state) {

        //用户停止滑动封面
            ViewPager.SCROLL_STATE_IDLE -> {
                iPlayingPresenter.playIndex(coverShower.currentItem, true)
            }
        }

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {

    }

    /**
     * 点击事件回调
     */
    override fun onClick(clickView: View) {

        when (clickView.id) {

        //点击封面，进行暂停
            R.id.coverShower -> {
                iPlayingPresenter.switchPlaybackState()
            }

        //循环切换
            R.id.loop -> {
                iPlayingPresenter.switchLoopState(!(loopButton.tag as Boolean))
            }

        //随机切换
            R.id.random -> {
                iPlayingPresenter.switchRandomState(!(randomButton.tag as Boolean))
            }

        //打开均衡器
            R.id.equalizer -> {
                drawerLayout.closeDrawer(Gravity.START)
                EqualizerActivity.startBlurActivity(5, Color.argb(0, 0, 0, 0), false, this@MusicPlayingActivity, EqualizerActivity::class.java, null)
            }

        //切换频谱
            R.id.spectrumSwitcher -> {
                iPlayingPresenter.switchSpectrum(!(this.spectrumSwitcher.tag as Boolean))
            }

        }

    }

    /**
     * 点击左上角drawer图标事件
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        drawerLayout.openDrawer(Gravity.START)
        return true
    }

    /**
     * @return  实现了显示层的界面
     */
    override fun getActivity(): Activity = this@MusicPlayingActivity

    /**
     * @return  负责背景显示的控件
     */
    override fun getBackgroundContainer(): AppCompatImageView = this.backgroundContainer

    /**
     * 显示音乐恢复暂停动画
     */
    override fun onMusicPause() {
        if (!(this.pausedButton.tag as Boolean)) {
            this.iPlayingPresenter.switchSpectrum(false)
            this.pausedButton.visibility = View.VISIBLE
            this.pausedButton.startAnimation(getFadeInAnim())
            this.pausedButton.tag = true
        }
    }

    /**
     * 显示音乐恢复播放动画
     */
    override fun onMusicResume() {
        if (this.pausedButton.tag as Boolean) {
            this.pausedButton.visibility = View.GONE
            this.pausedButton.startAnimation(getFadeOutAnim())
            this.pausedButton.tag = false
        }
    }

    /**
     * 更新标题文字
     *
     * @param   title   文字
     */
    override fun onUpdateTitle(title: String) {
        setTitle(title)
    }

    /**
     * 更新歌曲名称显示
     *
     * @param   musicName   歌曲名称
     */
    override fun onUpdateMusicName(musicName: String) {
        this.musicName.text = musicName
    }

    /**
     * 更新歌曲专辑名称显示
     *
     * @param   musicAlbum   歌曲专辑
     */
    override fun onUpdateMusicAlbum(musicAlbum: String) {
        this.musicAlbum.text = musicAlbum
    }

    /**
     * 更新进度文字显示
     *
     * @param   timerText   进度文字
     */
    override fun onUpdateTimer(timerText: String) {
        this.timer.text = timerText
    }

    /**
     * 更新进度条
     *
     * @param   progress    进度值
     * @param   max    最大值
     */
    override fun onUpdateSeekBar(progress: Int, max: Int) {
        if (this.seeker.max != max) {
            this.seeker.max = max
        }
        this.seeker.setProgress(progress)
    }

    /**
     * 更新当前封面轮播显示的位置
     *
     * @param   position 歌曲的位置
     */
    override fun onUpdateShowingCover(position: Int) {
        this.coverShower.setCurrentItem(position, false)
    }

    /**
     * 更新循环播放按钮状态
     *
     *  @param  enable  是否启用
     */
    override fun onUpdateLoopState(enable: Boolean) {
        //只有在状态不符下才进行更新
        if (this.loopButton.tag != enable) {
            if (enable) {
                this.loopButton.setImageResource(R.drawable.ic_action_loop_on)
            } else {
                this.loopButton.setImageResource(R.drawable.ic_action_loop_off)
            }

            this.loopButton.tag = enable
        }
    }

    /**
     * 更新随机播放按钮状态
     *
     *  @param  enable  是否启用
     */
    override fun onUpdateRandomState(enable: Boolean) {
        //只有在状态不符下才进行更新
        if (this.randomButton.tag != enable) {
            if (enable) {
                this.randomButton.setImageResource(R.drawable.ic_action_random_on)
            } else {
                this.randomButton.setImageResource(R.drawable.ic_action_random_off)
            }

            this.randomButton.tag = enable
        }
    }

    /**
     * 显示频谱动画
     */
    override fun onShowSpectrum() {
        if (this.spectrumSwitcher.tag as Boolean) {
            //已开启，不需要重复操作
            return
        }

        //先停用控件
        this.spectrumSwitcher.isEnabled = false
        this.spectrumSwitcher.setImageResource(R.drawable.ic_action_sp_on)

        //显示频谱
        this.spectrum.visibility = View.VISIBLE
        this.spectrumDisplay.start()

        this.spectrumSwitcher.isEnabled = true
        this.spectrumSwitcher.tag = true
    }

    /**
     * 不显示频谱动画
     */
    override fun onHideSpectrum() {
        if (!(this.spectrumSwitcher.tag as Boolean)) {
            //已关闭，不需要重复操作
            return
        }

        //先停用控件
        this.spectrumSwitcher.isEnabled = false
        this.spectrumSwitcher.setImageResource(R.drawable.ic_action_sp_off)

        //显示频谱
        this.spectrumDisplay.stop()
        this.spectrum.visibility = View.GONE

        this.spectrumSwitcher.isEnabled = true
        this.spectrumSwitcher.tag = false
    }

    /**
     * 更新背景图像
     *
     * @param   imageDrawable   背景图像
     */
    override fun onUpdateBackground(imageDrawable: TransitionDrawable) {
        this.backgroundContainer.setImageDrawable(null)
        this.backgroundContainer.setImageDrawable(imageDrawable)

        imageDrawable.startTransition(300)
    }

    /**
     * 发生异常
     */
    override fun onError() {
        ToastMaker.show(R.string.message_playing_error)
        finish()
    }

    /**
     * 服务连接失败、断开时 显示画面
     */
    override fun onServiceDisconnect() {
        this.serviceDisconnectBackground.visibility = View.VISIBLE
        this.serviceDisconnectBackground.startAnimation(getFadeInAnim())
    }

    /**
     * 服务连接成功时 显示画面
     */
    override fun onServiceConnected() {
        this.serviceDisconnectBackground.visibility = View.GONE
        this.serviceDisconnectBackground.startAnimation(getFadeOutAnim())
    }

    /**
     * 关闭页面
     */
    override fun onRequireFinishActivity() {
        finish()
    }

    override fun onResume() {
        super.onResume()
        iPlayingPresenter.onActivityResume()
    }

    override fun onPause() {
        super.onPause()
        iPlayingPresenter.switchSpectrum(false)
        iPlayingPresenter.onActivityPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        iPlayingPresenter.onActivityDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                //如果点击返回按钮时，打开着侧滑菜单，则需要关闭
                if (drawerLayout.isDrawerOpen(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START)
                    return true
                }
                finish()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 获取淡入显示的动画
     */
    private fun getFadeInAnim(): Animation {
        var anim: Animation? = fadeInAnim.get()

        return if (anim == null) {
            anim = AlphaAnimation(0.0f, 1.0f).let {
                it.duration = 300L
                it.fillAfter = true
                it
            }
            fadeInAnim = WeakReference(anim)
            anim
        } else {
            anim
        }
    }

    /**
     * 获取淡出显示的动画
     */
    private fun getFadeOutAnim(): Animation {
        var anim: Animation? = fadeOutAnim.get()

        return if (anim == null) {
            anim = AlphaAnimation(1.0f, 0.0f).let {
                it.duration = 300L
                it
            }
            fadeOutAnim = WeakReference(anim)
            anim
        } else {
            anim
        }
    }

}