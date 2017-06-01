package com.ocwvar.darkpurple.Services;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Services.Core.BASSCORE;
import com.ocwvar.darkpurple.Services.Core.CoreAdvFunctions;
import com.ocwvar.darkpurple.Services.Core.CoreBaseFunctions;
import com.ocwvar.darkpurple.Services.Core.EXOCORE;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.Units.CoverProcesser;
import com.ocwvar.darkpurple.Units.Logger;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Services
 * Data: 2016/7/12 16:01
 * Project: DarkPurple
 * 音频播放处理
 */
public class AudioNextCore {
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
     * 当前使用的音频播放方案类型
     */
    private CoreType coreType = CoreType.BASS_Library;

    /**
     * CORE调用接口
     */
    private CoreBaseFunctions coreInterface = null;

    AudioNextCore(@NonNull Context applicationContext, @NonNull CoreType defaultCoreType) {
        this.applicationContext = applicationContext;
        this.coreType = defaultCoreType;
        switch (this.coreType) {
            case BASS_Library:
                coreInterface = new BASSCORE(applicationContext);
                break;
            case EXO2:
                coreInterface = new EXOCORE(applicationContext);
                break;
            case COMPAT:
                break;
        }
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
                if (coreType != CoreType.EXO2) {
                    //EXO2 需要由核心自主发送广播
                    applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_UPDATE));
                }
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
                if (coreType != CoreType.EXO2) {
                    //EXO2 需要由核心自主发送广播
                    applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_UPDATE));
                }
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
     * @return 当前使用的播放方案类型
     */
    @NonNull
    CoreType currentCoreType() {
        return coreType;
    }

    /**
     * 播放音频
     *
     * @param songItem 音频信息集合
     * @param onlyInit 只加载音频 , 而不播放
     * @return 执行结果
     */
    private boolean playAudio(SongItem songItem, boolean onlyInit) {
        return coreInterface.play(songItem, onlyInit);
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
                    Logger.warnning(TAG, "播放列表已更新!");
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
                    Logger.warnning(TAG, "播放列表已更新!");
                    this.songList = songList;
                }
                //更新封面效果
                if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
                    CoverProcesser.INSTANCE.handleThis(songItem.getCustomCoverPath());
                } else if (songItem.isHaveCover()) {
                    CoverProcesser.INSTANCE.handleThis(CoverImage2File.getInstance().getCacheFile(songItem.getPath()));
                }
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
        if (coreInterface instanceof CoreAdvFunctions) {
            return ((CoreAdvFunctions) coreInterface).getEQParameters();
        } else {
            Logger.error(TAG, "\n\n当前CORE不支持此功能\n\n");
            return null;
        }
    }

    /**
     * 更改均衡器频段参数
     *
     * @param eqParameter 均衡器参数 -10 ~ 10
     * @param eqIndex     调节位置
     * @return 执行结果
     */
    boolean updateEqParameter(int eqParameter, int eqIndex) {
        if (coreInterface instanceof CoreAdvFunctions) {
            return ((CoreAdvFunctions) coreInterface).setEQParameters(eqParameter, eqIndex);
        } else {
            Logger.error(TAG, "\n\n当前CORE不支持此功能\n\n");
            return false;
        }
    }

    /**
     * 重置均衡器设置
     */
    void resetEqualizer() {
        if (coreInterface instanceof CoreAdvFunctions) {
            ((CoreAdvFunctions) coreInterface).resetEQ();
        } else {
            Logger.error(TAG, "\n\n当前CORE不支持此功能\n\n");
        }
    }

    /**
     * 得到当前的频谱
     *
     * @return 频谱数据数组
     */
    float[] getSpectrum() {
        if (coreInterface instanceof CoreAdvFunctions) {
            return ((CoreAdvFunctions) coreInterface).getSpectrum();
        } else {
            Logger.error(TAG, "\n\n当前CORE不支持此功能\n\n");
            return null;
        }
    }

    /**
     * @return 当前使用的播放核心是否支持高级功能
     */
    boolean isCoreSupportedAdvFunction() {
        return coreInterface instanceof CoreAdvFunctions;
    }

    /**
     * 获取当前音频播放的状态
     *
     * @return 当前状态
     */
    @NonNull
    AudioStatus getCurrentStatus() {
        return coreInterface.getAudioStatus();
    }

    /**
     * 暂停播放音频
     *
     * @return 执行结果
     */
    boolean pauseAudio() {
        return coreInterface.pause();
    }

    /**
     * 继续播放音频 , 如果音频是被暂停则继续播放  如果音频是被停止则从头播放 , 如果操作成功 , 则会发送广播
     *
     * @return 执行结果
     */
    boolean resumeAudio() {
        return coreInterface.resume();
    }

    /**
     * 释放歌曲占用的资源
     *
     * @return 执行结果
     */
    boolean releaseAudio() {
        return coreInterface.release();
    }

    /**
     * 获取当前播放的位置
     *
     * @return 当前音频播放位置
     */
    double getPlayingPosition() {
        return coreInterface.playingPosition();
    }

    /**
     * 获取当前播放的音频长度
     *
     * @return 音频长度
     */
    double getAudioLength() {
        return coreInterface.getAudioLength();
    }

    /**
     * 歌曲播放位置设置
     *
     * @param position 位置长度
     * @return 执行结果
     */
    boolean seek2Position(double position) {
        return coreInterface.seekPosition(position);
    }

    /**
     * 播放类型方案枚举类型
     */
    public enum CoreType {
        /**
         * Un4seen Bass Library 方案
         */
        BASS_Library,
        /**
         * Google ExoPlayer2 方案
         */
        EXO2,
        /**
         * 原生MediaPlayer方案
         */
        COMPAT
    }

}
