package com.ocwvar.darkpurple.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
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

    SongItem songItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
     * @return  Json数据 , 如果解析失败或未找到数据 , 则会返回NULL
     */
    private @Nullable JsonObject decodeSearchPage(@Nullable String pageText){
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
                    return new JsonParser().parse(page).getAsJsonObject();
                }catch (Exception e){
                    return null;
                }
            }
        }
        return null;
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

}
