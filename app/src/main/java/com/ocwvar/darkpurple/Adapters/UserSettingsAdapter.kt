package com.ocwvar.darkpurple.Adapters

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-8 下午6:35
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   用户设置界面适配器
 */
class UserSettingsAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val array: ArrayList<Setting> = ArrayList()

    init {
        initSettingData()
    }

    /**
     * 重新加载设置选项
     */
    fun initSettingData() {
        if (array.size > 0) {
            //如果列表里已有项目，则先全部清除
            array.clear()
        }
        //在这里添加设置条目
        if (!TextUtils.isEmpty(AppConfigs.USER.USERNAME)) {
            //如果当前已登录
            array.add(Setting(R.string.text_button_setting_userInfo, R.drawable.ic_action_user_info, true, false))
            array.add(Setting(R.string.text_button_setting_logout, R.drawable.ic_action_exit, true, false))
        } else {
            //未登录
            array.add(Setting(R.string.text_button_setting_login, R.drawable.ic_action_login, false, false))
            array.add(Setting(R.string.text_button_setting_register, R.drawable.ic_action_register, false, false))
        }
        array.add(Setting(R.string.text_button_setting_music_scanning, R.drawable.ic_action_folder, true, false))
        array.add(Setting(R.string.text_button_setting_music_sort, R.drawable.ic_action_sort, true, false))
        array.add(Setting(R.string.text_button_setting_other, R.drawable.ic_action_setting, true, false))
        array.add(Setting(R.string.text_button_setting_core, R.drawable.ic_action_core, true, false))
        array.add(Setting(R.string.text_button_setting_about, R.drawable.ic_action_about, true, false))
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (array[position].notShow) {
            //不显示返回 1
            return 1
        } else {
            return 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        if (viewType == 1) return null
        parent?.let {
            return UserSettingsVH(LayoutInflater.from(it.context).inflate(R.layout.item_user_setting, it, false))
        }
        return null
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder ?: return
        val data: Setting = array[position]
        val name: TextView = (holder as UserSettingsVH).name

        name.setText(data.strRes)
        name.setCompoundDrawablesWithIntrinsicBounds(data.iconRes, 0, 0, 0)

        if (!data.available) {
            name.alpha = 0.3f
        }
    }

    override fun getItemCount(): Int = array.size

    interface Callback {

        /**
         * 选项点击时的回调接口
         * @param   strRes    选项名称
         */
        fun onListClick(strRes: Int)

    }

    /**
     * 设置条目数据类
     * @param   strRes    选项的名称资源
     * @param   iconRes 选项的图标资源
     * @param   available   选择是否可用，不可用将会显示为半透明状态
     * @param   notShow 选项是否显示
     */
    private data class Setting(@StringRes val strRes: Int, @DrawableRes val iconRes: Int, val available: Boolean, val notShow: Boolean)

    private inner class UserSettingsVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val name: TextView = itemView as TextView

        init {
            itemView.setOnClickListener {
                val data: Setting = array[adapterPosition]
                if (data.available) {
                    //只有在选项可用的时候才进行回调
                    callback.onListClick(data.strRes)
                }
            }
        }

    }

}