package com.ocwvar.darkpurple.FragmentPages

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import com.ocwvar.darkpurple.Activities.DownloadCoverActivity
import com.ocwvar.darkpurple.Activities.PlayingActivity
import com.ocwvar.darkpurple.Adapters.MusicListAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.PlaylistItem
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Callbacks.MediaScannerCallback
import com.ocwvar.darkpurple.Callbacks.NetworkCallbacks.OnUploadFileCallback
import com.ocwvar.darkpurple.Network.Keys
import com.ocwvar.darkpurple.Network.NetworkRequest
import com.ocwvar.darkpurple.Network.NetworkRequestTypes
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.AudioService
import com.ocwvar.darkpurple.Services.ServiceHolder
import com.ocwvar.darkpurple.Units.Cover.CoverManager
import com.ocwvar.darkpurple.Units.Cover.CoverProcesser
import com.ocwvar.darkpurple.Units.Cover.CoverType
import com.ocwvar.darkpurple.Units.MediaScanner
import com.ocwvar.darkpurple.Units.PlaylistUnits
import com.ocwvar.darkpurple.Units.ToastMaker
import com.squareup.picasso.Picasso
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 11:50 AM
 * File Location com.ocwvar.darkpurple.FragmentPages
 * This file use to :   音乐选择Fragment（主要）
 */
class MusicListFragment : Fragment(), MediaScannerCallback, MusicListAdapter.Callback, View.OnClickListener {

    //歌曲条目展示适配器
    private val adapter: MusicListAdapter = MusicListAdapter(this@MusicListFragment)
    //Fragment页面缓存
    private lateinit var fragmentView: View
    //下拉刷新控件
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    //显示列表控件
    private lateinit var recycleView: RecyclerView
    //长按菜单处理类
    private var itemMoreDialog: ItemMoreDialog? = null
    //添加至播放列表菜单处理类
    private var add2PlaylistDialog: Add2PlaylistDialog? = null
    //创建播放列表处理类
    private var createPlaylistDialog: CreatePlaylistDialog? = null
    //歌曲切换监听广播接收器
    private var playingDataUpdateReceive: PlayingDataUpdateReceiver? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (inflater != null && container != null) {
            return inflater.inflate(R.layout.fragment_music_list, container, false)
        } else {
            return null
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view == null) {
            //Fragment页面创建失败
            ToastMaker.show(R.string.ERROR_SIMPLE)
            return
        } else {
            this.fragmentView = view
        }
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recycleView = view.findViewById(R.id.recycleView)
        recycleView.adapter = adapter
        recycleView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recycleView.setHasFixedSize(true)
        swipeRefreshLayout.setColorSchemeColors(AppConfigs.Color.DefaultCoverColor)
        swipeRefreshLayout.setOnRefreshListener {
            itemMoreDialog?.hide()
            if (adapter.isSelectingMode()) {
                //当前如果是多选模式，不能进行刷新
                ToastMaker.show(R.string.message_waitForSelecting)
                swipeRefreshLayout.isRefreshing = false
            } else {
                //下拉刷新回调接口，设为半透明并无法被触摸
                recycleView.alpha = 0.3f
                recycleView.setOnTouchListener({ _, _ -> true })
                MediaScanner.getInstance().start()
            }
        }

