package com.ocwvar.darkpurple.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

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
public class FolderSelectorActivity extends AppCompatActivity implements MusicFolderAdapter.OnPathChangedCallback, View.OnClickListener {

    MusicFolderAdapter adapter;
    RecyclerView recyclerView;
    TextView openUI;
    ImageButton addPath;
    EditText editText;

    public static final int DATA_CHANGED = 1;
    public static final int DATA_UNCHANGED = 0;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musicfloder);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        openUI = (TextView)findViewById(R.id.openUI);
        addPath = (ImageButton)findViewById(R.id.imageButton_addPath);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new MusicFolderAdapter(this);

        editText = (EditText)findViewById(R.id.editText);

        recyclerView = (RecyclerView)findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(FolderSelectorActivity.this,LinearLayoutManager.VERTICAL,false));
        recyclerView.setAdapter(adapter);

        openUI.setOnClickListener(this);
        addPath.setOnClickListener(this);
        setResult(DATA_UNCHANGED);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 777 && resultCode == FolderSelectorUI.RESULT_CODE){
            //从浏览器返回时检查数据
            if (data != null){
                try {
                    File[] folders = (File[]) data.getExtras().getSerializable("Data");
                    if (folders != null){
                        for (File folder : folders) {
                            if (isPathVaild(folder.getPath())){
                                adapter.addPath(folder.getPath(),true);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
        setResult(DATA_CHANGED);
    }

    /**
     * 添加路径成功
     */
    @Override
    public void onAddedPath() {
        AppConfigs.updatePathSet(adapter.getPaths());
        Snackbar.make(findViewById(android.R.id.content),R.string.info_addedPath,Snackbar.LENGTH_LONG).show();
        setResult(DATA_CHANGED);
    }

    /**
     * 添加数据失败
     */
    @Override
    public void onAddedFailed() {
        Snackbar.make(findViewById(android.R.id.content),R.string.info_existPath,Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.openUI:
                startActivityForResult(new Intent(FolderSelectorActivity.this , FolderSelectorUI.class) , 777);
                break;
            case R.id.imageButton_addPath:
                String string = editText.getText().toString();
                //先检查路径是否有效
                if (isPathVaild(string)){
                    adapter.addPath(string,false);
                }else {
                    Snackbar.make(findViewById(android.R.id.content),R.string.info_wrongPath,Snackbar.LENGTH_LONG).show();
                }
                editText.getText().clear();
                break;
        }
    }
}
