package com.ocwvar.darkpurple.Units.Cover

import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Units.JSONHandler
import java.io.File

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-7-23 下午4:13
 * File Location com.ocwvar.darkpurple.Units.Cover
 * This file use to :   封面以及封面颜色 统一管理器
 */
object CoverManager {

    //自定义封面库(音频路径，封面路径)
    private val customCoverLibrary: LinkedHashMap<String, String> = LinkedHashMap()

    //普通封面库(音频路径，封面路径)
    private val coverLibrary: LinkedHashMap<String, String> = LinkedHashMap()

    //模糊图像库(音频路径，封面路径)
    private val blurLibrary: LinkedHashMap<String, String> = LinkedHashMap()

    //普通封面颜色库(音频路径，封面颜色)
    private val coverColorLibrary: LinkedHashMap<String, Int> = LinkedHashMap()

    //自定义封面颜色库(音频路径，封面颜色)
    private val customCoverColorLibrary: LinkedHashMap<String, Int> = LinkedHashMap()

    /**
     * 从文件读取所有数据
     */
    fun initData() {
        //读取封面
        putSource(CoverType.NORMAL, JSONHandler.getCoverLibrary(CoverType.NORMAL), true)
        putSource(CoverType.CUSTOM, JSONHandler.getCoverLibrary(CoverType.CUSTOM), true)
        putSource(CoverType.BLUR, JSONHandler.getCoverLibrary(CoverType.BLUR), true)

        //读取颜色
        putColorSource(ColorType.NORMAL, JSONHandler.getColorLibrary(ColorType.NORMAL), true)
        putColorSource(ColorType.CUSTOM, JSONHandler.getColorLibrary(ColorType.CUSTOM), true)
    }

    /**
     * 清空库
     */
    fun clearAllLibrary() {
        coverLibrary.clear()
        customCoverLibrary.clear()
        blurLibrary.clear()
        coverColorLibrary.clear()
        customCoverColorLibrary.clear()
        File(AppConfigs.DataFolder + "CoverLibrary_" + CoverType.NORMAL.name + ".data").delete()
        File(AppConfigs.DataFolder + "CoverLibrary_" + CoverType.CUSTOM.name + ".data").delete()
        File(AppConfigs.DataFolder + "CoverLibrary_" + CoverType.BLUR.name + ".data").delete()
        File(AppConfigs.DataFolder + "ColorLibrary_" + ColorType.NORMAL.name + ".data").delete()
        File(AppConfigs.DataFolder + "ColorLibrary_" + ColorType.CUSTOM.name + ".data").delete()
    }

    /**
     * 异步保存所有数据
     */
    fun asyncUpdateFileCache() {
        synchronized(this@CoverManager, {
            //直接创建线程，不需要关心结果
            Thread(Runnable {
                //保存封面
                JSONHandler.saveCoverLibrary(coverLibrary, CoverType.NORMAL)
                JSONHandler.saveCoverLibrary(customCoverLibrary, CoverType.CUSTOM)
                JSONHandler.saveCoverLibrary(blurLibrary, CoverType.BLUR)

                //保存颜色
                JSONHandler.saveColorLibrary(coverColorLibrary, ColorType.NORMAL)
                JSONHandler.saveColorLibrary(customCoverColorLibrary, ColorType.CUSTOM)
            }).start()
        })
    }

    /**
     * 导入已有数据
     *
     * @param   type    获取类型
     * @param   source  数据源
     * @param   clearBeforePut  是否在导入之前清除已有数据
     */
    fun putSource(type: CoverType, source: LinkedHashMap<String, String>?, clearBeforePut: Boolean) {
        source ?: return

        when (type) {
            CoverType.NORMAL -> {
                if (clearBeforePut) {
                    coverLibrary.clear()
                }
                coverLibrary.putAll(source)
            }

            CoverType.CUSTOM -> {
                if (clearBeforePut) {
                    customCoverLibrary.clear()
                }
                customCoverLibrary.putAll(source)
            }

            CoverType.BLUR -> {
                if (clearBeforePut) {
                    blurLibrary.clear()
                }
                blurLibrary.putAll(source)
            }
        }
    }

    /**
     * 导入已有的颜色数据
     *
     * @param   type    获取类型
     * @param   source  数据源
     * @param   clearBeforePut  是否在导入之前清除已有数据
     */
    fun putColorSource(type: ColorType, source: LinkedHashMap<String, Int>?, clearBeforePut: Boolean) {
        source ?: return

        when (type) {
            ColorType.NORMAL -> {
                if (clearBeforePut) {
                    coverColorLibrary.clear()
                }
                coverColorLibrary.putAll(source)
            }

            ColorType.CUSTOM -> {
                if (clearBeforePut) {
                    customCoverColorLibrary.clear()
                }
                customCoverColorLibrary.putAll(source)
            }
        }
    }

    /**
     * 移除封面数据，同时移除对应的颜色资源
     *
     * @param   type    封面类型
     * @param   key 要移除的Key
     */
    fun removeSource(type: CoverType, key: String?) {
        key ?: return

        when (type) {
            CoverType.NORMAL -> {
                if (coverLibrary.containsKey(key)) {
                    coverLibrary.remove(key)
                    coverColorLibrary.remove(key)
                }
            }

            CoverType.CUSTOM -> {
                if (customCoverLibrary.containsKey(key)) {
                    customCoverLibrary.remove(key)
                    customCoverColorLibrary.remove(key)
                }
            }

            CoverType.BLUR -> {
                if (blurLibrary.containsKey(key)) {
                    blurLibrary.remove(key)
                }
            }
        }
    }

