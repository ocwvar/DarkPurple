package com.ocwvar.darkpurple.Network

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Callbacks.BaseCallback
import com.ocwvar.darkpurple.Callbacks.LoginUI.OnLoginCallbacks
import com.ocwvar.darkpurple.Callbacks.OnUploadFileCallback
import com.ocwvar.darkpurple.Network.Beans.ResultMsg
import com.ocwvar.darkpurple.Network.Callbacks.NetWorkResponseCallback
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.Logger
import com.ocwvar.reimu.Helper.RMThreadPool
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/15 11:17 PM
 * File Location com.ocwvar.darkpurple.Network
 * This file use to : 网络请求
 */
object NetworkRequest {

    private val TAG: String = "网络请求"
    private val requests: Request = Request()
    private val threadPool: RMThreadPool = RMThreadPool("网络请求", 2, 2)
    private val retrofitClient: Retrofit

    init {
        val gsonConverterFactory = GsonConverterFactory.create(GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create())
        val httpClient: OkHttpClient = OkHttpClient()
                .newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        retrofitClient = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(APIs.baseURL)
                .addConverterFactory(gsonConverterFactory)
                .build()
    }

    /**
     * 网络请求
     *
     * @param requestTypes  网络请求的类型
     * @param requestObjects  请求携带的东西
     * @param baseCallback  执行结果返回的回调接口
     */
    fun newRequest(requestTypes: NetworkRequestTypes, requestObjects: HashMap<String, *>, baseCallback: BaseCallback) {
        threadPool.run_single(FutureTask(TaskThread(requestTypes, requestObjects, baseCallback)), requestTypes.name)
    }

    /**
     * 网络请求执行线程
     */
    private class TaskThread(val requestTypes: NetworkRequestTypes, val requestObjects: HashMap<String, *>, val baseCallback: BaseCallback) : Callable<Any> {

        override fun call(): Any {
            try {
                when (requestTypes) {

                    NetworkRequestTypes.登录或注册 -> {
                        requests.loginOrRegister(requestObjects, baseCallback)
                    }

                    NetworkRequestTypes.上传文件 -> {
                        requests.uploadFile(requestObjects, baseCallback)
                    }

                }
            } catch (e: Exception) {
                baseCallback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_unknown_error))
            }

