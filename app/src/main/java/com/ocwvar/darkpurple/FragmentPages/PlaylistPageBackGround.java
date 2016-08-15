package com.ocwvar.darkpurple.FragmentPages;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.Activities.PlayingActivity;
import com.ocwvar.darkpurple.Adapters.PlaylistItemAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.PlaylistUnits;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.FragmentPages
 * Data: 2016/8/14 18:33
 * Project: DarkPurple
 * 播放列表页面的工作页面
 */
public class PlaylistPageBackGround extends Fragment implements PlaylistItemAdapter.OnButtonClickCallback, PlaylistUnits.PlaylistChangedCallbacks, View.OnClickListener {

    final public static String TAG = "PlaylistPageBackGround";

    View fragmentView;
    RecyclerView recyclerView;
    PlaylistItemAdapter adapter;
    ProgressDialog loadingDialog;
    AlertDialog moreDialog;

    PlaylistItem selectedPlaylistItem = null;
    int selectedPosition = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setTargetFragment(null,0);
    }

    /**
     * 初始化数据
     */
    protected void initData(){
        fragmentView = getTargetFragment().getView();
        if (fragmentView != null){
            adapter = new PlaylistItemAdapter();
            recyclerView = (RecyclerView)fragmentView.findViewById(R.id.recycleView);
            recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext(),LinearLayoutManager.VERTICAL,false));
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);

            adapter.setCallback(this);
            PlaylistUnits.getInstance().setPlaylistChangedCallbacks(this);

            loadingDialog = new ProgressDialog(fragmentView.getContext() , R.style.AlertDialog_AppCompat_ProgressDialog);
            loadingDialog.setMessage(AppConfigs.ApplicationContext.getString(R.string.text_playlist_loading));
            loadingDialog.setCancelable(false);

            AlertDialog.Builder builder = new AlertDialog.Builder(fragmentView.getContext() , R.style.FullScreen_TransparentBG);
            View dialogView = LayoutInflater.from(fragmentView.getContext()).inflate(R.layout.dialog_playlist_more,null);
            (dialogView.findViewById(R.id.fb_delete)).setOnClickListener(this);
            (dialogView.findViewById(R.id.fb_detail)).setOnClickListener(this);
            builder.setView(dialogView);
            moreDialog = builder.create();
        }
    }

    /**
     * 刷新数据
     */
    protected void refreshData(){
        if (adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 释放数据
     */
    protected void releaseData(){
        PlaylistUnits.getInstance().setPlaylistChangedCallbacks(null);
        fragmentView = null;
        recyclerView = null;
        if (adapter != null){
            adapter.setCallback(null);
            adapter = null;
        }
        selectedPlaylistItem = null;
        selectedPosition = -1;
    }

    //点击了播放列表右上角的播放按钮
    @Override
    public void onPlayButtonClick (PlaylistItem playlistItem , int position) {
        if (playlistItem.getPlaylist() == null){
            //如果选择的播放列表内容为空 , 则去获取 , 否则就直接播放即可
            PlaylistUnits.getInstance().loadPlaylistAudioesData(new PlaylistUnits.PlaylistLoadingCallbacks() {
                @Override
                public void onPreLoad() {
                    if (loadingDialog != null){
                        loadingDialog.show();
                    }
                }

                @Override
                public void onLoadCompleted(ArrayList<SongItem> data) {
                    //获取数据成功的时候就进行播放操作
                    if (loadingDialog != null){
                        loadingDialog.dismiss();
                    }
                    ServiceHolder.getInstance().getService().play(data,0);
                    startActivity(new Intent(getActivity(), PlayingActivity.class));
                }

                @Override
                public void onLoadFailed() {
                    if (loadingDialog != null){
                        loadingDialog.dismiss();
                        Snackbar.make(fragmentView,R.string.text_playlist_loadFailed,Snackbar.LENGTH_SHORT).show();
                    }
                }
            } , playlistItem);
        }else {
            ServiceHolder.getInstance().getService().play(playlistItem.getPlaylist(),0);
            startActivity(new Intent(getActivity(), PlayingActivity.class));
        }
    }

    //点击了播放列表的 更多 按钮
    @Override
    public void onMoreButtonClick(PlaylistItem playlistItem , int position) {
        if (moreDialog != null){
            selectedPlaylistItem = playlistItem;
            selectedPosition = position;
            moreDialog.show();
        }
    }

    @Override
    public void onPlaylistDataChanged() {
        if (adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fb_delete:
                adapter.removePlaylist(selectedPosition);
                moreDialog.dismiss();
                break;
            case R.id.fb_detail:
                moreDialog.dismiss();
                break;
        }
    }

}
