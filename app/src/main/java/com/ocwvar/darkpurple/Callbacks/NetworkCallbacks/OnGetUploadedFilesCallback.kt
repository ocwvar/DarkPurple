package com.ocwvar.darkpurple.Callbacks.NetworkCallbacks

import com.ocwvar.darkpurple.Callbacks.BaseCallback
import com.ocwvar.darkpurple.Network.Beans.RemoteMusic

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/23 2:34 PM
 * File Location com.ocwvar.darkpurple.Callbacks.NetworkCallbacks
 * This file use to :   获取已上传的文件回调接口
 */
interface OnGetUploadedFilesCallback : BaseCallback {

    fun onGotUploadedFiles(files: ArrayList<RemoteMusic>)

}