package com.ocwvar.darkpurple.Services;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Units.Logger;
import com.un4seen.bass.BASS;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Services
 * Data: 2016/7/12 16:01
 * Project: DarkPurple
 * 音频播放处理
 */
public class AudioCore {
    private final String TAG = "音频引擎";

    private Context applicationContext;

    private int playingChannel = 0;
    private ArrayList<SongItem> songList;
    private int playingIndex = -1;

    public enum AudioStatus{
        Playing,
        Paused,
        Stopped,
        Empty,
        Error
    }

    protected AudioCore(Context applicationContext) {
        this.applicationContext = applicationContext;
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_LATENCY);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 0);
    }

    /**
     * 播放音频
     * @param songItem  音频信息集合
     * @param onlyInit  只加载音频 , 而不播放
     * @return  执行结果
     */
    private boolean playAudio(SongItem songItem , boolean onlyInit){
        if (playingChannel != 0 || BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_STOPPED){
            //如果当前仍有音频数据 , 则先释放旧的
            releaseAudio();
            //释放掉回调
            BASS.BASS_ChannelRemoveSync(playingChannel,BASS.BASS_SYNC_END);
            Logger.warnning(TAG , "已释放旧资源");
        }

        //加载音频数据
        playingChannel = initAudio(songItem.getPath());

        if (playingChannel != 0 ){
            //创建播放回调 , 使得能自动播放下一首
            initCallback(playingChannel);
            Logger.warnning(TAG , "音频资源已加载");
            if (onlyInit){
                return true;
            }else {
                return BASS.BASS_ChannelPlay(playingChannel,true);
            }
        }else {
            return false;
        }
    }

    /**
     * 注册音频播放结束回调
     * @param playingChannel    需要注册的音频数据
     */
    private void initCallback(int playingChannel){
        BASS.BASS_ChannelSetSync(playingChannel, BASS.BASS_SYNC_END, 0, new BASS.SYNCPROC() {
            @Override
            public void SYNCPROC(int handle, int channel, int data, Object user) {
                if (applicationContext != null){
                    Logger.warnning(TAG,"播放结束!");
                    applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_NEXT));
                }
            }
        },0);
        BASS.BASS_ChannelSetSync(playingChannel, BASS.BASS_SYNC_POS, 0, new BASS.SYNCPROC() {
            @Override
            public void SYNCPROC(int handle, int channel, int data, Object user) {

            }
        },0);
    }

    /**
     * 加载歌曲数据
     * @param path  歌曲文件路径
     * @return  返回歌曲的Channel数据 , 否则返回 0 表示失败
     */
    private int initAudio(String path){
        if (!TextUtils.isEmpty(path)){
            //路径不为空
            File audioFile = new File(path);
            if (audioFile.exists() && audioFile.length() > 0){
                audioFile = null;
                return BASS.BASS_StreamCreateFile(path,0,0,0);
            }
        }
        return 0;
    }

    /**
     * 获取当前音频播放的状态
     * @return  当前状态
     */
    protected AudioStatus getCurrectStatus(){
        if (playingChannel != 0){
            switch (BASS.BASS_ChannelIsActive(playingChannel)){
                case BASS.BASS_ACTIVE_PLAYING:
                    return AudioStatus.Playing;
                case BASS.BASS_ACTIVE_PAUSED:
                    return AudioStatus.Paused;
                case BASS.BASS_ACTIVE_STOPPED:
                    return AudioStatus.Stopped;
                default:
                    return AudioStatus.Error;
            }
        }else {
            return AudioStatus.Empty;
        }
    }

    /**
     * 得到当前已激活的歌曲
     * @return  有则返回歌曲集合 , 没有则返回 NULL
     */
    protected SongItem getPlayingSong(){
        if (songList != null && playingIndex != -1){
            return songList.get(playingIndex);
        }else {
            return null;
        }
    }

    /**
     * 播放音频
     * @param songList  要播放的音频列表
     * @param playIndex  要播放的音频位置
     * @return  执行结果
     */
    protected boolean play(ArrayList<SongItem> songList , int playIndex){
        if (songList != null && songList.size() > 0 && playIndex >= 0 && playIndex < songList.size()){
            //如果音频列表和播放位置合法 , 则开始读取
            if (playAudio(songList.get(playIndex) , false)){
                //如果播放成功 , 则记录当前数据和列表 , 同时发送开始播放的广播
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_PLAY));
                this.playingIndex = playIndex;
                if (this.songList == null || !this.songList.equals(songList)){
                    Logger.warnning(TAG,"播放列表已更新!");
                    this.songList = songList;
                }
                return true;
            }else {
                //如果读取不成功 ,则放弃使用这个列表 , 并清空数据
                this.playingIndex = -1;
                this.songList = null;
            }
        }
        return false;
    }

    /**
     * 仅预加载音频 , 加载完后是 已停止 状态
     * @param songList  要播放的音频列表
     * @param playIndex  要播放的音频位置
     * @return  执行结果
     */
    protected boolean onlyInitAudio(ArrayList<SongItem> songList , int playIndex){
        if (songList != null && songList.size() > 0 && playIndex >= 0 && playIndex < songList.size()){
            //如果音频列表和播放位置合法 , 则开始读取
            if (playAudio(songList.get(playIndex) , true)){
                //如果读取成功 , 则记录当前数据和列表
                this.playingIndex = playIndex;
                if (this.songList == null || !this.songList.equals(songList)){
                    Logger.warnning(TAG,"播放列表已更新!");
                    this.songList = songList;
                }
                return true;
            }else {
                //如果读取不成功 ,则放弃使用这个列表 , 并清空数据
                this.playingIndex = -1;
                this.songList = null;
            }
        }
        return false;
    }

    /**
     * 停止播放音频
     * @return  执行结果
     */
    protected boolean stopAudio(){
        if (playingChannel != 0 && BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_STOPPED){
            //如果当前加载了频道数据 , 同时当前状态不是停止播放状态
            return BASS.BASS_ChannelStop(playingChannel);
        }else {
            return false;
        }
    }

    /**
     * 暂停播放音频
     * @return  执行结果
     */
    protected boolean pauseAudio(){
        if (playingChannel != 0 && BASS.BASS_ChannelIsActive(playingChannel) == BASS.BASS_ACTIVE_PLAYING){
            //如果当前加载了频道数据 , 同时当前状态为正在播放
            boolean result = BASS.BASS_ChannelPause(playingChannel);
            if (result){
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_PAUSED));
            }
            return result;
        }else {
            return false;
        }
    }

    /**
     * 继续播放音频 , 如果音频是被暂停则继续播放  如果音频是被停止则从头播放 , 如果操作成功 , 则会发送广播
     * @return  执行结果
     */
    protected boolean resumeAudio(){
        boolean result;

        if (playingChannel != 0 && BASS.BASS_ChannelIsActive(playingChannel) == BASS.BASS_ACTIVE_PAUSED){
            //暂停 , 继续播放
            result = BASS.BASS_ChannelPlay(playingChannel , false);
            if (result){
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_RESUMED));
            }
            return result;
        }else if (playingChannel != 0 && BASS.BASS_ChannelIsActive(playingChannel) == BASS.BASS_ACTIVE_STOPPED){
            //停止 , 从头播放
            result = BASS.BASS_ChannelPlay(playingChannel, true);
            if (result){
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_RESUMED));
            }
            return result;
        }else {
            return false;
        }
    }

    /**
     * 播放前一个音频数据
     * @return  执行结果
     */
    protected boolean playPrevious(){
        if (songList != null){
            if (playingIndex == 0){
                //如果当前正在播放的位置就是第一个 , 则跳到最后一个位置
                playingIndex = songList.size()-1;
            }else {
                //否则就继续向前一个位置读取数据
                playingIndex -= 1;
            }
            boolean result = play(songList,playingIndex);
            if (result){
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_SWITCH));
            }
            return result;
        }else {
            return false;
        }
    }

    /**
     * 播放下一个音频数据
     * @return  执行结果
     */
    protected boolean playNext(){
        if (songList != null){
            if (playingIndex == songList.size()-1){
                //如果当前播放的位置就是最后一个 , 则跳到第一个位置
                playingIndex = 0;
            }else {
                //否则就继续向下一个位置读取数据
                playingIndex += 1;
            }
            boolean result = play(songList,playingIndex);
            if (result){
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_SWITCH));
            }
            return result;
        }else {
            return false;
        }
    }

    /**
     * 释放歌曲占用的资源
     * @return  执行结果
     */
    protected boolean releaseAudio(){
        if (playingChannel != 0 ){
            //如果频道有数据
            if ( BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_STOPPED){
                //如果当前正在播放或者是暂停 , 则全部停止
                BASS.BASS_ChannelStop(playingChannel);
            }
            final boolean result = BASS.BASS_StreamFree(playingChannel);
            playingChannel = 0;
            return result;
        }else {
            return false;
        }
    }

    /**
     * @return  当前播放的音频列表
     */
    protected ArrayList<SongItem> getPlayingList(){
        return this.songList;
    }

    /**
     * @return  获取当前播放的位置 , 无的时候为 -1
     */
    protected int getPlayingIndex(){
        return this.playingIndex;
    }

    /**
     * 获取当前播放的位置
     * @return  当前音频播放位置
     */
    protected double getPlayingPosition(){
        if (playingChannel != 0){
            return BASS.BASS_ChannelBytes2Seconds(playingChannel , BASS.BASS_ChannelGetPosition(playingChannel,BASS.BASS_POS_BYTE));
        }else {
            return 0d;
        }
    }

    /**
     * 获取当前播放的音频长度
     * @return  音频长度
     */
    protected double getAudioLength(){
        if (playingChannel != 0){
            return BASS.BASS_ChannelBytes2Seconds(playingChannel , BASS.BASS_ChannelGetLength(playingChannel,BASS.BASS_POS_BYTE));
        }else {
            return 0d;
        }
    }

    /**
     * 歌曲播放位置设置
     * @param position  位置长度
     * @return  执行结果
     */
    protected boolean seek2Position(double position){
        if (playingChannel != 0){
            return BASS.BASS_ChannelSetPosition(playingChannel,BASS.BASS_ChannelSeconds2Bytes(playingChannel,position),BASS.BASS_POS_BYTE);
        }else {
            return false;
        }
    }

}
