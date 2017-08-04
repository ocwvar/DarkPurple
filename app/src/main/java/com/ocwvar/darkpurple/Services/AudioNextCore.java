package com.ocwvar.darkpurple.Services;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Services.Core.EXOCORE;
import com.ocwvar.darkpurple.Services.Core.IPlayer;
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser;
import com.ocwvar.darkpurple.Units.Logger;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Services
 * Data: 2016/7/12 16:01
 * Project: DarkPurple
 * 音频播放处理
 */
class AudioNextCore {
    private final String TAG = "音频引擎";
    private final Context applicationContext;

    /**
     * 播放的音频列表
     */
    private ArrayList<SongItem> songList;
    /**
     * 当前播放的音频位于列表内的位置
     */
    private int playingIndex = -1;

    /**
     * CORE调用接口
     */
    private IPlayer iPlayer = null;

    AudioNextCore(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext;
        this.iPlayer = new EXOCORE(applicationContext);
    }

    /**
     * 播放前一个音频数据
     *
     * @return 执行结果
     */
    boolean playPrevious() {
        if (songList != null) {
            if (playingIndex == 0) {
                //如果当前正在播放的位置就是第一个 , 则跳到最后一个位置
                playingIndex = songList.size() - 1;
            } else {
                //否则就继续向前一个位置读取数据
                playingIndex -= 1;
            }
            boolean result = play(songList, playingIndex);
            if (result) {
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_SWITCH));
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * 播放下一个音频数据
     *
     * @return 执行结果
     */
    boolean playNext() {
        if (songList != null) {
            if (playingIndex == songList.size() - 1) {
                //如果当前播放的位置就是最后一个 , 则跳到第一个位置
                playingIndex = 0;
            } else {
                //否则就继续向下一个位置读取数据
                playingIndex += 1;
            }
            boolean result = play(songList, playingIndex);
            if (result) {
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_SWITCH));
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * @return 当前播放的音频列表
     */
    @NonNull
    ArrayList<SongItem> getPlayingList() {
        return this.songList;
    }

    /**
     * 得到当前已激活的歌曲
     *
     * @return 有则返回歌曲集合 , 没有则返回 NULL
     */
    @Nullable
    SongItem getPlayingSong() {
        if (songList != null && playingIndex != -1) {
            return songList.get(playingIndex);
        } else {
            return null;
        }
    }

    /**
     * @return 获取当前播放的位置 , 无的时候为 -1
     */
    int getPlayingIndex() {
        return this.playingIndex;
    }

    /**
     * 播放音频
     *
     * @param songItem 音频信息集合
     * @param onlyInit 只加载音频 , 而不播放
     * @return 执行结果
     */
    private boolean playAudio(SongItem songItem, boolean onlyInit) {
        return iPlayer.play(songItem, onlyInit);
    }

    /**
     * 仅预加载音频 , 加载完后是 已停止 状态
     *
     * @param songList  要播放的音频列表
     * @param playIndex 要播放的音频位置
     * @return 执行结果
     */
    boolean onlyInitAudio(ArrayList<SongItem> songList, int playIndex) {
        if (songList != null && songList.size() > 0 && playIndex >= 0 && playIndex < songList.size()) {
            //如果音频列表和播放位置合法 , 则开始读取
            if (playAudio(songList.get(playIndex), true)) {
                //如果读取成功 , 则记录当前数据和列表
                this.playingIndex = playIndex;
                if (this.songList == null || !this.songList.equals(songList)) {
                    Logger.warning(TAG, "播放列表已更新!");
                    this.songList = songList;
                }
                return true;
            } else {
                //如果读取不成功 ,则放弃使用这个列表 , 并清空数据
                this.playingIndex = -1;
                this.songList = null;
            }
        }
        return false;
    }

    /**
     * 播放音频
     *
     * @param songList  要播放的音频列表
     * @param playIndex 要播放的音频位置
     * @return 执行结果
     */
    boolean play(ArrayList<SongItem> songList, int playIndex) {
        if (songList != null && songList.size() > 0 && playIndex >= 0 && playIndex < songList.size()) {
            //如果音频列表和播放位置合法 , 则开始读取
            final SongItem songItem = songList.get(playIndex);
            if (playAudio(songItem, false)) {
                //如果播放成功 , 则记录当前数据和列表
                this.playingIndex = playIndex;
                if (this.songList == null || !this.songList.equals(songList)) {
                    Logger.warning(TAG, "播放列表已更新!");
                    this.songList = songList;
                }
                //更新封面效果
                CoverProcesser.INSTANCE.handleThis(songItem.getCoverID());

                //发送开始播放广播
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_PLAY));
                return true;
            } else if (this.songList != null && this.songList.size() > 1) {
                //如果读取不成功 , 同时列表有多个音频，则返回错误信息，并尝试播放下一首
                this.playingIndex = playIndex;
                playNext();
            } else {
                // 如果列表只有这一个损坏的音频 , 则返回失败 , 同时放弃这个列表
                this.playingIndex = -1;
                this.songList = null;
            }
        }
        return false;
    }

    /**
     * 获取均衡器频段设置
     *
     * @return 频段参数
     */
    int[] getEqParameters() {
        return iPlayer.getEQParameters();
    }

    /**
     * 更改均衡器频段参数
     *
     * @param eqParameter 均衡器参数 -10 ~ 10
     * @param eqIndex     调节位置
     * @return 执行结果
     */
    boolean updateEqParameter(int eqParameter, int eqIndex) {
        return false;
    }

    /**
     * 重置均衡器设置
     */
    void resetEqualizer() {

    }

    /**
     * 得到当前的频谱
     *
     * @return 频谱数据数组
     */
    float[] getSpectrum() {
        return iPlayer.getSpectrum();
    }

    /**
     * 获取当前音频播放的状态
     *
     * @return 当前状态
     */
    @NonNull
    AudioStatus getCurrentStatus() {
        return iPlayer.getAudioStatus();
    }

    /**
     * 暂停播放音频
     *
     * @return 执行结果
     */
    boolean pauseAudio() {
        return iPlayer.pause();
    }

    /**
     * 继续播放音频 , 如果音频是被暂停则继续播放  如果音频是被停止则从头播放 , 如果操作成功 , 则会发送广播
     *
     * @return 执行结果
     */
    boolean resumeAudio() {
        return iPlayer.resume();
    }

    /**
     * 释放歌曲占用的资源
     *
     * @return 执行结果
     */
    boolean releaseAudio() {
        return iPlayer.release();
    }

    /**
     * 获取当前播放的位置
     *
     * @return 当前音频播放位置
     */
    double getPlayingPosition() {
        return iPlayer.playingPosition();
    }

    /**
     * 获取当前播放的音频长度
     *
     * @return 音频长度
     */
    double getAudioLength() {
        return iPlayer.getAudioLength();
    }

    /**
     * 歌曲播放位置设置
     *
     * @param position 位置长度
     * @return 执行结果
     */
    boolean seek2Position(long position) {
        return iPlayer.seekPosition(position);
    }

    /**
     * EXO2特有方法
     * 打开频谱数据接收器
     */
    void exo2_Visualizer_SwitchOn() {
        iPlayer.switchOnVisualizer();
    }

    /**
     * EXO2特有方法
     * 停止频谱数据接收器
     */
    void exo2_Visualizer_SwitchOff() {
        iPlayer.switchOffVisualizer();
    }

}
