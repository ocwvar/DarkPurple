package com.ocwvar.darkpurple.Activities

import android.annotation.SuppressLint
import android.support.v4.app.FragmentTransaction
import android.view.View
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.FragmentPages.ui.AllMusicFragment
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

    private var songPage: AllMusicFragment? = null
    private var playlistPage: PlaylistPageFragment? = null
    private var currentPageTAG: Any? = null

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
        songButton = findViewById(R.id.button_song)
        playlistButton = findViewById(R.id.button_playlist)
        userButton = findViewById(R.id.button_user)
        songButton.setOnClickListener(this@MainFrameworkActivity)
        playlistButton.setOnClickListener(this@MainFrameworkActivity)
        userButton.setOnClickListener(this@MainFrameworkActivity)
        onViewClick(songButton)
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
                if (songPage == null) {
                    songPage = AllMusicFragment()
                }
                fragmentTransaction.replace(R.id.fragmentWindow, songPage)
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