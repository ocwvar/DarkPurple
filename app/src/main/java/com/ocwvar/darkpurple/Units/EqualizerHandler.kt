package com.ocwvar.darkpurple.Units

import android.media.audiofx.Equalizer
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-12 下午5:23
 * File Location com.ocwvar.darkpurple.Units
 * This file use to :   均衡器控制器
 */
object EqualizerHandler {
    private val TAG: String = "均衡器"

    private var equalizer: Equalizer? = null
    private var usingAudioSessionID: Int = 0

    //最低带宽值，默认为无效值：-999
    private var lowestBandLimit: Short = -999
    //最高带宽值，默认为无效值：-999
    private var highestBandLimit: Short = -999
    //可控制带宽范围个数，默认为无效值：0
    private var numberOfBands: Short = 0

    //当前正在使用的均衡器参数
    private var usingEqualizerArgs: ShortArray = ShortArray(0)
    //均衡器配置参数Map
    private var equalizerArgs: LinkedHashMap<String, ShortArray> = LinkedHashMap()

    /**
     * 加载已有的均衡器配置
     */
    fun loadSavedEqualizerArgs() {
        val source: LinkedHashMap<String, ShortArray>? = JSONHandler.loadSavedEqualizerArgs()
        source ?: return

        this.equalizerArgs.clear()
        this.equalizerArgs.putAll(source)
    }

    /**
     * 异步储存所有均衡器配置
     */
    fun asyncSaveEqualizerArgs() {
        synchronized(this@EqualizerHandler, {
            Thread(Runnable {
                //获取Key和Value的迭代器进行遍历
                val names: Iterator<String> = equalizerArgs.keys.iterator()
                val dataS: Iterator<ShortArray> = equalizerArgs.values.iterator()

                while (names.hasNext() && dataS.hasNext()) {
                    val name: String = names.next()
                    val data: ShortArray = dataS.next()

                    //储存数据
                    JSONHandler.saveEqualizerArgs(name, data)
                }

            }).start()
        })
    }

    /**
     * 初始化并启动均衡器
     * @param   audioSessionID  需要进行调音的AudioSession ID
     */
    fun initEqualizer(audioSessionID: Int = MediaLibrary.getUsingAudioSessionID()) {
        if (audioSessionID == 0) {
            //当ID为 0 时，表明此ID是无效的，关闭并释放当前的均衡器资源
            disableEqualizer()
            return
        } else if (audioSessionID == this.usingAudioSessionID) {
            //与当前使用的ID相同，不进行操作
            return
        } else {
            //进行均衡器初始化
            this.equalizer?.release()
            try {
                //创建均衡器对象
                this.equalizer = Equalizer(0, audioSessionID)

                this.equalizer?.let {
                    //开启均衡器，开启后才能获取相关的参数
                    it.enabled = true

                    //获取带宽上下的极值
                    this.lowestBandLimit = it.bandLevelRange[0]
                    this.highestBandLimit = it.bandLevelRange[1]

                    //获取可控制范围个数
                    this.numberOfBands = it.numberOfBands
                }
            } catch(e: Exception) {
                Logger.error(TAG, "初始化均衡器对象时发生异常：\n" + e)
            }

        }

    }

    /**
     * 应用均衡器参数
     *
     * @param   argsProfile 参数配置名称，如果传入 NULL ，则恢复默认
     */
    fun applyEqualizerArgs(argsProfile: String?) {
        if (usingAudioSessionID != MediaLibrary.getUsingAudioSessionID()) {
            //当前使用的AudioSession ID与最新的ID不同，需要重新创建均衡器对象
            initEqualizer()
        }

        if (isEnabled()) {
            //如果此时均衡器已启动

            if (argsProfile != null) {
                //参数名有效，开始获取数据
                this.usingEqualizerArgs = getEqualizerArgsProfile(argsProfile)
            } else {
                //无参数名，生成默认数据
                this.usingEqualizerArgs = ShortArray(numberOfBands.toInt())
            }

            if (numberOfBands.toInt() == usingEqualizerArgs.size) {
                //如果均衡器已启动，并且均衡器参数长度与可设置的带宽数量相同，则应用
                for (i in 0..numberOfBands.toInt()) {
                    this.equalizer?.setBandLevel(i.toShort(), this.usingEqualizerArgs[i])
                }
            }

        } else {
            Logger.error(TAG, "均衡器未启动或 均衡器参数与可调节带宽数量不符")
        }

    }

    /**
     * 关闭并释放均衡器
     */
    fun disableEqualizer() {
        if (isEnabled()) {
            this.equalizer?.enabled = false
        }
        this.equalizer?.release()
        this.equalizer = null
    }

    /**
     * @return  当前均衡器是否已开启
     */
    fun isEnabled(): Boolean = this.equalizer?.enabled ?: false

    /**
     * 获取均衡器配置文件数据
     *
     * @param   argsProfile 配置文件名称
     * @return  配置文件数据，如果获取失败，返回默认数据
     */
    fun getEqualizerArgsProfile(argsProfile: String): ShortArray {
        if (equalizerArgs.containsKey(argsProfile)) {
            return equalizerArgs[argsProfile]!!
        } else {
            return ShortArray(numberOfBands.toInt())
        }
    }

}