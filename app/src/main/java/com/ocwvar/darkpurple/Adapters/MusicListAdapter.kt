package com.ocwvar.darkpurple.Adapters

import android.graphics.Bitmap
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
import com.squareup.picasso.Picasso

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 11:58 AM
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   音乐列表适配器
 */
class MusicListAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var array: ArrayList<SongItem> = ArrayList()

    fun source(): ArrayList<SongItem> = array

    fun changeSource(array: ArrayList<SongItem>) {
        this.array.clear()
        this.array.addAll(array)
        notifyDataSetChanged()
    }

    fun removeData(position: Int) {
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
                    .into(views.cover)
        } else if (songData.isHaveCover) {
            //有默认封面
            views.colorBar.setBackgroundColor(songData.paletteColor)
            Picasso.with(views.itemView.context)
                    .load(CoverImage2File.getInstance().getCacheFile(songData.path))
                    .config(Bitmap.Config.RGB_565)
                    .error(R.drawable.ic_music_mid)
                    .placeholder(R.drawable.ic_music_mid)
                    .into(views.cover)
        } else {
            //无封面
            views.colorBar.setBackgroundColor(AppConfigs.Color.DefaultCoverColor)
            views.cover.setImageResource(R.drawable.ic_music_mid)
        }

        views.title.text = songData.title
        views.artist.text = songData.artist
        views.album.text = songData.album
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
                callback.onListClick(array[adapterPosition], adapterPosition)
            }
            itemView.setOnLongClickListener {
                callback.onListLongClick(array[adapterPosition], adapterPosition)
                false
            }
        }

    }

}