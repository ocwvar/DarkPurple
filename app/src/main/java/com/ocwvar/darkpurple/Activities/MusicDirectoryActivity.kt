package com.ocwvar.darkpurple.Activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.ocwvar.darkpurple.Adapters.MusicDirectoryDisplayAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.BaseBlurActivity

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-11 下午10:25
 * File Location com.ocwvar.darkpurple.Activities
 * This file use to :   显示音乐目录界面
 */
class MusicDirectoryActivity : BaseBlurActivity(), MusicDirectoryDisplayAdapter.Callback {

    private val adapter: MusicDirectoryDisplayAdapter = MusicDirectoryDisplayAdapter(this@MusicDirectoryActivity)

    /**
     * 预先设置操作
     *
     * 执行顺序: 0

     * @return 是否进行接下来的操作 , False则会结束页面
     */
    override fun onPreSetup(): Boolean = true

    /**
     * 设置Activity使用的资源
     *
     * 执行顺序: 1

     * @return 布局资源
     */
    override fun setActivityView(): Int = R.layout.activity_directory

    /**
     * 设置Activity使用的资源
     *
     * 执行顺序: 2

     * @return ToolBar 资源ID
     */
    override fun onSetToolBar(): Int = R.id.toolbar

    /**
     * 初始化控件的操作
     *
     * 执行顺序: 3

     * @param savedInstanceState
     */
    override fun onSetupViews(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        (findViewById(R.id.recycleView) as RecyclerView).let {
            it.layoutManager = LinearLayoutManager(this@MusicDirectoryActivity, LinearLayoutManager.VERTICAL, false)
            it.adapter = adapter
        }

        findViewById(R.id.fab).setOnClickListener(this@MusicDirectoryActivity)

        val data: ArrayList<String> = ArrayList()
        AppConfigs.MusicFolders?.forEach { data.add(it) }

        adapter.updateSource(data)
    }

    /**
     * 控件的点击事件

     * @param clickedView 被点击的控件
     */
    override fun onViewClick(clickedView: View) {
        //启动选择界面
        startActivityForResult(Intent(this@MusicDirectoryActivity, MusicDirectorySelector::class.java).let {
            it.putExtra("Data", adapter.source())
            it
        }, 123)
    }

    /**
     * 长按的点击事件

     * @param holdedView 被点击的控件
     */
    override fun onViewLongClick(holdedView: View): Boolean = true

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return true
    }

    /**
     * 请求移除路径
     *
     * @param   path    删除后剩下的路径集合
     */
    override fun onDelete(path: ArrayList<String>) {
        //更新路径
        AppConfigs.updatePathSet(path)
    }

    /**
     * Dispatch incoming result to the correct fragment.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && data != null && data.hasExtra("Data")) {
            //获取数据
            val result: ArrayList<String> = data.getStringArrayListExtra("Data")

            //更新显示
            adapter.updateSource(result)

            //更新路径
            AppConfigs.updatePathSet(result)
        }
    }
}