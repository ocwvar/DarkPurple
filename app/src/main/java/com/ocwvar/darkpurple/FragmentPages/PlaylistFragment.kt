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
import android.text.TextUtils
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
        if (requestCode == 9 && resultCode == PlaylistDetailActivity.LIST_CHANGED && data != null && data.extras != null) {

            //如果列表被进行了操作，在退出界面的时候就进行保存。先获取所有参数
            val index: Int = data.extras.getInt(PlaylistDetailActivity.KEY_POSITION, -1)
            val oldName: String? = data.extras.getString(PlaylistDetailActivity.KEY_OLD_NAME, null)
            val newName: String = data.extras.getString(PlaylistDetailActivity.KEY_NEW_NAME, "")
            val changedPlaylist: PlaylistItem? = PlaylistUnits.getInstance().getPlaylistItem(index)

            changedPlaylist ?: return
            oldName ?: return

            if (changedPlaylist.playlist.isEmpty()) {
                //列表内没有数据，则删除列表
                PlaylistUnits.getInstance().removePlaylist(changedPlaylist)
                ToastMaker.show(R.string.message_playlist_empty_removed)
                adapter.notifyItemRemoved(index)

            } else if (!TextUtils.isEmpty(newName)) {
                //列表更改了名称，则作为一个新的播放数据进行保存，然后删除旧的数据
                PlaylistUnits.getInstance().removePlaylist(PlaylistItem(oldName))
                PlaylistUnits.getInstance().savePlaylist(newName, changedPlaylist.playlist)

            } else {
                //没有更新名称，只是更改了列表数据
                PlaylistUnits.getInstance().savePlaylist(oldName, changedPlaylist.playlist)

            }

            //如果需要，则进行媒体重置
            resetCurrentMedia(changedPlaylist)

            //更新播放列表展示界面
            ToastMaker.show(R.string.message_playlist_saved)
            adapter.notifyDataSetChanged()
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
     * 在对当前正在播放的 播放列表 进行操作后，重置当前的媒体状态
     * @param   playlistItem    发生更改的数据
     */
    private fun resetCurrentMedia(playlistItem: PlaylistItem?) {
        //如果使用着当前数据，则重置数据
        if (playlistItem != null && MediaLibrary.getUsingLibraryTAG() == playlistItem.name) {
            sendCommand(MediaPlayerService.COMMAND.COMMAND_RELEASE_CURRENT_MEDIA, null)
        }
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
                dialog = AlertDialog.Builder(fragmentView.context, R.style.Dialog_FullScreen_NoBackground)
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
                    //如果需要，则进行媒体重置
                    resetCurrentMedia(data)
                    hide()
                }
            }
        }

    }

}