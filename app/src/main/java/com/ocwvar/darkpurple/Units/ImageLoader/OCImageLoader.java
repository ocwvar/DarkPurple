package com.ocwvar.darkpurple.Units.ImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.ocwvar.darkpurple.Units.OCThreadExecutor;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static android.graphics.Color.TRANSPARENT;

/**
 * Created by 区成伟
 * Date: 2016/5/24  22:29
 * Version 20
 * Project: 图片读取帮助类
 *
 * 图片异步本地拉取
 * 图片异步网络拉取 (使用 OKHTTP3 网络框架)
 * 图片异步处理
 * 资源文件图片拉取
 * 图片缩略图生成
 * 图片缓存
 */

public class OCImageLoader {

    private Handler handler;
    private OCImageCacher imageCacher;
    private OCThreadExecutor fetherExecutor,cacheExecutor;
    private Context context;
    private static OCImageLoader imageLoader;

    private Drawable loadingRes , failedRes;

    public static int THREADPOOL_CACHE = 0;
    public static int THREADPOOL_MAIN = 1;

    int     durationMillis = 200,   //图片拉取动画渐变时间, 0 = 无动画
            max_Download=1,         //图片 网络拉取 任务最大线程数
            max_Cache=3;            //图片 缓存拉取 本地拉取 图片处理 任务最大线程数

    public OCImageLoader() {

        //创建图片缓存工具类
        if (imageCacher == null){
            imageCacher = new OCImageCacher();
        }

        //创建用于在UI线程处理的  Handler
        if (handler == null){
            handler = new Handler(Looper.getMainLooper());
        }
        
        //创建 网络拉取 工作线程池
        if (fetherExecutor == null){
            fetherExecutor = new OCThreadExecutor(max_Download,"FetcherThreads");
        }

        //创建 缓存拉取 本地拉取 图片处理 工作线程池
        if (cacheExecutor == null){
            cacheExecutor = new OCThreadExecutor(max_Cache,"CacheThreads");
        }

        //创建读取时候的展示资源
        loadingRes = new ColorDrawable(Color.TRANSPARENT);
        failedRes = new ColorDrawable(Color.TRANSPARENT);

    }

    static public OCImageLoader loader(){
        if (imageLoader == null) imageLoader = new OCImageLoader();
        return imageLoader;
    }

    public void loadLocalImage(String filePath,ImageView imageView){
        loadLocalImage(filePath, imageView, null, null);
    }

    public void loadLocalImage(String filePath,OnImageLoad onImageLoad,BitmapFactory.Options options){
        loadLocalImage(filePath,null,onImageLoad,options);
    }

    /**
     * 异步从本地拉取图片输出到ImageView进行显示
     * @param filePath  文件路径
     * @param imageView 图片要显示的ImageView
     * @param onImageLoad   图片读取状态接口
     * @param options   图片加载时使用的BitmapFactory.Option
     */
    public void loadLocalImage(String filePath,ImageView imageView,OnImageLoad onImageLoad,BitmapFactory.Options options){
        if (filePath == null || TextUtils.isEmpty(filePath)){
            Log.e("On loading image","FilePath is empty or null.");
            return;
        }else if (imageView == null && onImageLoad == null){
            Log.e("On loading image","Must set ImageView or OnImageLoad interface.");
            return;
        }
        if (imageView != null){
            if (imageView.getDrawable() != null && (imageView.getTag() != null && imageView.getTag().equals(buildTag(filePath)))){
                Log.d("OCImageLoader","ImageView still have the currect image.Skipped TAG:"+buildTag(filePath));
                return;
            }else {
                if (loadingRes != null ){
                    imageView.setImageDrawable(loadingRes);
                }else {
                    imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
                }
                imageView.setTag(buildTag(filePath));
            }
        }
        if (options == null){
            fetherExecutor.submit(new FutureTask<>(new FetcherByLocal(imageView, onImageLoad, filePath, null)), buildTag(filePath));
        }else {
            fetherExecutor.submit(new FutureTask<>(new FetcherByLocal(imageView, onImageLoad, filePath, options)), buildTag(filePath) + "withop");
        }
    }

    public void loadImage(String tag,ImageView imageView){
        loadImage(tag,null,imageView,null);
    }

    public void loadImage(String tag,String imageURL,ImageView imageView){
        loadImage(tag, imageURL, imageView, null);
    }

    public void loadImage(String tag,String imageURL,OnImageLoad onImageLoad){
        loadImage(tag, imageURL, null, onImageLoad);
    }

