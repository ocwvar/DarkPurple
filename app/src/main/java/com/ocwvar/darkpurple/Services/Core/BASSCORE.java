package com.ocwvar.darkpurple.Services.Core;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.AudioStatus;
import com.ocwvar.darkpurple.Units.EqualizerUnits;
import com.ocwvar.darkpurple.Units.Logger;
import com.un4seen.bass.BASS;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/12 12:51 PM
 * File Location com.ocwvar.darkpurple.Services.Core
 * This file use to :   BASS Library 播放方案
 */

public final class BASSCORE implements CoreAdvFunctions {

    private final String TAG = "BASS_CORE";
    private final Context applicationContext;

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

    public BASSCORE(Context applicationContext) {
        this.applicationContext = applicationContext;

        //获取最后一次保存的EQ设置数据
        this.eqParameters = EqualizerUnits.getInstance().loadLastTimeEqualizer();
        //CORE的初始化操作
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_LATENCY);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 0);
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

    /**
     * 释放歌曲占用的资源
     *
     * @return 执行结果
     */
    private boolean releaseAudio() {
        if (playingChannel != 0) {
            //如果频道有数据
            if (BASS.BASS_ChannelIsActive(playingChannel) != BASS.BASS_ACTIVE_STOPPED) {
                //如果当前正在播放或者是暂停 , 则全部停止
                BASS.BASS_ChannelStop(playingChannel);
            }
            final boolean result = BASS.BASS_StreamFree(playingChannel);
            playingChannel = 0;
            return result;
        } else {
            return false;
        }
    }

    /**
     * 播放音频
     *
     * @param songItem 音频信息对象
     * @param onlyInit 是否仅加载音频数据，而不进行播放
     * @return 执行结果
     */
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
     * 续播音频
     *
     * @return 执行结果
     */
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

    /**
     * 暂停音频
     *
     * @return 执行结果
     */
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

    /**
     * 释放音频资源
     *
     * @return 执行结果
     */
    @Override
    public boolean release() {
        if (this.playingChannel != 0) {
            //如果频道有数据
            if (BASS.BASS_ChannelIsActive(this.playingChannel) != BASS.BASS_ACTIVE_STOPPED) {
                //如果当前正在播放或者是暂停 , 则全部停止
                BASS.BASS_ChannelStop(this.playingChannel);
            }
            final boolean result = BASS.BASS_StreamFree(this.playingChannel);
            this.playingChannel = 0;
            return result;
        } else {
            return false;
        }
    }

    /**
     * 获取音频当前播放的位置，即已播放的长度
     *
     * @return 当前位置，异常返回 0
     */
    @Override
    public double playingPosition() {
        if (playingChannel != 0) {
            return BASS.BASS_ChannelBytes2Seconds(playingChannel, BASS.BASS_ChannelGetPosition(playingChannel, BASS.BASS_POS_BYTE));
        } else {
            return 0d;
        }
    }

    /**
     * 跳转至指定音频长度位置
     *
     * @return 执行结果
     */
    @Override
    public boolean seekPosition(double position) {
        if (playingChannel != 0) {
            return BASS.BASS_ChannelSetPosition(playingChannel, BASS.BASS_ChannelSeconds2Bytes(playingChannel, position), BASS.BASS_POS_BYTE);
        } else {
            return false;
        }
    }

    /**
     * 获取音频长度
     *
     * @return 音频长度，异常返回 0
     */
    @Override
    public double getAudioLength() {
        if (playingChannel != 0) {
            return BASS.BASS_ChannelBytes2Seconds(playingChannel, BASS.BASS_ChannelGetLength(playingChannel, BASS.BASS_POS_BYTE));
        } else {
            return 0d;
        }
    }

    /**
     * 获取当前音乐播放状态
     *
     * @return 当前状态
     */
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
     * 获取均衡器各个频段参数
     *
     * @return 均衡器参数
     */
    @NotNull
    @Override
    public int[] getEQParameters() {
        return this.eqParameters;
    }

    /**
     * 更改均衡器频段参数
     *
     * @param eqParameter 均衡器参数 -10 ~ 10
     * @param eqIndex     调节位置
     * @return 执行结果
     */
    @Override
    public boolean setEQParameters(int eqParameter, int eqIndex) {
        this.eqParameters[eqIndex] = eqParameter;
        BASS.BASS_DX8_PARAMEQ eq = new BASS.BASS_DX8_PARAMEQ();
        BASS.BASS_FXGetParameters(this.eqIndex[eqIndex], eq);
        eq.fGain = eqParameter;

        EqualizerUnits.getInstance().saveLastTimeEqualizer(this.eqParameters);

        return BASS.BASS_FXSetParameters(this.eqIndex[eqIndex], eq);
    }

    /**
     * 重置均衡器
     */
    @Override
    public void resetEQ() {
        this.eqParameters = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 10; i++) {
            BASS.BASS_DX8_PARAMEQ eq = new BASS.BASS_DX8_PARAMEQ();
            BASS.BASS_FXGetParameters(this.eqIndex[i], eq);
            eq.fGain = 0;
            BASS.BASS_FXSetParameters(this.eqIndex[i], eq);
        }
    }

    /**
     * 获取当前频谱数据
     *
     * @return 频谱数据，异常返回 NULL
     */
    @Nullable
    @Override
    public float[] getSpectrum() {
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
