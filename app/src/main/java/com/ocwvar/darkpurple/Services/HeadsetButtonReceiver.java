package com.ocwvar.darkpurple.Services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

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

        if (AppConfigs.isListenMediaButton && audioService != null) {
            //如果用户允许监听耳机多媒体按钮  同时  音频服务还存活 , 则响应操作
            audioService.onMediaButtonPress(intent);
        } else if (AppConfigs.isListenMediaButton && audioService == null) {
            //如果用户允许监听耳机多媒体按钮  但音频服务已经死亡 , 则重新创建音频对象
            ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    //当服务连接上的时候

                    if (iBinder != null) {
                        //获取服务对象
                        AudioService service = ((AudioService.ServiceObject) iBinder).getService();
                        if (service != null) {
                            //如果获取服务成功 , 则保存到全局储存器中 , 然后解除绑定
                            ServiceHolder.getInstance().setService(service);
                            audioService = service;
                        }
                        AppConfigs.ApplicationContext.unbindService(this);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    //当服务断开连接的时候 , 将全局储存器中的对象置为 NULL
                    ServiceHolder.getInstance().setService(null);
                    audioService = null;
                }
            };
            AppConfigs.ApplicationContext.startService(new Intent(AppConfigs.ApplicationContext, AudioService.class));
            AppConfigs.ApplicationContext.bindService(new Intent(AppConfigs.ApplicationContext, AudioService.class), serviceConnection, Context.BIND_AUTO_CREATE);

            audioService.onMediaButtonPress(intent);
        }

    }

}
