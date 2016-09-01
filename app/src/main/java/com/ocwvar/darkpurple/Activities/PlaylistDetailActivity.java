package com.ocwvar.darkpurple.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ocwvar.darkpurple.Adapters.PlaylistDetailAdapter;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Callbacks.OnDragChangedCallback;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.PlaylistUnits;

import java.lang.ref.WeakReference;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Activities
 * Data: 2016/8/16 0:14
 * Project: DarkPurple
 * 播放列表详情界面
 */
public class PlaylistDetailActivity extends AppCompatActivity implements PlaylistDetailAdapter.OnPlayButtonClickCallback, View.OnClickListener {

    public static final int LIST_CHANGED = 1;
    public static final int LIST_UNCHANGED = 2;
    Snackbar info = null;
    PlaylistItem selectPlaylistItem = null;
    PlaylistDetailAdapter adapter = null;
    WeakReference<AlertDialog> renameDialog = new WeakReference<>(null);
    EditText getNewname;
    Intent intent = null;
    int thisPosition = -1;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent();
        intent.putExtra("renamed", false);

        setResult(LIST_UNCHANGED, null);

        if (getIntent().getExtras() == null) {
            //没有传递播放列表对象位置 , 结束当前页面
            finish();
            return;
        } else {
            thisPosition = getIntent().getExtras().getInt("position");
            this.selectPlaylistItem = PlaylistUnits.getInstance().getPlaylistItem(thisPosition);
            if (this.selectPlaylistItem == null) {
                //如果无法获取到播放列表数据对象 , 结束当前页面
                finish();
                return;
            } else if (this.selectPlaylistItem.getPlaylist() == null) {
                //如果没有获取到这播放列表数据的音频列表 , 结束当前页面
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_playlist_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(this.selectPlaylistItem.getName() + " " + getApplicationContext().getString(R.string.title_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new PlaylistDetailAdapter(this.selectPlaylistItem.getPlaylist(), this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(PlaylistDetailActivity.this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecycleSwipeHelper(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        info = Snackbar.make(findViewById(android.R.id.content), R.string.info, Snackbar.LENGTH_INDEFINITE).setAction(R.string.simple_done, PlaylistDetailActivity.this);
        info.show();
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
            if (getNewname == null) {
                getNewname = new EditText(PlaylistDetailActivity.this);
                getNewname.setMaxLines(1);
                getNewname.setBackgroundColor(Color.argb(120, 0, 0, 0));
                getNewname.setTextSize(15f);
                getNewname.setTextColor(Color.WHITE);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistDetailActivity.this, R.style.FullScreen_TransparentBG);
            builder.setTitle(R.string.title_newplaylist_dialog);
            builder.setView(getNewname);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (!TextUtils.isEmpty(getNewname.getText().toString())) {
                        if (getNewname.getText().toString().equals(selectPlaylistItem.getName())) {
                            //输入的名字跟原本的相同
                            getNewname.getText().clear();
                            Snackbar.make(findViewById(android.R.id.content), R.string.title_dialog_error, Snackbar.LENGTH_SHORT).show();
                        } else if (PlaylistUnits.getInstance().isPlaylistExisted(getNewname.getText().toString())) {
                            //输入的名字和其他的播放列表名字相同
                            getNewname.getText().clear();
                            Snackbar.make(findViewById(android.R.id.content), R.string.title_dialog_error2, Snackbar.LENGTH_SHORT).show();
                        } else {
                            //可以开始更改
                            PlaylistUnits.getInstance().renamePlaylist(selectPlaylistItem.getName(), getNewname.getText().toString());
                            getNewname.getText().clear();
                            setTitle(selectPlaylistItem.getName() + " " + getApplicationContext().getString(R.string.title_detail));
                            intent.putExtra("renamed", true);
                            setResult(LIST_CHANGED, intent);
                            dialogInterface.dismiss();
                        }
                    }
                }
            });
            renameDialog = new WeakReference<>(builder.create());
        }
        getNewname.setText(selectPlaylistItem.getName());
        renameDialog.get().show();
    }

    /**
     * 点击了播放列表中的播放按钮
     * <p/>
     * PS: 当前界面 finish() 之后 , 会返回上一个界面 , 如果页面结束的时候 result 为 LIST_CHANGED , 则会触发保存
     * 如果上一个界面被回收了 , 则不会被保存. 所以应该在转跳到播放界面 结束当前界面 之前进行保存操作. 并
     * 设置 result 为 LIST_UNCHANGED
     *
     * @param position 点击播放的音频位置
     */
    @Override
    public void onPlayButtonClick(int position) {
        //保存数据
        PlaylistUnits.getInstance().savePlaylist(selectPlaylistItem.getName(), selectPlaylistItem.getPlaylist());
        //设置结果
        setResult(LIST_UNCHANGED);
        //播放音频
        ServiceHolder.getInstance().getService().play(this.selectPlaylistItem.getPlaylist(), position);
        //转跳到播放界面
        startActivity(new Intent(PlaylistDetailActivity.this, PlayingActivity.class));
        finish();
    }

    @Override
    public void onClick(View view) {
        info.dismiss();
    }

    /**
     * RecyclerView 拖动实现工具
     */
    class RecycleSwipeHelper extends ItemTouchHelper.Callback {

        OnDragChangedCallback changedCallback;

        public RecycleSwipeHelper(@NonNull OnDragChangedCallback changedCallback) {
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
                intent.putExtra("position", thisPosition);
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
            intent.putExtra("position", thisPosition);
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
