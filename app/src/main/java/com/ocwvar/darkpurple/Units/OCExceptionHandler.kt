package com.ocwvar.darkpurple.Units

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/19 12:29 AM
 * File Location com.ocwvar.darkpurple.Units
 * This file use to :   Kotlin 未处理异常捕捉器
 */
class OCExceptionHandler(val context: Context) : Thread.UncaughtExceptionHandler {

    /**
     * 异常日志储存文件夹
     */
    private val LOG_SAVE_PATH = Environment.getExternalStorageDirectory().path + "/OCLogs/"

    /**
     * 未处理异常发生回调器
     * @param   thread  异常发生的线程
     * @param   exception   未捕捉的异常
     */
    override fun uncaughtException(thread: Thread?, exception: Throwable?) {
        if (thread != null && exception != null && preCheckSavePath(LOG_SAVE_PATH) && (Build.VERSION.SDK_INT < 23 || (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))) {
            //线程不为空 , 异常不为空 , 拥有内存写入权限
            try {
                createLogFile(thread, exception)
            } catch(e: Exception) {
                Log.e(this@OCExceptionHandler::class.java.simpleName, "CAN NOT WRITE LOG FILE !")
            }
        }
        System.exit(0)
    }

    /**
     * 生成错误日志
     * @param   thread  异常发生的线程
     * @param   exception   未捕捉的异常
     */
    private fun createLogFile(thread: Thread, exception: Throwable) {
        val dataString: String = DateFormat.format("yyyy-MM-dd hh:mm:ss", Date()).toString()
        val logFile: File = File(LOG_SAVE_PATH + dataString + ".log")

        if (logFile.createNewFile()) {
            //成功创建空日志文件
            val fileWriter: FileWriter = FileWriter(logFile, false)
            val printWriter: PrintWriter = PrintWriter(fileWriter)
            printWriter.println("Date:" + dataString + "\n")
            printWriter.println("Exception Class Name: ")
            printWriter.println(exception.stackTrace[0].className)
            printWriter.println("")
            printWriter.println("From Thread: ")
            printWriter.println(thread.name)
            printWriter.println("")
            printWriter.println("Exception Class Position: ")
            printWriter.println("Line number: " + exception.stackTrace[0].lineNumber)
            printWriter.println("")
            printWriter.println("Exception Cause: ")
            printWriter.println(exception.message)
            printWriter.println("")
            printWriter.println("-----------------------------------\nException Message: \n")
            for (i in 0..exception.stackTrace.size - 1) {
                printWriter.println(exception.stackTrace[i])
            }
            printWriter.flush()
            fileWriter.flush()
            printWriter.close()
            fileWriter.close()
        }
    }

    /**
     * 检查并创建储存目录
     * @param   path    目录地址
     * @return  是否可用
     */
    private fun preCheckSavePath(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        } else {
            val folder: File = File(path)
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    return false
                }
            }

            return folder.canWrite() && folder.canRead()
        }
    }

}