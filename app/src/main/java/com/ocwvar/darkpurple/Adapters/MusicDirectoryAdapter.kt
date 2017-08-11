package com.ocwvar.darkpurple.Adapters

import android.support.v7.widget.AppCompatCheckBox
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ocwvar.darkpurple.Activities.MusicDirectorySelector
import com.ocwvar.darkpurple.R

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-11 下午7:20
 * File Location com.ocwvar.darkpurple.Adapters
 * This file use to :   音乐目录选择器列表适配器
 */
class MusicDirectoryAdapter(val callback: Callback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedDirs: ArrayList<String> = ArrayList()
    private val source: ArrayList<MusicDirectorySelector.Directory> = ArrayList()

    interface Callback {

        /**
         * 点击目录回调接口
         * @param   item    目录对象
         */
        fun onDirectoryClick(item: MusicDirectorySelector.Directory)

        /**
         * 当用户选择的目录发生变化
         */
        fun onSelectedDirsChanged()

    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        parent ?: return null
        return DirectoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_directory, parent, false))
    }

    override fun getItemCount(): Int = source.size

    /**
     * 更新已选择数据
     */
    fun updateSelected(selected: ArrayList<String>) {
        this.selectedDirs.clear()
        this.selectedDirs.addAll(selected)
        notifyDataSetChanged()
    }

    /**
     * 更新当前目录数据
     */
    fun updateSource(source: ArrayList<MusicDirectorySelector.Directory>) {
        this.source.clear()
        this.source.addAll(source)
        notifyDataSetChanged()
    }

    fun getSelectedDirs(): ArrayList<String> = this.selectedDirs

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder ?: return
        val views: DirectoryViewHolder = holder as DirectoryViewHolder
        val item: MusicDirectorySelector.Directory = source[position]

        views.name.text = item.name
        views.checkBox.visibility = View.VISIBLE
        views.checkBox.isChecked = selectedDirs.contains(item.path)

        //已选择的复选框是不透明的，否则是半透明的
        if (views.checkBox.isChecked) {
            views.checkBox.alpha = 1.0f
        } else {
            views.checkBox.alpha = 0.2f
        }

        when (item.type) {
            MusicDirectorySelector.DirectoryStyle.DIR -> {
                views.icon.setImageResource(R.drawable.ic_action_folder)
                views.checkBox.visibility = View.VISIBLE
            }

            MusicDirectorySelector.DirectoryStyle.DISK -> {
                views.icon.setImageResource(R.drawable.ic_action_disk)
                views.checkBox.visibility = View.INVISIBLE
            }
        }
    }

    private inner class DirectoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val checkBox: AppCompatCheckBox = itemView.findViewById(R.id.checkbox)
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val name: TextView = itemView.findViewById(R.id.name)

        init {
            this.itemView.setOnClickListener(this@DirectoryViewHolder)
            this.checkBox.setOnClickListener(this@DirectoryViewHolder)
        }

        override fun onClick(view: View) {
            when (view.id) {
                R.id.checkbox -> {
                    val currentDirPath: String = source[adapterPosition].path

                    if (selectedDirs.contains(currentDirPath)) {
                        //如果此目录已经选择
                        this.checkBox.isChecked = false
                        selectedDirs.remove(currentDirPath)
                    } else {
                        this.checkBox.isChecked = true
                        selectedDirs.add(currentDirPath)
                    }

                    //已选择的复选框是不透明的，否则是半透明的
                    if (this.checkBox.isChecked) {
                        this.checkBox.alpha = 1.0f
                    } else {
                        this.checkBox.alpha = 0.2f
                    }

                    callback.onSelectedDirsChanged()
                }

                else -> {
                    callback.onDirectoryClick(source[adapterPosition])
                }
            }
        }


    }
}