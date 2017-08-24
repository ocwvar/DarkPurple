package com.ocwvar.darkpurple.FragmentPages

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.media.session.MediaControllerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ocwvar.darkpurple.Activities.MusicDirectoryActivity
import com.ocwvar.darkpurple.Activities.SettingsActivity
import com.ocwvar.darkpurple.Adapters.UserSettingsAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.MediaPlayerService
import com.ocwvar.darkpurple.Units.MediaScanner
import java.lang.ref.WeakReference


/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-8 下午6:16
 * File Location com.ocwvar.darkpurple.FragmentPages
 * This file use to :   用户设置界面
 */
class UserFragment : Fragment(), UserSettingsAdapter.Callback {

    private val adapter: UserSettingsAdapter = UserSettingsAdapter(this@UserFragment)
    private var aboutDialogKeeper: WeakReference<AlertDialog?> = WeakReference(null)
    private lateinit var fragmentView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (inflater != null && container != null) {
            return inflater.inflate(R.layout.fragment_user_setting_list, container, false)
        } else {
            return null
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view ?: return
        this.fragmentView = view

        val recycleView: RecyclerView = fragmentView.findViewById(R.id.recycleView)
        recycleView.adapter = adapter
        recycleView.layoutManager = LinearLayoutManager(fragmentView.context, LinearLayoutManager.VERTICAL, false)
    }

    /**
     * 选项点击时的回调接口
     * @param   strRes    选项名称的资源地址
     */
    override fun onListClick(strRes: Int) {
        when (strRes) {
            R.string.text_button_setting_music_sort -> {
                //显示排序对话框
                showSortDialog()
            }
            R.string.text_button_setting_music_scanning -> {
                //扫描目录设置
                startActivityForResult(Intent(activity, MusicDirectoryActivity::class.java), 9)
            }
            R.string.text_button_setting_other -> {
                //其他设置
                startActivityForResult(Intent(activity, SettingsActivity::class.java), 10)
            }
            R.string.text_button_setting_about -> {
                //显示关于对话框
                showAboutDialog()
            }
        }
    }

    /**
     * 排序选择对话框
     * 此对话框对象不能使用弱引用，因为每次启动的时候都需要设置选择位置
     */
    private fun showSortDialog() {
        val sp = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext)
        val position: Int = sp.getString("scanner_sort_type", "0").toInt()
        @SuppressLint("CommitPrefEdits")
        val dialog: AlertDialog = AlertDialog.Builder(fragmentView.context, R.style.Dialog_FullScreen_NoBackground)
                .setSingleChoiceItems(R.array.sort_types_name, position) { dialog, which ->
                    //更新SP文件数据
                    val editor = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext).edit()
                    editor.putString("scanner_sort_type", which.toString())
                    editor.commit()

                    //重新读取所有项目数据
                    AppConfigs.reInitOptionValues()

                    //隐藏对话框
                    dialog.dismiss()
                }
                .create()
        dialog.show()
    }

    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        var dialog: AlertDialog? = aboutDialogKeeper.get()
        if (dialog == null) {
            dialog = AlertDialog.Builder(fragmentView.context, R.style.Dialog_FullScreen_NoBackground).setView(R.layout.dialog_about).create()
            aboutDialogKeeper = WeakReference(dialog)
        }
        aboutDialogKeeper.get()?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

        //更新路径
            9 -> {
                //更新媒体库信息
                MediaScanner.getInstance().start()
                MediaControllerCompat.getMediaController(activity)?.sendCommand(MediaPlayerService.COMMAND.COMMAND_RELEASE_CURRENT_MEDIA, null, null)
            }

        //更新全局设置
            10 -> {
                AppConfigs.reInitOptionValues()
            }

        }
    }

}