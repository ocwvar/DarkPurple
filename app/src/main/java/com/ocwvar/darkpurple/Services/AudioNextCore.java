package com.ocwvar.darkpurple.Services;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Units.EqualizerUnits;
import com.ocwvar.darkpurple.Units.Logger;
import com.un4seen.bass.BASS;

import java.io.File;
import java.nio.ByteBuffer;
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
     * BASS Library播放方案
     */
    private BASSCORE basscore = null;

    AudioNextCore(@NonNull Context applicationContext, @NonNull CoreType defaultCoreType) {
        this.applicationContext = applicationContext;
        this.coreType = defaultCoreType;
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
                applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_UPDATE));
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
                applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_UPDATE));
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * @return 当前播放的音频列表
     */
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
        switch (coreType) {
            case BASS_Library:
                return basscore.play(songItem, onlyInit);
            case EXO:
            case COMPAT:
            default:
                return false;
        }
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
            if (playAudio(songList.get(playIndex), false)) {
                //如果播放成功 , 则记录当前数据和列表 , 同时发送开始播放的广播
                this.playingIndex = playIndex;
                applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_PLAY));
                if (this.songList == null || !this.songList.equals(songList)) {
                    Logger.warnning(TAG, "播放列表已更新!");
                    this.songList = songList;
                }
                return true;
            } else if (this.songList != null && this.songList.size() > 1) {
                //如果读取不成功 , 同时列表有多个音频，则返回错误信息
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
        switch (coreType) {
            case BASS_Library:
                return basscore.getEqParameters();
            case EXO:
            case COMPAT:
            default:
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
        switch (coreType) {
            case BASS_Library:
                return basscore.updateEqParameter(eqParameter, eqIndex);
            case EXO:
            case COMPAT:
            default:
                return false;
        }
    }

    /**
     * 重置均衡器设置
     */
    void resetEqualizer() {
        switch (coreType) {
            case BASS_Library:
                basscore.resetEqualizer();
                break;
            case EXO:
            case COMPAT:
            default:
        }
    }

    /**
     * 得到当前的频谱
     *
     * @return 频谱数据数组
     */
    float[] getSpectrum() {
        switch (coreType) {
            case BASS_Library:
                return basscore.getSpectrum();
            case EXO:
            case COMPAT:
            default:
                return null;
        }
    }

    /**
     * 获取当前音频播放的状态
     *
     * @return 当前状态
     */
    AudioStatus getCurrectStatus() {
        switch (coreType) {
            case BASS_Library:
                return basscore.getAudioStatus();
            case EXO:
            case COMPAT:
            default:
                return null;
        }
    }

    /**
     * 暂停播放音频
     *
     * @return 执行结果
     */
    boolean pauseAudio() {
        switch (coreType) {
            case BASS_Library:
                return basscore.pause();
            case EXO:
            case COMPAT:
            default:
                return false;
        }
    }

    /**
     * 继续播放音频 , 如果音频是被暂停则继续播放  如果音频是被停止则从头播放 , 如果操作成功 , 则会发送广播
     *
     * @return 执行结果
     */
    boolean resumeAudio() {
        switch (coreType) {
            case BASS_Library:
                return basscore.resume();
            case EXO:
            case COMPAT:
            default:
                return false;
        }
    }

    /**
     * 释放歌曲占用的资源
     *
     * @return 执行结果
     */
    boolean releaseAudio() {
        switch (coreType) {
            case BASS_Library:
                return basscore.release();
            case EXO:
            case COMPAT:
            default:
                return false;
        }
    }

    /**
     * 获取当前播放的位置
     *
     * @return 当前音频播放位置
     */
    double getPlayingPosition() {
        switch (coreType) {
            case BASS_Library:
                return basscore.playingPosition();
            case EXO:
            case COMPAT:
            default:
                return 0d;
        }
    }

    /**
     * 获取当前播放的音频长度
     *
     * @return 音频长度
     */
    double getAudioLength() {
        switch (coreType) {
            case BASS_Library:
                return basscore.getAudioLength();
            case EXO:
            case COMPAT:
            default:
                return 0d;
        }
    }

    /**
     * 歌曲播放位置设置
     *
     * @param position 位置长度
     * @return 执行结果
     */
    boolean seek2Position(double position) {
        switch (coreType) {
            case BASS_Library:
                return basscore.seekPosition(position);
            case EXO:
            case COMPAT:
            default:
                return false;
        }
    }

    /**
     * 音频播放状态枚举类型
     */
    public enum AudioStatus {
        /**
         * 正在播放
         */
        Playing,
        /**
         * 暂停
         */
        Paused,
        /**
         * 未加载音频数据
         */
        Empty,
        /**
         * 错误或未知状态
         */
        Error
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
        EXO,
        /**
         * 原生MediaPlayer方案
         */
        COMPAT
    }

    /**
     * 音频播放方案基础功能需求接口
     */
    private interface CoreBaseFunctions {

        /**
         * 播放音频
         *
         * @param songItem 音频信息对象
         * @param onlyInit 是否仅加载音频数据，而不进行播放
         * @return
         */
        boolean play(SongItem songItem, boolean onlyInit);

        /**
         * 续播音频
         *
         * @return 执行结果
         */
        boolean resume();

        /**
         * 暂停音频
         *
         * @return 执行结果
         */
        boolean pause();

        /**
         * 释放音频
         *
         * @return 执行结果
         */
        boolean release();

        /**
         * 获取音频当前播放的位置，即已播放的长度
         *
         * @return 当前位置
         */
        double playingPosition();

        /**
         * 跳转至指定音频长度位置
         *
         * @return 执行结果
         */
        boolean seekPosition(double position);

        /**
         * 获取音频长度
         *
         * @return 音频长度
         */
        double getAudioLength();

        AudioStatus getAudioStatus();

    }

    /**
     * 基于Bass Library的音频播放方案
     * 支持以下功能：
     * 播放、暂停、续播、预加载、播放完成回调、释放、频谱、EQ
     */
    private final class BASSCORE implements CoreBaseFunctions {

        /**
         * 当前播放的音频频道
         */
        private int playingChannel = 0;
        /**
         * EQ的位置目录
         */
        private int[] eqIndex = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        /**
         * EQ每个位置对应的参数数据
         */
        private int[] eqParameters = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        public BASSCORE() {
            //获取最后一次保存的EQ设置数据
            this.eqParameters = EqualizerUnits.getInstance().loadLastTimeEqualizer();
            BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_LATENCY);
            BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 0);
        }

        @Override
        public boolean play(SongItem songItem, boolean onlyInit) {
            if (playingChannel != 0 || BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_STOPPED) {
                //如果当前仍有音频数据 , 则先释放旧的
                this.releaseAudio();
                //释放掉回调
                BASS.BASS_ChannelRemoveSync(playingChannel, BASS.BASS_SYNC_END);
                Logger.warnning(TAG, "已释放旧资源");
            }

            //加载音频数据
            this.playingChannel = this.initAudio(songItem.getPath());

            if (playingChannel != 0) {
                //创建播放回调 , 使得能自动播放下一首
                this.initCallback(playingChannel);

                //设置音频 10个频段的参数
                this.eqIndex[0] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[1] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[2] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[3] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[4] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[5] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[6] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[7] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[8] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);
                this.eqIndex[9] = BASS.BASS_ChannelSetFX(playingChannel, BASS.BASS_FX_DX8_PARAMEQ, 0);

                BASS.BASS_DX8_PARAMEQ p = new BASS.BASS_DX8_PARAMEQ();
                p.fBandwidth = 18;
                p.fCenter = 31.25f;
                p.fGain = this.eqParameters[0];
                BASS.BASS_FXSetParameters(this.eqIndex[0], p);
                p.fCenter = 62.5f;
                p.fGain = this.eqParameters[1];
                BASS.BASS_FXSetParameters(this.eqIndex[1], p);
                p.fCenter = 125;
                p.fGain = this.eqParameters[2];
                BASS.BASS_FXSetParameters(this.eqIndex[2], p);
                p.fCenter = 250;
                p.fGain = this.eqParameters[3];
                BASS.BASS_FXSetParameters(this.eqIndex[3], p);
                p.fCenter = 500;
                p.fGain = this.eqParameters[4];
                BASS.BASS_FXSetParameters(this.eqIndex[4], p);
                p.fCenter = 1000;
                p.fGain = this.eqParameters[5];
                BASS.BASS_FXSetParameters(this.eqIndex[5], p);
                p.fCenter = 2000;
                p.fGain = this.eqParameters[6];
                BASS.BASS_FXSetParameters(this.eqIndex[6], p);
                p.fCenter = 4000;
                p.fGain = this.eqParameters[7];
                BASS.BASS_FXSetParameters(this.eqIndex[7], p);
                p.fCenter = 8000;
                p.fGain = this.eqParameters[8];
                BASS.BASS_FXSetParameters(this.eqIndex[8], p);
                p.fCenter = 16000;
                p.fGain = this.eqParameters[9];
                BASS.BASS_FXSetParameters(this.eqIndex[9], p);
                Logger.warnning(TAG, "音频资源已加载");
                if (onlyInit) {
                    applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_UPDATE));
                    return true;
                } else {
                    return BASS.BASS_ChannelPlay(this.playingChannel, true);
                }
            } else {
                return false;
            }
        }

        /**
         * 加载歌曲数据
         *
         * @param path 歌曲文件路径
         * @return 返回歌曲的Channel数据 , 否则返回 0 表示失败
         */
        private int initAudio(String path) {
            if (!TextUtils.isEmpty(path)) {
                //路径不为空
                File audioFile = new File(path);
                if (audioFile.exists() && audioFile.length() > 0) {
                    audioFile = null;
                    return BASS.BASS_StreamCreateFile(path, 0, 0, 0);
                }
            }
            return 0;
        }

        /**
         * 注册音频播放结束回调
         *
         * @param playingChannel 需要注册的音频数据
         */
        private void initCallback(int playingChannel) {
            BASS.BASS_ChannelSetSync(playingChannel, BASS.BASS_SYNC_END, 0, new BASS.SYNCPROC() {
                @Override
                public void SYNCPROC(int handle, int channel, int data, Object user) {
                    if (applicationContext != null) {
                        Logger.warnning(TAG, "播放结束!");
                        applicationContext.sendBroadcast(new Intent(AudioService.NOTIFICATION_NEXT));
                    }
                }
            }, 0);
            BASS.BASS_ChannelSetSync(playingChannel, BASS.BASS_SYNC_POS, 0, new BASS.SYNCPROC() {
                @Override
                public void SYNCPROC(int handle, int channel, int data, Object user) {

                }
            }, 0);
        }

        @Override
        public boolean resume() {
            if (playingChannel != 0 && BASS.BASS_ChannelIsActive(playingChannel) == BASS.BASS_ACTIVE_PAUSED) {
                //暂停 , 继续播放
                final boolean result = BASS.BASS_ChannelPlay(playingChannel, false);
                if (result) {
                    applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_RESUMED));
                }
                return result;
            } else if (playingChannel != 0 && BASS.BASS_ChannelIsActive(playingChannel) == BASS.BASS_ACTIVE_STOPPED) {
                //停止 , 从头播放
                final boolean result = BASS.BASS_ChannelPlay(playingChannel, true);
                if (result) {
                    applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_RESUMED));
                }
                return result;
            } else {
                return false;
            }
        }

        @Override
        public boolean pause() {
            if (this.playingChannel != 0 && BASS.BASS_ChannelIsActive(this.playingChannel) == BASS.BASS_ACTIVE_PLAYING) {
                //如果当前加载了频道数据 , 同时当前状态为正在播放
                final boolean result = BASS.BASS_ChannelPause(this.playingChannel);
                if (result) {
                    applicationContext.sendBroadcast(new Intent(AudioService.AUDIO_PAUSED));
                }
                return result;
            } else {
                return false;
            }
        }

        @Override
        public boolean release() {
            if (this.playingChannel != 0) {
                //如果频道有数据
                if (BASS.BASS_ChannelIsActive(this.playingChannel) != BASS.BASS_ACTIVE_STOPPED) {
                    //如果当前正在播放或者是暂停 , 则全部停止
                    BASS.BASS_ChannelStop(this.playingChannel);
                }
                final boolean result = BASS.BASS_StreamFree(this.playingChannel);
                playingIndex = 0;
                this.playingChannel = 0;
                return result;
            } else {
                return false;
            }
        }

        @Override
        public double playingPosition() {
            if (playingChannel != 0) {
                return BASS.BASS_ChannelBytes2Seconds(playingChannel, BASS.BASS_ChannelGetPosition(playingChannel, BASS.BASS_POS_BYTE));
            } else {
                return 0d;
            }
        }

        @Override
        public boolean seekPosition(double position) {
            if (playingChannel != 0) {
                return BASS.BASS_ChannelSetPosition(playingChannel, BASS.BASS_ChannelSeconds2Bytes(playingChannel, position), BASS.BASS_POS_BYTE);
            } else {
                return false;
            }
        }

        @Override
        public double getAudioLength() {
            if (playingChannel != 0) {
                return BASS.BASS_ChannelBytes2Seconds(playingChannel, BASS.BASS_ChannelGetLength(playingChannel, BASS.BASS_POS_BYTE));
            } else {
                return 0d;
            }
        }

        @Override
        public AudioStatus getAudioStatus() {
            if (playingChannel != 0) {
                switch (BASS.BASS_ChannelIsActive(playingChannel)) {
                    case BASS.BASS_ACTIVE_PLAYING:
                        return AudioStatus.Playing;
                    case BASS.BASS_ACTIVE_PAUSED:
                    case BASS.BASS_ACTIVE_STOPPED:
                        return AudioStatus.Paused;
                    default:
                        return AudioStatus.Error;
                }
            } else {
                return AudioStatus.Empty;
            }
        }

        /**
         * 释放歌曲占用的资源
         *
         * @return 执行结果
         */
        boolean releaseAudio() {
            if (playingChannel != 0) {
                //如果频道有数据
                if (BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_STOPPED) {
                    //如果当前正在播放或者是暂停 , 则全部停止
                    BASS.BASS_ChannelStop(playingChannel);
                }
                final boolean result = BASS.BASS_StreamFree(playingChannel);
                playingIndex = 0;
                playingChannel = 0;
                return result;
            } else {
                return false;
            }
        }

        /**
         * 获取均衡器频段设置
         *
         * @return 频段参数
         */
        int[] getEqParameters() {
            return this.eqParameters;
        }

        /**
         * 更改均衡器频段参数
         *
         * @param eqParameter 均衡器参数 -10 ~ 10
         * @param eqIndex     调节位置
         * @return 执行结果
         */
        boolean updateEqParameter(int eqParameter, int eqIndex) {
            this.eqParameters[eqIndex] = eqParameter;
            BASS.BASS_DX8_PARAMEQ eq = new BASS.BASS_DX8_PARAMEQ();
            BASS.BASS_FXGetParameters(this.eqIndex[eqIndex], eq);
            eq.fGain = eqParameter;

            EqualizerUnits.getInstance().saveLastTimeEqualizer(this.eqParameters);

            return BASS.BASS_FXSetParameters(this.eqIndex[eqIndex], eq);
        }

        /**
         * 重置均衡器设置
         */
        void resetEqualizer() {
            this.eqParameters = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            for (int i = 0; i < 10; i++) {
                BASS.BASS_DX8_PARAMEQ eq = new BASS.BASS_DX8_PARAMEQ();
                BASS.BASS_FXGetParameters(this.eqIndex[i], eq);
                eq.fGain = 0;
                BASS.BASS_FXSetParameters(this.eqIndex[i], eq);
            }
        }

        /**
         * 得到当前的频谱
         *
         * @return 频谱数据数组
         */
        float[] getSpectrum() {
            if (playingChannel == 0 || BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_PLAYING) {
                return null;
            } else {
                ByteBuffer byteBuffer = null;
                if (byteBuffer == null) {
                    byteBuffer = ByteBuffer.allocate(256 << 1);
                    byteBuffer.order(null);
                }
                BASS.BASS_ChannelGetData(playingChannel, byteBuffer, BASS.BASS_DATA_FFT256);
                float[] spectrum = new float[256 >> 1];
                byteBuffer.asFloatBuffer().get(spectrum);
                return spectrum;
            }
        }

    }

}
