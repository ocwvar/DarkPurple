package com.ocwvar.darkpurple.Activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.ocwvar.darkpurple.Adapters.MusicFolderAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.R;

import java.io.File;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Activities
 * Data: 2016/7/26 22:34
 * Project: DarkPurple
 * 歌曲扫描目录设置
 */
public class FolderSelectorActivity extends AppCompatActivity implements MusicFolderAdapter.OnPathChangedCallback {

    MusicFolderAdapter adapter;
    RecyclerView recyclerView;
    EditText editText;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musicfloder);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new MusicFolderAdapter(this);

        editText = (EditText)findViewById(R.id.editText);

        recyclerView = (RecyclerView)findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(FolderSelectorActivity.this,LinearLayoutManager.VERTICAL,false));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_addfolder,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_add_folder:
                String string = editText.getText().toString();
                //先检查路径是否有效
                if (isPathVaild(string)){
                    adapter.addPath(string);
                }else {
                    Snackbar.make(findViewById(android.R.id.content),R.string.info_wrongPath,Snackbar.LENGTH_LONG).show();
                }
                editText.getText().clear();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 检测路径是否合法
     * @param path  用户输入的路径文字
     * @return  合法性
     */
    private boolean isPathVaild(String path){
        if (TextUtils.isEmpty(path)){
            return false;
        }else {
            File folder = new File(path);
            if (folder.exists() && folder.canRead()){
                folder = null;
                return true;
            }else {
                folder = null;
                return false;
            }
        }
    }

    /**
     * 移除路径成功
     */
    @Override
    public void onRemovedPath() {
        AppConfigs.updatePathSet(adapter.getPaths());
        Snackbar.make(findViewById(android.R.id.content),R.string.info_removedPath,Snackbar.LENGTH_LONG).show();
    }

    /**
     * 添加路径成功
     */
    @Override
    public void onAddedPath() {
        AppConfigs.updatePathSet(adapter.getPaths());
        Snackbar.make(findViewById(android.R.id.content),R.string.info_addedPath,Snackbar.LENGTH_LONG).show();
    }

    /**
     * 添加数据失败
     */
    @Override
    public void onAddedFailed() {
        Snackbar.make(findViewById(android.R.id.content),R.string.info_existPath,Snackbar.LENGTH_LONG).show();
    }

}