        //设置扫描回调接口
        MediaScanner.getInstance().setCallback(this@MusicListFragment)
    }

    /**
     * 扫描结束回调接口
     * @param   songItems   扫描得到的数据，有可能为NULL
     * @param   isFromLastSaved 此次得到的数据是上一次缓存的结果
     */
    override fun onScanCompleted(songItems: ArrayList<SongItem>?, isFromLastSaved: Boolean) {
        swipeRefreshLayout.isRefreshing = false
        recycleView.alpha = 1.0f
        recycleView.setOnTouchListener({ _, _ -> false })
        if (songItems == null) {
            ToastMaker.show(R.string.message_scan_result_empty)
        } else {
            adapter.changeSource(songItems)
            ToastMaker.show(R.string.message_scan_result_done)
        }
    }

    /**
     * 当前Fragment恢复完成时
     * 更新当前播放曲目条目样式
     * 注册接收器
     */
    override fun onResume() {
        super.onResume()
        if (MediaScanner.getInstance().isHasCachedData) {
            //优先获取上一次保存至本地的扫描结果
            MediaScanner.getInstance().getLastTimeCachedData()
        } else if (MediaScanner.getInstance().isUpdated) {
            //检查是否有最近的扫描缓存
            adapter.changeSource(MediaScanner.getInstance().cachedDatas)
        } else {
            //新的扫描
            MediaScanner.getInstance().start()
        }

        //恢复后先更新当前播放状态
        adapter.updatePlayingPath(ServiceHolder.getInstance().service?.playingSong?.path)
        //注册歌曲切换接收器
        if (playingDataUpdateReceive == null || !playingDataUpdateReceive!!.isRegistered) {
            if (playingDataUpdateReceive == null) {
                playingDataUpdateReceive = PlayingDataUpdateReceiver()
            }
            playingDataUpdateReceive?.let {
                AppConfigs.ApplicationContext.registerReceiver(it, it.intentFilter)
            }
        }
    }

    /**
     * 发送指令到媒体服务
     * @param   commandAction   服务的Action
     * @param   extra   附带的数据
     */
    fun sendCommand(commandAction: String, extra: Bundle?) {
        MediaControllerCompat.getMediaController(this.activity)?.sendCommand(commandAction, extra, null)
    }

    /**
     * 当前Fragment被暂停时
     * 退出多选模式
     * 注销接收器
     */
    override fun onPause() {
        super.onPause()
        //如果当前是多选模式，则退出
        if (adapter.isSelectingMode()) {
            adapter.switchMode()
        }
        //注销歌曲切换接收器
        if (playingDataUpdateReceive != null && playingDataUpdateReceive!!.isRegistered) {
            AppConfigs.ApplicationContext.unregisterReceiver(playingDataUpdateReceive!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == DownloadCoverActivity.DATA_CHANGED) {
            //如果封面发生修改，则回到界面时重新刷新数据
            recycleView.alpha = 0.3f
            recycleView.setOnTouchListener({ _, _ -> true })
            MediaScanner.getInstance().start()
        }
    }

    /**
     * 歌曲单击回调接口
     * @param   songData    歌曲数据
     * @param   position    在列表中的位置
     * @param   itemView    条目的View对象
     */
    override fun onListClick(songData: SongItem, position: Int, itemView: View) {
        ServiceHolder.getInstance().service?.let {
            ////测试 —— 使用媒体服务播放
            /* sendCommand(MediaPlayerService.COMMAND.COMMAND_PLAY_LIBRARY, Bundle().let {
                 it.putString(MediaPlayerService.COMMAND_EXTRA.ARG_STRING_LIBRARY_NAME, "MAIN")
                 it.putInt(MediaPlayerService.COMMAND_EXTRA.ARG_INT_LIBRARY_INDEX, position)
                 it
             })*/

            if (it.play(adapter.source(), position)) {
                //播放成功
                if (AppConfigs.isAutoSwitchPlaying) {
                    //如果设置为自动跳转到播放界面，则进行跳转
                    activity.startActivity(Intent(activity, PlayingActivity::class.java))
                }
            } else {
                //播放失败
                ToastMaker.show(R.string.message_play_error)
            }
        }
    }

    /**
     * 歌曲长按回调接口
     * @param   songData    歌曲数据
     * @param   position    在列表中的位置
     * @param   itemView    条目的View对象
     */
    override fun onListLongClick(songData: SongItem, position: Int, itemView: View) {
        if (itemMoreDialog == null) {
            itemMoreDialog = ItemMoreDialog()
        }
        itemMoreDialog?.show(songData, position)
    }

    /**
     * 点击事件处理位置
     * 1.浮动按钮点击事件，保存播放列表数据
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.fab -> {
                if (adapter.isSelectingMode()) {
                    //确认当前的确为多选模式
                    val array: ArrayList<SongItem> = adapter.selected()
                    if (array.size <= 0) {
                        //没有选择歌曲对象
                        ToastMaker.show(R.string.message_playlist_add_error_no_song)
                    } else {
                        //添加播放列表
                        createPlaylistDialog?.let {
                            PlaylistUnits.getInstance().savePlaylist(it.lastPlaylistName, array)
                            ToastMaker.show(R.string.message_playlist_add_done)
                        }
                    }
                    adapter.switchMode()
                } else {
                    ToastMaker.show(R.string.message_playlist_add_error_no_selecting)
                }
                //隐藏点击按钮并取消点击事件监听
                ((fragmentView.findViewById<FloatingActionButton>(R.id.fab))).let {
                    it.hide()
                    it.visibility = View.GONE
                    it.setOnClickListener(null)
                }
            }
        }
    }

    /**
     * 长按选项菜单处理类
     */
    private inner class ItemMoreDialog : View.OnClickListener {
        //对话框持有器
        private var dialogKeeper: WeakReference<AlertDialog?> = WeakReference(null)
        //当前显示菜单是基于的音频数据
        private lateinit var songData: SongItem
        //当前显示菜单是基于的列表项目位置
        private var songDataPosition: Int = 0

        /**
         * 显示对话框
         * @param   songData    菜单基于的音频数据
         * @param   songDataPosition    列表中的位置
         */
        @SuppressLint("InflateParams")
        fun show(songData: SongItem, songDataPosition: Int) {
            this.songDataPosition = songDataPosition
            this.songData = songData

            val view: View = LayoutInflater.from(fragmentView.context).inflate(R.layout.dialog_music_list_menu, null)
            view.findViewById<View>(R.id.menu_music_delete).setOnClickListener(this@ItemMoreDialog)
            view.findViewById<View>(R.id.menu_music_upload).setOnClickListener(this@ItemMoreDialog)
            view.findViewById<View>(R.id.menu_music_add2playlist).setOnClickListener(this@ItemMoreDialog)
            view.findViewById<View>(R.id.menu_music_create).setOnClickListener(this@ItemMoreDialog)

            val removeCover: View = view.findViewById<View>(R.id.menu_music_cover_remove).let {
                it.visibility = View.GONE
                it.setOnClickListener(this@ItemMoreDialog)
                it
            }
            val downloadCover: View = view.findViewById<View>(R.id.menu_music_online_cover).let {
                it.visibility = View.GONE
                it.setOnClickListener(this@ItemMoreDialog)
                it
            }
            if (!TextUtils.isEmpty(CoverManager.getSource(CoverType.CUSTOM, songData.coverID))) {
                //如果当前有自定义封面，则显示移除按钮，否则显示下载按钮
                removeCover.visibility = View.VISIBLE
            } else {
                downloadCover.visibility = View.VISIBLE
            }

            var dialog: AlertDialog = AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG).setView(view).create()
            dialogKeeper = WeakReference(dialog)
            dialog.show()
        }

        /**
         * 隐藏对话框
         */
        fun hide() {
            dialogKeeper.get()?.dismiss()
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.menu_music_delete -> {
                    //删除文件操作
                    val file: File = File(songData.path)
                    if (file.exists() && file.canWrite() && file.delete()) {
                        //删除文件成功
                        adapter.removeData(songDataPosition)
                        ToastMaker.show(R.string.message_song_deleted)
                        hide()
                    } else {
                        ToastMaker.show(R.string.message_song_delete_failed)
                    }
                }
                R.id.menu_music_upload -> {
                    //上传文件操作
                    if (TextUtils.isEmpty(AppConfigs.USER.TOKEN)) {
                        //如果当前没有Token,则认为当前为离线模式
                        ToastMaker.show(R.string.message_upload_offline)
                    } else {
                        //上传音频文件
                        val args = HashMap<String, String>()
                        args.put(Keys.Token, AppConfigs.USER.TOKEN)
                        args.put(Keys.FilePath, songData.path)
                        args.put(Keys.MusicTitle, songData.title)
                        if (!TextUtils.isEmpty(CoverManager.getSource(CoverType.NORMAL, songData.coverID))) {
                            //有封面存在才需要传递
                            args.put(Keys.CoverPath, CoverManager.getSource(CoverType.NORMAL, songData.coverID)!!)
                        }
                        val result: Boolean = NetworkRequest.newRequest(NetworkRequestTypes.上传文件, args, object : OnUploadFileCallback {
                            override fun OnUploaded(message: String) {
                                //上传成功回调
                                ToastMaker.show(message)
                            }

                            override fun onError(message: String) {
                                //上传失败回调
                                ToastMaker.show(message)
                            }
                        })
                        hide()
                        if (result) {
                            //成功提交任务
                            ToastMaker.show(R.string.message_upload_started)
                        } else {
                            //线程池仍有相同的任务未完成
                            ToastMaker.show(R.string.message_upload_busy)
                        }
                    }
                }
                R.id.menu_music_create -> {
                    //显示创建播放列表对话框
                    if (createPlaylistDialog == null) {
                        createPlaylistDialog = CreatePlaylistDialog()
                    }
                    createPlaylistDialog?.show()
                    hide()
                }
                R.id.menu_music_add2playlist -> {
                    //显示添加至播放列表对话框
                    if (add2PlaylistDialog == null) {
                        add2PlaylistDialog = Add2PlaylistDialog()
                    }
                    add2PlaylistDialog?.show(songData)
                    hide()
                }
                R.id.menu_music_cover_remove -> {
                    //恢复原来的封面

                    //删除下载的封面
                    File(CoverManager.getSource(CoverType.CUSTOM, songData.coverID)!!).delete()

                    //清空自定义数据 和 Picasso的缓存
                    Picasso.with(fragmentView.context).invalidate(File(CoverManager.getSource(CoverType.CUSTOM, songData.coverID)!!))

                    //移除封面库中的数据
                    CoverManager.removeSource(CoverType.CUSTOM, songData.coverID)
                    CoverManager.removeSource(CoverType.BLUR, songData.coverID)

                    //异步保存封面设置到文件
                    CoverManager.asyncUpdateFileCache()

                    //如果当前正在播放的数据就是当前曲目，需要更新缓存的模糊图像
                    if (CoverProcesser.getLastCompletedCoverID() == songData.coverID) {
                        CoverProcesser.handleThis(songData.coverID)
                    }

                    hide()
                    //清除对话框缓存，防止按钮不随封面状态变化
                    dialogKeeper.clear()

                    //重新刷新
                    recycleView.alpha = 0.3f
                    recycleView.setOnTouchListener({ _, _ -> true })
                    MediaScanner.getInstance().start()
                }
                R.id.menu_music_online_cover -> {
                    //打开在线封面获取界面

                    val bundle: Bundle = Bundle().let {
                        it.putParcelable("item", songData)
                        it
                    }
                    DownloadCoverActivity.startBlurActivityForResultByFragment(10, Color.argb(100, 0, 0, 0), false, this@MusicListFragment, DownloadCoverActivity::class.java, bundle, 10)
                    hide()
                    //清除对话框缓存，防止按钮不随封面状态变化
                    dialogKeeper.clear()
                }
            }
        }
    }

    /**
     * 添加歌曲到现有播放列表菜单
     */
    private inner class Add2PlaylistDialog : AdapterView.OnItemClickListener {
        //对话框持有器
        private var dialogKeeper: WeakReference<AlertDialog?> = WeakReference(null)
        //当前显示菜单是基于的音频数据
        private lateinit var songData: SongItem

        /**
         * 显示对话框
         * @param   songData    菜单基于的音频数据
         */
        @SuppressLint("InflateParams")
        fun show(songData: SongItem) {
            this.songData = songData
            var dialog: AlertDialog? = dialogKeeper.get()
            if (dialog == null) {
                val view: View = LayoutInflater.from(fragmentView.context).inflate(R.layout.dialog_addto_playlist, null)
                val listView: ListView = view.findViewById(R.id.listView)
                val adapter: ArrayAdapter<String> = ArrayAdapter(fragmentView.context, R.layout.simple_textview)
                PlaylistUnits.getInstance().playlistSet.forEach {
                    adapter.add(it.name)
                }

                listView.adapter = adapter
                listView.onItemClickListener = this@Add2PlaylistDialog
                dialog = AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG).setView(view).create()
                dialogKeeper = WeakReference(dialog)
            }
            dialog?.show()
        }

        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (PlaylistUnits.getInstance().playlistSet[position].playlist == null) {
                //如果要添加到的列表还没加载 , 则先加载
                PlaylistUnits.getInstance().loadPlaylistAudiosData(object : PlaylistUnits.PlaylistLoadingCallbacks {

                    /**
                     * 准备读取列表数据
                     */
                    override fun onPreLoad() {
                        ToastMaker.show(R.string.message_playlist_loading)
                    }

                    /**
                     * 读取播放列表数据成功
                     * @param playlistItem  读取的播放列表数据
                     * *
                     * @param dataObject  对应的歌曲列表
                     */
                    override fun onLoadCompleted(playlistItem: PlaylistItem, dataObject: ArrayList<SongItem>?) {
                        if (PlaylistUnits.getInstance().addAudio(playlistItem, songData)) {
                            ToastMaker.show(R.string.message_playlist_add_done_item)
                        } else {
                            ToastMaker.show(R.string.message_playlist_add_error_existed)
                        }
                    }

                    /**
                     * 读取播放列表数据失败
                     */
                    override fun onLoadFailed() {
                        ToastMaker.show(R.string.text_playlist_loadFailed)
                    }

                }, PlaylistUnits.getInstance().playlistSet[position])
            } else {
                //如果已经加载了 , 则直接添加进去
                if (PlaylistUnits.getInstance().addAudio(PlaylistUnits.getInstance().playlistSet[position], songData)) {
                    ToastMaker.show(R.string.message_playlist_add_done_item)
                    dialogKeeper.get()?.dismiss()
                } else {
                    ToastMaker.show(R.string.message_playlist_add_error_existed)
                }
            }
        }

    }

    /**
     * 创建新的播放列表对话框处理类
     */
    private inner class CreatePlaylistDialog {
        //对话框持有器
        private var dialogKeeper: WeakReference<AlertDialog?> = WeakReference(null)
        //保存的有效播放列表名称
        var lastPlaylistName: String = ""

        /**
         * 显示对话框
         */
        fun show() {
            var dialog: AlertDialog? = dialogKeeper.get()
            if (dialog == null) {
                //创建输入框对象
                val inputText: EditText = EditText(fragmentView.context)
                inputText.maxLines = 1
                inputText.setBackgroundColor(Color.argb(120, 0, 0, 0))
                inputText.textSize = 15.0f
                inputText.setTextColor(Color.WHITE)
                inputText.setSingleLine(true)
                //创建对话框对象
                dialog = AlertDialog
                        .Builder(fragmentView.context, R.style.FullScreen_TransparentBG)
                        .setView(inputText)
                        .setPositiveButton(R.string.simple_done) { dialogInterface, _ ->
                            //确认    按钮处理
                            val playlistName: String = inputText.text.toString()
                            if (TextUtils.isEmpty(playlistName)) {
                                //检查输入的名称有效性
                                ToastMaker.show(R.string.message_playlist_name_error)
                                return@setPositiveButton
                            }
                            if (PlaylistUnits.getInstance().isPlaylistExisted(playlistName)) {
                                //检查名称是否与现有的播放列表名称重复
                                ToastMaker.show(R.string.message_playlist_name_existed)
                            }
                            dialogInterface.dismiss()
                            //保存当前播放列表名称
                            this.lastPlaylistName = playlistName
                            //切换当前列表为多选模式
                            if (!adapter.isSelectingMode()) {
                                ToastMaker.show(R.string.message_plzSelect)
                                adapter.switchMode()
                                //显示确定按钮并设置监听事件
                                ((fragmentView.findViewById<FloatingActionButton>(R.id.fab))).let {
                                    it.show()
                                    it.visibility = View.VISIBLE
                                    it.setOnClickListener(this@MusicListFragment)
                                }
                            }
                            //清空输入框
                            inputText.text.clear()
                        }
                        .setNegativeButton(R.string.simple_cancel, { dialogInterface, _ ->
                            //取消    按钮处理
                            dialogInterface.dismiss()
                            inputText.text.clear()
                        })
                        .create()
                dialogKeeper = WeakReference(dialog)
            }
            dialogKeeper.get()?.show()
        }

        /**
         * 隐藏对话框
         */
        fun hide() {
            dialogKeeper.get()?.dismiss()
        }

    }

    /**
     * 接收当前播放音频数据
     */
    private inner class PlayingDataUpdateReceiver : BroadcastReceiver() {

        val intentFilter: IntentFilter = IntentFilter(AudioService.NOTIFICATION_UPDATE)
        var isRegistered: Boolean = false

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                AudioService.NOTIFICATION_UPDATE -> {
                    //用正在播放的曲目路径与适配器内的曲目路径比较，确定当前正在播放的曲目，从而能设置正在播放样式
                    adapter.updatePlayingPath(ServiceHolder.getInstance().service.playingSong?.path)
                }
            }
        }

    }

}