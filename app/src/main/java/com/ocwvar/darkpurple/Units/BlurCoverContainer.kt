package com.ocwvar.darkpurple.Units

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
import java.io.File
import java.lang.ref.WeakReference

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 12:08 AM
 * File Location com.ocwvar.darkpurple.Units
 * This file use to :   封面模糊处理与储存类
 */
object BlurCoverContainer {

    //模糊封面Drawable对象储存容器
    private var weakContainer: WeakReference<Drawable>? = null
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
     * @param   coverPath   图像文件路径
     */
    fun handleThis(coverPath: String) {
        val coverFile: File = File(coverPath)
        handleThis(coverFile)
    }

    /**
     * 生成模糊图像
     * @param   coverFile   图像文件
     */
    fun handleThis(coverFile: File) {
        if (handlingFilePath == coverFile.path) {
            //已有相同的任务
            Logger.error(javaClass.simpleName, "当前已有相同的任务：" + coverFile.path)
        } else if (lastCompletedPath == coverFile.path) {
            //上一次完成的任务与本次相同
            val blurDrawable: Drawable? = weakContainer?.get()
            if (blurDrawable == null) {
                //如果上次生成的图像已经失效，则重新进行生成操作
                Logger.warnning(javaClass.simpleName, "上次图像已失效，进行重新生成：" + coverFile.path)
                handlingFilePath = coverFile.path
                jobThread?.cancel(true)
                jobThread = BlurThread(coverFile.path)
                jobThread?.execute()
            } else {
                //上次生成的图像仍可以使用，直接通过回调接口返回
                Logger.warnning(javaClass.simpleName, "请求图像与现有图像相同：" + coverFile.path)
                callback?.onCompleted(blurDrawable)
            }
        } else if (coverFile.exists() && coverFile.canRead()) {
            //开始执行新的任务
            Logger.warnning(javaClass.simpleName, "开始执行新的模糊处理：" + coverFile.path)
            handlingFilePath = coverFile.path
            jobThread?.cancel(true)
            jobThread = BlurThread(coverFile.path)
            jobThread?.execute()
        } else {
            //无法执行任务
            Logger.error(javaClass.simpleName, "封面图像文件缺失，不执行此次任务：" + coverFile.path)
        }
    }

    /**
     * @param   callback    结果回调接口
     */
    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun onCompleted(drawable: Drawable)
        fun onFailed(drawable: Drawable = ColorDrawable(AppConfigs.Color.WindowBackground_Color))
    }

    /**
     * 图像模糊处理工作子线程
     */
    class BlurThread(val coverFilePath: String) : AsyncTask<Void, Int, Drawable?>() {
        val scaleSize: Float = 0.2f

        override fun doInBackground(vararg params: Void?): Drawable? {
            //原始图像
            var bitmap: Bitmap = BitmapFactory.decodeFile(coverFilePath) ?: return null
            //模糊图像
            var blurBitmap: Bitmap = Bitmap.createBitmap((bitmap.width / scaleSize).toInt(), (bitmap.height / scaleSize).toInt(), Bitmap.Config.ARGB_8888)
            val matrix: Matrix = Matrix()
            matrix.postScale(scaleSize, scaleSize)
            //得到缩小指定倍数后的原始图像
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, (bitmap.width / scaleSize).toInt(), (bitmap.height / scaleSize).toInt(), matrix, false)

            try {
                //原始图像进行模糊处理后存入模糊图像
                if (Build.VERSION.SDK_INT >= 17) {
                    val renderScript = RenderScript.create(AppConfigs.ApplicationContext)
                    val inAllocation: Allocation = Allocation.createFromBitmap(renderScript, bitmap)
                    val outAllocation = Allocation.createFromBitmap(renderScript, blurBitmap)
                    val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, inAllocation.element)
                    scriptIntrinsicBlur.setInput(inAllocation)
                    scriptIntrinsicBlur.setRadius(25f)
                    scriptIntrinsicBlur.forEach(outAllocation)
                    outAllocation.copyTo(blurBitmap)

                    scriptIntrinsicBlur.destroy()
                    renderScript.destroy()
                } else {
                    blurBitmap = FastBlur.doBlur(bitmap, 25, false)
                }
                val blurDrawable: Drawable = BitmapDrawable(AppConfigs.ApplicationContext.resources, blurBitmap)
                weakContainer = WeakReference(blurDrawable)
                return blurDrawable
            } catch(e: Exception) {
                return null
            }
        }

        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)
            handlingFilePath = null
            if (result != null) {
                lastCompletedPath = coverFilePath
                callback?.onCompleted(result)
            } else {
                callback?.onFailed()
            }
        }
    }

}