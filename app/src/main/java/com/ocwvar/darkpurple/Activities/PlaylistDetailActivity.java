package com.ocwvar.darkpurple.Activities;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;

import com.ocwvar.darkpurple.Adapters.PlaylistDetailAdapter;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Callbacks.OnDragChangedCallback;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.PlaylistUnits;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Activities
 * Data: 2016/8/16 0:14
 * Project: DarkPurple
 * 播放列表详情界面
 */
public class PlaylistDetailActivity extends AppCompatActivity implements PlaylistDetailAdapter.OnPlayButtonClickCallback,View.OnClickListener {

    Snackbar info = null;
    PlaylistItem selectPlaylistItem = null;
    PlaylistDetailAdapter adapter = null;

    public static final int LIST_CHANGED = 1;
    public static final int LIST_UNCHANGED = 2;

    Intent intent = null;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent();

        setResult(LIST_UNCHANGED , null);

        if (getIntent().getExtras() == null) {
            //没有传递播放列表对象位置 , 结束当前页面
            finish();
            return;
        }else {
            int position = getIntent().getExtras().getInt("position");
            this.selectPlaylistItem = PlaylistUnits.getInstance().getPlaylistIten(position);
            if (this.selectPlaylistItem == null){
                //如果无法获取到播放列表数据对象 , 结束当前页面
                finish();
                return;
            }else if (this.selectPlaylistItem.getPlaylist() == null){
                //如果没有获取到这播放列表数据的音频列表 , 结束当前页面
                finish();
                return;
            }
            //创建一个Intent 给予当列表更改时 , 返回上一个界面带回的数据 , 用于在那边进行列表的保存操作
            intent.putExtra("position" , position);
        }

        setContentView(R.layout.activity_playlist_detail);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(this.selectPlaylistItem.getName()+" "+getApplicationContext().getString(R.string.title_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new PlaylistDetailAdapter(this.selectPlaylistItem.getPlaylist(),this);

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycleView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(PlaylistDetailActivity.this,LinearLayoutManager.VERTICAL,false));
        recyclerView.setHasFixedSize(true);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecycleSwipeHelper(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        info = Snackbar.make(findViewById(android.R.id.content),R.string.info,Snackbar.LENGTH_INDEFINITE).setAction(R.string.simple_done , PlaylistDetailActivity.this);
        info.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onPlayButtonClick(int position) {
        ServiceHolder.getInstance().getService().play(this.selectPlaylistItem.getPlaylist(),position);
        startActivity(new Intent(PlaylistDetailActivity.this,PlayingActivity.class));
        finish();
    }

    @Override
    public void onClick(View view) {
        info.dismiss();
    }

    /**
     * RecyclerView 拖动实现工具
     */
    class RecycleSwipeHelper extends ItemTouchHelper.Callback{

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
            return makeMovementFlags(dragFlag,swipeFlag);
        }

        /**
         * 拖动时的回调
         */
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            if (viewHolder.getItemViewType() != target.getItemViewType()){
                return false;
            }else {
                changedCallback.onItemPositionChange( viewHolder , viewHolder.getAdapterPosition() , target.getAdapterPosition() );
                setResult(LIST_CHANGED , intent);
                return true;
            }
        }

        /**
         * 滑动时的回调
         */
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            changedCallback.onItemDelete( viewHolder.getAdapterPosition() );
            setResult(LIST_CHANGED , intent);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
                //根据侧滑的位移来修改item的透明度
                final float alpha = 1 - Math.abs(dX)*2 / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

    }

}
