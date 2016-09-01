package com.ocwvar.darkpurple.Units;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Callbacks.MediaScannerCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units.ImageLoader
 * Data: 2016/7/9 10:48
 * Project: DarkPurple
 * 音频文件扫描器
 */
public class MediaScanner {

    private static MediaScanner mediaScanner;

    //用于执行线程的线程池
    private OCThreadExecutor threadExecutor;
    //扫描器的回调接口
    private MediaScannerCallback callback;
    //用于临时存放扫描结果的数据列表
    private ArrayList<SongItem> cachedList;
    //用于处理UI界面的数据
    private Handler handler;
    //数据是否更新标识
    private boolean isUpdated = false;

    public MediaScanner() {
        threadExecutor = new OCThreadExecutor(1,"Scanner");
        handler = new Handler(Looper.getMainLooper());
        cachedList = new ArrayList<>();
    }

    public static synchronized MediaScanner getInstance() {
        if (mediaScanner == null){
            mediaScanner = new MediaScanner();
        }
        return mediaScanner;
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
     * 设置扫描器回调接口
     * @param callback  回调接口
     */
    public void setCallback(@Nullable MediaScannerCallback callback) {
        this.callback = callback;
    }

    /**
     * 缓存数据结果
     * @param datas 要缓存的数据
     */
    private void cacheDatas(ArrayList<SongItem> datas){
        isUpdated = true;
        cachedList.clear();
        if (datas != null){
            cachedList.addAll(datas);
        } //如果就算没扫描到结果 , 也当作是扫描结果
    }

    /**
     * 得到缓存之后的数据
     * @return  缓存数据
     */
    public ArrayList<SongItem> getCachedDatas(){
        isUpdated = false;
        return cachedList;
    }

    /**
     * 列表排序类型枚举
     */
    public enum SortType{       ByDate , ByName     }

    /**
     * 数据是否有更新
     */
    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * 开始扫描 , 如果有已缓存的数据 则直接返回数据
     */
    public void start(){
        if (isUpdated && callback != null){
            callback.onScanCompleted(cachedList);
        }else {
            if (AppConfigs.MusicFolders == null){
                //如果没有设置音乐文件夹 , 则从媒体数据库中获取
                threadExecutor.submit(new FutureTask<>( new ScanByMediaStore() ) , ScanByMediaStore.TAG);
            }else {
                //如果有设置音乐文件夹 , 则扫描每个目录
                threadExecutor.submit(new FutureTask<>( new ScanByFolder(AppConfigs.MusicFolders) ) , ScanByFolder.TAG);
            }
        }
    }

    /**
     * 检查是否有上一次储存的数据
     * @return  数据是否可用
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean isHasCachedData(){
        File cachedFile = new File(JSONHandler.folderPath+AppConfigs.CACHE_NAME+".pl");
        if ( cachedFile.exists() && cachedFile.length() > 0){
            cachedFile = null;
            return true;
        }else {
            cachedFile.delete();
            cachedFile = null;
            return false;
        }
    }

    /**
     * 读取上次缓存的数据
     */
    public void getLastTimeCachedData(){
        threadExecutor.submit(new FutureTask<>(new LoadCachedData()),LoadCachedData.TAG);
    }

    /**
     * 扫描器 (扫描本地媒体数据库)
     */
    final class ScanByMediaStore implements Callable<String>{

        final public static String TAG = "ScanByMediaStore";
        final private Context context = AppConfigs.ApplicationContext;

        @Override
        public String call() throws Exception {

            ArrayList<SongItem> arrayList = null;

            try {
                arrayList  = core();
            } catch (Exception ignored) {
            }

            if (callback == null){
                //如果没有设置回调接口 , 则吧扫描到的数据放入临时列表中
                cacheDatas(arrayList);
            }else {
                JSONHandler.cacheSearchResult(arrayList);
                onCompleted(arrayList);
            }

            threadExecutor.removeTag(TAG);
            return null;
        }

        private void onCompleted(@Nullable final ArrayList<SongItem> arrayList){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    callback.onScanCompleted(arrayList);
                }
            });
        }

        /**
         * 扫描媒体库
         * @return  扫描结果,如果没有数据或失败则返回Null
         * @throws Exception    执行过程中产生的异常
         */
        private @Nullable ArrayList<SongItem> core() throws Exception{
            //从系统媒体数据库扫描
            final Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

            if (cursor != null && cursor.getCount() > 0){
                ArrayList<SongItem> songList = new ArrayList<>();

                while (cursor.moveToNext()){

                    String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));

                    if (isMusicFile(fileName)){
                        //如果当前的是支持的歌曲文件格式 , 则开始解析
                        SongItem songItem = new SongItem();

                        //文件名
                        songItem.setFileName(fileName);
                        //文件尺寸
                        songItem.setFileSize(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)));
                        //文件路径
                        songItem.setPath(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                        //歌曲长度
                        final long length = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        if (length < AppConfigs.LengthLimited) {
                            //如果歌曲长度不符合最低要求 , 则不继续解析
                            songItem = null;
                            continue;
                        }
                        else {
                            songItem.setLength(length);
                        }

                        //标题
                        songItem.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                        if (TextUtils.isEmpty(songItem.getTitle())){
                            //如果无法获取到歌曲名 , 则使用文件名代替
                            songItem.setTitle(songItem.getFileName());
                        }
                        //专辑名
                        songItem.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                        if (TextUtils.isEmpty(songItem.getAlbum())){
                            //如果无法获取到专辑名 , 则使用未知代替
                            songItem.setAlbum(AppConfigs.UNKNOWN);
                        }
                        //作者
                        songItem.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                        if (TextUtils.isEmpty(songItem.getArtist())){
                            //如果无法获取到歌手名 , 则使用未知代替
                            songItem.setArtist(AppConfigs.UNKNOWN);
                        }


                        //专辑ID  主要用于读取封面图像
                        final long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                        if (albumID > 0){
                            songItem.setAlbumID(albumID);
                            songItem.setAlbumCoverUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumID));
                        }

                        Logger.normal("媒体库扫描器",songItem.getTitle());
                        cacheAlbumCover(songItem);

                        songList.add(songItem);
                    }else {
                        fileName = null;
                        continue;
                    }
                }

                cursor.close();

                //进行歌曲文件的排序
                switch (AppConfigs.SortType){
                    case ByDate:
                        Collections.sort(songList,new ComparatorByData());
                        break;
                    case ByName:
                        Collections.sort(songList,new ComparatorByName());
                        break;
                }

                return songList;

            }

            return null;

        }

        /**
         * 判断文件是否为音乐文件
         * @param fileName  要检查的文件名
         * @return  有效性
         */
        private boolean isMusicFile(String fileName){
            if (TextUtils.isEmpty(fileName)){
                return false;
            }

            String[] temp;
            try {
                temp = fileName.split("\\.");
            } catch (Exception e) {
                //如果解析文字失败 , 则表示文件没有后缀名 , 则不予以解析 , 当作文件非法
                return false;
            }
            if (temp.length >= 1){
                //开始从后缀名判断是否为音频文件
                switch (temp[temp.length-1]){
                    case "mp3":
                    case "wav":
                        return true;
                    default:
                        return false;
                }
            }else {
                return false;
            }
        }

        /**
         * 预先缓存专辑图像
         * @param songItem  要处理的歌曲信息
         */
        private void cacheAlbumCover(SongItem songItem){
            Bitmap coverImage = null;
            try {
                coverImage = Picasso.with(AppConfigs.ApplicationContext).load(CoverImage2File.getInstance().getCacheFile(songItem.getPath())).get();
            } catch (IOException ignored) {}

            if (coverImage == null){
                try {
                    coverImage = MediaStore.Images.Media.getBitmap(AppConfigs.ApplicationContext.getContentResolver(),songItem.getAlbumCoverUri());
                    CoverImage2File.getInstance().makeImage2File(coverImage,songItem.getPath());
                } catch (Exception e) {
                    Logger.error("媒体库扫描器","缓存封面图像失败 "+songItem.getTitle());
                    return;
                }
                songItem.setHaveCover(true);
                songItem.setPaletteColor(getAlbumCoverColor(coverImage));
            }else {
                songItem.setHaveCover(true);
                songItem.setPaletteColor(getAlbumCoverColor(coverImage));
            }
        }

        /**
         * 获取封面混合颜色  以暗色调优先 亮色调为次  如果都没有则使用默认颜色
         * @param coverImage    封面图像
         * @return  混合颜色
         */
        private int getAlbumCoverColor(Bitmap coverImage){
            Palette palette;

            try {
                palette = new Palette.Builder(coverImage).generate();
            } catch (Exception e) {
                //如果图像解析失败 或 图像为Null 则使用默认颜色
                return AppConfigs.DefaultPaletteColor;
            }

            int color = AppConfigs.DefaultPaletteColor , item = 0;
            //获取封面混合颜色  以暗色调优先 亮色调为次  如果都没有则使用默认颜色
            while (color == AppConfigs.DefaultPaletteColor && item < 7){
                switch (item){
                    case 0:
                        color = palette.getDarkMutedColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 1:
                        color = palette.getDarkVibrantColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 3:
                        color = palette.getMutedColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 4:
                        color = palette.getLightMutedColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 5:
                        color = palette.getLightVibrantColor(AppConfigs.DefaultPaletteColor);
                        break;
                    default:
                        color = AppConfigs.DefaultPaletteColor;
                        break;
                }
                item += 1;
            }
            return color;
        }

    }

    /**
     * 文件夹音频扫描器
     */
    final class ScanByFolder implements Callable<String>{
        final public static String TAG = "ScanByFolder";

        //文件夹路径集合
        private String[] paths;
        private final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return isMusicFile(file);
            }
        };

        public ScanByFolder(String[] paths) {
            this.paths = paths;
        }

        @Override
        public String call() throws Exception {

            if (chackFolders(paths)){
                //如果设置的目录都合法的话才进行扫描

                ArrayList<SongItem> arrayList = null;

                try {
                    arrayList  = core();
                } catch (Exception ignored) {
                }

                if (callback == null){
                    //如果没有设置回调接口 , 则吧扫描到的数据放入临时列表中
                    cacheDatas(arrayList);
                }else {
                    JSONHandler.cacheSearchResult(arrayList);
                    onCompleted(arrayList);
                }
            }

            threadExecutor.removeTag(TAG);
            return null;
        }

        private void onCompleted(@Nullable final ArrayList<SongItem> arrayList){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    callback.onScanCompleted(arrayList);
                }
            });
        }

        /**
         * 检查目录是否有效
         * @param paths  目录地址
         * @return  目录的有效性
         */
        private boolean chackFolders(String[] paths){
            if (paths == null || paths.length <= 0){
                return false;
            }else {
                for (String path: paths) {
                    if (!TextUtils.isEmpty(path)){
                        File folder = new File(path);
                        if (!folder.isDirectory() || !folder.canWrite()) {
                            folder = null;
                            return false;
                        }
                        folder = null;
                    }else {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * 判断文件是否为音乐文件
         * @param file  要检查的文件
         * @return  有效性
         */
        private boolean isMusicFile(File file){
            if (file.length() <= 0){
                //如果文件大小为 0 则肯定不合法
                return false;
            }else {
                String[] temp;
                try {
                    temp = file.getName().split("\\.");
                } catch (Exception e) {
                    //如果解析文字失败 , 则表示文件没有后缀名 , 则不予以解析 , 当作文件非法
                    return false;
                }
                if (temp.length >= 1){
                    //开始从后缀名判断是否为音频文件
                    switch (temp[temp.length-1]){
                        case "mp3":
                        case "wav":
                            return true;
                        default:
                            return false;
                    }
                }else {
                    return false;
                }
            }
        }

        /**
         * 扫描获取设置的目录下的音频文件
         * @return  音频文件列表
         * @throws Exception
         */
        private @Nullable ArrayList<SongItem> core() throws Exception{

            ArrayList<SongItem> songList = new ArrayList<>();

            //循环遍历每一个音频文件夹
            for (String path : paths) {
                File[] files = new File(path).listFiles(filter);
                if (files.length <= 0) {
                    //如果当前文件夹下没有任何合法的音频文件 , 则跳到下一个文件夹路径
                    continue;
                }

                //循环遍历每一个音频文件
                for (File musicFile : files) {
                    long musicLength;

                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(musicFile.getPath());

                    try {
                        musicLength = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        if (musicLength < AppConfigs.LengthLimited){
                            //如果歌曲长度小于限制 , 则不继续解析这个文件
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        //如果无法获取到歌曲长度 , 则不解析这个文件
                        continue;
                    }

                    SongItem songItem = new SongItem();


                    songItem.setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                    if (TextUtils.isEmpty(songItem.getTitle())){
                        //如果无法获取到歌曲名 , 则使用文件名代替
                        songItem.setTitle(musicFile.getName());
                    }
                    songItem.setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                    if (TextUtils.isEmpty(songItem.getArtist())){
                        //如果无法获取到歌手名 , 则使用未知代替
                        songItem.setArtist(AppConfigs.UNKNOWN);
                    }
                    songItem.setAlbum(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
                    if (TextUtils.isEmpty(songItem.getAlbum())){
                        //如果无法获取到专辑名 , 则使用未知代替
                        songItem.setAlbum(AppConfigs.UNKNOWN);
                    }
                    songItem.setLength(musicLength);
                    songItem.setFileName(musicFile.getName());
                    songItem.setPath(musicFile.getPath());
                    songItem.setFileSize(Long.toString(musicFile.length()));



                    cacheAlbumCover(retriever,songItem);

                    songList.add(songItem);
                    Logger.normal("歌曲文件扫描器",songItem.getTitle());
                }

            }

            if (songList.size() == 0 ){
                songList = null;
                return null;
            }else {
                //进行歌曲文件的排序
                switch (AppConfigs.SortType){
                    case ByDate:
                        Collections.sort(songList,new ComparatorByData());
                        break;
                    case ByName:
                        Collections.sort(songList,new ComparatorByName());
                        break;
                }
                return songList;
            }

        }

        /**
         * 缓存歌曲封面图像
         * @param retriever  MediaMetadataRetriever
         * @param songItem  操作的歌曲数据
         */
        private void cacheAlbumCover(MediaMetadataRetriever retriever, SongItem songItem){
            Bitmap coverImage = null;
            try {
                coverImage = Picasso.with(AppConfigs.ApplicationContext).load(CoverImage2File.getInstance().getCacheFile(songItem.getPath())).get();
            } catch (IOException ignored) {}

            if (retriever != null && coverImage == null){
                //如果没有缓存过图像 , 则获取图像资源并进行缓存和提取图像颜色资源
                byte[] bytes = retriever.getEmbeddedPicture();
                if (bytes != null){
                    coverImage = BitmapFactory.decodeByteArray(bytes , 0 , bytes.length);
                    bytes = null;
                    if (coverImage != null){
                        CoverImage2File.getInstance().makeImage2File(coverImage,songItem.getPath());
                        songItem.setPaletteColor(getAlbumCoverColor(coverImage));
                        songItem.setHaveCover(true);
                    }else {
                        Logger.error("媒体库扫描器","缓存封面图像失败 "+songItem.getTitle());
                    }
                }else {
                    Logger.error("媒体库扫描器","该音频没有图像文件");
                }
            }else {
                songItem.setPaletteColor(getAlbumCoverColor(coverImage));
                songItem.setHaveCover(true);
            }
        }

        /**
         * 获取封面混合颜色  以暗色调优先 亮色调为次  如果都没有则使用默认颜色
         * @param coverImage    封面图像
         * @return  混合颜色
         */
        private int getAlbumCoverColor(Bitmap coverImage){
            Palette palette;

            try {
                palette = new Palette.Builder(coverImage).generate();
            } catch (Exception e) {
                //如果图像解析失败 或 图像为Null 则使用默认颜色
                return AppConfigs.DefaultPaletteColor;
            }

            int color = AppConfigs.DefaultPaletteColor , item = 0;
            //获取封面混合颜色  以暗色调优先 亮色调为次  如果都没有则使用默认颜色
            while (color == AppConfigs.DefaultPaletteColor && item < 7){
                switch (item){
                    case 0:
                        color = palette.getDarkMutedColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 1:
                        color = palette.getDarkVibrantColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 3:
                        color = palette.getMutedColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 4:
                        color = palette.getLightMutedColor(AppConfigs.DefaultPaletteColor);
                        break;
                    case 5:
                        color = palette.getLightVibrantColor(AppConfigs.DefaultPaletteColor);
                        break;
                    default:
                        color = AppConfigs.DefaultPaletteColor;
                        break;
                }
                item += 1;
            }
            return color;
        }

    }

    /**
     * 加载上一次搜索记录线程
     */
    final class LoadCachedData implements Callable<String>{
        final public static String TAG = "LoadCachedData";

        @Override
        public String call() throws Exception {

            ArrayList<SongItem> cachedList = JSONHandler.loadPlaylist(AppConfigs.CACHE_NAME);

            if (callback == null){
                cacheDatas(cachedList);
            }else {
                onCompleted(cachedList);
            }

            threadExecutor.removeTag(TAG);
            return null;
        }

        private void onCompleted(@Nullable final ArrayList<SongItem> arrayList){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    callback.onScanCompleted(arrayList);
                }
            });
        }

    }

    /**
     * 按文件创建日期排序器
     */
    final class ComparatorByData implements Comparator<SongItem>{

        public ComparatorByData() {
            Logger.warnning("ComparatorByData","正在按照日期排序");
        }

        @Override
        public int compare(SongItem songItem, SongItem t1) {
            File file1 = new File(songItem.getPath());
            File file2 = new File(t1.getPath());
            final long file1Time = file1.lastModified();
            final long file2Time = file2.lastModified();
            file1 = null;
            file2 = null;
            return (int) (file2Time - file1Time);
        }

    }

    /**
     * 按名字排序器
     */
    final class ComparatorByName implements Comparator<SongItem>{

        public ComparatorByName() {
            Logger.warnning("ComparatorByName","正在按照名称排序");
        }

        @Override
        public int compare(SongItem songItem, SongItem t1) {
            return songItem.getTitle().compareTo(t1.getTitle());
        }

    }

}
