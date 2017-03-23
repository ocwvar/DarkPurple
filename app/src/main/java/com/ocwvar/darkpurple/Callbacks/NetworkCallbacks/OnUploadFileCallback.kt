package com.ocwvar.darkpurple.Callbacks.NetworkCallbacks

import com.ocwvar.darkpurple.Callbacks.BaseCallback

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/19 11:17 PM
 * File Location com.ocwvar.darkpurple.Callbacks
 * This file use to :   上传文件回调接口
 */
interface OnUploadFileCallback : BaseCallback {

    /**
     * 上传文件成功接口
     */
    fun OnUploaded(message: String)

}