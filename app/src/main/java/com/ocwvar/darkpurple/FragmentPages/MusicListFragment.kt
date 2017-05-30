package com.ocwvar.darkpurple.FragmentPages

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
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
import android.widget.ListView
import com.ocwvar.darkpurple.Activities.DownloadCoverActivity
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
import com.ocwvar.darkpurple.Units.CoverImage2File
import com.ocwvar.darkpurple.Units.MediaScanner
import com.ocwvar.darkpurple.Units.PlaylistUnits
import com.ocwvar.darkpurple.Units.ToastMaker
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
class MusicListFragment : Fragment(), MediaScannerCallback, MusicListAdapter.Callback {

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
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        recycleView = view.findViewById(R.id.recycleView) as RecyclerView
        recycleView.adapter = adapter
        recycleView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recycleView.setHasFixedSize(true)
        swipeRefreshLayout.setColorSchemeColors(AppConfigs.Color.DefaultCoverColor)
        swipeRefreshLayout.setOnRefreshListener {
            //下拉刷新回调接口，设为半透明并无法被触摸
            swipeRefreshLayout.isRefreshing = true
            recycleView.alpha = 0.3f
            recycleView.setOnTouchListener({ _, _ -> true })
            MediaScanner.getInstance().start()
        }

        //设置扫描回调接口
        MediaScanner.getInstance().setCallback(this@MusicListFragment)

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
            ToastMaker.show(R.string.noMusic)
        } else {
            adapter.changeSource(songItems)
            ToastMaker.show(R.string.gotMusicDone)
        }
    }

    /**
     * 歌曲单击回调接口
     * @param   songData    歌曲数据
     * @param   position    在列表中的位置
     */
    override fun onListClick(songData: SongItem, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * 歌曲长按回调接口
     * @param   songData    歌曲数据
     * @param   position    在列表中的位置
     */
    override fun onListLongClick(songData: SongItem, position: Int) {
        if (itemMoreDialog == null) {
            itemMoreDialog = ItemMoreDialog()
        }
        itemMoreDialog?.show(songData, position)
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
        private var songDataPosition: Int = -1

        /**
         * 显示对话框
         * @param   songData    菜单基于的音频数据
         * @param   songDataPosition    列表中的位置
         */
        fun show(songData: SongItem, songDataPosition: Int) {
            this.songDataPosition = songDataPosition
            this.songData = songData
            var dialog: AlertDialog? = dialogKeeper.get()
            if (dialog == null) {
                val view: View = LayoutInflater.from(fragmentView.context).inflate(R.layout.music_list_item_menu, null)
                view.findViewById(R.id.menu_music_delete).setOnClickListener(this@ItemMoreDialog)
                view.findViewById(R.id.menu_music_upload).setOnClickListener(this@ItemMoreDialog)
                view.findViewById(R.id.menu_music_add2playlist).setOnClickListener(this@ItemMoreDialog)
                view.findViewById(R.id.menu_music_online_cover).setOnClickListener(this@ItemMoreDialog)
                dialog = AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG).setView(view).create()
                dialogKeeper = WeakReference(dialog)
            }
            dialog?.show()
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.menu_music_delete -> {
                    //删除文件操作
                    val file: File = File(songData.path)
                    if (file.exists() && file.canWrite() && file.delete()) {
                        //删除文件成功
                        adapter.removeData(songDataPosition)
                        ToastMaker.show(R.string.musicList_deleted)
                        dialogKeeper.get()?.dismiss()
                    } else {
                        ToastMaker.show(R.string.musicList_delete_failed)
                    }
                }
                R.id.menu_music_upload -> {
                    //上传文件操作
                    if (TextUtils.isEmpty(AppConfigs.USER.TOKEN)) {
                        //如果当前没有Token,则认为当前为离线模式
                        ToastMaker.show(R.string.error_need_online)
                    } else {
                        //上传音频文件
                        val args = HashMap<String, String>()
                        args.put(Keys.Token, AppConfigs.USER.TOKEN)
                        args.put(Keys.FilePath, songData.path)
                        args.put(Keys.MusicTitle, songData.title)
                        if (!TextUtils.isEmpty(CoverImage2File.getInstance().getNormalCachePath(songData.path))) {
                            //有封面存在才需要传递
                            args.put(Keys.CoverPath, CoverImage2File.getInstance().getNormalCachePath(songData.path))
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
                        dialogKeeper.get()?.dismiss()
                        if (result) {
                            //成功提交任务
                            ToastMaker.show(R.string.musicList_uploading)
                        } else {
                            //线程池仍有相同的任务未完成
                            ToastMaker.show(R.string.musicList_upload_wait)
                        }
                    }
                }
                R.id.menu_music_add2playlist -> {
                    //显示添加至播放列表对话框
                    if (add2PlaylistDialog == null) {
                        add2PlaylistDialog = Add2PlaylistDialog()
                    }
                    add2PlaylistDialog?.show(songData)
                }
                R.id.menu_music_online_cover -> {
                    //打开在线封面获取界面
                    if (songData != null) {
                        val bundle = Bundle()
                        bundle.putParcelable("item", songData)
                        DownloadCoverActivity.startBlurActivityForResultByFragment(10, Color.argb(100, 0, 0, 0), false, this@MusicListFragment, DownloadCoverActivity::class.java, bundle, 10)
                    }
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
        fun show(songData: SongItem) {
            this.songData = songData
            var dialog: AlertDialog? = dialogKeeper.get()
            if (dialog == null) {
                val view: View = LayoutInflater.from(fragmentView.context).inflate(R.layout.dialog_addto_playlist, null)
                val listView: ListView = view.findViewById(R.id.listview) as ListView
                val adapter: ArrayAdapter<String> = ArrayAdapter(fragmentView.context, R.layout.simple_textview)
                PlaylistUnits.getInstance().playlists.forEach {
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
            if (PlaylistUnits.getInstance().playlists[position].playlist == null) {
                //如果要添加到的列表还没加载 , 则先加载
                PlaylistUnits.getInstance().loadPlaylistAudiosData(object : PlaylistUnits.PlaylistLoadingCallbacks {

                    override fun onPreLoad() {
                        ToastMaker.show(R.string.text_playlist_loading)
                    }

                    override fun onLoadCompleted(playlistItem: PlaylistItem, data: ArrayList<SongItem>) {
                        if (PlaylistUnits.getInstance().addAudio(playlistItem, songData)) {
                            ToastMaker.show(R.string.text_playlist_addNewSong)
                        } else {
                            ToastMaker.show(R.string.text_playlist_addNewSong_Failed)
                        }
                    }

                    override fun onLoadFailed() {
                        ToastMaker.show(R.string.text_playlist_loadFailed)
                    }
                }, PlaylistUnits.getInstance().playlists[position])
            } else run {
                //如果已经加载了 , 则直接添加进去
                if (PlaylistUnits.getInstance().addAudio(PlaylistUnits.getInstance().playlists[position], songData)) {
                    ToastMaker.show(R.string.text_playlist_addNewSong)
                    dialogKeeper.get()?.dismiss()
                } else {
                    ToastMaker.show(R.string.text_playlist_addNewSong_Failed)
                }
            }
        }

    }

}