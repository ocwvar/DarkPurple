package com.ocwvar.darkpurple.Network.Callbacks

import com.ocwvar.darkpurple.Network.Beans.ResultMsg
import retrofit2.Call
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 9:29 PM
 * File Location com.ocwvar.darkpurple.Network.Callbacks
 * This file use to :   简单的回调接口
 */
interface NormalResponseCallback {

    @POST
    fun SimpleResponseCallback(@Url apiURL: String, @HeaderMap headers: HashMap<String, String>): Call<ResultMsg<Any?>>

}