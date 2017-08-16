package com.ocwvar.darkpurple.Adapters

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ocwvar.darkpurple.R

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-15 下午2:46
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   均衡器配置列表
 */
class EqualizerListAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Callback {

        /**
         * 点击均衡器配置
         *
         * @param name  配置名称
         * @param position 点击的位置
         */
        fun onItemClick(name: String, position: Int)

        /**
         * 点击删除配置
         *
         * @param name  配置名称
         * @param position 点击的位置
         */
        fun onDelete(name: String, position: Int)

    }

    private val source: ArrayList<String> = ArrayList()
    private var usingName: String = "Default"

    fun putSource(source: ArrayList<String>) {
        this.source.clear()
        this.source.addAll(source)
        notifyDataSetChanged()
    }

    fun addSource(source: String) {
        this.source.add(source)
        notifyDataSetChanged()
    }

    fun removeSource(source: String) {
        this.source.remove(source)
    }

    fun setUsingName(name: String) {
        this.usingName = name
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder ?: return
        val views: EqualizerProfileViewHolder = holder as EqualizerProfileViewHolder

        views.delete.visibility = View.VISIBLE
        views.itemView.background = null
        views.name.text = source[position]

        //Default 配置不显示删除按钮
        if (source[position] == "Default") {
            views.delete.visibility = View.GONE
        } else {
            views.delete.visibility = View.VISIBLE
        }

        //当前正在使用的配置，样式不一样
        if (source[position] == this.usingName) {
            views.name.setTextColor(Color.WHITE)
        } else {
            views.name.setTextColor(Color.argb(120, 255, 255, 255))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        parent ?: return null
        return EqualizerProfileViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_equalizer_profile, parent, false))
    }

    override fun getItemCount(): Int = this.source.size

    private inner class EqualizerProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val delete: View = itemView.findViewById(R.id.delete)
        val name: TextView = itemView.findViewById(R.id.name)

        init {
            delete.setOnClickListener {
                callback.onDelete(source[adapterPosition], adapterPosition)
            }

            name.setOnClickListener {
                callback.onItemClick(source[adapterPosition], adapterPosition)
            }
        }

    }

}