    /**
     * 添加数据
     *
     * @param   type    操作类型
     * @param   key Key，音频路径
     * @param   source 数据源，封面路径
     * @param   force 当Key不存在时，添加数据
     */
    fun setSource(type: CoverType, key: String, source: String, force: Boolean) {
        when (type) {
            CoverType.NORMAL -> {
                if (coverLibrary.containsKey(key)) {
                    coverLibrary[key] = source
                } else if (force) {
                    coverLibrary.put(key, source)
                }
            }
            CoverType.CUSTOM -> {
                if (customCoverLibrary.containsKey(key)) {
                    customCoverLibrary[key] = source
                } else if (force) {
                    customCoverLibrary.put(key, source)
                }
            }
            CoverType.BLUR -> {
                if (blurLibrary.containsKey(key)) {
                    blurLibrary[key] = source
                } else if (force) {
                    blurLibrary.put(key, source)
                }
            }
        }
    }

    /**
     * 添加颜色数据
     *
     * @param   type    操作类型
     * @param   key Key，音频路径
     * @param   source 数据源，封面颜色
     * @param   force 当Key不存在时，添加数据
     */
    fun setColorSource(type: ColorType, key: String, source: Int, force: Boolean) {
        when (type) {
            ColorType.NORMAL -> {
                if (coverColorLibrary.containsKey(key)) {
                    coverColorLibrary[key] = source
                } else if (force) {
                    coverColorLibrary.put(key, source)
                }
            }
            ColorType.CUSTOM -> {
                if (customCoverColorLibrary.containsKey(key)) {
                    customCoverColorLibrary[key] = source
                } else if (force) {
                    customCoverColorLibrary.put(key, source)
                }
            }
        }
    }

    /**
     * 获取颜色
     *
     * @param   type    获取类型
     * @param   key 提取Key，音频路径
     * @return  存在则返回对应的颜色，否则返回默认封面颜色
     */
    fun getColor(type: ColorType, key: String?): Int {
        key ?: return AppConfigs.Color.DefaultCoverColor
        val coverColor: Int?
        when (type) {
            ColorType.NORMAL -> {
                coverColor = coverColorLibrary[key]
            }

            ColorType.CUSTOM -> {
                coverColor = customCoverColorLibrary[key]
            }
        }
        coverColor ?: return AppConfigs.Color.DefaultCoverColor
        return coverColor
    }

    /**
     * 获取合法的一个颜色。优先获取 ColorType.NORMAL 的，如果没有数据则获取 ColorType.CUSTOM 的
     * @param   key 提取Key
     * @return  存在则返回对应的颜色，否则返回默认封面颜色
     */
    fun getValidColor(key: String?): Int {
        val color: Int = getColor(ColorType.CUSTOM, key)
        if (color == AppConfigs.Color.DefaultCoverColor) {
            return getColor(ColorType.NORMAL, key)
        } else {
            return color
        }
    }

    /**
     * 获取封面文件路径
     *
     * @param   type    获取类型
     * @param   key 提取Key，音频路径
     * @return  封面文件路径(String或URI)，如果Key没有对应的数据，则返回 NULL
     */
    fun getSource(type: CoverType, key: String?): String? {
        key ?: return null
        val result: String?
        when (type) {
            CoverType.NORMAL -> {
                result = coverLibrary[key]
            }

            CoverType.CUSTOM -> {
                result = customCoverLibrary[key]
            }

            CoverType.BLUR -> {
                result = blurLibrary[key]
            }
        }

        result ?: return null

        return result
    }

    /**
     * 获取合法的一个封面数据。优先获取 CoverType.CUSTOM 的，如果没有数据则获取 CoverType.NORMAL 的
     * @param   key 提取Key
     * @return  封面数据，String 或 Uri。无数据则返回 NULL
     */
    fun getValidSource(key: String?): String? {
        var source: String? = getSource(CoverType.CUSTOM, key)
        if (source == null) {
            source = getSource(CoverType.NORMAL, key)
        }
        return source
    }

    /**
     * 获取封面文件
     *
     * @param   type    获取类型
     * @param   key 提取Key，音频路径
     * @return  如果文件存在并且可以读取，则返回文件对象。否则返回 NULL
     */
    fun getCover(type: CoverType, key: String): File? {
        val path: String?
        when (type) {
            CoverType.NORMAL -> {
                path = coverLibrary[key]
            }

            CoverType.CUSTOM -> {
                path = customCoverLibrary[key]
            }

            CoverType.BLUR -> {
                path = blurLibrary[key]
            }
        }
        path?.let {
            val fileObject: File = File(it)
            if (fileObject.exists() && fileObject.canRead()) {
                return fileObject
            }
        }
        return null
    }

    /**
     * 获取封面文件路径的绝对路径
     *
     * @param   source  数据源
     * @return  如果数据源为空或数据源为URI数据，返回 NULL
     */
    fun getAbsoluteSource(source: String?): String? {
        source ?: return null
        if (source.startsWith("content:")) {
            return null
        } else {
            return "file:///" + source
        }
    }

}