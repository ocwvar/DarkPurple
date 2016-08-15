package com.ocwvar.darkpurple.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.ocwvar.darkpurple.Adapters.PlaylistDetailAdapter;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
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

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

}
