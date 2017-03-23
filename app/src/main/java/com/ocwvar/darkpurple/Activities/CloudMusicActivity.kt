package com.ocwvar.darkpurple.Activities

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.ocwvar.darkpurple.Adapters.CloudMusicAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Callbacks.NetworkCallbacks.OnGetUploadedFilesCallback
import com.ocwvar.darkpurple.Network.Beans.RemoteMusic
import com.ocwvar.darkpurple.Network.Keys
import com.ocwvar.darkpurple.Network.NetworkRequest
import com.ocwvar.darkpurple.Network.NetworkRequestTypes
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.BaseBlurActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/23 3:12 PM
 * File Location com.ocwvar.darkpurple.Activities
 * This file use to :   我的云音乐显示界面
 */
class CloudMusicActivity : BaseBlurActivity(), OnGetUploadedFilesCallback, CloudMusicAdapter.OnListClickCallback {

    var selectedPosition: Int = 0
    val adapter: CloudMusicAdapter = CloudMusicAdapter(this@CloudMusicActivity)
    val requestObject: HashMap<String, String> = HashMap()

    init {
        requestObject.put(Keys.Token, AppConfigs.USER.TOKEN)
    }

    override fun onPreSetup(): Boolean {
        title = AppConfigs.ApplicationContext.getString(R.string.text_cloudMusic_title)
        return true
    }

    override fun setActivityView(): Int {
        return R.layout.activity_cloud_music
    }

    override fun onSetToolBar(): Int {
        return R.id.toolbar
    }

    override fun onSetupViews() {
        val recycleView: RecyclerView = findViewById(R.id.recycleView) as RecyclerView
        recycleView.setHasFixedSize(true)
        recycleView.layoutManager = LinearLayoutManager(this@CloudMusicActivity, LinearLayoutManager.VERTICAL, false)
        recycleView.adapter = adapter

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        NetworkRequest.newRequest(NetworkRequestTypes.获取已上传文件, requestObject, this@CloudMusicActivity)
        showHoldingSnackBar(AppConfigs.ApplicationContext.getString(R.string.simple_loading))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onViewClick(clickedView: View?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onViewLongClick(holdedView: View?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListItemClick(musicObject: RemoteMusic, position: Int) {
        selectedPosition = position
        DownloadThread(musicObject, this@CloudMusicActivity).execute()
    }

    override fun onGotUploadedFiles(files: ArrayList<RemoteMusic>) {
        dismissHoldingSnackBar()
        toolBar?.subtitle = String.format("%s%s", AppConfigs.ApplicationContext.getString(R.string.text_cloudMusic_subTitle), files.size.toString())
        adapter.updateSource(files)
    }

    override fun onError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
        toolBar?.subtitle = message
    }

    /**
     * 下载线程
     */
    private inner class DownloadThread(val musicObject: RemoteMusic, val context: Context) : AsyncTask<Void, Int, Boolean>(), DialogInterface.OnCancelListener {

        private val progressDialog: ProgressDialog = ProgressDialog(context)

        init {
            progressDialog.setTitle(R.string.text_cloudMusic_download_status_title);
            progressDialog.setMessage(musicObject.name)
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.progress = 0
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setOnCancelListener(this@DownloadThread)
        }

        override fun onCancel(dialog: DialogInterface) {
            File(AppConfigs.DownloadMusicFolder + musicObject.fileName).delete()
            cancel(true)
            dialog.dismiss()
            Toast.makeText(context, R.string.text_cloudMusic_download_status_cancel, Toast.LENGTH_SHORT).show()
        }

        override fun onPreExecute() {
            progressDialog.show()
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            val downloadFile: File = createDownloadFile() ?: return false

            val client: OkHttpClient = OkHttpClient()
            val request: Request = Request.Builder().url(musicObject.musicURL).build()
            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody: ResponseBody = response.body()
                val totalLength: Float = responseBody.contentLength().toFloat()
                val inStream: InputStream = responseBody.byteStream()
                val outStream: FileOutputStream = FileOutputStream(downloadFile, false)
                val bytes: ByteArray = kotlin.ByteArray(1024)

                var raidLength: Int
                while (true) {
                    raidLength = inStream.read(bytes)
                    if (raidLength == -1) {
                        break
                    } else {
                        outStream.write(bytes, 0, raidLength)
                        publishProgress((downloadFile.length().toFloat() / totalLength * 100).toInt())
                    }
                }

                inStream.close()
                outStream.flush()
                outStream.close()
                return true
            } else {
                return false
            }
        }

        /**
         * 创建下载文件对象
         * @return  下载文件对象,NULL为无法生成
         */
        private fun createDownloadFile(): File? {
            val folder: File = File(AppConfigs.DownloadMusicFolder)
            if (!folder.exists()) {
                folder.mkdirs()
            }

            if (!folder.isDirectory || !folder.canWrite()) {
                return null
            } else {
                return File(AppConfigs.DownloadMusicFolder + musicObject.fileName)
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            progressDialog.progress = values[0]!!
        }

        override fun onPostExecute(result: Boolean) {
            progressDialog.dismiss()
            if (result) {
                adapter.notifyItemChanged(selectedPosition)
                Toast.makeText(context, R.string.text_cloudMusic_download_status_done, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.text_cloudMusic_download_status_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

}