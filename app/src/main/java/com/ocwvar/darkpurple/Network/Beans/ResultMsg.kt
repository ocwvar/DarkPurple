package com.ocwvar.darkpurple.Network.Beans

import com.google.gson.annotations.SerializedName

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 11:26 PM
 * File Location com.ocwvar.darkpurple.Network.Beans
 * This file use to :   返回数据基类
 */
class ResultMsg<T>(isSuccess: Boolean, message: String, innerObject: T?) {

    @SerializedName("isSuccess")
    var isSuccess: Boolean = false

    @SerializedName("message")
    var message: String = ""

    @SerializedName("inObject")
    var inObject: T? = null

}