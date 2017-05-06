package com.ocwvar.darkpurple.Activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ocwvar.darkpurple.Adapters.CoverPreviewAdapter;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.CoverPreviewBean;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;
import com.ocwvar.darkpurple.Units.JSONHandler;
import com.ocwvar.darkpurple.Units.Logger;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class DownloadCoverActivity extends BaseBlurActivity implements CoverPreviewAdapter.OnPreviewClickCallback {

    public static int DATA_UNCHANGED = 0;
    public static int DATA_CHANGED = 1;
    CoverPreviewAdapter adapter;
    RecyclerView recyclerView;
    SongItem songItem;
    String headResult;
    String headSearch;
    View panel;
    TextView progress;
    WeakReference<AlertDialog> copyRightDialog = new WeakReference<>(null);
    WeakReference<AlertDialog> infoDialog = new WeakReference<>(null);

    @Override
    protected boolean onPreSetup() {
        if (getIntent().getExtras() != null) {
            songItem = getIntent().getExtras().getParcelable("item");
        } else {
            Toast.makeText(DownloadCoverActivity.this, R.string.ERROR_songitem, Toast.LENGTH_SHORT).show();
            return false;
        }

        headResult = getResources().getString(R.string.head_result);
        headSearch = getResources().getString(R.string.head_searchText);
        adapter = new CoverPreviewAdapter();
        return true;
    }

    @Override
    protected int onSetToolBar() {
        return R.id.toolbar;
    }

    @Override
    protected int setActivityView() {
        return R.layout.activity_download_cover;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onSetupViews() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(headResult + "0");

        panel = findViewById(R.id.view_panel);
        progress = (TextView) findViewById(R.id.textView_cover_progress);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);

        adapter.setCallback(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(DownloadCoverActivity.this, 2, LinearLayoutManager.VERTICAL, false));

        showInfoDialog();

        if (TextUtils.isEmpty(songItem.getTitle()) || songItem.getTitle().equals("<unknown>") || songItem.getTitle().equals("未知")) {
            //如果没有专辑名称,则使用音频文件名来搜索
            new LoadAllPreviewTask(songItem.getFileName()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            //用专辑名来搜索数据
            new LoadAllPreviewTask(songItem.getTitle()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }

        setResult(DATA_UNCHANGED, null);
    }

    @Override
    protected void onViewClick(View clickedView) {

    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }

    /**
     * 显示信息对话框
     */
    private void showInfoDialog() {

        if (AppConfigs.ApplicationContext.getSharedPreferences(AppConfigs.SP_ONCE, 0).getBoolean("show_cover_info_dialog", true)) {
            AlertDialog dialog = infoDialog.get();
            if (dialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DownloadCoverActivity.this, R.style.FullScreen_TransparentBG);
                builder.setMessage(R.string.cover_info);
                builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        AppConfigs.ApplicationContext.getSharedPreferences(AppConfigs.SP_ONCE, 0).edit().putBoolean("show_cover_info_dialog", false).apply();
                    }
                });
                dialog = builder.create();
                infoDialog = new WeakReference<>(dialog);
            }
            dialog.show();
        }

    }

    /**
     * 显示版权对话框
     */
    private void showCopyRightDialog() {
        AlertDialog dialog = copyRightDialog.get();
        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DownloadCoverActivity.this, R.style.FullScreen_TransparentBG);
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
        getMenuInflater().inflate(R.menu.menu_cover_download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
     * 恢复原本的封面
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onRecoverCover() {

        if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
            //删除下载的封面
            new File(AppConfigs.DownloadCoversFolder + songItem.getFileName() + ".jpg").delete();
            //清空自定义数据 和 Picasso的缓存
            Picasso.with(DownloadCoverActivity.this).invalidate(songItem.getCustomCoverPath());
            songItem.setCustomCoverPath("");
            songItem.setCustomPaletteColor(AppConfigs.Color.DefaultCoverColor);

            Toast.makeText(DownloadCoverActivity.this, R.string.recover_successful, Toast.LENGTH_SHORT).show();

            //设置返回页面结束传递的数据
            Intent intent = new Intent();
            intent.putExtra("item", songItem);
            setResult(DATA_CHANGED, intent);
            finish();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.recover_failed, Snackbar.LENGTH_LONG).show();
        }

    }

    /**
     * 获取封面混合颜色  以暗色调优先 亮色调为次  如果都没有则使用默认颜色
     *
     * @param coverImage 封面图像
     * @return 混合颜色
     */
    private int getAlbumCoverColor(Bitmap coverImage) {
        Palette palette;

        try {
            palette = new Palette.Builder(coverImage).generate();
        } catch (Exception e) {
            //如果图像解析失败 或 图像为Null 则使用默认颜色
            return AppConfigs.Color.DefaultCoverColor;
        }

        int color = AppConfigs.Color.DefaultCoverColor, item = 0;
        //获取封面混合颜色  以暗色调优先 亮色调为次  如果都没有则使用默认颜色
        while (color == AppConfigs.Color.DefaultCoverColor && item < 7) {
            switch (item) {
                case 0:
                    color = palette.getDarkMutedColor(AppConfigs.Color.DefaultCoverColor);
                    break;
                case 1:
                    color = palette.getDarkVibrantColor(AppConfigs.Color.DefaultCoverColor);
                    break;
                case 3:
                    color = palette.getMutedColor(AppConfigs.Color.DefaultCoverColor);
                    break;
                case 4:
                    color = palette.getLightMutedColor(AppConfigs.Color.DefaultCoverColor);
                    break;
                case 5:
                    color = palette.getLightVibrantColor(AppConfigs.Color.DefaultCoverColor);
                    break;
                default:
                    color = AppConfigs.Color.DefaultCoverColor;
                    break;
            }
            item += 1;
        }
        return color;
    }

    /**
     * 点击封面预览图
     *
     * @param coverPreviewBean 预览图信息Bean
     */
    @Override
    public void onPreviewClick(CoverPreviewBean coverPreviewBean) {
        new LoadSizesTask(coverPreviewBean).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * 获取歌曲封面预览图任务
     */
    final class LoadAllPreviewTask extends AsyncTask<Integer, Integer, ArrayList<CoverPreviewBean>> {
        final String TAG = "封面搜索任务";
        final String searchText;

        @SuppressWarnings("ConstantConditions")
        LoadAllPreviewTask(String searchText) {
            this.searchText = searchText;
            getSupportActionBar().setSubtitle(headSearch + searchText);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setText(R.string.simple_loading);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch (values[0]) {
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
            Logger.warnning(TAG, "搜索关键字: " + searchText);
            return JSONHandler.loadCoverPreviewList(decodeSearchPage(searchCover(searchText)));
        }

        @Override
        protected void onPostExecute(ArrayList<CoverPreviewBean> list) {
            super.onPostExecute(list);
            if (list == null || list.size() <= 0) {
                setTitle(headResult + "0");
                progress.setText(R.string.noResult_Preview);
            } else {
                setTitle(headResult + String.valueOf(list.size() - 1));
                panel.setVisibility(View.GONE);
            }
            adapter.addDatas(list);

        }

        /**
         * 搜索封面数据
         *
         * @param name 搜索名称
         * @return 搜索得到的结果网页文本 , 如果网络错误 , 则会返回NULL
         */
        private
        @Nullable
        String searchCover(@Nullable String name) {
            if (TextUtils.isEmpty(name)) {
                return null;
            }
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(10, TimeUnit.SECONDS);
            client.setReadTimeout(10, TimeUnit.SECONDS);
            client.setWriteTimeout(10, TimeUnit.SECONDS);
            RequestBody requestBody = new FormEncodingBuilder().add("input", name).build();
            Request request = new Request.Builder().url("http://coverbox.sinaapp.com/list").post(requestBody).build();
            Response response;
            try {
                response = client.newCall(request).execute();
                if (response.isSuccessful()) {
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
         *
         * @param pageText 网页文本
         * @return Json数据文本 , 如果解析失败或未找到数据 , 则会返回NULL
         */
        private
        @Nullable
        String decodeSearchPage(@Nullable String pageText) {
            if (TextUtils.isEmpty(pageText)) {
                return null;
            }
            Document document = Jsoup.parse(pageText);
            Elements array = document.getElementsByTag("script");
            for (int i = 0; i < array.size(); i++) {
                String page = array.get(i).html();
                if (page.contains("resultCount")) {
                    try {
                        page = page.substring(49);
                        page = page.substring(0, page.length() - 23);
                        publishProgress(2);
                        return page;
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            return null;
        }

    }

    /**
     * 获取封面详情任务
     */
    private final class LoadSizesTask extends AsyncTask<Integer, Void, ArrayList<String[]>> {

        final ProgressDialog progressDialog;
        final CoverPreviewBean coverPreviewBean;

        WeakReference<AlertDialog> selectorDialog;

        LoadSizesTask(@NonNull CoverPreviewBean coverPreviewBean) {
            this.coverPreviewBean = coverPreviewBean;
            progressDialog = new ProgressDialog(DownloadCoverActivity.this, R.style.FullScreen_TransparentBG);
            progressDialog.setMessage(getString(R.string.simple_loading));
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(true);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected ArrayList<String[]> doInBackground(Integer... integers) {
            ArrayList<String[]> result;

            if (!TextUtils.isEmpty(coverPreviewBean.getArtworkUrl100())) {
                result = decodeCoverDetail(getCovers(coverPreviewBean.getArtworkUrl100()));
            } else if (!TextUtils.isEmpty(coverPreviewBean.getArtworkUrl60())) {
                result = decodeCoverDetail(getCovers(coverPreviewBean.getArtworkUrl60()));
            } else {
                result = null;
            }

            return result;

        }

        @Override
        protected void onPostExecute(ArrayList<String[]> result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            if (result == null || result.size() <= 0) {
                Snackbar.make(findViewById(android.R.id.content), R.string.noResult_Detail, Snackbar.LENGTH_LONG).show();
            } else {
                preLoadSelectorDialog(result);
                AlertDialog dialog = selectorDialog.get();
                if (dialog != null) {
                    dialog.show();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), R.string.ERROR_OOM, Snackbar.LENGTH_LONG).show();
                }
            }
        }

        /**
         * 获取封面详情界面数据
         *
         * @param url 预览图链接
         * @return 得到的结果网页文本 , 如果网络错误 , 则会返回NULL
         */
        private String getCovers(String url) {
            if (TextUtils.isEmpty(url)) {
                return null;
            }

            //详情搜索结果
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(10, TimeUnit.SECONDS);
            client.setReadTimeout(10, TimeUnit.SECONDS);
            client.setWriteTimeout(10, TimeUnit.SECONDS);
            RequestBody requestBody = new FormEncodingBuilder()
                    .add("input", url)
                    .add("way", "smart")
                    .build();
            Request request = new Request.Builder()
                    .url("http://coverbox.sinaapp.com/result")
                    .post(requestBody).build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
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
         *
         * @param pageText 网页文本
         * @return pageText  封面数据数组  [0]:封面尺寸  [1]:封面地址
         */
        private ArrayList<String[]> decodeCoverDetail(String pageText) {

            if (TextUtils.isEmpty(pageText)) {
                return null;
            }

            Document document = Jsoup.parse(pageText);
            Elements coverSizes = document.getElementsByTag("figure");
            ArrayList<String[]> covers = new ArrayList<>();

            for (int i = 0; i < coverSizes.size(); i++) {
                String[] singleCover = new String[2];
                singleCover[0] = coverSizes.get(i).getElementsByTag("figcaption").get(0).text();
                singleCover[1] = coverSizes.get(i).getElementsByTag("a").attr("href");
                covers.add(singleCover);
            }

            return covers;
        }

        /**
         * 预加载选择对话框
         *
         * @param result 搜索解析得到的结果
         */
        @SuppressLint("InflateParams")
        private void preLoadSelectorDialog(ArrayList<String[]> result) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DownloadCoverActivity.this, R.style.FullScreen_TransparentBG);
            //加载对话框布局
            View dialogView = LayoutInflater.from(DownloadCoverActivity.this).inflate(R.layout.dialog_selector_cover, null);
            ListView listView = (ListView) dialogView.findViewById(R.id.listView);
            //加载适配器
            SelectorAdapter adapter = new SelectorAdapter(result);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(adapter);
            builder.setView(dialogView);
            //将对话框放入弱引用中,给予最大限度的自动释放机会
            selectorDialog = new WeakReference<>(builder.create());
        }

        /**
         * 选择器的ListView适配器
         */
        final class SelectorAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

            ArrayList<String[]> result;

            SelectorAdapter(ArrayList<String[]> result) {
                this.result = result;
            }

            @Override
            public int getCount() {
                return result.size();
            }

            @Override
            public Object getItem(int i) {
                return result.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                SelectorViewHolder viewHolder;
                if (view == null) {
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_cover_selector_size, viewGroup, false);
                    viewHolder = new SelectorViewHolder(view);
                    view.setTag(viewHolder);
                } else {
                    viewHolder = (SelectorViewHolder) view.getTag();
                }

                viewHolder.sizeTextView.setText(result.get(i)[0]);

                return view;
            }

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                new DownloadThread(result.get(i)[1], songItem.getFileName()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                if (selectorDialog.get() != null) {
                    selectorDialog.get().dismiss();
                }
            }

            class SelectorViewHolder {

                TextView sizeTextView;

                SelectorViewHolder(View itemView) {
                    sizeTextView = (TextView) itemView.findViewById(R.id.textView_cover_selector_size);
                }

            }

        }

    }

    /**
     * 简易图片下载线程
     */
    private final class DownloadThread extends AsyncTask<Integer, Integer, Boolean> {

        final String url;
        final String fileName;

        final ProgressDialog progressDialog;

        DownloadThread(String url, String fileName) {
            this.url = url;
            this.fileName = fileName;
            this.progressDialog = new ProgressDialog(DownloadCoverActivity.this, R.style.FullScreen_TransparentBG);
            progressDialog.setMessage(getString(R.string.simple_downloading));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(true);
                }
            });
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMax(100);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        protected Boolean doInBackground(Integer... integers) {

            OkHttpClient client = new OkHttpClient();
            client.setWriteTimeout(10, TimeUnit.SECONDS);
            client.setReadTimeout(10, TimeUnit.SECONDS);
            client.setConnectTimeout(10, TimeUnit.SECONDS);

            Request request = new Request.Builder().url(url).build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {

                    File file = new File(AppConfigs.DownloadCoversFolder);
                    if (!file.exists()) {
                        //优先检查是否存在下载的保存目录
                        file.mkdirs();
                    }
                    file = new File(AppConfigs.DownloadCoversFolder + fileName + ".jpg");
                    if (!file.exists()) {
                        //再检查是否存在文件 , 如果不存在则创建新的空白文件
                        file.createNewFile();
                    }
                    //无论是否文件内已经写有数据 , 都重新写入
                    FileOutputStream fileOutputStream = new FileOutputStream(file, false);
                    InputStream inputStream = response.body().byteStream();
                    byte[] buffer = new byte[1024];
                    long totalLength = response.body().contentLength();
                    int readLength, totalReaded = 0;
                    while ((readLength = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, readLength);
                        totalReaded += readLength;
                        publishProgress((int) (((float) totalReaded / (float) totalLength) * 100));
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    inputStream.close();
                    response.body().close();
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    if (bitmap != null) {
                        songItem.setCustomPaletteColor(getAlbumCoverColor(bitmap));
                        //这里要设置成绝对路径 , 给Picasso读取使用
                        songItem.setCustomCoverPath("file:///" + file.getPath());
                        bitmap.recycle();
                        bitmap = null;
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                new File(AppConfigs.DownloadCoversFolder + fileName + ".jpg").delete();
                return false;
            }
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        protected void onCancelled() {
            super.onCancelled();
            new File(AppConfigs.DownloadCoversFolder + fileName + ".jpg").delete();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();
            if (!aBoolean) {
                Snackbar.make(findViewById(android.R.id.content), R.string.simple_download_failed, Snackbar.LENGTH_LONG).show();
            } else {
                Picasso.with(DownloadCoverActivity.this).invalidate(songItem.getCustomCoverPath());
                Intent intent = new Intent();
                intent.putExtra("item", songItem);
                setResult(DATA_CHANGED, intent);
                Toast.makeText(DownloadCoverActivity.this, R.string.simple_download_completed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

}
