package com.ocwvar.darkpurple.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-8 下午11:48
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   Android 4.4专用媒体按钮 广播接收器
 */
class KKMediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        p0 ?: return
        p1 ?: return

        p0.sendBroadcast(Intent("ROUTER").let {
            it.putExtra("EXTRA", p1)
        })
    }

}
