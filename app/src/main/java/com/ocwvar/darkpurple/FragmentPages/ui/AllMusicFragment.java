package com.ocwvar.darkpurple.FragmentPages.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.FragmentPages.work.AllMusicBackGround;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.MediaScanner;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.FragmentPages
 * Data: 2016/7/5 16:03
 * Project: DarkPurple
 * 所有歌曲页面
 */
public class AllMusicFragment extends Fragment {

    private AllMusicBackGround workFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null && inflater != null) {
            return inflater.inflate(R.layout.fragment_allmusic, container, false);
        } else {
            return null;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        workFragment = (AllMusicBackGround) getFragmentManager().findFragmentByTag(AllMusicBackGround.TAG);
        if (workFragment == null) {
            workFragment = new AllMusicBackGround();
            workFragment.setTargetFragment(this, 0);
            getFragmentManager().beginTransaction().add(workFragment, AllMusicBackGround.TAG).commit();
        } else {
            workFragment.setTargetFragment(this, 0);
        }

        workFragment.initData();
        if (MediaScanner.getInstance().isHasCachedData()) {
            //如果有上一次搜索的记录缓存 , 则直接使用
            workFragment.getLastTimeData();
        } else {
            //如果没有缓存 , 则重新搜索数据
            workFragment.refreshData();
        }

    }

    /**
     * 手动更新列表数据
     */
    public void refreshListData() {
        if (workFragment != null) {
            workFragment.refreshData();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        workFragment.releaseData();
    }

    public boolean onActivityKeyDown(int keyCode, KeyEvent event) {
        return workFragment != null && workFragment.onActivityKeyDown(keyCode, event);
    }

}
