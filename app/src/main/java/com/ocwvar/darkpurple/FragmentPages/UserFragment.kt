package com.ocwvar.darkpurple.FragmentPages

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ocwvar.darkpurple.Adapters.UserSettingsAdapter
import com.ocwvar.darkpurple.R

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-8 下午6:16
 * File Location com.ocwvar.darkpurple.FragmentPages
 * This file use to :   用户设置界面
 */
class UserFragment : Fragment(), UserSettingsAdapter.Callback {

    private val adapter: UserSettingsAdapter = UserSettingsAdapter(this@UserFragment)
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
     * @param   strRes    选项名称
     */
    override fun onListClick(strRes: Int) {
    }

}