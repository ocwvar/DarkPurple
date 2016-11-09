package com.ocwvar.darkpurple.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.ocwvar.darkpurple.AppConfigs;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/11/7 13:09
 * File Location com.ocwvar.darkpurple.Services
 * 通话状态监听器中转接收器
 */

public class PhoneStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null && intent.getExtras() != null) {
            final String status = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            switch (status) {
                case "IDLE":
                    AppConfigs.ApplicationContext.sendBroadcast(new Intent(AudioService.PhoneStatusServiceReceiver.PHONE_STATUS_IDLE));
                    break;
                default:
                    AppConfigs.ApplicationContext.sendBroadcast(new Intent(AudioService.PhoneStatusServiceReceiver.PHONE_STATUS_BUSY));
                    break;
            }
        }

    }

}
