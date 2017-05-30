package com.ocwvar.darkpurple.FragmentPages

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ocwvar.darkpurple.Adapters.MusicListAdapter
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Bean.SongItem
import com.ocwvar.darkpurple.Callbacks.MediaScannerCallback
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.MediaScanner
import com.ocwvar.darkpurple.Units.ToastMaker
import java.util.*

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/30 11:50 AM
 * File Location com.ocwvar.darkpurple.FragmentPages
 * This file use to :   音乐选择Fragment（主要）
 */
class MusicListFragment : Fragment(), MediaScannerCallback {
    //歌曲条目展示适配器
    private val adapter: MusicListAdapter = MusicListAdapter()
    //Fragment页面缓存
    private var fragmentView: View? = null
    //下拉刷新控件
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    //显示列表控件
    private lateinit var recycleView: RecyclerView

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

}