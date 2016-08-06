package com.ocwvar.darkpurple.FragmentPages;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.ocwvar.darkpurple.Activities.FolderSelectorActivity;
import com.ocwvar.darkpurple.Activities.PlayingActivity;
import com.ocwvar.darkpurple.Adapters.AllMusicAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Callbacks.MediaScannerCallback;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.MediaScanner;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.adapters.AlphaInAnimationAdapter;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.FragmentPages
 * Data: 2016/7/5 16:16
 * Project: DarkPurple
 * 所有歌曲后台Fragment
 */
public class AllMusicBackGround extends Fragment implements MediaScannerCallback, AllMusicAdapter.OnClick{
    public static final String TAG = "AllMusicBackGround";

    View loadingPanel;
    View fragmentView;
    RecyclerView recyclerView;
    AllMusicAdapter allMusicAdapter;
    AlphaInAnimationAdapter animationAdapter;

    public AllMusicBackGround() {
        setRetainInstance(true);
        allMusicAdapter = new AllMusicAdapter();
        allMusicAdapter.setOnClick(this);
        animationAdapter = new AlphaInAnimationAdapter(allMusicAdapter);
        animationAdapter.setInterpolator(new OvershootInterpolator());
        animationAdapter.setDuration(300);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setTargetFragment(null, 0);
    }

    @Override
    public void onScanCompleted(ArrayList<SongItem> songItems) {
        if (songItems == null) {
            if (fragmentView == null) {
                Toast.makeText(getActivity(), R.string.noMusic, Toast.LENGTH_SHORT).show();
            } else {
                Snackbar.make(fragmentView, R.string.noMusic, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            allMusicAdapter.setDatas(songItems);
            animationAdapter.notifyDataSetChanged();
            if (fragmentView == null) {
                Toast.makeText(getActivity(), R.string.gotMusicDone, Toast.LENGTH_SHORT).show();
            } else {
                Snackbar.make(fragmentView, R.string.gotMusicDone, Snackbar.LENGTH_SHORT).show();
            }
        }

        if (loadingPanel != null) {
            loadingPanel.setVisibility(View.GONE);
        }

    }

    /**
     * 刷新数据
     */
    public void refreshData() {
        if (loadingPanel != null) {
            loadingPanel.setVisibility(View.VISIBLE);
        }
        MediaScanner.getInstance().setCallback(this);
        MediaScanner.getInstance().start();
    }

    /**
     * 初始化后台数据
     */
    public void initData() {
        fragmentView = getTargetFragment().getView();

        if (fragmentView != null) {
            loadingPanel = fragmentView.findViewById(R.id.loadingPanel);
            recyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycleView);

            recyclerView.setAdapter(animationAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(fragmentView.getContext(), 2, GridLayoutManager.VERTICAL, false));
            recyclerView.setHasFixedSize(true);

            if (MediaScanner.getInstance().isUpdated()) {
                allMusicAdapter.setDatas(MediaScanner.getInstance().getCachedDatas());
                allMusicAdapter.notifyDataSetChanged();
                animationAdapter.notifyDataSetChanged();
            }

        }

    }

    /**
     * 释放后台数据
     */
    public void releaseData() {

        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView.setLayoutManager(null);
            allMusicAdapter.setOnClick(null);
            recyclerView = null;
            loadingPanel = null;
            fragmentView = null;
            MediaScanner.getInstance().setCallback(null);
        }

    }

    @Override
    public void onListClick(ArrayList<SongItem> songList, int position) {
        if (ServiceHolder.getInstance().getService() != null){

            if (ServiceHolder.getInstance().getService().play(songList, position)){
                //如果播放成功 , 则发送广播和跳转界面
                getActivity().sendBroadcast(new Intent(AudioService.NOTIFICATION_REFRESH));
                getActivity().startActivity(new Intent(getActivity(),PlayingActivity.class));
            }else {
                //如果播放失败 , 则说明这个音频不合法 , 从列表中移除
                allMusicAdapter.removeItem(position);
                allMusicAdapter.notifyItemRemoved(position+1);
                animationAdapter.notifyItemRemoved(position+1);
                Snackbar.make(fragmentView,R.string.music_failed,Snackbar.LENGTH_LONG).show();
            }

        }
    }

    @Override
    public void onOptionClick() {
        startActivity(new Intent(getActivity() , FolderSelectorActivity.class));
    }

}
