package com.ocwvar.darkpurple.Adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Network.Beans.RemoteMusic
import com.ocwvar.darkpurple.R
import com.squareup.picasso.Picasso
import java.io.File

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/23 4:16 PM
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   云音乐列表适配器
 */
class CloudMusicAdapter(val callback: OnListClickCallback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val sourceList: ArrayList<RemoteMusic> = ArrayList()

    interface OnListClickCallback {

        fun onDownloadRequest(musicObject: RemoteMusic, position: Int)

        fun onRemoveRequest(musicObject: RemoteMusic, position: Int)

    }

    fun updateSource(source: ArrayList<RemoteMusic>) {
        sourceList.clear()
        sourceList.addAll(source)
        notifyDataSetChanged()
    }

    fun removeSourece(position: Int) {
        sourceList.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        parent?.let {
            return CloudMusicViewHolder(LayoutInflater.from(it.context).inflate(R.layout.item_cloud_music, it, false))
        }
        return null
    }

    override fun getItemCount(): Int {
        return sourceList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder?.let {
            val item: RemoteMusic = sourceList[position]
            val viewHolder: CloudMusicViewHolder = it as CloudMusicViewHolder

            viewHolder.owner.text = String.format("%s%s", AppConfigs.ApplicationContext.getString(R.string.text_cloud_header_owner), item.ownerName)
            viewHolder.title.text = item.name

            if (File(AppConfigs.DownloadMusicFolder + item.fileName).exists()) {
                viewHolder.downloadButton.setText(R.string.text_cloudMusic_download_button_exist)
                viewHolder.downloadButton.isEnabled = false
            } else {
                viewHolder.downloadButton.setText(R.string.text_cloudMusic_download_button_download)
                viewHolder.downloadButton.isEnabled = true
            }

            Picasso.with(it.itemView.context)
                    .load(item.coverURL)
                    .placeholder(R.drawable.ic_music_big)
                    .error(R.drawable.ic_music_big)
                    .into(viewHolder.cover)
        }
    }

    inner private class CloudMusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val title: TextView = itemView.findViewById(R.id.cloudMusic_title) as TextView
        val owner: TextView = itemView.findViewById(R.id.cloudMusic_owner) as TextView
        val cover: ImageView = itemView.findViewById(R.id.cloudMusic_cover) as ImageView
        val downloadButton: TextView = itemView.findViewById(R.id.cloudMusic_download) as TextView
        val removeButton: TextView = itemView.findViewById(R.id.cloudMusic_remove) as TextView

        init {
            downloadButton.setOnClickListener(this@CloudMusicViewHolder)
            removeButton.setOnClickListener(this@CloudMusicViewHolder)
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.cloudMusic_download -> {
                    callback.onDownloadRequest(sourceList[adapterPosition], adapterPosition)
                }
                R.id.cloudMusic_remove -> {
                    callback.onRemoveRequest(sourceList[adapterPosition], adapterPosition)
                }
            }
        }
    }
}