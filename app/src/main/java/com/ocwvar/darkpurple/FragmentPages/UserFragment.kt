package com.ocwvar.darkpurple.FragmentPages

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import com.ocwvar.darkpurple.Activities.FolderSelectorActivity
import com.ocwvar.darkpurple.Activities.SettingsActivity
import com.ocwvar.darkpurple.Adapters.UserSettingsAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.ServiceHolder
import com.ocwvar.darkpurple.Units.ActivityManager
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
    private var coreTypeDialogKeeper: WeakReference<CoreSelectorDialog?> = WeakReference(null)
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

        val recycleView: RecyclerView = fragmentView.findViewById(R.id.recycleView) as RecyclerView
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
                startActivity(Intent(activity, FolderSelectorActivity::class.java))
            }
            R.string.text_button_setting_other -> {
                //其他设置
                startActivityForResult(Intent(activity, SettingsActivity::class.java), 10)
            }
            R.string.text_button_setting_about -> {
                //显示关于对话框
                showAboutDialog()
            }
            R.string.text_button_setting_core -> {
                //设置播放核心
                if (coreTypeDialogKeeper.get() == null) {
                    coreTypeDialogKeeper = WeakReference(CoreSelectorDialog())
                }
                coreTypeDialogKeeper.get()?.show()
            }
        }
    }

    /**
     * 排序选择对话框
     * 此对话框对象不能使用弱引用，因为每次启动的时候都需要设置选择位置
     */
    fun showSortDialog() {
        val sp = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext)
        val position: Int = sp.getString("scanner_sort_type", "0").toInt()
        val dialog: AlertDialog = AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG)
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
    fun showAboutDialog() {
        var dialog: AlertDialog? = aboutDialogKeeper.get()
        if (dialog == null) {
            dialog = AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG).setView(R.layout.dialog_about).create()
            aboutDialogKeeper = WeakReference(dialog)
        }
        aboutDialogKeeper.get()?.show()
    }

    /**
     * 音频核心选择对话框处理类
     */
    private inner class CoreSelectorDialog {

        private var selectedPos: Int = -1

        fun show() {
            var view: View = LayoutInflater.from(fragmentView.context).inflate(R.layout.dialog_audio_core_type, null, false)
            view.findViewById(R.id.button_coreType_done).setOnClickListener {
                //点击确定按钮事件
                if (selectedPos in 0..2 && selectedPos != AppConfigs.audioCoreType) {
                    //如果选择的项目在0~2之间，同时发生了类型的改变
                    val spEditor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext).edit()
                    //应用设置
                    spEditor.putInt("audioCoreType", selectedPos).commit()
                    AppConfigs.reInitOptionValues()
                    //关闭现有服务
                    ServiceHolder.getInstance().service.closeService()
                    //重新启动界面
                    ActivityManager.getInstance().restartMainActivity()
                }
            }
            (view.findViewById(R.id.radioButton_coreType_bass) as RadioButton).setOnCheckedChangeListener { _, isChecked ->
                //BASS类型 点击事件
                if (isChecked) {
                    selectedPos = 0
                }
            }
            (view.findViewById(R.id.radioButton_coreType_exo) as RadioButton).setOnCheckedChangeListener { _, isChecked ->
                //EXO类型 点击事件
                if (isChecked) {
                    selectedPos = 1
                }
            }
            (view.findViewById(R.id.radioButton_coreType_compat) as RadioButton).setOnCheckedChangeListener { _, isChecked ->
                //Compat类型 点击事件
                if (isChecked) {
                    selectedPos = 2
                }
            }
            //选择已选定的条目
            selectCurrentCoreType(view)
            //显示对话框
            AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG).setView(view).show()
        }

        /**
         * 选择当下已选择的项目
         */
        private fun selectCurrentCoreType(view: View) {
            when (AppConfigs.audioCoreType) {
                0 -> {
                    //BASS
                    (view.findViewById(R.id.radioButton_coreType_bass) as RadioButton).isChecked = true
                }

                1 -> {
                    //EXO2
                    (view.findViewById(R.id.radioButton_coreType_exo) as RadioButton).isChecked = true
                }

                2 -> {
                    //Compat
                    (view.findViewById(R.id.radioButton_coreType_compat) as RadioButton).isChecked = true
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10) {
            AppConfigs.reInitOptionValues()
        }
    }

}