package com.ocwvar.darkpurple.Network.Callbacks

import com.ocwvar.darkpurple.Network.Beans.RemoteMusic
import com.ocwvar.darkpurple.Network.Beans.ResultMsg
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 9:29 PM
 * File Location com.ocwvar.darkpurple.Network.Callbacks
 * This file use to :   Retrofit网络请求接口
 */
interface NetWorkResponseCallback {

    @POST
    fun SimpleResponseCallback(@Url apiURL: String, @HeaderMap headers: HashMap<String, String>): Call<ResultMsg<Any?>>

    @POST
    @Multipart
    fun UploadFileCallback(@Url apiURL: String, @HeaderMap headers: HashMap<String, String>, @PartMap filePostBody: HashMap<String, RequestBody>): Call<ResultMsg<Any?>>

    @GET
    fun GetUploadedFiles(@Url apiURL: String, @Header("token") token: String): Call<ResultMsg<ArrayList<RemoteMusic>>>

    @POST
    fun RemoveUploadedFile(@Url apiURL: String, @Header("token") token: String, @Header("fileName") fileName: String): Call<ResultMsg<Any?>>

}