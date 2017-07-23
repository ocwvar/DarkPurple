package com.ocwvar.darkpurple.Units

import android.content.Context
import android.support.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.R
import java.lang.ref.WeakReference

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 5:16 PM
 * File Location com.ocwvar.darkpurple.Units
 * This file use to :   自定义Toast显示器
 */
object ToastMaker {

    //Toast布局缓存容器
    private var layoutKeeper: WeakReference<View?> = WeakReference(null)

    /**
     * 显示Toast
     * @param   resource 要显示的内容资源ID
     */
    fun show(@StringRes resource: Int) {
        show(AppConfigs.ApplicationContext.getString(resource))
    }

    /**
     * 显示Toast
     * @param   message 要显示的内容
     */
    fun show(message: String) {
        val context: Context = AppConfigs.ApplicationContext
        var layout: View? = layoutKeeper.get()

        if (layout == null) {
            layout = LayoutInflater.from(context).inflate(R.layout.toast_layout, null)
            layoutKeeper = WeakReference(layout)
        }
        layout?.let {
            (it.findViewById<TextView>(R.id.toast_message)).text = message
            val toast: Toast = Toast(context)
            toast.view = it
            toast.duration = Toast.LENGTH_SHORT
            toast.show()
        }
    }

}