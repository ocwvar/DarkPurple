package com.ocwvar.darkpurple.Adapters

import android.graphics.Bitmap
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.CoverImage2File
import com.ocwvar.darkpurple.Units.ToastMaker
import com.squareup.picasso.Picasso

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 11:58 AM
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   音乐列表适配器
 */
class MusicListAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //被选择背景颜色
    private val selectedColor: Int = Color.argb(30, 255, 255, 255)
    //当前正在播放背景颜色
    private val playingColor: Int = Color.rgb(23, 3, 35)
    //多选模式标记
    private var isSelecting: Boolean = false
    //用于显示的歌曲信息列表
    private var array: ArrayList<SongItem> = ArrayList()
    //用于储存的歌曲信息列表
    private var selected: ArrayList<SongItem> = ArrayList()
    //用于储存已选择项目的下标，供快速查询
    private var selectedIndex: ArrayList<Int> = ArrayList()
    //当前正在播放的歌曲文件路径
    private var playingPath: String = ""

    /**
     * @return  当前显示数据源
     */
    fun source(): ArrayList<SongItem> = array

    /**
     * @return  当前已选择的数据
     */
    fun selected(): ArrayList<SongItem> = selected

    /**
     * @return  当前是否为多选模式
     */
    fun isSelectingMode(): Boolean = isSelecting

    /**
     * 更新当前正在播放的歌曲路径
     * @param   songPath    歌曲路径
     */
    fun updatePlayingPath(songPath: String?) {
        songPath ?: return
        //更改歌曲条目样式
        val newPlaying: SongItem? = array.find { it.path == songPath }
        val lastPlaying: SongItem? = array.find { it.path == this.playingPath }
        val newPosition: Int = array.indexOf(newPlaying)
        val lastPosition: Int = array.indexOf(lastPlaying)
        if (newPosition >= 0) {
            notifyItemChanged(newPosition)
        }
        if (lastPosition >= 0) {
            notifyItemChanged(lastPosition)
        }
        this.playingPath = songPath
    }

    /**
     * 切换当前模式
     */
    fun switchMode() {
        isSelecting = !isSelecting
        if (!isSelecting) {
            selected.clear()
            selectedIndex.clear()
        }
        notifyDataSetChanged()
    }

    /**
     * 更换当前显示的数据，多选模式下无法添加数据
     * @param   array   要添加的数据源
     */
    fun changeSource(array: ArrayList<SongItem>) {
        if (isSelecting) return
        this.array.clear()
        this.array.addAll(array)
        notifyDataSetChanged()
    }

    /**
     * 移除当前显示的数据，多选模式下无法删除
     * @param   position    要移除的数据位置
     */
    fun removeData(position: Int) {
        if (isSelecting) return
        if (position >= 0 && position < array.size && array.removeAt(position) != null) {
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        parent ?: return null
        return MusicViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_music_line, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder ?: return
        val views: MusicViewHolder = holder as MusicViewHolder
        val songData: SongItem = this.array[position]

        if (!TextUtils.isEmpty(songData.customCoverPath)) {
            //有自定义封面
            views.colorBar.setBackgroundColor(songData.customPaletteColor)
            Picasso.with(views.itemView.context)
                    .load(songData.customCoverPath)
                    .config(Bitmap.Config.RGB_565)
                    .error(R.drawable.ic_music_mid)
                    .placeholder(R.drawable.ic_music_mid)
                    .fit()
                    .into(views.cover)
        } else if (songData.isHaveCover) {
            //有默认封面
            views.colorBar.setBackgroundColor(songData.paletteColor)
            Picasso.with(views.itemView.context)
                    .load(CoverImage2File.getInstance().getCacheFile(songData.path))
                    .config(Bitmap.Config.RGB_565)
                    .error(R.drawable.ic_music_mid)
                    .placeholder(R.drawable.ic_music_mid)
                    .fit()
                    .into(views.cover)
        } else {
            //无封面
            views.colorBar.setBackgroundColor(AppConfigs.Color.DefaultCoverColor)
            views.cover.setImageResource(R.drawable.ic_music_mid)
        }

        views.title.text = songData.title
        views.artist.text = songData.artist
        views.album.text = songData.album

        views.itemView.setBackgroundColor(Color.TRANSPARENT)

        if (isSelecting && selectedIndex.indexOf(position) != -1) {
            //当前此项已被选择
            views.itemView.setBackgroundColor(selectedColor)
        } else if (songData.path == playingPath) {
            //如果当前正在播放此条目
            views.itemView.setBackgroundColor(playingColor)
        } else {
            views.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    interface Callback {
        fun onListClick(songData: SongItem, position: Int)
        fun onListLongClick(songData: SongItem, position: Int)
    }

    private inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val colorBar: View = itemView.findViewById(R.id.item_menu_list_color)
        val cover: ImageView = itemView.findViewById(R.id.item_menu_list_cover) as ImageView
        val title: TextView = itemView.findViewById(R.id.item_menu_list_title) as TextView
        val artist: TextView = itemView.findViewById(R.id.item_menu_list_artist) as TextView
        val album: TextView = itemView.findViewById(R.id.item_menu_list_album) as TextView

        init {
            itemView.setOnClickListener {
                if (isSelecting) {
                    if (selectedIndex.indexOf(adapterPosition) != -1) {
                        //如果是已选状态，则将其移除
                        selectedIndex.remove(adapterPosition)
                        selected.remove(array[adapterPosition])
                        itemView.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        //如果是未选状态，则将其添加
                        selectedIndex.add(adapterPosition)
                        selected.add(array[adapterPosition])
                        itemView.setBackgroundColor(selectedColor)
                    }
                    notifyItemChanged(adapterPosition)
                } else {
                    //非多选模式下将点击事件传递给回调接口
                    callback.onListClick(array[adapterPosition], adapterPosition)
                }
            }
            itemView.setOnLongClickListener {
                if (isSelecting) {
                    ToastMaker.show(R.string.message_waitForSelecting)
                } else {
                    //非多选模式下将长按事件传递给回调接口
                    callback.onListLongClick(array[adapterPosition], adapterPosition)
                }
                false
            }
        }

    }

}