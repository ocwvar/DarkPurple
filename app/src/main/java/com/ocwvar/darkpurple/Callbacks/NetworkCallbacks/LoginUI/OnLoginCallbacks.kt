package com.ocwvar.darkpurple.Callbacks.NetworkCallbacks.LoginUI

import com.ocwvar.darkpurple.Callbacks.BaseCallback

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 9:37 PM
 * File Location com.ocwvar.darkpurple.Callbacks.NetworkCallbacks.LoginUI
 * This file use to :   登录界面使用的回调接口
 */
interface OnLoginCallbacks : BaseCallback {

    fun onLoginSuccess(username: String)

    fun onRegisterSuccess(username: String)

}