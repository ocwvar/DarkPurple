package com.ocwvar.darkpurple.FragmentPages;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.Activities.PlayingActivity;
import com.ocwvar.darkpurple.Activities.PlaylistDetailActivity;
import com.ocwvar.darkpurple.Adapters.PlaylistItemAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.Units.PlaylistUnits;

import java.lang.ref.WeakReference;
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
    View noPlaylistView;
    RecyclerView recyclerView;
    PlaylistItemAdapter adapter;
    ProgressDialog loadingDialog;
    WeakReference<AlertDialog> moreDialog;

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
        setTargetFragment(null, 0);
    }

    /**
     * 初始化数据
     */
    protected void initData() {
        fragmentView = getTargetFragment().getView();
        if (fragmentView != null) {
            adapter = new PlaylistItemAdapter();
            adapter.setOnButtonClickCallback(this);
            noPlaylistView = fragmentView.findViewById(R.id.noPlaylist);
            recyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycleView);
            recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext(), LinearLayoutManager.VERTICAL, false));
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);
            PlaylistUnits.getInstance().setPlaylistChangedCallbacks(this);
            loadingDialog = new ProgressDialog(fragmentView.getContext(), R.style.AlertDialog_AppCompat_ProgressDialog);
            loadingDialog.setMessage(AppConfigs.ApplicationContext.getString(R.string.text_playlist_loading));
            loadingDialog.setCancelable(false);

            shouldShowNoPlaylistMessage();
        }
    }

    /**
     * 刷新数据
     */
    protected void refreshData() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            shouldShowNoPlaylistMessage();
        }
    }

    /**
     * 释放数据
     */
    protected void releaseData() {
        PlaylistUnits.getInstance().setPlaylistChangedCallbacks(null);
        fragmentView = null;
        recyclerView = null;
        if (adapter != null) {
            adapter.setOnButtonClickCallback(null);
            adapter = null;
        }
        moreDialog = new WeakReference<>(null);
        selectedPlaylistItem = null;
        selectedPosition = -1;
    }

    /**
     * 是否显示  当前无播放列表  消息
     */
    private void shouldShowNoPlaylistMessage() {
        if (adapter != null) {
            if (adapter.getItemCount() == 0) {
                recyclerView.setVisibility(View.GONE);
                noPlaylistView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                noPlaylistView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 显示更多选项对话框
     */
    @SuppressLint("InflateParams")
    protected void showMoreDialog() {
        if (moreDialog == null || moreDialog.get() == null) {
            View dialogView = LayoutInflater.from(fragmentView.getContext()).inflate(R.layout.dialog_playlist_more, null);
            (dialogView.findViewById(R.id.fb_delete)).setOnClickListener(this);
            (dialogView.findViewById(R.id.fb_detail)).setOnClickListener(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(fragmentView.getContext(), R.style.FullScreen_TransparentBG);
            builder.setView(dialogView);
            moreDialog = new WeakReference<>(builder.create());
        }

        if (moreDialog != null && moreDialog.get() != null) {
            moreDialog.get().show();
        }

    }

    //点击了播放列表右上角的播放按钮
    @Override
    public void onPlayButtonClick(PlaylistItem playlistItem, int position) {
        if (playlistItem.getPlaylist() == null) {
            //如果选择的播放列表内容为空 , 则去获取 , 否则就直接播放即可
            PlaylistUnits.getInstance().loadPlaylistAudioesData(new PlaylistUnits.PlaylistLoadingCallbacks() {
                @Override
                public void onPreLoad() {
                    if (loadingDialog != null) {
                        loadingDialog.show();
                    }
                }

                @Override
                public void onLoadCompleted(PlaylistItem playlistItem1, ArrayList<SongItem> data) {
                    //获取数据成功的时候就进行播放操作
                    if (loadingDialog != null) {
                        loadingDialog.dismiss();
                    }
                    ServiceHolder.getInstance().getService().play(data, 0);
                    startActivity(new Intent(getActivity(), PlayingActivity.class));
                }

                @Override
                public void onLoadFailed() {
                    if (loadingDialog != null) {
                        loadingDialog.dismiss();
                        Snackbar.make(fragmentView, R.string.text_playlist_loadFailed, Snackbar.LENGTH_SHORT).show();
                    }
                }
            }, playlistItem);
        } else {
            ServiceHolder.getInstance().getService().play(playlistItem.getPlaylist(), 0);
            if (AppConfigs.isAutoSwitchPlaying) {
                startActivity(new Intent(getActivity(), PlayingActivity.class));
            }
        }
    }

    //点击了播放列表的 更多 按钮
    @Override
    public void onMoreButtonClick(PlaylistItem playlistItem, int position) {
        selectedPlaylistItem = playlistItem;
        selectedPosition = position;
        showMoreDialog();
    }

    @Override
    public void onPlaylistDataChanged() {
        if (adapter != null) {
            Logger.warnning(TAG, "收到播放列表变更信息 , 更新适配器");
            adapter.notifyDataSetChanged();
            shouldShowNoPlaylistMessage();
        }
    }

    //点击对话框内 删除按钮 和 详情按钮 的回调处
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fb_delete:
                adapter.removePlaylist(selectedPosition);
                moreDialog.get().dismiss();
                break;
            case R.id.fb_detail:
                if (selectedPlaylistItem.getPlaylist() == null) {
                    //如果选择的播放列表内容为空 , 则去获取 , 否则就开始转跳
                    PlaylistUnits.getInstance().loadPlaylistAudioesData(new PlaylistUnits.PlaylistLoadingCallbacks() {
                        @Override
                        public void onPreLoad() {
                            if (loadingDialog != null) {
                                loadingDialog.show();
                            }
                        }

                        @Override
                        public void onLoadCompleted(PlaylistItem playlistItem, ArrayList<SongItem> data) {
                            if (data != null) {
                                //获取数据成功的时候就进行转跳
                                if (loadingDialog != null) {
                                    loadingDialog.dismiss();
                                }

                                Bundle bundle = new Bundle();
                                bundle.putInt("position", PlaylistUnits.getInstance().indexOfPlaylistItem(selectedPlaylistItem));
                                PlaylistDetailActivity.startBlurActivityForResultByFragment(10, Color.argb(50, 0, 0, 0), false, PlaylistPageBackGround.this, PlaylistDetailActivity.class, bundle, 9);

                            } else {
                                //如果获取得到的数据为空 , 则说明这个播放列表无效 , 自动进行移除操作
                                PlaylistUnits.getInstance().removePlaylist(selectedPlaylistItem);
                                selectedPlaylistItem = null;
                                selectedPosition = -1;
                                adapter.notifyDataSetChanged();
                                Snackbar.make(fragmentView, R.string.error_auto_deletePL, Snackbar.LENGTH_LONG).show();
                                if (loadingDialog != null) {
                                    loadingDialog.dismiss();
                                }
                            }
                        }

                        @Override
                        public void onLoadFailed() {
                            if (loadingDialog != null) {
                                loadingDialog.dismiss();
                                Snackbar.make(fragmentView, R.string.text_playlist_loadFailed, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }, selectedPlaylistItem);
                } else {
                    //启动播放列表详情界面

                    Bundle bundle = new Bundle();
                    bundle.putInt("position", PlaylistUnits.getInstance().indexOfPlaylistItem(selectedPlaylistItem));
                    PlaylistDetailActivity.startBlurActivityForResultByFragment(10, Color.argb(50, 0, 0, 0), false, PlaylistPageBackGround.this, PlaylistDetailActivity.class, bundle, 9);

                }
                moreDialog.get().dismiss();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9 && resultCode == PlaylistDetailActivity.LIST_CHANGED && data != null) {
            //如果列表被进行了操作 , 在退出界面的时候就进行保存
            final boolean isRenamed = data.getExtras().getBoolean("renamed", false);
            if (isRenamed) {
                adapter.notifyDataSetChanged();
            }
            int position = data.getIntExtra("position", -1);
            if (position != -1) {
                PlaylistItem playlistItem = PlaylistUnits.getInstance().getPlaylistItem(position);
                if (playlistItem.getPlaylist().size() == 0) {
                    //如果用户删除了所有的歌曲 , 则移除整个播放列表
                    PlaylistUnits.getInstance().removePlaylist(playlistItem);
                    Snackbar.make(fragmentView, R.string.text_playlist_clear, Snackbar.LENGTH_SHORT).show();
                } else {
                    PlaylistUnits.getInstance().savePlaylist(playlistItem.getName(), playlistItem.getPlaylist());
                    Snackbar.make(fragmentView, R.string.text_playlist_saved, Snackbar.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
                return;
            }
            if (!isRenamed) {
                Snackbar.make(fragmentView, R.string.text_playlist_saveFail, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

}
