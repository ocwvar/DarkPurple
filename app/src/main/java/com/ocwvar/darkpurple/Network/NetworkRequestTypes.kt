package com.ocwvar.darkpurple.Network

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 9:42 PM
 * File Location com.ocwvar.darkpurple.Network
 * This file use to :   网络请求类型
 */
enum class NetworkRequestTypes {
    /**
     * 回调接口:OnLoginCallbacks
     * @param args HashMap请求头. 包含username,password
     * @param Keys.isLoginAction Boolean . False则为注册请求
     */
    登录或注册,

    /**
     *
     * 回调接口:OnUploadFileCallback
     * @param   Keys.Token  String. 使用请求的token
     * @param   Keys.FilePath    String.    要上传的文件路径
     * @param   Keys.MusicTitle    String.  歌曲名称
     * @param   Keys.CoverPath    String.   封面路径 **非必需
     */
    上传文件

}