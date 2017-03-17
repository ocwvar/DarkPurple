package com.ocwvar.darkpurple.Network

import android.os.Handler
import android.os.Looper
import com.google.gson.GsonBuilder
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Callbacks.BaseCallback
import com.ocwvar.darkpurple.Callbacks.LoginUI.OnLoginCallbacks
import com.ocwvar.darkpurple.Network.Beans.ResultMsg
import com.ocwvar.darkpurple.Network.Callbacks.NormalResponseCallback
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.Logger
import com.ocwvar.darkpurple.Units.OCThreadExecutor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

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
    private val threadPool: OCThreadExecutor = OCThreadExecutor(1, "NetworkTask")
    private val retrofitClient: Retrofit

    init {
        val gsonConverterFactory = GsonConverterFactory.create(GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create())
        retrofitClient = Retrofit.Builder()
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
        threadPool.submit(FutureTask(TaskThread(requestTypes, requestObjects, baseCallback)), requestTypes.name)
    }

    /**
     * 网络请求执行线程
     */
    private class TaskThread(val requestTypes: NetworkRequestTypes, val requestObjects: HashMap<String, *>, val baseCallback: BaseCallback) : Callable<Any> {

        override fun call(): Any {
            try {
                when (requestTypes) {

                    NetworkRequestTypes.登录或注册 -> {
                        requests.login(requestObjects, baseCallback)
                    }

                }
            } catch (e: Exception) {
                baseCallback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_unknown_error))
            }

            threadPool.removeTag(requestTypes.name)
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
         * 登录网络请求
         */
        fun login(requestObjects: HashMap<String, *>, baseCallback: BaseCallback) {

            val callback = baseCallback as OnLoginCallbacks
            val handler: Handler = Handler(Looper.getMainLooper())

            //检查参数是否齐全
            if (!isHasRequireKeys(arrayOf("args", "isLogin"), requestObjects)) {
                Logger.error(TAG, "请求参数不完全")
                handler.post {
                    callback.onError(AppConfigs.ApplicationContext.getString(R.string.network_simple_unknown_error))
                }
            } else {
                //提取请求头
                val headers: HashMap<String, String> = requestObjects["args"] as HashMap<String, String>

                //创建请求接口
                val normalResponse: NormalResponseCallback = retrofitClient.create(NormalResponseCallback::class.java)

                //判断是否为登录请求,分别设置不同的URL
                val apiUrl: String
                if (requestObjects["isLogin"] as Boolean) {
                    apiUrl = APIs.loginURL
                } else {
                    apiUrl = APIs.registerURL
                }

                try {
                    //执行请求
                    val response: Response<ResultMsg<Any?>> = normalResponse.SimpleResponseCallback(apiUrl, headers).execute()
                    if (response.isSuccessful) {
                        if (response.body().isSuccess) {
                            //执行成功 , 将TOKEN存入用户数据中
                            AppConfigs.USER.TOKEN = response.body().inObject as String

                            //返回执行结果至回调接口
                            if (requestObjects["isLogin"] as Boolean) {
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

    }

}