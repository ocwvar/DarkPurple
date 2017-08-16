package com.ocwvar.darkpurple.Units

import android.media.audiofx.Equalizer
import android.preference.PreferenceManager
import android.text.TextUtils
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import java.lang.Exception

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
    private var callback: Callback? = null

    //最低带宽值，默认为无效值：-999
    private var lowestBandLimit: Short = -999
    //最高带宽值，默认为无效值：-999
    private var highestBandLimit: Short = -999
    //可控制带宽范围个数，默认为无效值：0
    private var numberOfBands: Short = 0

    //当前正在使用的均衡器配置名称
    private var usingEqualizerName: String = "Default"
    //当前正在使用的均衡器参数
    private var usingEqualizerArgs: ShortArray = ShortArray(1, { 0 })
    //均衡器配置参数Map
    private var equalizerArgs: LinkedHashMap<String, ShortArray> = LinkedHashMap()

    interface Callback {

        /**
         * 当使用的配置文件发生变化
         *
         * @param name  配置文件名称
         */
        fun onUsingProfileChanged(name: String)
    }

    /**
     * 设置相关的回调接口
     *
     * @param   callback    回调接口，传入NULL，取消回调监听
     */
    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    /**
     * @return  当前正在使用的配置名称
     */
    fun usingProfileName(): String = this.usingEqualizerName

    /**
     * @return  支持调整频谱带宽数量
     */
    fun numberOfBands(): Short = this.numberOfBands

    /**
     * 加载已有的均衡器配置
     */
    fun loadSavedEqualizerArgs() {
        val source: LinkedHashMap<String, ShortArray> = JSONHandler.loadSavedEqualizerArgs()

        this.equalizerArgs.clear()
        this.equalizerArgs.putAll(source)

        //获取最后使用的均衡器配置
        val lastUsingEqualizerName: String? = PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext).getString("LastEqz", null)
        if (!TextUtils.isEmpty(lastUsingEqualizerName) && this.equalizerArgs.containsKey(lastUsingEqualizerName)) {
            //如果成功获取到最后一次使用的配置，并且配置有效
            this.usingEqualizerName = lastUsingEqualizerName!!
            this.equalizerArgs[lastUsingEqualizerName]?.let {
                this.usingEqualizerArgs = it
            }
        } else {
            //获取失败，使用默认配置
            this.usingEqualizerName = "Default"
            this.usingEqualizerArgs = ShortArray(1, { 0 })
        }
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
     * 移除配置数据
     *
     * @param   name    均衡器配置名称
     */
    fun removeSavedEqualizerArgs(name: String) {
        if (this.equalizerArgs.remove(name) != null) {
            JSONHandler.removeEqualizerArg(name)
        }
    }

    /**
     * @return  当前已保存的数据名称列表
     */
    fun savedProfilesName(): ArrayList<String> = ArrayList(this.equalizerArgs.keys)

    /**
     * @return  是否已经存在对应的配置文件
     */
    fun isProfileExisted(name: String): Boolean = this.equalizerArgs.containsKey(name)

    /**
     * 初始化并启动均衡器
     * @param   audioSessionID  需要进行调音的AudioSession ID
     */
    fun initEqualizer(audioSessionID: Int = MediaLibrary.getUsingAudioSessionID()) {
        when (audioSessionID) {
        //当ID为 0 时，表明此ID是无效的，关闭并释放当前的均衡器资源
            0 -> {

                disableEqualizer()
                return
            }

        //与当前使用的ID相同，不进行操作
            this.usingAudioSessionID ->
                return

        //进行均衡器初始化
            else -> {

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

                        //获取可控制的带宽数量
                        this.numberOfBands = it.numberOfBands
                    }
                } catch (e: Exception) {
                    Logger.error(TAG, "初始化均衡器对象时发生异常：\n" + e)
                }

            }
        }

    }

    /**
     * 应用均衡器参数
     * <b> 如果均衡器没有初始化或AudioSession ID异常，则会重新初始化均衡器 </b>
     *
     * @param   argsProfile 参数配置名称，不传入数据则继续使用上一次的均衡器参数。默认配置名称为 Default
     * @return  是否应用成功
     */
    fun applyEqualizerArgs(argsProfile: String = this.usingEqualizerName): Boolean {
        if (!isEnabled() || usingAudioSessionID != MediaLibrary.getUsingAudioSessionID()) {
            //当前使用的AudioSession ID与最新的ID不同，需要重新创建均衡器对象
            initEqualizer()
        }

        if (isEnabled()) {
            //如果此时均衡器已启动

            //尝试进行获取
            var args: ShortArray? = getEqualizerArgsProfile(argsProfile)

            if (args == null) {
                //如果获取失败，则恢复使用 Default配置
                args = getEqualizerArgsProfile("Default")!!
                this.usingEqualizerName = "Default"
            } else {
                //获取成功，设置当前使用的配置名称
                this.usingEqualizerName = argsProfile
            }
            this.usingEqualizerArgs = args

            //开始应用配置数据到均衡器
            for (i in 0 until this.numberOfBands) {
                //在应用配置的时候，如果配置的长度小于可调节数量，则未被配置长度覆盖的用0设置
                if (i in this.usingEqualizerArgs.indices) {
                    applyEqualizerArg(i.toShort(), this.usingEqualizerArgs[i])
                } else {
                    applyEqualizerArg(i.toShort(), 0)
                }
            }

            PreferenceManager.getDefaultSharedPreferences(AppConfigs.ApplicationContext).edit().putString("LastEqz", this.usingEqualizerName)

            if (this.callback != null) {
                this.callback?.onUsingProfileChanged(this.usingEqualizerName)
            }

            return true
        } else {
            Logger.error(TAG, "均衡器未启动或 均衡器参数与可调节带宽数量不符")
        }

        return false
    }

    /**
     * 调整单个带宽的等级
     *
     * @param   band    带宽
     * @param   level   等级
     */
    fun applyEqualizerArg(band: Short, level: Short) {
        if (isEnabled() && band in 0..numberOfBands - 1 && level in lowestBandLimit..highestBandLimit) {
            //调节范围合法，并条件允许调节
            this.equalizer?.setBandLevel(band, level)
        }
    }

    /**
     * 更新均衡器保存的参数
     *
     * @param   name    参数名称
     * @param   args    参数
     */
    fun updateEqualizerArgs(name: String, args: ShortArray) {
        if (isProfileExisted(name)) {
            //已存在数据，进行替换
            this.equalizerArgs[name] = args
        } else {
            //新的配置，进行储存
            this.equalizerArgs.put(name, args)
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
     * @return  配置文件数据，如果获取失败，返回 NULL
     */
    fun getEqualizerArgsProfile(argsProfile: String): ShortArray? {
        if (equalizerArgs.containsKey(argsProfile)) {
            return equalizerArgs[argsProfile]!!
        } else {
            return null
        }
    }

}