    public void loadImage(String tag,String imageURL,ImageView imageView,OnImageLoad onImageLoad){
        loadImage(tag, imageURL, imageView, onImageLoad , null);
    }

    /**
     * 异步从网络拉取图片输出到ImageView进行显示.
     * @param tag   图片的唯一TAG
     * @param imageURL  图片的网址
     * @param imageView 图片要显示的ImageView
     * @param onImageLoad   图片读取状态接口
     * @param onLoaded  图片拉取完毕后对图像进行处理的接口
     */
    public void loadImage(String tag,String imageURL,ImageView imageView,OnImageLoad onImageLoad,HandleOnLoaded onLoaded){

        if (TextUtils.isEmpty(tag)){
            //如果没有设置Tag
            Log.e("OCImageLoader","TAG is empty or null");
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
            return;
        }

        //重新构建Tag标识
        tag = buildTag(tag);

        if (imageView != null){
            //如果ImageView不为空 , 则设置读取中的资源文件
            if (loadingRes != null ){
                imageView.setImageDrawable(loadingRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
            //给ImageView设置Tag标识
            imageView.setTag(tag);
        }

        if (imageURL == null || imageCacher.isCacheExist(tag)){
            //如果URL为空 或 有Tag相符的缓存资源
            Log.d("OCImageLoader","CacheThread "+tag+" task added");
            cacheExecutor.submit(new FutureTask<>(new FetcherByCache(tag, imageURL, imageView, onImageLoad,onLoaded)), tag);
        }else {
            Log.d("OCImageLoader","FetcherThread "+tag+" task added");
            fetherExecutor.submit(new FutureTask<>(new Fetcher(tag, imageURL, imageView, onImageLoad,onLoaded)), tag);
        }
    }

    public void reduceImage(String tag,String imageURL,BitmapFactory.Options options,OnHandleBitmap onHandleBitmap,boolean cacheAsFile){
        reduceImage(tag,null,imageURL,options,cacheAsFile,onHandleBitmap);
    }

    public void reduceImage(String tag,BitmapFactory.Options options,String filePath,OnHandleBitmap onHandleBitmap,boolean cacheAsFile){
        reduceImage(tag,filePath,null,options,cacheAsFile,onHandleBitmap);
    }

    /**
     * 处理图片效果
     * @param tag   图片的唯一TAG
     * @param filePath  图片本地路径
     * @param imageURL  图片网络地址
     * @param options   图片加载时使用的 BitmapFactory.Options
     * @param cacheAsFile   是否进行文件缓存
     * @param onHandleBitmap    图片拉取完毕后对图片进行处理的接口
     */
    private void reduceImage(final String tag, final String filePath, final String imageURL, final BitmapFactory.Options options,final boolean cacheAsFile, final OnHandleBitmap onHandleBitmap){
        if (imageURL != null){
            loadImage(imageURL, imageURL, new OnImageLoad() {
                @Override
                public void onLoadCompleted(Bitmap image,String tag) {
                    cacheExecutor.submit(new FutureTask<>(new ReduceImageEffect(onHandleBitmap, image, options, cacheAsFile, filePath, imageURL, buildTag(tag))), buildTag(tag));
                }

                @Override
                public void onLoadFailed() {

                }
            });
        }else {
            cacheExecutor.submit( new FutureTask<>(new ReduceImageEffect( onHandleBitmap , null , options , cacheAsFile , filePath , null , buildTag(tag) )) , buildTag(tag));
        }
    }

    /**
     * 获取本地图片的缩略图
     * @param path  文件路径
     * @param imageView 图片要显示的ImageView
     * @param handleOnLoaded    图片缩略图生成完毕后对缩略图进行处理的接口
     */
    public void loadImageThumbnail(@NonNull  String path , ImageView imageView , HandleOnLoaded handleOnLoaded){

        File checkFile = new File(path);

        if ( checkFile.isFile() && checkFile.canWrite() && imageView != null){
            //目标文件是文件 , 可被读取 , ImageView不为空

            checkFile = null;

            //设置加载前要使用的资源
            if (loadingRes != null ){
                imageView.setImageDrawable(loadingRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }

            //获取Tag标识 , 并给ImageView设置Tag标识
            String tag = buildTag(path+"Thumbnail");
            imageView.setTag(tag);

            if (imageCacher.isCacheExist(tag)){
                //若有缓存数据
                cacheExecutor.submit(new FutureTask<>(new FetcherByCache(tag,imageView,handleOnLoaded)),tag);
            }else {
                //没有缓存数据则开始读取
                fetherExecutor.submit(new FutureTask<>(new GenerateImageThumbnail(path,tag,imageView,handleOnLoaded)),tag);
            }

        }else if (imageView != null && (!checkFile.canWrite() || !checkFile.isFile())){
            //非法请求 , 则设置失败的图像资源
            imageView.setTag("Failed");
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
        }
    }

    /**
     * 从资源文件读取图像
     * @param resID 资源ID
     * @param imageView 目标ImageView
     * @param context   加载资源文件需要的ApplicationContext . 如果有预先设置 ApplicationContext 可不再传入
     */
    public void loadImageFromResource(@NonNull int resID , @NonNull ImageView imageView , Context context){
        if (resID == 0 || ( this.context == null && context == null)){
            //如果资源号为 0 ,或没有可以用于使用的 ApplicationContext
            Log.e("OCImageLoader", "Invaild resource id or no available context to use.");
            //设置读取失败使用的资源文件
            imageView.setTag("Failed");
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
            return;

        }else {

            if (this.context == null && context != null){
                //如果没有缓存全局 ApplicationContext 则进行缓存
                this.context = context.getApplicationContext();
            }

            //设置正在读取使用的资源文件
            if (loadingRes != null ){
                imageView.setImageDrawable(loadingRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
            //生成唯一Tag
            String tag = Integer.toString(resID);
            imageView.setTag(tag);

            fetherExecutor.submit(new FutureTask<>(new FetcherByResource(tag,resID,this.context,imageView)),tag);
        }

    }

    /**
     * 设置用于读取资源文件的 ApplicationContext
     * @param context ApplicationContext
     */
    public void setApplicationContext(Context context){
        this.context = context.getApplicationContext();
    }

    /**
     * 释放LRU缓存
     */
    public void releaseLruCaches(){
        imageCacher.releaseCaches();
    }

    /**
     * 缓存图片
     * @param tag   图片唯一TAG
     * @param image 图片对象
     */
    public void cacheImage(String tag,Bitmap image){
        tag = buildTag(tag);
        imageCacher.putCache(tag, image);
    }

    /**
     * 获取图片缓存
     * @param tag   图片唯一TAG
     * @return  返回缓存的图片对象,若没有缓存则返回 NULL
     */
    public Bitmap getCache(String tag){
        tag = buildTag(tag);
        return imageCacher.getCache(tag);
    }

    /**
     * 设置图片加载渐变动画的时长.  默认 500ms.  0ms = 无渐变效果
     * @param durationMillis    动画时长. 单位:ms
     */
    public void setDurationMillis(int durationMillis){
        this.durationMillis = durationMillis;
    }

    /**
     * 生成唯一 TAG
     * @param string    源数据
     * @return  TAG字符串
     */
    private String buildTag(String string){
        string = string.replaceAll("[^\\w]","");
        string = string.replaceAll(" ", "");
        return string;
    }

    /**
     * UI 线程
     * @param runnable 在UI线程运行的任务
     */
    private void runOnUIThread(Runnable runnable){
        boolean done = handler.post(runnable);
        while (!done){
            handler = new Handler(Looper.getMainLooper());
            runOnUIThread(runnable);
        }
    }

    /**
     * 取消拉取任务
     * @param tag   图片的唯一TAg
     * @param isContainText 是否取消同一图片的所有拉取任务
     * @param threadType    要取消哪个线程池内的任务.
     */
    public void cancelTask(String tag , boolean isContainText , int threadType){
        switch (threadType) {
            case 0:
                cacheExecutor.cancelTask(tag, isContainText);
                break;
            case 1:
                fetherExecutor.cancelTask(tag, isContainText);
                break;
            default:
                fetherExecutor.cancelTask(tag, isContainText);
                cacheExecutor.cancelTask(tag, isContainText);
                break;
        }

    }

    /**
     * 获取本地缓存文件夹当前大小
     * @return  本地缓存文件夹大小长度. 当返回 -1 时,表示没有权限或缓存文件夹不存在.
     */
    public long getCacheDirectorySize(){
        return imageCacher.getCacheDirectorySize();
    }

    /**
     * 清空缓存文件夹.仅删除文件夹根下的所有文件,不包括子文件夹内的文件
     */
    public void clearCacheDirectory(){
        imageCacher.clearCacheDirectory();
    }

    /**
     * 设置读取时要显示的图片资源
     * @param drawable  图片资源
     */
    public void setDrawableOfLoading(Drawable drawable){
        if (drawable != null){
            this.loadingRes = drawable;
        }
    }

    /**
     * 设置读取失败时要显示的图片资源
     * @param drawable  图片资源
     */
    public void setDrawableOfFailed(Drawable drawable){
        if (drawable != null){
            this.failedRes = drawable;
        }
    }

    /*
    网络拉取任务
     */
    private class Fetcher implements Callable<String> {

        private String tag;
        private String imageUrl;
        private ImageView imageView;
        private OnImageLoad onImageLoad;
        private HandleOnLoaded onLoaded;
        private Bitmap cacheImage;

        public Fetcher(String tag,String imageUrl,ImageView imageView,OnImageLoad onImageLoad,HandleOnLoaded onLoaded) {
            this.tag = tag;
            this.imageUrl = imageUrl;
            this.imageView = imageView;
            this.onImageLoad = onImageLoad;
            this.onLoaded = onLoaded;
        }

        @Override
        public String call() throws Exception {

            if (imageView != null || onImageLoad != null){
                //ImageView 不为空 或 有图片获取回调
                try {
                    //从网址获取图片对象
                    cacheImage = load(imageUrl);

                    if(cacheImage != null){
                        //若读取结果有图像 , 则放入缓存
                        imageCacher.putCache(tag,cacheImage);
                        if (onLoaded != null){
                            //若有处理图像回调 , 则将图像进行处理
                            cacheImage = onLoaded.reduce(cacheImage,tag);
                            if (cacheImage == null){
                                //如果处理完之后的图像为空 , 抛出异常
                                throw new Exception("Bitmap become NULL after reduce interface.");
                            }
                        }
                    }else {
                        //若得到的图像为空 , 抛出异常
                        throw new Exception("Failed to download image.");
                    }

                } catch (Exception e) {
                    Log.e("OCImageLoader", "\nurl:" + imageUrl +"\nError:"+ e.toString());
                    runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            onError();
                        }
                    });
                    return null;
                }
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("OCImageLoader","Image :"+imageUrl+" downloaded");
                        onDownloadCompleted(cacheImage,imageView,tag,onImageLoad);
                    }
                });
            }else {
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onError();
                    }
                });
            }
            return null;
        }

        /**
         * 从网络读取图片
         * @param url   网址
         * @return  图片对象 . 请求失败返回 Null
         * @throws Exception    处理期间发生的异常
         */
        private Bitmap load(String url)throws Exception{
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response.isSuccessful()){
                return BitmapFactory.decodeStream(response.body().byteStream());
            }else {
                return null;
            }
        }

        public void onDownloadCompleted(Bitmap bitmap, ImageView imageView, String tag,OnImageLoad onImageLoadCompleted) {
            fetherExecutor.removeTag(tag);

            if (bitmap != null && imageView != null && imageView.getTag().toString().equals(tag)){
                if (durationMillis > 0 ){
                    Drawable prevDrawable = new ColorDrawable(TRANSPARENT);
                    Drawable nextDrawable = new BitmapDrawable(imageView.getResources(), bitmap);
                    TransitionDrawable transitionDrawable = new TransitionDrawable(
                            new Drawable[] { prevDrawable, nextDrawable });
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(durationMillis);
                }else {
                    imageView.setImageBitmap(bitmap);
                }
            }

            if (onImageLoadCompleted != null){
                onImageLoadCompleted.onLoadCompleted(bitmap,tag);
            }

        }

        public void onError() {
            fetherExecutor.removeTag(tag);

            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
            if (onImageLoad != null){
                onImageLoad.onLoadFailed();
            }
        }

        @Override
        public int hashCode() {
            return tag.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this.hashCode() == o.hashCode() && o instanceof Fetcher;
        }

    }

    /*
    缓存拉取任务
     */
    private class FetcherByCache implements Callable<String>{

        private String tag;
        private String imageUrl;
        private ImageView imageView;
        private OnImageLoad onImageLoad;
        private HandleOnLoaded onLoaded;
        private Bitmap cacheImage;

        public FetcherByCache(String tag,String imageUrl,ImageView imageView,OnImageLoad onImageLoad,HandleOnLoaded onLoaded) {
            this.tag = tag;
            this.imageUrl = imageUrl;
            this.imageView = imageView;
            this.onImageLoad = onImageLoad;
            this.onLoaded = onLoaded;
        }

        public FetcherByCache(String tag,ImageView imageView,HandleOnLoaded onLoaded) {
            this.tag = tag;
            this.imageView = imageView;
            this.onImageLoad = null;
            this.onLoaded = onLoaded;
        }

        @Override
        public String call() throws Exception {
            //从缓存中以 Tag 为标志获取缓存
            cacheImage = imageCacher.getCache(tag);

            if (cacheImage != null){
                //如果从缓存中获取到了对象
                //如果有需要进行图像处理 , 则进行处理
                if (onLoaded != null) cacheImage = onLoaded.reduce(cacheImage,tag);

                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onCompleted(cacheImage,imageView,tag,onImageLoad);
                    }
                });
            }else if ( imageUrl != null){
                //如果获取不到图像 , 同时又有获取网址 , 则请求网络拉取
                requestLoadFromInternet();
            }else {
                //如果获取不到对象 , 同时有没有请求网址 , 则任务失败
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onError();
                    }
                });
            }

            return null;
        }

        /*
        获取完成回调
         */
        public void onCompleted(Bitmap bitmap, ImageView imageView, String tag, OnImageLoad onImageLoadCompleted) {
            cacheExecutor.removeTag(tag);

            if (bitmap != null && imageView != null && imageView.getTag().equals(tag)){
                if (durationMillis > 0 ){
                    Drawable prevDrawable = new ColorDrawable(TRANSPARENT);
                    Drawable nextDrawable = new BitmapDrawable(imageView.getResources(), bitmap);
                    TransitionDrawable transitionDrawable = new TransitionDrawable(
                            new Drawable[] { prevDrawable, nextDrawable });
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(durationMillis);
                }else {
                    imageView.setImageBitmap(bitmap);
                }
            }

            if (onImageLoadCompleted != null){
                onImageLoadCompleted.onLoadCompleted(bitmap,tag);
            }

        }

        /*
        重新请求网络加载
         */
        public void requestLoadFromInternet() {
            cacheExecutor.removeTag(tag);
            loadImage(tag, imageUrl, imageView, onImageLoad);
        }

        /*
        任务失败回调
         */
        public void onError() {
            cacheExecutor.removeTag(tag);
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
        }

        @Override
        public int hashCode() {
            return tag.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this.hashCode() == o.hashCode() && o instanceof FetcherByCache;
        }

    }

    /*
    本地拉取任务
     */
    private class FetcherByLocal implements Callable<String>{

        private OnImageLoad onImageLoad;
        private ImageView imageView;
        private String filePath;
        private String tag;
        private BitmapFactory.Options options;
        private Bitmap bitmap;

        public FetcherByLocal(ImageView imageView,OnImageLoad onImageLoad, String filePath,BitmapFactory.Options options) {
            this.onImageLoad = onImageLoad;
            this.imageView = imageView;
            this.filePath = filePath;
            this.options = options;
            this.tag = buildTag(filePath);
        }

        @Override
        public String call() throws Exception {

            File file = new File(filePath);
            if (options == null){
                bitmap = imageCacher.getByLruCache(tag);
            }else {
                bitmap = imageCacher.getByLruCache(tag+"withop");
            }
            if (bitmap == null && file.exists() && file.canRead()){
                if (options != null){
                    bitmap = BitmapFactory.decodeFile(filePath,options);
                    imageCacher.putInLruCaches(tag + "withop", bitmap);
                }
                else{
                    bitmap = BitmapFactory.decodeFile(filePath,options);
                    imageCacher.putInLruCaches(tag, bitmap);
                }
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadCompleted(bitmap, imageView, tag, onImageLoad);
                    }
                });
            }else if (bitmap != null){
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadCompleted(bitmap,imageView,tag,onImageLoad);
                    }
                });
            }else{
                if (options != null) {
                    fetherExecutor.removeTag(tag + "withop");
                }else {
                    fetherExecutor.removeTag(tag);
                }
                Log.e("OCImageLoader","Image file is not exist or is not readable");
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onError();
                    }
                });
            }
            return null;
        }

        public void onDownloadCompleted(Bitmap bitmap, ImageView imageView, String tag, OnImageLoad onImageLoadCompleted) {
            if (options != null) {
                fetherExecutor.removeTag(tag + "withop");
            }else {
                fetherExecutor.removeTag(tag);
            }
            if (bitmap == null){
                Log.e("On loading image", "Load image failed. Path: " + tag);
                return;
            }

            if (imageView != null && imageView.getTag().equals(this.tag)){
                if (durationMillis > 0 ){
                    Drawable prevDrawable = new ColorDrawable(TRANSPARENT);
                    Drawable nextDrawable = new BitmapDrawable(imageView.getResources(), bitmap);
                    TransitionDrawable transitionDrawable = new TransitionDrawable(
                            new Drawable[] { prevDrawable, nextDrawable });
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(durationMillis);
                }else {
                    imageView.setImageBitmap(bitmap);
                }
            }

            if (onImageLoadCompleted != null){
                onImageLoadCompleted.onLoadCompleted(bitmap,tag);
            }

        }

        public void onError() {
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
            if (onImageLoad != null){
                onImageLoad.onLoadFailed();
            }
        }

        @Override
        public int hashCode() {
            return tag.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this.hashCode() == o.hashCode() && o instanceof FetcherByLocal;
        }

    }

    /*
    图片处理任务
     */
    private class ReduceImageEffect implements Callable<String>{

        private OnHandleBitmap onHandleBitmap;
        private boolean cacheAsFile;
        private BitmapFactory.Options options;
        private Bitmap bitmap;
        private String path , url;
        private String cacheTAG;

        public ReduceImageEffect(OnHandleBitmap onHandleBitmap, Bitmap bitmap,BitmapFactory.Options options,boolean cacheAsFile,String path,String url,String id) {
            this.onHandleBitmap = onHandleBitmap;
            this.options = options;
            this.bitmap = bitmap;
            this.cacheAsFile = cacheAsFile;
            this.url = url;
            this.path = path;
            this.cacheTAG = id;
        }

        @Override
        public String call() throws Exception {


            if(bitmap == null || options != null){

                //先尝试从 LRU 缓存读取图片
                bitmap = imageCacher.getByLruCache(cacheTAG);

                if (bitmap == null){

                    //在尝试从本地缓存读取已经修改过的图片
                    Log.e("OCImageLoader","No reduced LRUcache.Trying to load reduced File cache...");
                    bitmap = imageCacher.getByFileCache(cacheTAG);
                    if (bitmap == null){

                        //在尝试从本地缓存读取原图片.由于可能是从网络拉取或本地拉取,所以要对 TAG 进行不同处理来读取本地缓存
                        Log.e("OCImageLoader","No reduced File cache.Trying to load original File cache...");

                        String cachePath = null;
                        if (url != null){

                            //如果是网络拉取
                            cachePath = imageCacher.getCacheFile(buildTag(url));
                        }else if (path != null){

                            //如果是本地拉取
                            cachePath = imageCacher.getCacheFile(buildTag(path));
                        }

                        if (cachePath == null && path != null && imageCacher.isCanCacheAsFile()){

                            //最后还是没有缓存文件,这尝试读取原图文件. (本地拉取情况)
                            Log.e("OCImageLoader","No original File cache.Trying to load original File by path...");
                            bitmap = BitmapFactory.decodeFile(path,options);
                        }else if (cachePath != null){

                            bitmap = BitmapFactory.decodeFile(cachePath,options);
                        }
                    }
                }else {
                    Log.d("OCImageLoader","LRUcache found.");
                }
            }else{
                Log.d("OCImageLoader","Option is NULL , using original cache");
            }

            if (bitmap != null && onHandleBitmap != null){
                bitmap = onHandleBitmap.onAsynHandleBitmap(path, bitmap);
                if (bitmap != null){
                    if (cacheAsFile){
                        Log.d("OCImageLoader","Tag:"+cacheTAG+" Cached as LRU & File ");
                        imageCacher.putCache(cacheTAG,bitmap);
                    }else {
                        Log.d("OCImageLoader","Tag:"+cacheTAG+" Cached as LRU ");
                        imageCacher.putInLruCaches(cacheTAG,bitmap);
                    }
                    cacheExecutor.removeTag(cacheTAG);
                    runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            onHandleBitmap.onReduceCompleted(bitmap);
                        }
                    });
                }else {
                    Log.e("OCImageLoader","Bitmap become NULL , after onHandleBitmap.");
                    onError();
                }
            }else {
                Log.e("OCImageLoader","Failed to load bitmap...");
                onError();
            }
            return null;
        }

        public void onError() {
            cacheExecutor.removeTag(cacheTAG);
        }

        @Override
        public int hashCode() {
            return cacheTAG.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this.hashCode() == o.hashCode() && o instanceof ReduceImageEffect;
        }

    }

    /*
    获取图片缩略图任务
     */
    private class GenerateImageThumbnail implements Callable<String>{

        private int scaleTimes;
        private Bitmap.Config loadConfig = Bitmap.Config.RGB_565;

        private String path;
        private String tag;
        private ImageView imageView;
        private Bitmap bitmap;
        private HandleOnLoaded handleOnLoaded;

        public GenerateImageThumbnail(String path, String tag, ImageView imageView, HandleOnLoaded handleOnLoaded) {
            this.path = path;
            this.tag = tag;
            this.imageView = imageView;
            this.handleOnLoaded = handleOnLoaded;
        }

        @Override
        public String call() throws Exception {

            //进行图片剪裁
            try {
                bitmap = cropBitmap();
            } catch (IllegalArgumentException e) {
                //如果裁剪出现了异常
                fetherExecutor.removeTag(tag);
                Log.e("OCImageLoader", "Exception on croping bitmap. "+e);
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onError();
                    }
                });
                return null;
            }

            if (handleOnLoaded != null){
                //如果有需要进行图片处理,则使用回调处理
                bitmap = handleOnLoaded.reduce(bitmap,tag);
            }

            if (bitmap != null){
                //图片进行缓存
                imageCacher.putCache(tag,bitmap);
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onCompleted(imageView , bitmap);
                    }
                });
            }
            fetherExecutor.removeTag(tag);

            return null;
        }

        private void onCompleted(ImageView imageView , Bitmap bitmap){

            if (bitmap != null && imageView != null && imageView.getTag().equals(tag)){
                if (durationMillis > 0 ){
                    Drawable prevDrawable = new ColorDrawable(TRANSPARENT);
                    Drawable nextDrawable = new BitmapDrawable(imageView.getResources(), bitmap);
                    TransitionDrawable transitionDrawable = new TransitionDrawable(
                            new Drawable[] { prevDrawable, nextDrawable });
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(durationMillis);
                }else {
                    imageView.setImageBitmap(bitmap);
                }
            }

        }

        private void onError(){
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
        }

        /**
         * 进行 Bitmap 的裁剪缩放操作
         * @return  处理后的Bitmap
         */
        private Bitmap cropBitmap() throws IllegalArgumentException{

            scaleTimes = 2;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path,options);

            int cropType;

            //原图尺寸
            int pictureWidth = options.outWidth;
            int pictureHeight = options.outHeight;

            //ImageView尺寸. 就要尽可能得到这个尺寸的图片
            int showWidth = imageView.getMeasuredWidth();
            int showHeight = imageView.getMeasuredHeight();

            if (showHeight * showWidth == 0){
                //如果有一项数据为 0 , 则赋予默认值.
                showHeight = 120;
                showWidth = 120;
            }

            if ( pictureHeight < showHeight && pictureWidth < showWidth ){
                //如果原图小于要显示的尺寸,则直接返回原图对象
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = loadConfig;
                return BitmapFactory.decodeFile(path);
            }else if ( pictureHeight < showHeight ){
                //如果原图的高度小于要显示的尺寸,就直接将  原图的长度  进行裁剪
                cropType = 1;
            }else if ( pictureWidth < showWidth ){
                //如果原图的宽度小于要显示的尺寸,就直接将  原图的高度  进行裁剪
                cropType = 2;
            }else {
                //普通状态
                cropType = 0;
            }

            switch (cropType){
                case 0:
                    //普通裁剪
                    bitmap = normalCropBitmap(pictureWidth,pictureHeight,showHeight,showWidth,options);
                    break;
                case 1:
                    //长度裁剪
                    bitmap = widthCropBitmap(pictureWidth,pictureHeight,showWidth,options);
                    break;
                case 2:
                    //高度裁剪
                    bitmap = heightCropBitmap(pictureWidth,pictureHeight,showHeight,options);
                    break;
                default:
                    bitmap = normalCropBitmap(pictureWidth,pictureHeight,showHeight,showWidth,options);
                    break;
            }

            Log.d("OCImageLoader", "Generating thumbnail of :"+path);
            Log.d("OCImageLoader", "Croped   size   Width: "+bitmap.getWidth()+"   Height: "+bitmap.getHeight());

            return bitmap;
        }

        /**
         * 普通裁剪
         * @return  裁剪后的Bitmap
         */
        private Bitmap normalCropBitmap(int pictureWidth , int pictureHeight , int showHeight , int showWidth , BitmapFactory.Options options)
        throws IllegalArgumentException{

            //临时存储计算得到的上一次结果,预设为图片原始尺寸.
            int reducedWidth = pictureWidth;
            int reducedHeight = pictureHeight;

            int lastWidth;
            int lastHeight;
            while (true){

                lastWidth = pictureWidth/scaleTimes;
                lastHeight = pictureHeight/scaleTimes;

                if ( lastHeight < showHeight || lastWidth < showWidth){
                    //如果计算得到的尺寸小于要得到的尺寸,则跳出
                    break;
                }else {
                    reducedWidth = lastWidth;
                    reducedHeight = lastHeight;
                    scaleTimes += 1;
                }

            }

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = loadConfig;
            options.inSampleSize = (pictureHeight / reducedHeight + pictureWidth / reducedWidth) /2;

            //先获取等待截取的Bitmap
            Bitmap pictureBitmap = BitmapFactory.decodeFile(path,options);
            Log.d("OCImageLoader", "Original size   Width: "+pictureBitmap.getWidth()+"   Height: "+pictureBitmap.getHeight());

            //计算裁剪的起点
            int cropX , cropY;
            cropX = (reducedWidth/2)-(showWidth/2);
            cropY = (reducedHeight/2)-(showHeight/2);

            pictureBitmap = Bitmap.createBitmap(pictureBitmap,cropX,cropY,showWidth,showHeight);

            return pictureBitmap;
        }

        /**
         * 高度裁剪
         * @return  裁剪后的Bitmap
         */
        private Bitmap heightCropBitmap(int pictureWidth , int pictureHeight , int showHeight , BitmapFactory.Options options)
        throws IllegalArgumentException{
            int cropY = ( pictureHeight / 2 ) - ( showHeight / 2 );
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = loadConfig;
            bitmap = BitmapFactory.decodeFile(path,options);
            Log.d("OCImageLoader", "Original size   Width: "+bitmap.getWidth()+"   Height: "+bitmap.getHeight());
            bitmap = Bitmap.createBitmap(bitmap,0,cropY,pictureWidth,showHeight);
            return bitmap;
        }

        /**
         * 长度裁剪
         * @return  裁剪后的Bitmap
         */
        private Bitmap widthCropBitmap(int pictureWidth , int pictureHeight , int showWidth , BitmapFactory.Options options)
        throws IllegalArgumentException{
            int cropX = ( pictureWidth / 2 ) - ( showWidth / 2 );
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = loadConfig;
            bitmap = BitmapFactory.decodeFile(path,options);
            Log.d("OCImageLoader", "Original size   Width: "+bitmap.getWidth()+"   Height: "+bitmap.getHeight());
            bitmap = Bitmap.createBitmap(bitmap,cropX,0,showWidth,pictureHeight);
            return bitmap;
        }

    }

    /*
    从资源文件获取图像
     */
    private class FetcherByResource implements Callable<String>{

        private String tag;
        private Context context;
        private int resourceID;
        private ImageView imageView;
        private Drawable drawable;

        public FetcherByResource(String tag, int resourceID, Context context, ImageView imageView) {
            this.tag = tag;
            this.resourceID = resourceID;
            this.context = context;
            this.imageView = imageView;
        }

        @Override
        public String call() throws Exception {

            try {
                drawable = getDrawableByResource(resourceID);
            } catch (Exception e) {
                Log.e("OCImageLoader", "Cannot load from this resource ID" );
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onFailed();
                    }
                });
            }

            if (drawable != null){
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onCompleted(drawable);
                    }
                });
            }else {
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onFailed();
                    }
                });
            }

            return null;
        }

        /**
         * 从资源ID获取Drawable文件
         * @param resourceID 资源ID
         * @return  Drawable文件
         * @throws Exception    抛出的异常 (资源不存在)
         */
        private Drawable getDrawableByResource(int resourceID) throws Exception{
            if (Build.VERSION.SDK_INT >= 22){
                return context.getDrawable(resourceID);
            }else {
                return context.getResources().getDrawable(resourceID);
            }
        }

        /*
        成功时的回调
         */
        private void onCompleted(Drawable drawable){
            fetherExecutor.removeTag(tag);

            if (imageView != null && imageView.getTag().equals(tag)){
                if (durationMillis > 0 ){
                    Drawable prevDrawable = new ColorDrawable(TRANSPARENT);
                    Drawable nextDrawable = drawable;
                    TransitionDrawable transitionDrawable = new TransitionDrawable(
                            new Drawable[] { prevDrawable, nextDrawable });
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(durationMillis);
                }else {
                    imageView.setImageDrawable(drawable);
                }
            }
        }

        /*
         * 失败时的回调
         */
        private void onFailed(){
            fetherExecutor.removeTag(tag);
            if (failedRes != null ){
                imageView.setImageDrawable(failedRes);
            }else {
                imageView.setImageDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
            }
        }

    }

}
