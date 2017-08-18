package com.ocwvar.darkpurple.Units.Cover

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.TextUtils
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
    //储存最后一次完成的任务图像路径
    private var lastCompletedCoverID: String? = null
    //储存当前正在处理的图像ID，供新的任务请求时检查使用
    private var handlingCoverID: String? = null
    //模糊任务线程
    private var jobThread: BlurThread? = null

    /**
     * 生成模糊图像
     * @param   coverID   图像ID
     */
    fun handleThis(coverID: String) {
        if (handlingCoverID == coverID) {
            //已有相同的任务

            Logger.error(javaClass.simpleName, "当前已有相同的任务：" + coverID)
        } else if (lastCompletedCoverID == coverID) {
            //上一次完成的任务与本次相同

            if (blurd == null && !TextUtils.isEmpty(CoverManager.getValidSource(coverID))) {
                //如果上次生成的图像已经失效，则重新进行生成操作
                Logger.warning(javaClass.simpleName, "上次图像已失效，进行重新生成：" + coverID)
                handlingCoverID = coverID
                jobThread?.cancel(true)
                jobThread = BlurThread(coverID)
                jobThread?.execute()
            } else if (blurd != null) {
                //上次生成的图像仍可以使用，直接通过回调接口返回
                Logger.warning(javaClass.simpleName, "请求图像与现有图像相同：" + coverID)
                AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATED))
            } else {
                //封面ID无效
                Logger.error(javaClass.simpleName, "无效封面ID：" + coverID)
                AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATE_FAILED))
            }

        } else if (!TextUtils.isEmpty(CoverManager.getValidSource(coverID))) {
            //开始执行新的任务

            Logger.warning(javaClass.simpleName, "开始执行新的模糊处理：" + coverID)
            handlingCoverID = coverID
            jobThread?.cancel(true)
            jobThread = BlurThread(coverID)
            jobThread?.execute()

        } else {
            //无法执行任务

            Logger.error(javaClass.simpleName, "封面图像文件缺失，不执行此次任务：" + coverID)
            AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATE_FAILED))
        }
    }

    /**
     * @return  上一次完成处理的封面ID
     */
    fun getLastCompletedCoverID(): String = this.lastCompletedCoverID ?: ""

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
     * 释放所有数据
     */
    fun release() {
        jobThread?.cancel(true)
        lastCompletedCoverID = null
        handlingCoverID = null
        blurd = null
        original = null
    }

    /**
     * 图像模糊处理工作子线程
     */
    class BlurThread(private val coverID: String) : AsyncTask<Void, Int, Drawable?>() {

        private val TAG: String = "图像模糊处理"

        //模糊图像缩小倍数
        val scaleSize: Float = 0.4f

        override fun doInBackground(vararg params: Void?): Drawable? {
            Logger.normal(TAG, "开始处理图像：" + coverID)

            //获取原始图像，获取失败则直接判断这次操作为失败
            val originalBitmap: Bitmap = BitmapFactory.decodeFile(CoverManager.getValidSource(coverID)) ?: return null

            //缩放处理原始图像
            val matrix: Matrix = Matrix()
            matrix.postScale(scaleSize, scaleSize)

            //得到缩小指定倍数后的原始图像
            val scaledBitmap: Bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            originalBitmap.recycle()

            //储存原始图像
            original = BitmapDrawable(AppConfigs.ApplicationContext.resources, scaledBitmap)

            //尝试获取已缓存的模糊数据
            val cachedBlurBitmap: Bitmap? = BitmapFactory.decodeFile(CoverManager.getSource(CoverType.BLUR, coverID))
            if (cachedBlurBitmap != null) {
                //如果此时已经获取到了模糊图像，则不需要进行下一步模糊处理
                blurd = BitmapDrawable(AppConfigs.ApplicationContext.resources, cachedBlurBitmap)
                return blurd
            }

            //开始进行模糊处理，创建模糊图像容器，图像尺寸为已缩小后的图像尺寸
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
                if (blurBitmap != null) {
                    //进行图像的暗化处理
                    val canvas = Canvas(blurBitmap)
                    canvas.drawRect(0f, 0f, blurBitmap.width.toFloat(), blurBitmap.height.toFloat(), Paint().let {
                        it.color = CoverManager.getValidColor(coverID)
                        it.alpha = 60
                        it
                    })
                    canvas.drawBitmap(blurBitmap, 0f, 0f, null)
                    //模糊图像生成成功，进行文件缓存
                    val cachedFile: File? = CoverImage2File.getInstance().makeImage2File(CoverType.BLUR, blurBitmap, coverID)
                    cachedFile?.let {
                        //缓存图像路径
                        CoverManager.setSource(CoverType.BLUR, coverID, it.path, true)
                    }
                }
                blurd = BitmapDrawable(AppConfigs.ApplicationContext.resources, blurBitmap)
                Logger.normal(TAG, "图像处理完成：" + coverID)
                return blurd
            } catch (e: Exception) {
                blurd = null
                Logger.normal(TAG, "图像处理失败：" + coverID)
                return null
            }
        }

        override fun onCancelled() {
            super.onCancelled()
            handlingCoverID = null
        }

        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)
            handlingCoverID = null
            if (result != null) {
                lastCompletedCoverID = coverID
                AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATED))
            } else {
                //封面无效，删除缓存数据
                CoverManager.removeSource(CoverType.BLUR, coverID)
                AppConfigs.ApplicationContext.sendBroadcast(Intent(ACTION_BLUR_UPDATE_FAILED))
            }
        }
    }

}