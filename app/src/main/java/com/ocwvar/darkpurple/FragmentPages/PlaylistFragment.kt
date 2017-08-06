package com.ocwvar.darkpurple.FragmentPages

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.session.MediaControllerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ocwvar.darkpurple.Activities.PlayingActivity
import com.ocwvar.darkpurple.Activities.PlaylistDetailActivity
import com.ocwvar.darkpurple.Adapters.PlaylistAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.PlaylistItem
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Services.MediaPlayerService
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary
import com.ocwvar.darkpurple.Units.PlaylistUnits
import com.ocwvar.darkpurple.Units.ToastMaker
import java.lang.ref.WeakReference

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-7 下午6:13
 * File Location com.ocwvar.darkpurple.FragmentPages
 * This file use to :   播放列表Fragment
 */
class PlaylistFragment : Fragment(), PlaylistAdapter.Callback {

    private lateinit var fragmentView: View
    private var playlistMenuDialog: PlaylistMenuDialog? = null
    private val adapter: PlaylistAdapter = PlaylistAdapter(this@PlaylistFragment)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (inflater != null && container != null) {
            return inflater.inflate(R.layout.fragment_playlist_list, container, false)
        } else {
            return null
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view == null) {
            return
        } else {
            this.fragmentView = view
        }
        val recycleView: RecyclerView = fragmentView.findViewById(R.id.recycleView)
        recycleView.layoutManager = GridLayoutManager(fragmentView.context, 2, GridLayoutManager.VERTICAL, false)
        recycleView.setHasFixedSize(true)
        recycleView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    /**
     * 列表点击事件
     * @param   data    所点击项目的数据
     * @param   position    项目的位置
     * @param   itemView    项目的View
     */
    override fun onListClick(data: PlaylistItem, position: Int, itemView: View) {
        if (data.playlist != null && data.playlist.size > 0) {
            //如果已经有有效的歌曲列表数据，则可以直接进行播放
            sendCommand(MediaPlayerService.COMMAND.COMMAND_PLAY_LIBRARY, Bundle().let {
                it.putString(MediaPlayerService.COMMAND_EXTRA.ARG_STRING_LIBRARY_NAME, data.name)
                it.putInt(MediaPlayerService.COMMAND_EXTRA.ARG_INT_LIBRARY_INDEX, 0)
                it
            })
            if (AppConfigs.isAutoSwitchPlaying) {
                //播放成功，同时需要转跳至播放界面
                startActivity(Intent(activity, PlayingActivity::class.java))
            }
        }
    }

    /**
     * 列表长按事件
     * @param   data    所长按项目的数据
     * @param   position    项目的位置
     * @param   itemView    项目的View
     */
    override fun onListLongClick(data: PlaylistItem, position: Int, itemView: View) {
        if (playlistMenuDialog == null) {
            playlistMenuDialog = PlaylistMenuDialog()
        }
        playlistMenuDialog?.show(data, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9 && resultCode == PlaylistDetailActivity.LIST_CHANGED && data != null) {
            //如果列表被进行了操作，在退出界面的时候就进行保存
            val isRenamed = data.extras.getBoolean("renamed", false)
            val position = data.getIntExtra("position", -1)
            if (isRenamed) {
                //如果更改了名称
                adapter.notifyItemChanged(position)
            }
            if (position != -1) {
                val playlistItem = PlaylistUnits.getInstance().getPlaylistItem(position)
                if (playlistItem.playlist.size == 0) {
                    //如果用户删除了所有的歌曲，则移除整个播放列表
                    PlaylistUnits.getInstance().removePlaylist(playlistItem)
                    //如果使用着当前数据，则重置数据
                    if (MediaLibrary.getUsingLibraryTAG() == playlistItem.name) {
                        sendCommand(MediaPlayerService.COMMAND.COMMAND_RELEASE_CURRENT_MEDIA, null)
                    }
                    ToastMaker.show(R.string.message_playlist_empty_removed)
                    adapter.notifyItemRemoved(position)
                } else {
                    //更改了曲目顺序
                    PlaylistUnits.getInstance().savePlaylist(playlistItem.name, playlistItem.playlist)
                    ToastMaker.show(R.string.message_playlist_saved)
                    adapter.notifyItemChanged(position)
                }
                return
            }
        }
    }

    /**
     * 我的云储存点击事件
     */
    override fun onCloudClick() {
    }

    /**
     * 发送指令到媒体服务
     * @param   commandAction   服务的Action
     * @param   extra   附带的数据
     */
    private fun sendCommand(commandAction: String, extra: Bundle?) {
        MediaControllerCompat.getMediaController(this.activity)?.sendCommand(commandAction, extra, null)
    }

    /**
     * 播放列表长按菜单处理类
     */
    private inner class PlaylistMenuDialog : View.OnClickListener {


        private var dialogKeeper: WeakReference<AlertDialog?> = WeakReference(null)
        private lateinit var data: PlaylistItem
        private var position: Int = -1

        /**
         * 显示对话框
         */
        @SuppressLint("InflateParams")
        fun show(data: PlaylistItem, position: Int) {
            this.data = data
            this.position = position
            var dialog: AlertDialog? = dialogKeeper.get()
            if (dialog == null) {
                val dialogView: View = LayoutInflater.from(fragmentView.context).inflate(R.layout.dialog_playlist_menu, null)
                dialogView.findViewById<View>(R.id.menu_playlist_edit).setOnClickListener(this@PlaylistMenuDialog)
                dialogView.findViewById<View>(R.id.menu_playlist_delete).setOnClickListener(this@PlaylistMenuDialog)
                dialog = AlertDialog.Builder(fragmentView.context, R.style.FullScreen_TransparentBG)
                        .setView(dialogView)
                        .create()
                dialogKeeper = WeakReference(dialog)
            }
            dialog?.show()
        }

        /**
         * 隐藏对话框
         */
        fun hide() {
            dialogKeeper.get()?.dismiss()
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.menu_playlist_edit -> {
                    //编辑播放列表动作
                    if (data.playlist != null) {
                        //列表有数据，可以直接打开编辑页面
                        val bundle: Bundle = Bundle().let {
                            it.putInt("position", position)
                            it
                        }
                        PlaylistDetailActivity.startBlurActivityForResultByFragment(10, Color.argb(50, 0, 0, 0), false, this@PlaylistFragment, PlaylistDetailActivity::class.java, bundle, 9)
                    }
                    hide()
                }
                R.id.menu_playlist_delete -> {
                    //删除播放列表动作
                    PlaylistUnits.getInstance().removePlaylist(data)
                    adapter.notifyItemRemoved(position)
                    //如果使用着当前数据，则重置数据
                    if (MediaLibrary.getUsingLibraryTAG() == data.name) {
                        sendCommand(MediaPlayerService.COMMAND.COMMAND_RELEASE_CURRENT_MEDIA, null)
                    }
                    hide()
                }
            }
        }

    }

}