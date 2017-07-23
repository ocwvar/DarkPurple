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
import com.ocwvar.darkpurple.Bean.PlaylistItem
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.CoverImage2File
import com.ocwvar.darkpurple.Units.PlaylistUnits
import com.squareup.picasso.Picasso

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-6-7 下午9:53
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   播放列表适配器
 */
class PlaylistAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val array: ArrayList<PlaylistItem> = PlaylistUnits.getInstance().playlistSet

    override fun getItemCount(): Int = array.size + 1

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        parent?.let {
            if (viewType == 0) {
                return PlaylistCloudVH(LayoutInflater.from(it.context).inflate(R.layout.item_playlist_cloud_option, it, false))
            } else {
                return PlaylistVH(LayoutInflater.from(it.context).inflate(R.layout.item_playlist, it, false))
            }
        }
        return null
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder ?: return
        if (holder is PlaylistVH) {
            val data: PlaylistItem = array[position - 1]

            holder.name.text = data.name
            holder.count.text = String.format("%s%d%s", AppConfigs.ApplicationContext.getString(R.string.part_playlist_count_head), data.counts, AppConfigs.ApplicationContext.getString(R.string.part_playlist_count_end))
            Picasso.with(holder.itemView.context)
                    .load(CoverImage2File.getInstance().getCacheFile(data.firstAudioPath))
                    .config(Bitmap.Config.RGB_565)
                    .error(R.drawable.ic_music_mid)
                    .placeholder(R.drawable.ic_music_mid)
                    .fit()
                    .into(holder.cover)
        } else if (holder is PlaylistCloudVH) {
            if (TextUtils.isEmpty(AppConfigs.USER.USERNAME)) {
                //离线模式
                holder.bg.setBackgroundResource(R.drawable.myclound_offline)
                holder.message.setText(R.string.text_cloud_offline)
                holder.icon.setImageResource(R.drawable.ic_myclound_offline)
            } else {
                //在线模式
                holder.bg.setBackgroundResource(R.drawable.myclound_online)
                holder.message.text = String.format("%s%s", AppConfigs.ApplicationContext.getString(R.string.text_cloud_userText_head), AppConfigs.USER.USERNAME)
                holder.icon.setImageResource(R.drawable.ic_myclound_online)
            }
        }
    }

    interface Callback {
        /**
         * 列表点击事件
         * @param   data    所点击项目的数据
         * @param   position    项目的位置
         * @param   itemView    项目的View
         */
        fun onListClick(data: PlaylistItem, position: Int, itemView: View)

        /**
         * 列表长按事件
         * @param   data    所长按项目的数据
         * @param   position    项目的位置
         * @param   itemView    项目的View
         */
        fun onListLongClick(data: PlaylistItem, position: Int, itemView: View)

        /**
         * 我的云储存点击事件
         */
        fun onCloudClick()

    }

    private inner class PlaylistVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val name: TextView = itemView.findViewById(R.id.playlist_name)
        val count: TextView = itemView.findViewById(R.id.playlist_count)
        val cover: ImageView = itemView.findViewById(R.id.playlist_cover)

        init {
            itemView.setOnClickListener {
                callback.onListClick(array[adapterPosition - 1], adapterPosition - 1, itemView)
            }

            itemView.setOnLongClickListener {
                callback.onListLongClick(array[adapterPosition - 1], adapterPosition - 1, itemView)
                false
            }
        }

    }

    private inner class PlaylistCloudVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val bg: View = itemView.findViewById(R.id.myclound_bar_bg)
        val icon: ImageView = itemView.findViewById(R.id.myclound_bar_icon)
        val message: TextView = itemView.findViewById(R.id.myclound_bar_subtitle)

        init {
            itemView.setOnClickListener {
                callback.onCloudClick()
            }
        }

    }

}