package com.ocwvar.darkpurple.FragmentPages;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.ocwvar.darkpurple.Activities.DownloadCoverActivity;
import com.ocwvar.darkpurple.Activities.FolderSelectorActivity;
import com.ocwvar.darkpurple.Activities.PlayingActivity;
import com.ocwvar.darkpurple.Adapters.AllMusicAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Callbacks.MediaScannerCallback;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.JSONHandler;
import com.ocwvar.darkpurple.Units.MediaScanner;
import com.ocwvar.darkpurple.Units.PlaylistUnits;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.FragmentPages
 * Data: 2016/7/5 16:16
 * Project: DarkPurple
 * 所有歌曲后台Fragment
 */
public class AllMusicBackGround extends Fragment implements MediaScannerCallback, AllMusicAdapter.OnClick, View.OnTouchListener, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "AllMusicBackGround";
    final AllMusicAdapter allMusicAdapter;
    View fragmentView;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    FloatingActionButton floatingActionButton;
    WeakReference<AlertDialog> moreDialog = new WeakReference<>(null);
    SelectPlaylistDialogHolder addToPLDialogHolder;
    AlertDialog newPlaylistDialog;
    ProgressDialog loadingDialog;
    EditText getPlaylistTitle;

    SongItem selectedSongitem;
    int selectedPosition = -1;

    Snackbar requestPermission;

    public AllMusicBackGround() {
        setRetainInstance(true);
        allMusicAdapter = new AllMusicAdapter(AppConfigs.layoutStyle);
        allMusicAdapter.setOnClick(this);
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
    public void onDestroy() {
        super.onDestroy();
        if (allMusicAdapter.isMuiltSelecting()) {
            cancelMulitMode();
        }
    }

    /**
     * 歌曲数据刷新回调
     *
     * @param songItems       得到的数据
     * @param isFromLastSaved 这次的数据是否来自最后一次的储存
     */
    @Override
    public void onScanCompleted(ArrayList<SongItem> songItems, boolean isFromLastSaved) {
        if (songItems == null) {
            if (fragmentView == null) {
                Toast.makeText(getActivity(), R.string.noMusic, Toast.LENGTH_SHORT).show();
            } else {
                Snackbar.make(fragmentView, R.string.noMusic, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            allMusicAdapter.setDatas(songItems);
            allMusicAdapter.notifyDataSetChanged();
            if (fragmentView == null) {
                Toast.makeText(getActivity(), R.string.gotMusicDone, Toast.LENGTH_SHORT).show();
            } else {
                Snackbar.make(fragmentView, R.string.gotMusicDone, Snackbar.LENGTH_SHORT).show();
            }

            /*if (isFromLastSaved) {
                final AudioService service = ServiceHolder.getInstance().getService();
                if (service != null) {
                    service.initAudio(songItems, 0);
                }
            }*/

        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

    }

    /**
     * 显示新建播放列表对话框
     * <p>
     * 包括其中的点击事件处理
     */
    private void showCreatePlaylistDialog() {
        if (newPlaylistDialog == null) {
            //创建输入框对象
            getPlaylistTitle = new EditText(fragmentView.getContext());
            getPlaylistTitle.setMaxLines(1);
            getPlaylistTitle.setBackgroundColor(Color.argb(120, 0, 0, 0));
            getPlaylistTitle.setTextSize(15f);
            getPlaylistTitle.setTextColor(Color.WHITE);

            //创建对话框对象
            AlertDialog.Builder builder = new AlertDialog.Builder(fragmentView.getContext(), R.style.FullScreen_TransparentBG);
            builder.setMessage(R.string.title_newplaylist_dialog);
            builder.setCancelable(false);
            builder.setView(getPlaylistTitle);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //确定键点击回调
                    if (TextUtils.isEmpty(getPlaylistTitle.getText().toString())) {
                        //如果输入的内容无效
                        getPlaylistTitle.getText().clear();
                        Snackbar.make(fragmentView, R.string.wrongPlaylistName, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (PlaylistUnits.getInstance().isPlaylistExisted(getPlaylistTitle.getText().toString())) {
                        //已经有相同名称的播放列表
                        getPlaylistTitle.getText().clear();
                        Snackbar.make(fragmentView, R.string.existedlaylistName, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    //如果一切正常 , 则开启多选模式 , 更改浮动按钮图样 , 显示提示 , 关闭对话框
                    allMusicAdapter.startMuiltMode();
                    floatingActionButton.setImageResource(R.drawable.ic_action_done);
                    Snackbar.make(fragmentView, R.string.startSelectPlaylist, Snackbar.LENGTH_LONG).show();

                    //禁用下拉刷新
                    swipeRefreshLayout.setEnabled(false);
                    dialogInterface.dismiss();
                }
            });
            builder.setNegativeButton(R.string.simple_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //取消键点击回调
                    getPlaylistTitle.getText().clear();
                    dialogInterface.dismiss();
                }
            });
            newPlaylistDialog = builder.create();
        }
        newPlaylistDialog.show();
    }

    /**
     * 显示音频的更多选项
     * <p>
     * 包括其中的点击事件处理
     */
    @SuppressLint("InflateParams")
    private void showMoreDialog() {
        if (moreDialog == null || moreDialog.get() == null) {
            View itemView = LayoutInflater.from(fragmentView.getContext()).inflate(R.layout.dialog_allmusic_more, null);
            if (itemView != null) {
                //删除按钮
                (itemView.findViewById(R.id.fb_delete)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        moreDialog.get().dismiss();
                        if (selectedPosition != -1) {
                            allMusicAdapter.removeItem(selectedPosition);
                            allMusicAdapter.notifyItemRemoved(selectedPosition + 1);
                        }
                    }
                });
                //添加按钮
                (itemView.findViewById(R.id.fb_add)).setOnClickListener(this);
                //获取封面图像按钮
                (itemView.findViewById(R.id.fb_download_cover)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        moreDialog.get().dismiss();
                        if (selectedSongitem != null) {
                            Bundle bundle = new Bundle();
                            bundle.putParcelable("item", selectedSongitem);
                            DownloadCoverActivity.startBlurActivityForResultByFragment(10, Color.argb(100, 0, 0, 0), false, AllMusicBackGround.this, DownloadCoverActivity.class, bundle, 10);
                        }
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(fragmentView.getContext(), R.style.FullScreen_TransparentBG);
                builder.setView(itemView);
                moreDialog = new WeakReference<>(builder.create());
            }
        }

        if (moreDialog != null && moreDialog.get() != null) {
            moreDialog.get().show();
        }
    }

    /**
     * 获取上一次 的搜索记录
     */
    public void getLastTimeData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        MediaScanner.getInstance().setCallback(this);
        MediaScanner.getInstance().getLastTimeCachedData();
    }

    /**
     * 刷新数据
     */
    public void refreshData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        MediaScanner.getInstance().setCallback(this);

        if (Build.VERSION.SDK_INT < 23 || (Build.VERSION.SDK_INT >= 23 && AppConfigs.ApplicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            //拥有权限的时候 , 才开始搜索
            MediaScanner.getInstance().start();
        } else {
            //否则提示对应的消息
            requestPermission.show();
            swipeRefreshLayout.setRefreshing(false);
        }

    }

    /**
     * 初始化后台数据
     */
    public void initData() {
        fragmentView = getTargetFragment().getView();

        if (fragmentView != null) {

            requestPermission = Snackbar.make(fragmentView, R.string.error_noPermission, Snackbar.LENGTH_LONG)
                    .setActionTextColor(AppConfigs.ApplicationContext.getResources().getColor(R.color.colorSecond))
                    .setAction(R.string.request_permission_button, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                //如果应用还可以请求权限,则弹出请求对话框
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 9);
                            } else {
                                //如果用户选择了不再提醒,则不弹出请求对话框,直接跳转到设置界面
                                Uri packageURI = Uri.parse("package:" + AppConfigs.ApplicationContext.getPackageName());
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                                startActivity(intent);
                            }
                        }
                    });

            swipeRefreshLayout = (SwipeRefreshLayout) fragmentView.findViewById(R.id.swipeRefreshLayout);
            floatingActionButton = (FloatingActionButton) fragmentView.findViewById(R.id.floatMenu_createList);
            recyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycleView);

            recyclerView.setAdapter(allMusicAdapter);
            if (allMusicAdapter.getLayoutStyle() == AllMusicAdapter.LayoutStyle.Grid) {
                recyclerView.setLayoutManager(new GridLayoutManager(fragmentView.getContext(), 2, GridLayoutManager.VERTICAL, false));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(fragmentView.getContext(), 1, GridLayoutManager.VERTICAL, false));
            }
            recyclerView.setHasFixedSize(true);
            recyclerView.setOnTouchListener(this);

            allMusicAdapter.setOnRecycleViewScrollController(recyclerView);

            floatingActionButton.setColorNormal(AppConfigs.Color.FloatingButton_Color);
            floatingActionButton.setPadding(0, 0, 0, AppConfigs.NevBarHeight);
            floatingActionButton.setOnClickListener(this);

            loadingDialog = new ProgressDialog(fragmentView.getContext());
            loadingDialog.setMessage(AppConfigs.ApplicationContext.getString(R.string.text_playlist_loading));
            loadingDialog.setCancelable(false);

            swipeRefreshLayout.setColorSchemeColors(AppConfigs.Color.ToolBar_color);
            swipeRefreshLayout.setOnRefreshListener(this);

            if (MediaScanner.getInstance().isUpdated()) {
                allMusicAdapter.setDatas(MediaScanner.getInstance().getCachedDatas());
                allMusicAdapter.notifyDataSetChanged();
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
            recyclerView.setOnTouchListener(null);
            allMusicAdapter.setOnClick(null);
            recyclerView = null;
            swipeRefreshLayout = null;
            fragmentView = null;
            floatingActionButton = null;
            getPlaylistTitle = null;
            newPlaylistDialog = null;
            MediaScanner.getInstance().setCallback(null);
        }

    }

    /**
     * 取消多选模式
     */
    private void cancelMulitMode() {
        if (allMusicAdapter.isMuiltSelecting()) {
            swipeRefreshLayout.setEnabled(true);
            allMusicAdapter.stopMuiltMode();
            getPlaylistTitle.getText().clear();
            floatingActionButton.setImageResource(R.drawable.ic_action_favorite);
            Snackbar.make(fragmentView, R.string.playliseSaveCanceled, Snackbar.LENGTH_SHORT).show();
        }
    }

    public boolean onActivityKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (allMusicAdapter.isMuiltSelecting()) {
                //如果当前正在多选模式 , 按返回键则会取消
                cancelMulitMode();
                return true;
            }
        }
        return false;
    }

    /**
     * 列表歌曲点击回调
     *
     * @param songList 所属歌曲列表
     * @param position 在歌曲列表中的位置
     */
    @Override
    public void onListClick(ArrayList<SongItem> songList, int position, View itemView) {
        if (ServiceHolder.getInstance().getService() != null) {

            if (ServiceHolder.getInstance().getService().play(songList, position, false)) {
                //如果播放成功 , 则发送广播刷新状态栏通知和跳转界面
                getActivity().sendBroadcast(new Intent(AudioService.NOTIFICATION_REFRESH));
                if (AppConfigs.isAutoSwitchPlaying) {
                    Intent intent = new Intent(getActivity(), PlayingActivity.class);
                    if (Build.VERSION.SDK_INT >= 21) {
                        getActivity().startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), new Pair<>(itemView.findViewById(R.id.item_title), "title"), new Pair<>(itemView.findViewById(R.id.item_artist), "artist")).toBundle());
                    } else {
                        getActivity().startActivity(intent);
                    }
                }
            } else {
                //如果播放失败 , 则说明这个音频不合法 , 从列表中移除
                allMusicAdapter.removeItem(position);
                allMusicAdapter.notifyItemRemoved(position + 1);
                Snackbar.make(fragmentView, R.string.music_failed, Snackbar.LENGTH_LONG).show();
            }

        }
    }

    /**
     * 列表歌曲长按回调
     *
     * @param songItem 歌曲信息对象
     * @param position 歌曲信息在列表中的位置
     */
    @Override
    public void onListItemLongClick(SongItem songItem, int position) {
        selectedSongitem = songItem;
        selectedPosition = position;
        showMoreDialog();
    }

    /**
     * 列表选项项目点击回调
     */
    @Override
    public void onOptionClick() {
        if (Build.VERSION.SDK_INT < 23 || (Build.VERSION.SDK_INT >= 23 && AppConfigs.ApplicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            //如果当前系统不是Android6.0或者已经授予文件读写权限 , 才打开歌曲文件夹设置
            FolderSelectorActivity.startBlurActivityForResultByFragment(5, Color.argb(100, 0, 0, 0), false, AllMusicBackGround.this, FolderSelectorActivity.class, null, 9);
        } else {
            //如果权限不正常 , 则提示错误
            requestPermission.show();
        }
    }

    /**
     * 用户从设置歌曲目录界面 以及 从设置封面界面返回之后的操作
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 9:
                if (resultCode == FolderSelectorActivity.DATA_CHANGED) {
                    //当用户更改了选歌目录 , 我们就需要马上更新目录下的歌曲 , 同时停止当前的播放
                    ServiceHolder.getInstance().getService().release();
                    refreshData();
                }
                break;
            case 10:
                //更改了封面返回要做列表的刷新操作
                if (resultCode == DownloadCoverActivity.DATA_CHANGED && data != null && data.getExtras() != null) {
                    SongItem resultItem = data.getParcelableExtra("item");
                    allMusicAdapter.replaceSongItem(resultItem);
                    allMusicAdapter.notifyDataSetChanged();
                    WeakReference<AsyncUpadteCachedList> task = new WeakReference<>(new AsyncUpadteCachedList(allMusicAdapter.getSongList()));
                    task.get().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                }
                break;
        }

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (floatingActionButton != null) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (!floatingActionButton.isShown()) {
                        floatingActionButton.hide(true);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (floatingActionButton.isHidden()) {
                        floatingActionButton.show(true);
                    }
                    break;
            }
        }
        return false;
    }

    /**
     * 浮动按钮 (创建新播放列表)
     * <p></p>
     * 插入播放列表按钮 (将歌曲加入已存在的播放列表)
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            //歌曲的更多选项中的添加到已存在播放列表按钮
            case R.id.fb_add:

                //这里不需要检测是否 NULL , 因为这时候对话框正在显示
                moreDialog.get().dismiss();
                if (addToPLDialogHolder == null) {
                    addToPLDialogHolder = new SelectPlaylistDialogHolder();
                }
                addToPLDialogHolder.showDialog();

                break;

            //浮动按钮 , 用于创建播放列表
            case R.id.floatMenu_createList:

                if (allMusicAdapter.isMuiltSelecting()) {
                    //如果已经是选择模式 , 则代表执行的是开始创建播放列表. 关闭多选模式并且开始记录数据

                    //启用下拉刷新
                    swipeRefreshLayout.setEnabled(true);

                    if (PlaylistUnits.getInstance().savePlaylist(getPlaylistTitle.getText().toString(), allMusicAdapter.stopMuiltMode())) {
                        //储存成功
                        Snackbar.make(fragmentView, R.string.playliseSaved, Snackbar.LENGTH_SHORT).show();
                    } else {
                        //储存失败
                        Snackbar.make(fragmentView, R.string.playliseSaveFailed, Snackbar.LENGTH_SHORT).show();
                    }
                    floatingActionButton.setImageResource(R.drawable.ic_action_favorite);
                    allMusicAdapter.notifyDataSetChanged();
                    getPlaylistTitle.getText().clear();
                } else {
                    showCreatePlaylistDialog();
                }
                break;
        }
    }

    /**
     * 用户下拉刷新的回调
     */
    @Override
    public void onRefresh() {
        refreshData();
    }

    /**
     * 播放列表选择对话框ViewHolder
     */
    private final class SelectPlaylistDialogHolder implements AdapterView.OnItemClickListener {

        View itemView;
        ListView listView;
        ArrayAdapter<String> stringArrayAdapter;
        WeakReference<AlertDialog> addToDialog = new WeakReference<>(null);

        @SuppressLint("InflateParams")
        private void createItemView() {
            if (this.itemView == null) {
                this.itemView = LayoutInflater.from(fragmentView.getContext()).inflate(R.layout.dialog_addto_playlist, null);
                this.listView = (ListView) itemView.findViewById(R.id.listview);
                this.stringArrayAdapter = new ArrayAdapter<>(itemView.getContext(), R.layout.simple_textview);
                this.listView.setAdapter(stringArrayAdapter);
                this.listView.setOnItemClickListener(this);
            }
        }

        private void refreshPlaylistSet() {
            if (stringArrayAdapter != null) {
                stringArrayAdapter.clear();
                ArrayList<PlaylistItem> playlistItems = PlaylistUnits.getInstance().getPlaylists();
                for (PlaylistItem item : playlistItems) {
                    stringArrayAdapter.add(item.getName());
                }
                stringArrayAdapter.notifyDataSetChanged();
            }
        }

        /**
         * 显示添加对话框
         */
        private void showDialog() {
            if (addToDialog == null || addToDialog.get() == null) {
                if (itemView == null) {
                    createItemView();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(fragmentView.getContext(), R.style.FullScreen_TransparentBG);
                builder.setView(this.itemView);
                addToDialog = new WeakReference<>(builder.create());
            }

            if (addToDialog.get() != null) {
                refreshPlaylistSet();
                addToDialog.get().show();
            }

        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if (PlaylistUnits.getInstance().getPlaylistItem(i).getPlaylist() == null) {
                //如果要添加到的列表还没加载 , 则先加载
                PlaylistUnits.getInstance().loadPlaylistAudioesData(new PlaylistUnits.PlaylistLoadingCallbacks() {

                    @Override
                    public void onPreLoad() {
                        if (loadingDialog != null) {
                            loadingDialog.show();
                        }
                    }

                    @Override
                    public void onLoadCompleted(PlaylistItem playlistItem, ArrayList<SongItem> data) {
                        if (loadingDialog != null) {
                            loadingDialog.dismiss();
                        }
                        if (PlaylistUnits.getInstance().addAudio(playlistItem, selectedSongitem)) {
                            Snackbar.make(fragmentView, R.string.text_playlist_addNewSong, Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(fragmentView, R.string.text_playlist_addNewSong_Failed, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadFailed() {
                        if (loadingDialog != null) {
                            loadingDialog.dismiss();
                        }
                        Snackbar.make(fragmentView, R.string.text_playlist_loadFailed, Snackbar.LENGTH_SHORT).show();
                    }


                }, PlaylistUnits.getInstance().getPlaylistItem(i));
            } else {
                //如果已经加载了 , 则直接添加进去
                if (PlaylistUnits.getInstance().addAudio(PlaylistUnits.getInstance().getPlaylistItem(i), selectedSongitem)) {
                    Snackbar.make(fragmentView, R.string.text_playlist_addNewSong, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(fragmentView, R.string.text_playlist_addNewSong_Failed, Snackbar.LENGTH_SHORT).show();
                }
            }
            if (addToDialog != null && addToDialog.get() != null) {
                addToDialog.get().dismiss();
            }
        }

    }

    /**
     * 异步储存播放列表数据
     */
    private final class AsyncUpadteCachedList extends AsyncTask<Integer, Void, Boolean> {

        ArrayList<SongItem> list;

        AsyncUpadteCachedList(ArrayList<SongItem> list) {
            this.list = list;
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            JSONHandler.cacheSearchResult(list);
            return true;
        }

    }

}
