package com.ocwvar.darkpurple.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.ocwvar.darkpurple.Units.Logger;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/21 13:17
 * File Location com.ocwvar.darkpurple.Services
 */
public class HeadsetButtonReceiver extends BroadcastReceiver {
    private final String TAG = "耳机按钮";

    HeadsetButtonThread thread;
    AudioService audioService;

    public HeadsetButtonReceiver() {
        audioService = ServiceHolder.getInstance().getService();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case Intent.ACTION_MEDIA_BUTTON:
                if (intent.getExtras() != null && intent.hasExtra(Intent.EXTRA_KEY_EVENT)){
                    KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK && event.getAction() == KeyEvent.ACTION_DOWN){
                        //当用户按下耳机按钮的时候
                        Logger.warnning(TAG,"耳机按钮事件");
                        onButtonPass();
                    }
                }
                break;
        }
    }

    /**
     * 发生耳机按钮点击事件时 执行此方法
     * @return 成功增加计数的时候返回 True ，当计数达到 3 次的时候便不再能增加，此时返回 False 直到执行完此次事件后才能继续操作
     */
    protected boolean onButtonPass(){
        System.out.println(HeadsetButtonReceiver.this);
        if (thread == null || !thread.isAlive()){
            //如果当前是首次按下 ， 则启动线程
            thread = new HeadsetButtonThread();
            thread.start();
            return true;
        }else if (thread.isAlive()){
            //如果当前是第二次或者以上按下 ，则增加计数
            return thread.addCount();
        }else {
            return false;
        }
    }

    /**
     * 负责处理当前的耳机按钮事件的线程
     */
    class HeadsetButtonThread extends Thread{

        int count;

        public HeadsetButtonThread() {
            this.count = 1;
        }

        private boolean addCount(){
            if (count < 3){
                count += 1;
                interrupt();
                return true;
            }else {
                return false;
            }
        }

        @Override
        public void run() {
            super.run();
            while ( true ){
                Logger.warnning(TAG,"开始等待 Count:"+count);
                if (count < 3){
                    try {
                        Thread.sleep(50000);
                    } catch (InterruptedException e) {
                        Logger.warnning(TAG,"中断--新按钮事件 Count:"+count);
                        continue;
                    }
                    break;
                }else {
                    break;
                }
            }
            Logger.warnning(TAG,"开始执行 Count:"+count);
            if (audioService != null){
                switch (count){
                    case 1:
                        if (audioService.getAudioStatus() == AudioCore.AudioStatus.Paused){
                            audioService.resume();
                        }else {
                            audioService.pause();
                        }
                        break;
                    case 2:
                        audioService.playNext();
                        break;
                    case 3:
                        audioService.playPrevious();
                        break;
                }
            }else {
                Logger.warnning(TAG,"音频服务未启动 ， 执行失败");
            }
        }
    }

}
