package com.ocwvar.darkpurple.Activities.MainFramework

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.ocwvar.darkpurple.Activities.MusicPlaying.MusicPlayingActivity
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R
import kotlinx.android.synthetic.main.activity_framework.*
import kotlinx.android.synthetic.main.item_button_playlist.*
import kotlinx.android.synthetic.main.item_button_song.*
import kotlinx.android.synthetic.main.item_button_user.*
import java.lang.ref.WeakReference

@SuppressLint("NewApi")
/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-9-2 下午6:58
 * File Location com.ocwvar.darkpurple.Activities.MainFramework
 * This file use to :
 */
class FrameworkActivity : AppCompatActivity(), IFrameViews, View.OnClickListener {

    //逻辑处理控制层
    private val iFramePresenter: IFramePresenter by lazy { FrameworkPresenter(this@FrameworkActivity) }

    //请求权限SnackBar
    private val permissionSnackBar: Snackbar by lazy {
        Snackbar.make(fragmentContainer(), R.string.ERROR_Permission, Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(AppConfigs.Color.getColor(R.color.colorSecond))
                .setAction(R.string.text_snackbar_request_permission_button, {
                    permissionSnackBar.dismiss()

                    if (!this@FrameworkActivity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //如果应用还可以请求权限,则弹出请求对话框
                        this@FrameworkActivity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 9)
                    } else {
                        //如果用户选择了不再提醒,则不弹出请求对话框,直接跳转到设置界面
                        val packageURI = Uri.parse("package:" + AppConfigs.ApplicationContext.packageName)
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI)
                        this@FrameworkActivity.startActivity(intent)
                    }
                })
    }

    //动画对象若连接容器
    private var headerBackgroundAnim: WeakReference<Animation?> = WeakReference(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //单独设定页面的状态栏和导航栏样式
        window.statusBarColor = AppConfigs.Color.StatusBar_color
        window.navigationBarColor = AppConfigs.Color.NavBar_Color

        //设置布局
        setContentView(R.layout.activity_framework)

        //设置点击事件
        fab.setOnClickListener(this@FrameworkActivity)
        button_song.setOnClickListener(this@FrameworkActivity)
        button_playlist.setOnClickListener(this@FrameworkActivity)
        button_user.setOnClickListener(this@FrameworkActivity)

        //默认切换到歌曲列表
        iFramePresenter.onSwitchPage(IFrameViews.PageName.主界面)

        //设置默认的TAG
        FrameworkHeaderBackground.tag = ""
        FrameworkHeaderCover.tag = ""
    }

    override fun onClick(view: View?) {
        view ?: return

        when (view.id) {

            R.id.fab -> {
                //转跳到播放界面
                startActivity(Intent(this@FrameworkActivity, MusicPlayingActivity::class.java))
            }

            R.id.button_song -> {
                iFramePresenter.onSwitchPage(IFrameViews.PageName.主界面)
            }

            R.id.button_playlist -> {
                iFramePresenter.onSwitchPage(IFrameViews.PageName.播放列表)
            }

            R.id.button_user -> {
                iFramePresenter.onSwitchPage(IFrameViews.PageName.设置)
            }

        }
    }

    /**
     * 更新标题文字
     *
     * @param text  标题，NULL = 留空
     */
    override fun onUpdateTitle(text: String?) {
        FrameworkHeaderTitle.text = text
    }

    /**
     * 更新专辑文字
     *
     * @param text  标题，NULL = 留空
     */
    override fun onUpdateAlbum(text: String?) {
        FrameworkHeaderAlbum.text = text
    }

    /**
     * 更新作家文字
     *
     * @param text  标题，NULL = 留空
     */
    override fun onUpdateArtist(text: String?) {
        FrameworkHeaderArtist.text = text
    }

    /**
     * 更新顶部背景图像
     *
     * @param drawable  图像Drawable
     * @param tag  图像TAG，如果是相同的TAG，则不更新图像
     */
    override fun onUpdateBackground(drawable: Drawable, tag: String) {
        if (FrameworkHeaderBackground.tag ?: "" != tag) {
            FrameworkHeaderBackground.tag = tag
            FrameworkHeaderBackground.background = drawable
            FrameworkHeaderBackground.startAnimation(getHeaderBackgroundAnim())
        }
    }

    /**
     * 更新顶部封面
     *
     * @param drawable 封面Drawable，NULL = 不显示
     * @param tag  图像TAG，如果是相同的TAG，则不更新图像
     */
    override fun onUpdateCover(drawable: Drawable?, tag: String) {
        if (FrameworkHeaderCover.tag ?: "" != tag) {
            FrameworkHeaderCover.tag = tag
            FrameworkHeaderCover.setImageDrawable(null)
            FrameworkHeaderCover.setImageDrawable(drawable)
        }
    }

    /**
     * 更新底部按钮状态
     *
     * @param pageName  激活的按钮名称
     */
    override fun onUpdateFragmentButtonState(pageName: IFrameViews.PageName) {
        button_user.alpha = 0.5f
        button_playlist.alpha = 0.5f
        button_song.alpha = 0.5f

        when (pageName) {

            IFrameViews.PageName.主界面 -> {
                button_song.alpha = 1.0f
            }

            IFrameViews.PageName.播放列表 -> {
                button_playlist.alpha = 1.0f
            }

            IFrameViews.PageName.设置 -> {
                button_user.alpha = 1.0f
            }

        }
    }

    /**
     * 缺失权限
     */
    override fun onLeakPermission() {
        if (!this.permissionSnackBar.isShown) {
            this.permissionSnackBar.show()
        }
    }

    /**
     * 切换是否显示正在播放按钮
     *
     * @param   enable  是否显示
     */
    override fun onSwitchPlayingButton(enable: Boolean) {
        if (enable) {
            fab.visibility = View.VISIBLE
        } else {
            fab.visibility = View.GONE
        }
    }

    /**
     * @return  负责显示的Activity
     */
    override fun activity(): AppCompatActivity = this@FrameworkActivity

    /**
     * @return  负责加载Fragment的FrameLayout容器
     */
    override fun fragmentContainer(): FrameLayout = FragmentContainer

    override fun onResume() {
        super.onResume()
        iFramePresenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        iFramePresenter.onPause()
    }

    /**
     * @return 控件动画
     */
    private fun getHeaderBackgroundAnim(): Animation {
        var anim: Animation? = headerBackgroundAnim.get()
        return if (anim != null) {
            anim
        } else {
            anim = AnimationUtils.loadAnimation(this@FrameworkActivity, R.anim.framework_anim_header)
            headerBackgroundAnim = WeakReference(anim)
            anim
        }
    }

}