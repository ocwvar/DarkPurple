package com.ocwvar.darkpurple.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ocwvar.darkpurple.Adapters.PlaylistDetailAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Callbacks.OnDragChangedCallback;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;
import com.ocwvar.darkpurple.Units.PlaylistUnits;
import com.ocwvar.darkpurple.Units.ToastMaker;

import java.lang.ref.WeakReference;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Activities
 * Data: 2016/8/16 0:14
 * Project: DarkPurple
 * 播放列表详情界面
 */
public final class PlaylistDetailActivity extends BaseBlurActivity {

    public static final int LIST_CHANGED = 1;
    public static final int LIST_UNCHANGED = 2;
    public static final String KEY_OLD_NAME = "KEY_OLD_NAME";
    public static final String KEY_NEW_NAME = "KEY_NEW_NAME";
    public static final String KEY_POSITION = "KEY_POSITION";
    final Intent intent = new Intent();
    Snackbar info = null;
    PlaylistItem selectPlaylistItem = null;
    PlaylistDetailAdapter adapter = null;
    WeakReference<AlertDialog> renameDialog = new WeakReference<>(null);
    EditText getNewName;
    int thisPosition = -1;

    @Override
    protected boolean onPreSetup() {

        setResult(LIST_UNCHANGED, null);

        if (getIntent().getExtras() == null) {
            //没有传递播放列表对象位置 , 结束当前页面
            return false;
        } else {
            //获取从上一个界面得到的索引位置
            this.thisPosition = getIntent().getExtras().getInt("position");

            //尝试获取对应索引位置的播放列表数据
            this.selectPlaylistItem = PlaylistUnits.getInstance().getPlaylistItem(this.thisPosition);

            if (this.selectPlaylistItem == null) {
                //如果无法获取到播放列表数据对象 , 结束当前页面
                ToastMaker.INSTANCE.show(R.string.message_playlist_position_error);
                finish();
                return false;
            } else if (this.selectPlaylistItem.getPlaylist() == null) {
                //如果没有获取到这播放列表数据的音频列表 , 结束当前页面
                ToastMaker.INSTANCE.show(R.string.message_playlist_empty);
                finish();
                return false;
            }
        }
        return true;
    }

    @Override
    protected int setActivityView() {
        return R.layout.activity_playlist_detail_page;
    }

    @Override
    protected int onSetToolBar() {
        return R.id.toolbar;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onSetupViews(Bundle savedInstanceState) {

        setTitle(this.selectPlaylistItem.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new PlaylistDetailAdapter(this.selectPlaylistItem.getPlaylist());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(PlaylistDetailActivity.this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecycleSwipeHelper(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        info = Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_playlist_detail_tip, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.simple_done, PlaylistDetailActivity.this)
                .setActionTextColor(AppConfigs.Color.FloatingButton_Color);

        //设置播放列表的旧名称
        intent.putExtra(KEY_OLD_NAME, this.selectPlaylistItem.getName());
        //设置播放列表的新名称，默认为空
        intent.putExtra(KEY_NEW_NAME, "");
        //设置此播放列表的索引位置
        intent.putExtra(KEY_POSITION, this.thisPosition);

        setResult(LIST_UNCHANGED, intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        info.show();
    }

    @Override
    protected void onViewClick(@NonNull View clickedView) {
        info.dismiss();
    }

    @Override
    protected boolean onViewLongClick(@NonNull View holdedView) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_playlist_rename, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_action_rename:
                showRenameDialog();
                break;
        }
        return true;
    }

    /**
     * 显示更改名字对话框
     */
    private void showRenameDialog() {
        if (renameDialog.get() == null) {
            if (getNewName == null) {
                getNewName = new EditText(PlaylistDetailActivity.this);
                getNewName.setMaxLines(1);
                getNewName.setBackgroundColor(Color.argb(120, 0, 0, 0));
                getNewName.setTextSize(15f);
                getNewName.setTextColor(Color.WHITE);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistDetailActivity.this, R.style.Dialog_FullScreen_NoBackground);
            builder.setTitle(R.string.dialog_playlist_detail_rename_title);
            builder.setView(getNewName);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (!TextUtils.isEmpty(getNewName.getText().toString())) {

                        //记录输入的名称，并清空输入框
                        final String newName = getNewName.getText().toString();
                        getNewName.getText().clear();

                        if (newName.equals(selectPlaylistItem.getName())) {
                            //输入的名字跟原本的相同
                            Snackbar.make(findViewById(android.R.id.content), R.string.ERROR_title_exist_song, Snackbar.LENGTH_SHORT).show();

                        } else if (PlaylistUnits.getInstance().isPlaylistExisted(newName)) {
                            //输入的名字和其他的播放列表名字相同
                            Snackbar.make(findViewById(android.R.id.content), R.string.ERROR_title_exist_playlist_name, Snackbar.LENGTH_SHORT).show();

                        } else {
                            //名称可用，保存至Intent
                            setTitle(newName);

                            intent.putExtra(KEY_NEW_NAME, newName);
                            setResult(LIST_CHANGED, intent);
                            dialogInterface.dismiss();
                        }
                    }
                }
            });
            renameDialog = new WeakReference<>(builder.create());
        }
        getNewName.setText(selectPlaylistItem.getName());
        renameDialog.get().show();
    }

    /**
     * RecyclerView 拖动实现工具
     */
    private final class RecycleSwipeHelper extends ItemTouchHelper.Callback {

        OnDragChangedCallback changedCallback;

        RecycleSwipeHelper(@NonNull OnDragChangedCallback changedCallback) {
            this.changedCallback = changedCallback;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        /**
         * 设置拖动 滑动 方向
         */
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int swipeFlag = ItemTouchHelper.LEFT;
            int dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlag, swipeFlag);
        }

        /**
         * 拖动时的回调
         */
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            if (viewHolder.getItemViewType() != target.getItemViewType()) {
                return false;
            } else {
                changedCallback.onItemPositionChange(viewHolder, viewHolder.getAdapterPosition(), target.getAdapterPosition());
                setResult(LIST_CHANGED, intent);
                return true;
            }
        }

        /**
         * 滑动时的回调
         */
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            changedCallback.onItemDelete(viewHolder.getAdapterPosition());
            setResult(LIST_CHANGED, intent);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                //根据侧滑的位移来修改item的透明度
                final float alpha = 1 - Math.abs(dX) * 2 / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

    }

}
