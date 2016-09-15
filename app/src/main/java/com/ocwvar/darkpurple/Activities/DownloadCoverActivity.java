package com.ocwvar.darkpurple.Activities;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ocwvar.darkpurple.Adapters.CoverPreviewAdapter;
import com.ocwvar.darkpurple.Bean.CoverPreviewBean;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.JSONHandler;
import com.ocwvar.darkpurple.Units.Logger;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/9/14 23:01
 * File Location com.ocwvar.darkpurple.Activities
 * 下载歌曲封面页面 所有数据均解析自网站: coverbox.sinaapp.com
 */
public class DownloadCoverActivity extends AppCompatActivity{

    CoverPreviewAdapter adapter;
    RecyclerView recyclerView;
    Toolbar toolbar;
    SongItem songItem;

    String headResult;
    String headSearch;
    View panel;
    TextView progress;
    WeakReference<AlertDialog> copyRightDialog = new WeakReference<>(null);
    WeakReference<AlertDialog> infoDialog = new WeakReference<>(null);

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null){
            songItem = getIntent().getExtras().getParcelable("item");
        }else {
            Toast.makeText(DownloadCoverActivity.this, R.string.error_songitem , Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        headResult = getResources().getString(R.string.head_result);
        headSearch = getResources().getString(R.string.head_searchText);

        setContentView(R.layout.activity_download_cover);
        adapter = new CoverPreviewAdapter();
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(headResult+"0");
        panel = findViewById(R.id.view_panel);
        progress = (TextView)findViewById(R.id.textView_cover_progress);
        recyclerView = (RecyclerView)findViewById(R.id.recycleView);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(DownloadCoverActivity.this,2,LinearLayoutManager.VERTICAL,false));

        showInfoDialog();

        if (TextUtils.isEmpty(songItem.getAlbum()) || songItem.getAlbum().equals("<unknown>")){
            //如果没有专辑名称,则使用音频文件名来搜索
            new LoadAllPreviewTask(songItem.getFileName()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }else {
            //用专辑名来搜索数据
            new LoadAllPreviewTask(songItem.getAlbum()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }

    }

    /**
     * 获取封面详情界面数据
     * @param url   预览图链接
     * @return  得到的结果网页文本 , 如果网络错误 , 则会返回NULL
     */
    private String getCovers(String url){
        //详情搜索结果
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(10, TimeUnit.SECONDS);
        client.setWriteTimeout(10, TimeUnit.SECONDS);
        RequestBody requestBody = new FormEncodingBuilder()
                .add("input",url)
                .add("way","smart")
                .build();
        Request request = new Request.Builder()
                .url("http://coverbox.sinaapp.com/result")
                .post(requestBody).build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                String result = response.body().string();
                response.body().close();
                return result;
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * 解析详情页面数据
     * @param pageText  网页文本
     * @return  pageText  封面数据数组  [0]:封面尺寸  [1]:封面地址
     */
    private ArrayList<String[]> decodeCoverDetail(String pageText){
        Document document = Jsoup.parse(pageText);
        Elements coverSizes = document.getElementsByTag("figure");
        ArrayList<String[]> covers = new ArrayList<>();

        for (int i = 0; i < coverSizes.size(); i++) {
            String[] singleCover  = new String[2];
            singleCover[0] = coverSizes.get(i).getElementsByTag("figcaption").get(0).text();
            singleCover[1] = coverSizes.get(i).getElementsByTag("a").attr("href");
            covers.add(singleCover);
        }

        return covers;
    }

    /**
     * 显示信息对话框
     */
    private void showInfoDialog(){
        AlertDialog dialog = infoDialog.get();
        if (dialog == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(DownloadCoverActivity.this,R.style.FullScreen_TransparentBG);
            builder.setMessage(R.string.cover_info);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog = builder.create();
            infoDialog = new WeakReference<>(dialog);
        }
        dialog.show();
    }

    /**
     * 显示版权对话框
     */
    private void showCopyRightDialog(){
        AlertDialog dialog = copyRightDialog.get();
        if (dialog == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(DownloadCoverActivity.this,R.style.FullScreen_TransparentBG);
            builder.setMessage(R.string.coverbox_info);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog = builder.create();
            copyRightDialog = new WeakReference<>(dialog);
        }
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cover_download,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_action_info:
                showCopyRightDialog();
                break;
        }
        return true;
    }

    /**
     * 获取歌曲封面预览图任务
     */
    class LoadAllPreviewTask extends AsyncTask<Integer,Integer,ArrayList<CoverPreviewBean>>{
        final String TAG = "封面搜索任务";
        final String searchText;

        @SuppressWarnings("ConstantConditions")
        public LoadAllPreviewTask(String searchText) {
            this.searchText = searchText;
            getSupportActionBar().setSubtitle(headSearch+searchText);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setText(R.string.simple_loading);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch (values[0]){
                case 1:
                    progress.setText(R.string.progress_loading);
                    break;
                case 2:
                    progress.setText(R.string.progress_decode);
                    break;
            }
        }

        @Override
        protected ArrayList<CoverPreviewBean> doInBackground(Integer... integers) {
            Logger.warnning(TAG,"搜索关键字: "+searchText);
            return JSONHandler.loadCoverPreviewList(decodeSearchPage(searchCover(searchText)));
        }

        @Override
        protected void onPostExecute(ArrayList<CoverPreviewBean> list) {
            super.onPostExecute(list);
            if (list == null || list.size() <= 0){
                setTitle(headResult+"0");
                progress.setText(R.string.noResult);
            }else {
                setTitle(headResult+String.valueOf(list.size()));
                panel.setVisibility(View.GONE);
            }
            adapter.addDatas(list);

        }

        /**
         * 搜索封面数据
         * @param name  搜索名称
         * @return  搜索得到的结果网页文本 , 如果网络错误 , 则会返回NULL
         */
        private @Nullable String searchCover(@Nullable String name){
            if (TextUtils.isEmpty(name)){
                return null;
            }
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(10, TimeUnit.SECONDS);
            client.setReadTimeout(10, TimeUnit.SECONDS);
            client.setWriteTimeout(10, TimeUnit.SECONDS);
            RequestBody requestBody = new FormEncodingBuilder().add("input",name).build();
            Request request = new Request.Builder().url("http://coverbox.sinaapp.com/list").post(requestBody).build();
            Response response;
            try {
                response = client.newCall(request).execute();
                if (response.isSuccessful()){
                    String result = response.body().string();
                    response.body().close();
                    publishProgress(1);
                    return result;
                }
            } catch (IOException e) {
                return null;
            }
            return null;
        }

        /**
         * 解析搜索页面
         * @param pageText  网页文本
         * @return  Json数据文本 , 如果解析失败或未找到数据 , 则会返回NULL
         */
        private @Nullable String decodeSearchPage(@Nullable String pageText){
            if (TextUtils.isEmpty(pageText)){
                return null;
            }
            Document document = Jsoup.parse(pageText);
            Elements array = document.getElementsByTag("script");
            for (int i = 0; i < array.size(); i++) {
                String page = array.get(i).html();
                if (page.contains("resultCount")){
                    try {
                        page = page.substring(49);
                        page = page.substring(0,page.length()-23);
                        publishProgress(2);
                        return page;
                    }catch (Exception e){
                        return null;
                    }
                }
            }
            return null;
        }

    }

}
