package com.ocwvar.darkpurple.Activities

import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.ocwvar.darkpurple.Adapters.CloudMusicAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Callbacks.NetworkCallbacks.OnGetUploadedFilesCallback
import com.ocwvar.darkpurple.Network.Beans.RemoteMusic
import com.ocwvar.darkpurple.Network.Keys
import com.ocwvar.darkpurple.Network.NetworkRequest
import com.ocwvar.darkpurple.Network.NetworkRequestTypes
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.BaseBlurActivity

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/23 3:12 PM
 * File Location com.ocwvar.darkpurple.Activities
 * This file use to :   我的云音乐显示界面
 */
class CloudMusicActivity : BaseBlurActivity(), OnGetUploadedFilesCallback {

    val adapter: CloudMusicAdapter = CloudMusicAdapter()
    val requestObject: HashMap<String, String> = HashMap()

    init {
        requestObject.put(Keys.Token, AppConfigs.USER.TOKEN)
    }

    override fun onPreSetup(): Boolean {
        title = AppConfigs.ApplicationContext.getString(R.string.text_cloudMusic_title)
        return true
    }

    override fun setActivityView(): Int {
        return R.layout.activity_cloud_music
    }

    override fun onSetToolBar(): Int {
        return R.id.toolbar
    }

    override fun onSetupViews() {
        val recycleView: RecyclerView = findViewById(R.id.recycleView) as RecyclerView
        recycleView.setHasFixedSize(true)
        recycleView.layoutManager = LinearLayoutManager(this@CloudMusicActivity, LinearLayoutManager.VERTICAL, false)
        recycleView.adapter = adapter

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        NetworkRequest.newRequest(NetworkRequestTypes.获取已上传文件, requestObject, this@CloudMusicActivity)
        showHoldingSnackBar(AppConfigs.ApplicationContext.getString(R.string.simple_loading))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onViewClick(clickedView: View?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onViewLongClick(holdedView: View?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onGotUploadedFiles(files: ArrayList<RemoteMusic>) {
        dismissHoldingSnackBar()
        toolBar?.subtitle = String.format("%s%s", AppConfigs.ApplicationContext.getString(R.string.text_cloudMusic_subTitle), files.size.toString())
        adapter.updateSource(files)
    }

    override fun onError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
        toolBar?.subtitle = message
    }
}