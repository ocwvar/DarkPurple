package com.ocwvar.darkpurple.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ocwvar.darkpurple.AppConfigs;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/21 13:17
 * File Location com.ocwvar.darkpurple.Services
 * 耳机按键广播接收器
 */
public class HeadsetButtonReceiver extends BroadcastReceiver {
    AudioService audioService;

    public HeadsetButtonReceiver() {
        audioService = ServiceHolder.getInstance().getService();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppConfigs.isListenMediaButton) {
            audioService.onMediaButtonPress(intent);
        }
    }

}
