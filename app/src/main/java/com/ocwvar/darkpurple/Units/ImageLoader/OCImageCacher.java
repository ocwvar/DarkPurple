package com.ocwvar.darkpurple.Units.ImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 区成伟
 * Date: 2016/2/6  13:25
 * Version 4 16.4.12
 * Project: 图片缓存帮助类
 * 带TAG的LRUCache、本地缓存
 */

public class OCImageCacher {

    //图片本地缓存文件夹
    private String fileCachesPath = Environment.getExternalStorageDirectory().getPath()+"/ocCaches/";

    private CustomLRUCache lruCache;
    private boolean canCacheAsFile = false;

    protected OCImageCacher() {
        lruCache = new CustomLRUCache((int)Runtime.getRuntime().totalMemory()/8);
        File testFile = new File(fileCachesPath);
        if (!testFile.exists()){
            canCacheAsFile = testFile.mkdirs();
        }else {
            canCacheAsFile = testFile.canRead() && testFile.canWrite();
        }
        Log.d("OCCacher STATUS","Can cache as file?  "+canCacheAsFile);
    }

    /**
     * 本地缓存状态
     * @return  是否可以进行本地缓存
     */
    protected boolean isCanCacheAsFile(){
        return canCacheAsFile;
    }

    /**
     * 获取缓存 包括LRU缓存和本地缓存
     * @param tag   图片唯一TAG
     * @return  缓存的图片对象. 如果没有缓存则返回 NULL
     */
    protected Bitmap getCache(String tag){
        Bitmap cacheImage = getByLruCache(tag);
        if (cacheImage != null){
            Log.d("OCCacher",tag+"  Got by    LRUCache");
            return cacheImage;
        } else if (canCacheAsFile){
            cacheImage =  getByFileCache(tag);
            if (cacheImage != null) putInLruCaches(tag,cacheImage);
            else {
                Log.d("OCCacher",tag+"  No cache here [cache file has been deleted]");
                return null;
            }
            return cacheImage;
        }else{
            Log.d("OCCacher",tag+"  No cache here");
            return null;
        }
    }

    /**
     * 检查是否存在LRU缓存和本地缓存
     * @param tag   图片唯一TAG
     * @return  是否存在缓存
     */
    protected boolean isCacheExist(String tag){
        return lruCache.isExist(tag) || new File(fileCachesPath+tag+".tmp").exists();
    }

    /**
     * 释放LRU缓存
     */
    protected void releaseCaches(){
        lruCache.releaseCaches();
    }

    /**
     * 进行图片的缓存 包括LRU缓存和本地缓存
     * @param tag   图片唯一TAG
     * @param image 要缓存的图片对象
     */
    protected void putCache(String tag,Bitmap image){
        if (canCacheAsFile){
            File fileCache = new File(fileCachesPath+tag+".tmp");
            if (fileCache.length() > 0) putInLruCaches(tag, image);
            else{
                try {
                    putInFileCaches(fileCache,image);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }else {
            putInLruCaches(tag, image);
        }
    }

    /**
     * 进行图片本地缓存
     * @param file  图片缓存的位置 File 对象
     * @param image 缓存的图片对象
     * @throws FileNotFoundException    文件不存在报错
     */
    protected void putInFileCaches(File file,Bitmap image) throws FileNotFoundException {
        Log.d("OCCacher",file.getName()+" File Cache Created");
        FileOutputStream outputStream = new FileOutputStream(file);
        image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
    }

    /**
     * 进行图片LRU缓存
     * @param tag   图片唯一TAG
     * @param image 缓存的图片对象
     */
    protected void putInLruCaches(String tag,Bitmap image){
        if (lruCache.get(tag) == null){
            Log.d("OCCacher",tag+"LRU Cache Created");
            lruCache.putANDcount(tag, image);
        }
    }

    /**
     * 获取图片的本地缓存
     * @param tag   图片唯一TAG
     * @return  缓存的图片对象.若 不存在缓存 或 本地缓存功能无效 则返回 NULL
     */
    protected Bitmap getByFileCache(String tag){
        if (canCacheAsFile){
            File fileCache = new File(fileCachesPath+tag+".tmp");
            if (fileCache.length() > 0){
                Log.d("OCCacher",tag+"  Got by    FILECache");
                return BitmapFactory.decodeFile(fileCache.getPath());
            }else {
                Log.d("OCCacher", tag + "  No cache here");
                return null;
            }
        }else {
            return null;
        }
    }

    /**
     * 获取图片的LRU缓存
     * @param tag   图片唯一TAG
     * @return  缓存的图片对象.若不存在缓存则返回 NULL
     */
    protected Bitmap getByLruCache(String tag){
        return lruCache.getCache(tag);
    }

    /**
     * 获取本地缓存的路径
     * @param tag   图片唯一TAG
     * @return  本地缓存的图片对象路径.若 不存在缓存 或 本地缓存功能无效 则返回 NULL
     */
    protected String getCacheFile(String tag){
        if (canCacheAsFile){
            File fileCache = new File(fileCachesPath+tag+".tmp");
            if (fileCache.length() > 0){
                Log.d("OCCacher",tag+"  Got cache file of "+tag);
                return fileCache.getPath();
            }else {
                Log.d("OCCacher",tag+"  No cache file of "+tag);
                return null;
            }
        }else {
            return null;
        }
    }

    /**
     * 获取本地缓存文件夹当前大小
     * @return  本地缓存文件夹大小长度. 当返回 -1 时,表示没有权限或缓存文件夹不存在.
     */
    protected long getCacheDirectorySize(){
        if (canCacheAsFile){
            long size = 0L;
            File[] files = new File(fileCachesPath).listFiles();
            if(files != null){
                for(File file : files){
                    if (file.isFile()){
                        size += file.length();
                    }
                }
            }else {
                return -1;
            }
            return size;
        }else {
            return -1;
        }
    }

    /**
     * 清空本地缓存文件夹
     */
    protected void clearCacheDirectory() {
        boolean result;
        File[] files = new File(fileCachesPath).listFiles();
        for(File file : files){
            if (file.isFile()){
                result = file.delete();
                Log.d("OCCacher","Flie Cache:"+file.getName()+" Delete status:"+result);
            }
        }
    }

    /**
     * 自定义LRU类 实现缓存统计
     */
    private class CustomLRUCache extends LruCache<String,Bitmap> {

        public CustomLRUCache(int maxSize) {
            super(maxSize);
        }

        List<String> keys = new ArrayList<>();

        @Override
        protected int sizeOf(String  key, Bitmap value) {
            return value.getHeight()*value.getRowBytes();
        }

        public void putANDcount(String key, Bitmap value){
            put(key, value);
            keys.add(key);
        }

        public boolean isExist(String key){
            return keys.contains(key) ;
        }

        public Bitmap getCache(String key){
            Bitmap cache = get(key);
            if (cache == null){
                this.keys.remove(key);
                return null;
            }else {
                return cache;
            }
        }

        public void releaseCaches(){
            evictAll();
            this.keys.clear();
        }

    }

}
