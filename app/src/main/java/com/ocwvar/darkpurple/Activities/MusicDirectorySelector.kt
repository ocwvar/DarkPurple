package com.ocwvar.darkpurple.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import com.ocwvar.darkpurple.Adapters.MusicDirectoryAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.BaseBlurActivity
import com.ocwvar.darkpurple.Units.ToastMaker
import java.io.File
import java.util.*

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-11 下午7:19
 * File Location com.ocwvar.darkpurple.Activities
 * This file use to :   音乐目录选择
 */
class MusicDirectorySelector : BaseBlurActivity(), MusicDirectoryAdapter.Callback {

    private val adapter: MusicDirectoryAdapter = MusicDirectoryAdapter(this@MusicDirectorySelector)
    private val dirController: DirectoryController = DirectoryController()

    /**
     * 预先设置操作
     *
     * 执行顺序: 0

     * @return 是否进行接下来的操作 , False则会结束页面
     */
    override fun onPreSetup(): Boolean {
        //更新已选择的目录
        val data: ArrayList<String> = ArrayList()
        intent?.getStringArrayListExtra("Data")?.forEach { data.add(it) }
        adapter.updateSelected(data)

        return true
    }

    /**
     * 设置Activity使用的资源
     *
     * 执行顺序: 1

     * @return 布局资源
     */
    override fun setActivityView(): Int = R.layout.activity_directory_selector

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
        supportActionBar?.let {
            it.setHomeAsUpIndicator(R.drawable.ic_action_done)
            it.setDisplayHomeAsUpEnabled(true)
        }

        //列表控件配置
        (findViewById(R.id.recycleView) as RecyclerView).let {
            it.layoutManager = LinearLayoutManager(this@MusicDirectorySelector, LinearLayoutManager.VERTICAL, false)
            it.adapter = adapter
        }

        //首次更新数据
        adapter.updateSource(dirController.currentDirectory())
    }

    override fun onResume() {
        super.onResume()
        toolBar?.title = String.format("%s: %d", AppConfigs.ApplicationContext.getString(R.string.title_selected_directory), adapter.getSelectedDirs().size)
    }

    /**
     * 控件的点击事件

     * @param clickedView 被点击的控件
     */
    override fun onViewClick(clickedView: View) {
    }

    /**
     * 点击左上角按钮(home)，返回上一个界面，并返回数据
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            android.R.id.home -> {
                //获取路径集合
                val result: ArrayList<String> = adapter.getSelectedDirs()

                //设置返回数据
                setResult(145, Intent().putExtra("Data", result))

                //结束页面
                finish()
            }

        }
        return true
    }

    /**
     * 点击目录回调接口
     * @param   item    目录对象
     */
    override fun onDirectoryClick(item: Directory) {
        adapter.updateSource(dirController.intoDirectory(item))
        if (adapter.itemCount <= 0) {
            //没有读取到数据
            ToastMaker.show(R.string.message_no_valid_directory)
        }
    }

    /**
     * 当用户选择的目录发生变化
     */
    override fun onSelectedDirsChanged() {
        //更新Toolbar文字
        toolBar?.title = String.format("%s: %d", AppConfigs.ApplicationContext.getString(R.string.title_selected_directory), adapter.getSelectedDirs().size)
    }

    /**
     * 长按的点击事件

     * @param holdedView 被点击的控件
     */
    override fun onViewLongClick(holdedView: View): Boolean = true

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            adapter.updateSource(dirController.leaveDirectory())
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 用于控制目录浏览的工具类
     */
    private inner class DirectoryController {

        //目录栈
        private val dirStack: ArrayList<Directory> = ArrayList()

        /**
         * 进入目录
         * @param   dir 目录对象
         * @return  目录下的子目录
         */
        fun intoDirectory(dir: Directory): ArrayList<Directory> {
            //新目录入栈
            this.dirStack.add(dir)

            return currentDirectory()
        }

        /**
         * 返回上一个目录
         * @return  目录下的子目录
         */
        fun leaveDirectory(): ArrayList<Directory> {
            if (this.dirStack.size - 1 >= 0) {
                //出栈目录
                this.dirStack.removeAt(this.dirStack.size - 1)
            }

            return currentDirectory()
        }

        /**
         * @return 当前目录下的子目录
         */
        fun currentDirectory(): ArrayList<Directory> {
            return if (this.dirStack.size == 0) {
                //如果当前目录栈是空的，则返回驱动器列表
                ArrayList<Directory>().let {
                    it.add(Directory(DirectoryStyle.DISK, AppConfigs.ApplicationContext.getString(R.string.type_directory_disk_default), Environment.getExternalStorageDirectory().path))
                    it.add(Directory(DirectoryStyle.DISK, AppConfigs.ApplicationContext.getString(R.string.type_directory_disk_storage), "/storage/"))
                    it
                }
            } else {
                searchDirectory(this.dirStack.last())
            }
        }

        /**
         * 搜索目录下的子目录
         * @return  子目录列表
         */
        private fun searchDirectory(dir: Directory): ArrayList<Directory> {
            val files: Array<File> = File(dir.path).listFiles()
            val result: ArrayList<Directory> = ArrayList()

            files.forEach {
                if (it.isDirectory && it.listFiles() != null) {
                    //只添加可以读取的目录
                    result.add(Directory(DirectoryStyle.DIR, it.name, it.path))
                }
            }

            //进行排序
            Collections.sort(result, { current, next ->
                current.name.toLowerCase().compareTo(next.name.toLowerCase())
            })

            return result
        }

    }

    enum class DirectoryStyle { DIR, DISK }

    data class Directory(val type: DirectoryStyle, val name: String, val path: String)

}