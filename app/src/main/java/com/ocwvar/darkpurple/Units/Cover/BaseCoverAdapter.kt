package com.ocwvar.darkpurple.Units.Cover

import android.graphics.Bitmap
import android.net.Uri
import android.support.annotation.IntegerRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.ocwvar.darkpurple.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-7-26 下午1:55
 * File Location com.ocwvar.darkpurple.Units.Cover
 * This file use to :   带有封面加载操作的适配器
 */
abstract class BaseCoverAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * 加载封面
     *
     * @param   coverID 封面ID
     * @param   defaultRes  默认显示资源
     * @param   targetView  加载到的View
     */
    fun loadCover(coverID: String, @IntegerRes defaultRes: Int, targetView: ImageView) {

        val loader: Picasso = Picasso.with(targetView.context)
        val coverSource: String? = CoverManager.getValidSource(coverID)

        val loadRequest: RequestCreator = if (coverSource == null) {
            //无封面数据
            loader.load(defaultRes)
        } else if (coverSource.startsWith("content:")) {
            //封面为Uri
            loader.load(Uri.parse(coverSource))
        } else if (coverSource.startsWith("/")) {
            //封面为String路径
            loader.load(CoverManager.getAbsoluteSource(coverSource))
        } else {
            //不支持的封面格式类型
            loader.load(defaultRes)
        }

        loadRequest
                .config(Bitmap.Config.RGB_565)
                .error(R.drawable.ic_music_mid)
                .placeholder(defaultRes)
                .fit()
                .into(targetView)
    }

    /**
     * 加载封面颜色
     *
     * @param   coverID 封面ID
     * @param   targetView  加载到的View
     */
    fun loadColor(coverID: String, targetView: View) {
        targetView.setBackgroundColor(CoverManager.getValidColor(coverID))
    }

}