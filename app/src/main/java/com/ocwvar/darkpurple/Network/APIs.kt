package com.ocwvar.darkpurple.Network

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 11:03 PM
 * File Location com.ocwvar.darkpurple.Network
 * This file use to :   网络接口
 */
object APIs {

    val baseURL: String = "http://192.168.1.101:1008/"

    val loginURL: String = baseURL + "api/User/Login"

    val registerURL: String = baseURL + "api/User/Register"

    val uploadFile: String = baseURL + "api/Files/Upload"

}