package com.ocwvar.darkpurple.Adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ocwvar.darkpurple.R

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-11 下午10:42
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   音乐文件夹显示适配器
 */
class MusicDirectoryDisplayAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val source: ArrayList<String> = ArrayList()

    interface Callback {

        /**
         * 请求移除路径
         * @param   path    删除后剩下的路径集合
         */
        fun onDelete(path: ArrayList<String>)

    }

    fun source(): ArrayList<String> = this.source

    fun updateSource(source: ArrayList<String>) {
        this.source.clear()
        this.source.addAll(source)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        parent ?: return null
        return DirectoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_music_directory, parent, false))
    }

    override fun getItemCount(): Int = source.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder ?: return
        val data: String = source[position]
        val views: DirectoryViewHolder = holder as DirectoryViewHolder

        views.name.text = data
    }

    private inner class DirectoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val name: TextView = itemView.findViewById(R.id.name)

        init {
            this.itemView.setOnClickListener {
                source.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)

                callback.onDelete(source)
            }
        }

    }

}