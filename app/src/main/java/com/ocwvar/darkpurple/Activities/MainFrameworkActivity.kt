package com.ocwvar.darkpurple.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.view.View
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.FragmentPages.MusicListFragment
import com.ocwvar.darkpurple.FragmentPages.ui.PlaylistPageFragment
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.BaseActivity

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

    private var musicPage: MusicListFragment? = null
    private var playlistPage: PlaylistPageFragment? = null
    private var currentPageTAG: Any? = null

    //请求权限用的Snackbar
    private lateinit var requestPermission: Snackbar

    override fun onPreSetup(): Boolean {
        if (AppConfigs.OS_5_UP) {
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
                .setActionTextColor(AppConfigs.ApplicationContext.resources.getColor(R.color.colorSecond))
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
        songButton = findViewById(R.id.button_song)
        playlistButton = findViewById(R.id.button_playlist)
        userButton = findViewById(R.id.button_user)
        songButton.setOnClickListener(this@MainFrameworkActivity)
        playlistButton.setOnClickListener(this@MainFrameworkActivity)
        userButton.setOnClickListener(this@MainFrameworkActivity)
        onViewClick(songButton)
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermission.show()
        } else if (requestPermission.isShown) {
            requestPermission.dismiss()
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
}