package com.ocwvar.darkpurple.Activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ocwvar.darkpurple.Adapters.FolderSelectorAdapter;
import com.ocwvar.darkpurple.R;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/29 10:18
 * File Location com.ocwvar.darkpurple.Activities
 * 文件夹选择UI
 */
public class FolderSelectorUI extends AppCompatActivity implements FolderSelectorAdapter.OnFolderSelectCallback, View.OnClickListener {

    public static final int RESULT_CODE = 9990;

    private List<File> pathIndex;
    private FolderSelectorAdapter adapter;
    private RecyclerView recyclerView;
    private TextView folderCount;
    private ProgressBar progressBar;
    private FetchFolderTask fetchFolderTask;
    private Button done;

    private WeakReference<AlertDialog> confirmDialog = new WeakReference<>(null);

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_ui);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        setTitle(R.string.title_select_folder_ui);
        setResult(RESULT_CODE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pathIndex = new ArrayList<>();
        adapter = new FolderSelectorAdapter();
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        done = (Button)findViewById(R.id.button_done);
        folderCount = (TextView)findViewById(R.id.textView_count);
        recyclerView = (RecyclerView)findViewById(R.id.recycleView);

        recyclerView.setLayoutManager(new LinearLayoutManager( FolderSelectorUI.this , LinearLayoutManager.VERTICAL ,false ));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        progressBar.setVisibility(View.GONE);
        pathIndex.add(Environment.getExternalStorageDirectory());

        done.setOnClickListener(this);
        adapter.setCallback(this);
        folderCount.setText(String.format("%s%s", getString(R.string.info_folder_count), Integer.toString(adapter.getSelectedPathCount())));

        fetchFolderTask = new FetchFolderTask(pathIndex.get(0) , null);
        fetchFolderTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * 进入文件夹回调
     * @param folder  要进入的文件夹路径
     */
    @Override
    public void onEnter(File folder) {
        if ( folder != null && !pathIndex.get(0).equals(folder) && fetchFolderTask == null || fetchFolderTask.getStatus() == AsyncTask.Status.FINISHED ){
            //如果请求的地址不为空 , 请求的地址跟当前地址不相同 , 上一个文件夹获取任务已结束 , 同时满足才进行获取
            fetchFolderTask = null;
            fetchFolderTask = new FetchFolderTask(folder, new OnCompletedCallback() {
                @Override
                public void onCompleted(File folder) {
                    pathIndex.add( 0 , folder);
                }
            });
            fetchFolderTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    /**
     * 返回上一个目录位置
     */
    private void onPrevious(){
        if (pathIndex.size() > 1 && (fetchFolderTask == null || fetchFolderTask.getStatus() == AsyncTask.Status.FINISHED )){
            fetchFolderTask = null;
            fetchFolderTask = new FetchFolderTask(pathIndex.get(1) , new OnCompletedCallback() {
                @Override
                public void onCompleted(File folder) {
                    pathIndex.remove(0);
                }
            });
            fetchFolderTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }else {
            Snackbar.make(findViewById(android.R.id.content) , R.string.info_noPrevious , Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSelectedFolderChanged() {
        folderCount.setText(String.format("%s%s", getString(R.string.info_folder_count), Integer.toString(adapter.getSelectedPathCount())));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if (adapter.getSelectedPathCount() > 0){
                    showConfirmDialog();
                }else {
                    finish();
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.setCallback(null);
        done.setOnClickListener(null);
        recyclerView = null;
        adapter = null;
        folderCount = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_done:
                if (adapter.getSelectedPathCount() > 0){
                    Intent intent = new Intent();
                    intent.putExtra("Data",adapter.getSelectedPath().toArray(new File[adapter.getSelectedPathCount()]));
                    setResult(RESULT_CODE , intent);
                }else {
                    setResult(RESULT_CODE , null);
                }
                finish();
                break;
        }
    }

    /**
     * 显示退出确认对话框
     */
    private void showConfirmDialog(){
        if (confirmDialog.get() == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(FolderSelectorUI.this , R.style.FullScreen_TransparentBG);
            builder.setMessage(R.string.info_confirm);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });
            builder.setNegativeButton(R.string.simple_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            confirmDialog = new WeakReference<>(builder.create());
        }

        confirmDialog.get().show();
    }

    /**
     *  获取请求路径下的目录
     */
    class FetchFolderTask extends AsyncTask<Integer , Void , List<File>>{

        private File folder;
        final private FileFilter fileFilter;
        private OnCompletedCallback callback;

        public FetchFolderTask(@NonNull File folder , OnCompletedCallback callback) {
            this.callback = callback;
            this.folder = folder;
            this.fileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    //过滤所有文件夹出来
                    return file.isDirectory();
                }
            };
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<File> doInBackground(Integer... integers) {
            List<File> paths = null;
            File[] folders = new File(folder.getPath()).listFiles(fileFilter);

            if (folders != null && folders.length != 0){
                paths = new ArrayList<>();
                Collections.addAll (paths , folders);
                Collections.sort(paths, new Comparator<File>() {
                    @Override
                    public int compare(File file, File t1) {
                        return file.getName().compareTo(t1.getName());
                    }
                });
            }
            return paths;
        }

        @Override
        protected void onPostExecute(List<File> folders) {
            super.onPostExecute(folders);
            adapter.addDatas(folders);
            progressBar.setVisibility(View.GONE);
            if (callback != null){
                callback.onCompleted(folder);
            }
        }

    }
    interface OnCompletedCallback{

        void onCompleted(File folder);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            onPrevious();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
