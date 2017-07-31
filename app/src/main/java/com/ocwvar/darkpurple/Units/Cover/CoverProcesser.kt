package com.ocwvar.darkpurple.Units.Cover

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Units.FastBlur
import com.ocwvar.darkpurple.Units.Logger
import java.io.File

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 12:08 AM
 * File Location com.ocwvar.darkpurple.Units
 * This file use to :   封面模糊处理与储存类
 */
object CoverProcesser {

    //广播通知更新的Action
    val ACTION_BLUR_UPDATED: String = "ACTION_BLUR_UPDATED"
    val ACTION_BLUR_UPDATE_FAILED: String = "ACTION_BLUR_UPDATE_FAILED"
    //清晰封面Drawable对象储存容器
    private var original: Drawable? = null
    //模糊封面Drawable对象储存容器
    private var blurd: Drawable? = null
    //储存最后一次完成的任务图像路径，供弱引用图像失效时重新生成使用
    private var lastCompletedPath: String? = null
    //储存当前正在处理的图像路径，供新的任务请求时检查使用
    private var handlingFilePath: String? = null
    //模糊任务线程
    private var jobThread: BlurThread? = null
    //任务回调接口
    private var callback: Callback? = null

    /**
     * 生成模糊图像
     * @param   coverFile   图像文件
     * @param   coverID   图像ID
     */
    fun handleThis(coverFile: File, coverID: String) {
        if (handlingFilePath == coverFile.path) {
            //已有相同的任务
            Logger.error(javaClass.simpleName, "当前已有相同的任务：" + coverFile.path)
        } else if (lastCompletedPath == coverFile.path) {
            //上一次完成的任务与本次相同
            if (blurd == null) {
                //如果上次生成的图像已经失效，则重新进行生成操作
                Logger.warnning(javaClass.simpleName, "上次图像已失效，进行重新生成：" + coverFile.path)
                handlingFilePath = coverFile.path
                jobThread?.cancel(true)
                jobThread = BlurThread(coverFile.path, coverID)
                jobThread?.execute()
            } else {
                //上次生成的图像仍可以使用，直接通过回调接口返回
                Logger.warnning(javaClass.simpleName, "请求图像与现有图像相同：" + coverFile.path)
                callback?.onCompleted(blurd!!)
            }
        } else if (coverFile.exists() && coverFile.canRead()) {
            //开始执行新的任务
            Logger.warnning(javaClass.simpleName, "开始执行新的模糊处理：" + coverFile.path)
            handlingFilePath = coverFile.path
            jobThread?.cancel(true)
            jobThread = BlurThread(coverFile.path, coverID)
            jobThread?.execute()
        } else {
            //无法执行任务
            Logger.error(javaClass.simpleName, "封面图像文件缺失，不执行此次任务：" + coverFile.path)
        }
    }

    /**
     * 获取上一次的模糊结果
     * @return  Drawable图像，当资源被释放时返回NULL
     */
    fun getBlur(): Drawable? = blurd

    /**
     * 获取上一次的原图结果
     * @return  Drawable图像，当资源被释放时返回NULL
     */
    fun getOriginal(): Drawable? = original

    /**
     * @param   callback    结果回调接口
     */
    fun setCallback(callback: Callback) {
        CoverProcesser.callback = callback
    }

    /**
     * 释放所有数据
     */
    fun release() {
        jobThread?.cancel(true)
        lastCompletedPath = null
        handlingFilePath = null
        blurd = null
        original = null
    }

    interface Callback {
        fun onCompleted(drawable: Drawable)
        fun onFailed(drawable: Drawable = ColorDrawable(AppConfigs.Color.WindowBackground_Color))
    }

    /**
     * 图像模糊处理工作子线程
     */
    class BlurThread(val coverFilePath: String, val coverID: String) : AsyncTask<Void, Int, Drawable?>() {
        //模糊图像缩小倍数
        val scaleSize: Float = 0.4f

        override fun doInBackground(vararg params: Void?): Drawable? {
            //原始图像
            val originalBitmap: Bitmap = BitmapFactory.decodeFile(coverFilePath) ?: return null
            val matrix: Matrix = Matrix()
            matrix.postScale(scaleSize, scaleSize)
            //得到缩小指定倍数后的原始图像
            val scaledBitmap: Bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            //储存原始图像
            original = BitmapDrawable(AppConfigs.ApplicationContext.resources, scaledBitmap)
            //模糊图像容器，图像尺寸为已缩小后的图像尺寸
            var blurBitmap: Bitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)

            try {
                //原始图像进行模糊处理后存入模糊图像
                if (Build.VERSION.SDK_INT >= 17) {
                    //RenderScript处理方式
                    val renderScript = RenderScript.create(AppConfigs.ApplicationContext)
                    val inAllocation: Allocation = Allocation.createFromBitmap(renderScript, scaledBitmap)
                    val outAllocation = Allocation.createFromBitmap(renderScript, blurBitmap)
                    val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, inAllocation.element)
                    scriptIntrinsicBlur.setInput(inAllocation)
                    scriptIntrinsicBlur.setRadius(25f)
                    scriptIntrinsicBlur.forEach(outAllocation)
                    outAllocation.copyTo(blurBitmap)

                    scriptIntrinsicBlur.destroy()
                    renderScript.destroy()
                } else {
                    //低版本API兼容方式，并不能保证每次处理都正确
                    blurBitmap = FastBlur.doBlur(scaledBitmap, 25, false)
                }
                blurd = BitmapDrawable(AppConfigs.ApplicationContext.resources, blurBitmap)
                return blurd
            } catch(e: Exception) {
                blurd = null
                return null
            }
        }

        override fun onCancelled() {
            super.onCancelled()
            handlingFilePath = null
        }

        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)
            handlingFilePath = null
            if (result != null) {
                lastCompletedPath = coverFilePath
                CoverManager.setSource(CoverType.BLUR, coverID, coverFilePath, true)
                AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATED))
                callback?.onCompleted(result)
            } else {
                CoverManager.removeSource(CoverType.BLUR, coverID)
                AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATE_FAILED))
                callback?.onFailed()
            }
        }
    }

}