            threadPool.onTaskEnd(requestTypes.name)
            return 0
        }

    }

    /**
     * 网络请求处理模块
     */
    @Suppress("UNCHECKED_CAST")
    private class Request {

        /**
         * 检查是否有所需的键值
         *
         * @param   keysName    所需要的键值名称
         * @param   requestObjects  要检查的容器
         * @return  是否完全相符
         */
        private fun isHasRequireKeys(keysName: Array<String>, requestObjects: HashMap<String, *>): Boolean {
            keysName.forEach {
                if (!requestObjects.containsKey(it)) {
                    return false
                }
            }
            return true
        }

        /**
         * 登录或注册
         * @param   requestObjects  参数容器
         * @param   baseCallback    回调接口
         */
        fun loginOrRegister(requestObjects: HashMap<String, *>, baseCallback: BaseCallback) {

            val callback = baseCallback as OnLoginCallbacks
            val handler: Handler = Handler(Looper.getMainLooper())

            //检查参数是否齐全
            if (!isHasRequireKeys(arrayOf(Keys.argsPackage, Keys.isLoginAction), requestObjects)) {
                Logger.error(TAG, "请求参数不完全")
                handler.post {
                    callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_args_error))
                }
            } else {
                //提取请求头
                val headers: HashMap<String, String> = requestObjects[Keys.argsPackage] as HashMap<String, String>

                //创建请求接口
                val netWorkResponse: NetWorkResponseCallback = retrofitClient.create(NetWorkResponseCallback::class.java)

                //判断是否为登录请求,分别设置不同的URL
                val apiUrl: String
                if (requestObjects[Keys.isLoginAction] as Boolean) {
                    apiUrl = APIs.loginURL
                } else {
                    apiUrl = APIs.registerURL
                }

                try {
                    //执行请求
                    val response: Response<ResultMsg<Any?>> = netWorkResponse.SimpleResponseCallback(apiUrl, headers).execute()
                    if (response.isSuccessful) {
                        if (response.body().isSuccess) {
                            //执行成功 , 将TOKEN存入用户数据中
                            AppConfigs.USER.TOKEN = response.body().inObject as String

                            //返回执行结果至回调接口
                            if (requestObjects[Keys.isLoginAction] as Boolean) {
                                handler.post {
                                    callback.onLoginSuccess(headers[Keys.Username] as String)
                                }
                            } else {
                                handler.post {
                                    callback.onRegisterSuccess(headers[Keys.Username] as String)
                                }
                            }
                        } else {
                            //执行不成功 返回错误信息至回调接口
                            handler.post {
                                callback.onError(response.body().message)
                            }
                        }
                    } else {
                        //服务器异常
                        handler.post {
                            callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_server_error))
                        }
                    }
                } catch(e: Exception) {
                    handler.post {
                        callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_timeout_error))
                    }
                }
            }
        }

        /**
         * 上传文件
         * @param   requestObjects  参数容器
         * @param   baseCallback    回调接口
         */
        fun uploadFile(requestObjects: HashMap<String, *>, baseCallback: BaseCallback) {
            val callback: OnUploadFileCallback = baseCallback as OnUploadFileCallback
            val handler: Handler = Handler(Looper.getMainLooper())

            //请求参数检查
            if (!isHasRequireKeys(arrayOf(Keys.Token, Keys.FilePath, Keys.MusicTitle), requestObjects)) {
                Logger.error(TAG, "请求参数不完全")
                handler.post {
                    callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_args_error))
                }
            } else {
                //获取token字符串
                val token: String = requestObjects[Keys.Token] as String
                //要上传的文件名
                val musicTitle: String = requestObjects[Keys.MusicTitle] as String
                //获取文件路径对象
                val uploadFile: File = File(requestObjects[Keys.FilePath] as String)

                //如果Token以及文件名不为空 同时 要上传的文件存在以及可读
                if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(musicTitle) && uploadFile.exists() && uploadFile.canRead()) {
                    //需要传递的RequestBody Map对象
                    val requestBodys: HashMap<String, RequestBody> = HashMap()

                    //得到文件类型
                    val fileTypes = (requestObjects[Keys.FilePath] as String).substring((requestObjects[Keys.FilePath] as String).lastIndexOf('.') + 1)

                    //创建请求接口
                    val netWorkResponse: NetWorkResponseCallback = retrofitClient.create(NetWorkResponseCallback::class.java)

                    //添加音频文件上传RequestBody
                    requestBodys.put("music", MultipartBody.Part.createFormData("music", null, RequestBody.create(MediaType.parse("application/octet-stream"), uploadFile)).body())

                    //判断是否有封面需要上传
                    if (requestObjects.containsKey(Keys.CoverPath)) {
                        val coverFile: File = File(requestObjects[Keys.CoverPath] as String)
                        if (coverFile.exists() && coverFile.canRead()) {
                            //添加封面文件上传RequestBody
                            requestBodys.put("cover", MultipartBody.Part.createFormData("cover", null, RequestBody.create(MediaType.parse("application/octet-stream"), coverFile)).body())
                        }
                    }

                    //创建请求头MAP
                    val headers: HashMap<String, String> = HashMap()
                    headers.put(Keys.Token, token)
                    headers.put(Keys.FileType, fileTypes)
                    headers.put(Keys.MusicTitle, musicTitle)

                    //执行网络命令
                    val response: Response<ResultMsg<Any?>> = netWorkResponse.UploadFileCallback(APIs.uploadFile, headers, requestBodys).execute()
                    //执行完后检查结果
                    if (response.isSuccessful) {
                        if (response.body().isSuccess) {
                            handler.post {
                                callback.OnUploaded(response.body().message)
                            }
                        } else {
                            handler.post {
                                callback.onError(response.body().message)
                            }
                        }
                    } else {
                        handler.post {
                            callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_timeout_error))
                        }
                    }
                } else {
                    Logger.error(TAG, "请求参数完全 , 但不正确")
                    handler.post {
                        callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_args_error))
                    }
                }
            }
        }

    }

}