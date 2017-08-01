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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Callbacks.MediaScannerCallback;
import com.ocwvar.darkpurple.Units.Cover.ColorType;
import com.ocwvar.darkpurple.Units.Cover.CoverImage2File;
import com.ocwvar.darkpurple.Units.Cover.CoverManager;
import com.ocwvar.darkpurple.Units.Cover.CoverType;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
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

    private MediaScanner() {
        threadExecutor = new OCThreadExecutor(1, "Scanner");
        handler = new Handler(Looper.getMainLooper());
        cachedList = new ArrayList<>();
    }

    public static synchronized MediaScanner getInstance() {
        if (mediaScanner == null) {
            mediaScanner = new MediaScanner();
        }
        return mediaScanner;
    }

    /**
     * UI 线程
     *
     * @param runnable 在UI线程运行的任务
     */
    private void runOnUIThread(Runnable runnable) {
        boolean done = handler.post(runnable);
        while (!done) {
            handler = new Handler(Looper.getMainLooper());
            runOnUIThread(runnable);
        }
    }

    /**
     * 设置扫描器回调接口
     *
     * @param callback 回调接口
     */
    public void setCallback(@Nullable MediaScannerCallback callback) {
        this.callback = callback;
    }

    /**
     * 缓存数据结果
     *
     * @param datas 要缓存的数据
     */
    private void cacheDatas(ArrayList<SongItem> datas) {
        isUpdated = true;
        cachedList.clear();
        if (datas != null) {
            cachedList.addAll(datas);
        } //如果就算没扫描到结果 , 也当作是扫描结果
    }

    /**
     * 得到缓存之后的数据
     *
     * @return 缓存数据
     */
    public ArrayList<SongItem> getCachedDatas() {
        isUpdated = false;
        return cachedList;
    }

    /**
     * 数据是否有更新
     */
    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * 开始扫描 , 如果有已缓存的数据 则直接返回数据
     */
    public void start() {
        if (isUpdated && callback != null) {
            callback.onScanCompleted(cachedList, false);
        } else {
            if (AppConfigs.MusicFolders == null) {
                //如果没有设置音乐文件夹 , 则从媒体数据库中获取
                threadExecutor.submit(new FutureTask<>(new ScanByMediaStore()), ScanByMediaStore.TAG);
            } else {
                //如果有设置音乐文件夹 , 则扫描每个目录
                threadExecutor.submit(new FutureTask<>(new ScanByFolder(AppConfigs.MusicFolders)), ScanByFolder.TAG);
            }
        }
    }

    /**
     * 检查是否有上一次储存的数据
     *
     * @return 数据是否可用
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean isHasCachedData() {
        File cachedFile = new File(AppConfigs.PlaylistFolder + AppConfigs.CACHE_NAME + ".pl");
        if (cachedFile.exists() && cachedFile.length() > 0) {
            cachedFile = null;
            return true;
        } else {
            cachedFile.delete();
            cachedFile = null;
            return false;
        }
    }

    /**
     * 读取上次缓存的数据
     */
    public void getLastTimeCachedData() {
        threadExecutor.submit(new FutureTask<>(new LoadCachedData()), LoadCachedData.TAG);
    }

    /**
     * 进行封面数据验证以及提取缓存，并在数据有效的时候进行颜色计算
     *
     * @param coverType  封面类型
     * @param coverID    封面ID
     * @param coverBytes 封面字节数据，当从文件获取封面时需要使用到，如果封面是存在于媒体库中则不需要传入
     */
    private void albumCoverHandler(final @NonNull CoverType coverType, final @NonNull String coverID, @Nullable final byte[] coverBytes) throws Exception {
        if (TextUtils.isEmpty(coverID)) {
            return;
        }

        //获取封面数据源
        final String source = CoverManager.INSTANCE.getSource(coverType, coverID);

        //封面位图对象声明
        Bitmap coverBitmap = null;

        //第一步：
        //通过使用 Picasso来加载数据源 检查已有的缓存，如果是从文件解析(coverBytes != null)，则此时 source(缓存位置) 是空的，直接跳到第二步
        if (TextUtils.isEmpty(source) && coverBytes == null) {
            //当前没有数据源 并且 没有封面数据
            return;
        } else if (!TextUtils.isEmpty(source) && source.startsWith("content:")) {
            //数据源为Uri
            try {
                coverBitmap = Picasso.with(AppConfigs.ApplicationContext).load(Uri.parse(source)).get();
            } catch (Exception ignore) {
                //Uri获取对应文件失败
                coverBitmap = null;
            }
        } else if (!TextUtils.isEmpty(source) && source.startsWith("/")) {
            //数据源为String路径，需要使用绝对路径
            coverBitmap = Picasso.with(AppConfigs.ApplicationContext).load(CoverManager.INSTANCE.getAbsoluteSource(source)).get();
        } else if (coverBytes == null) {
            //不支持的数据源类型
            CoverManager.INSTANCE.removeSource(coverType, coverID);
            return;
        }

        //第二步
        //数据源有效，但并未缓存数据，尝试从媒体库或字节数组中加载封面
        if (coverBitmap == null) {
            //先清除原始路径，准备添加缓存路径
            CoverManager.INSTANCE.removeSource(coverType, coverID);

            if (!TextUtils.isEmpty(source) && source.startsWith("content:")) {
                //封面存在于媒体库中
                try {
                    coverBitmap = MediaStore.Images.Media.getBitmap(AppConfigs.ApplicationContext.getContentResolver(), Uri.parse(source));
                } catch (Exception ignore) {
                    //Uri获取对应文件失败
                    coverBitmap = null;
                }
            } else if (coverBytes != null) {
                //从字节数组中加载
                coverBitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.length);
            } else {
                //不支持的数据源类型
                return;
            }
        }

        //第三步
        //封面缓存，内部已判断位图是否有效
        final File cachedFile = CoverImage2File.getInstance().makeImage2File(coverType, coverBitmap, coverID);
        if (cachedFile != null) {
            //缓存成功，重新设置ID数据
            CoverManager.INSTANCE.setSource(coverType, coverID, cachedFile.getPath(), true);
        }

        //第四步
        //如果封面位图成功获取，则进行封面颜色计算
        if (coverBitmap != null) {
            //先根据当前封面操作类型来确定操作的颜色类型
            final ColorType colorType;
            switch (coverType) {
                case NORMAL:
                    colorType = ColorType.NORMAL;
                    break;
                case CUSTOM:
                    colorType = ColorType.CUSTOM;
                    break;
                default:
                    //其他类型封面不进行计算
                    coverBitmap.recycle();
                    return;
            }
            //获取颜色
            int coverMixColor = CoverManager.INSTANCE.getColor(colorType, coverID);

            //如果颜色为默认颜色，则说明没有颜色缓存，需要进行计算
            if (coverMixColor == AppConfigs.Color.DefaultCoverColor) {
                final Palette palette = new Palette.Builder(coverBitmap).generate();
                coverMixColor = palette.getMutedColor(AppConfigs.Color.DefaultCoverColor);
            }

            //设置颜色
            CoverManager.INSTANCE.setColorSource(colorType, coverID, coverMixColor, true);
            //所有操作完成，回收位图资源
            coverBitmap.recycle();
        }
    }

    /**
     * 列表排序类型枚举
     */
    public enum SortType {
        ByDate, ByName
    }

    /**
     * 扫描器 (扫描本地媒体数据库)
     */
    final private class ScanByMediaStore implements Callable<String> {

        final public static String TAG = "ScanByMediaStore";
        final private Context context = AppConfigs.ApplicationContext;

        @Override
        public String call() throws Exception {

            ArrayList<SongItem> arrayList = null;

            try {
                //读取所有歌曲数据
                arrayList = core();

                //保存所有封面数据
                CoverManager.INSTANCE.asyncUpdateFileCache();

            } catch (Exception e) {
                Logger.error(TAG, "在从媒体库中获取数据时发生异常：\n" + e);
            }

            if (callback == null) {
                //如果没有设置回调接口 , 则吧扫描到的数据放入临时列表中
                cacheDatas(arrayList);
            } else {
                JSONHandler.cacheSearchResult(arrayList);
                onCompleted(arrayList);
            }

            threadExecutor.removeTag(TAG);
            return null;
        }

        private void onCompleted(@Nullable final ArrayList<SongItem> arrayList) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    callback.onScanCompleted(arrayList, false);
                }
            });
        }

        /**
         * 扫描媒体库
         *
         * @return 扫描结果, 如果没有数据或失败则返回Null
         * @throws Exception 执行过程中产生的异常
         */
        private
        @Nullable
        ArrayList<SongItem> core() throws Exception {
            //从系统媒体数据库扫描
            final Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

            if (cursor != null && cursor.getCount() > 0) {
                ArrayList<SongItem> songList = new ArrayList<>();

                while (cursor.moveToNext()) {

                    //提取文件名
                    final String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));

                    //提取文件路径(同时作为媒体数据的 UID、封面ID、封面颜色ID)
                    final String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    //判断文件是否有效 同时 是否属于可以播放的音频文件类型，如果都可以则开始解析数据
                    if (isFileValid(filePath) && isMusicFile(fileName)) {

                        //检查歌曲长度
                        final long duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        if (duration < AppConfigs.LengthLimited) {
                            //如果歌曲长度不符合最低要求 , 则不继续解析
                            continue;
                        }

                        //创建媒体数据构造器
                        final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

                        //音频长度
                        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

                        //文件名
                        metadataBuilder.putString(SongItem.SONGITEM_KEY_FILE_NAME, fileName);

                        //文件路径
                        metadataBuilder.putString(SongItem.SONGITEM_KEY_FILE_PATH, filePath);

                        //封面ID
                        metadataBuilder.putString(SongItem.SONGITEM_KEY_COVER_ID, filePath);

                        //媒体ID
                        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, filePath);

                        //媒体Uri
                        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, FileProvider.getUriForFile(AppConfigs.ApplicationContext, "FProvider", new File(filePath)).toString());

                        //标题
                        final String musicTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, TextUtils.isEmpty(musicTitle) ? fileName : musicTitle);

                        //专辑名
                        final String musicAlbum = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, TextUtils.isEmpty(musicAlbum) ? AppConfigs.UNKNOWN : musicAlbum);

                        //作者
                        final String musicArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, TextUtils.isEmpty(musicArtist) ? AppConfigs.UNKNOWN : musicArtist);

                        //封面Uri路径字符串
                        final long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                        if (albumID > 0) {
                            CoverManager.INSTANCE.setSource(CoverType.NORMAL, filePath, ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumID).toString(), true);
                        }

                        //自定义封面检查
                        final File customCoverFile = new File(AppConfigs.DownloadCoversFolder + fileName + ".jpg");
                        if (customCoverFile.exists() && customCoverFile.length() > 0) {
                            CoverManager.INSTANCE.setSource(CoverType.CUSTOM, filePath, customCoverFile.getPath(), true);
                        } else {
                            //noinspection ResultOfMethodCallIgnored
                            customCoverFile.delete();
                        }

                        //开始处理所有类型的封面数据
                        albumCoverHandler(CoverType.NORMAL, filePath, null);
                        albumCoverHandler(CoverType.CUSTOM, filePath, null);

                        songList.add(new SongItem(filePath, metadataBuilder.build()));
                    }
                }

                cursor.close();

                //进行歌曲文件的排序
                switch (AppConfigs.SortType) {
                    case ByDate:
                        Collections.sort(songList, new ComparatorByData());
                        break;
                    case ByName:
                        Collections.sort(songList, new ComparatorByName());
                        break;
                }

                return songList;

            }

            return null;

        }

        /**
         * 判断文件是否为音乐文件
         *
         * @param fileName 要检查的文件名
         * @return 有效性
         */
        private boolean isMusicFile(String fileName) {
            if (TextUtils.isEmpty(fileName)) {
                return false;
            }

            String[] temp;
            try {
                temp = fileName.split("\\.");
            } catch (Exception e) {
                //如果解析文字失败 , 则表示文件没有后缀名 , 则不予以解析 , 当作文件非法
                return false;
            }
            if (temp.length >= 1) {
                //开始从后缀名判断是否为音频文件
                switch (temp[temp.length - 1]) {
                    case "mp3":
                        return true;
                    default:
                        return false;
                }
            } else {
                return false;
            }
        }

        /**
         * 文件是否有效
         *
         * @param path 文件路径地址
         * @return 文件有效性
         */
        private boolean isFileValid(String path) {
            final File checkFile = new File(path);
            return checkFile.canRead() && checkFile.canWrite() && checkFile.length() > 0;
        }

    }

    /**
     * 文件夹音频扫描器
     */
    final private class ScanByFolder implements Callable<String> {

        public final static String TAG = "ScanByFolder";

        //默认扫描的位置  路径结尾不能带"/"
        private final String[] defaultPaths = new String[]{
                AppConfigs.DownloadMusicFolder
        };

        private final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return isMusicFile(file);
            }
        };

        //文件夹路径集合
        private ArrayList<String> paths;

        ScanByFolder(String[] paths) {
            this.paths = new ArrayList<>();
            Collections.addAll(this.paths, paths);
        }

        @Override
        public String call() throws Exception {

            if (checkFolders(paths)) {
                //如果设置的目录都合法的话才进行扫描

                //添加默认路径
                addDefaultFolders();
                ArrayList<SongItem> arrayList = null;

                try {
                    //读取所有歌曲数据
                    arrayList = core();

                    //保存所有封面数据
                    CoverManager.INSTANCE.asyncUpdateFileCache();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (callback == null) {
                    //如果没有设置回调接口 , 则吧扫描到的数据放入临时列表中
                    cacheDatas(arrayList);
                } else {
                    JSONHandler.cacheSearchResult(arrayList);
                    onCompleted(arrayList);
                }
            }

            threadExecutor.removeTag(TAG);
            return null;
        }

        private void onCompleted(@Nullable final ArrayList<SongItem> arrayList) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    callback.onScanCompleted(arrayList, false);
                }
            });
        }

        /**
         * 添加默认路径
         */
        private void addDefaultFolders() {
            for (String defaultPath : defaultPaths) {
                final File file = new File(defaultPath);
                if (!this.paths.contains(defaultPath) && file.exists() && file.isDirectory() && file.canWrite()) {
                    this.paths.add(defaultPath);
                }
            }
        }

        /**
         * 检查目录是否有效
         *
         * @param paths 目录地址
         * @return 目录的有效性
         */
        private boolean checkFolders(ArrayList<String> paths) {
            if (paths == null || paths.size() <= 0) {
                return false;
            } else {
                for (String path : paths) {
                    if (!TextUtils.isEmpty(path)) {
                        File folder = new File(path);
                        if (!folder.isDirectory() || !folder.canWrite()) {
                            folder = null;
                            return false;
                        }
                        folder = null;
                    } else {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * 判断文件是否为音乐文件
         *
         * @param file 要检查的文件
         * @return 有效性
         */
        private boolean isMusicFile(File file) {
            if (file.length() <= 0) {
                //如果文件大小为 0 则肯定不合法
                return false;
            } else {
                String[] temp;
                try {
                    temp = file.getName().split("\\.");
                } catch (Exception e) {
                    //如果解析文字失败 , 则表示文件没有后缀名 , 则不予以解析 , 当作文件非法
                    return false;
                }
                if (temp.length >= 1) {
                    //开始从后缀名判断是否为音频文件
                    switch (temp[temp.length - 1]) {
                        case "mp3":
                            return true;
                        default:
                            return false;
                    }
                } else {
                    return false;
                }
            }
        }

        /**
         * 扫描获取设置的目录下的音频文件
         *
         * @return 音频文件列表
         * @throws Exception 扫描发生的异常
         */
        private
        @Nullable
        ArrayList<SongItem> core() throws Exception {

            final ArrayList<SongItem> songList = new ArrayList<>();

            //循环遍历每一个音频文件夹
            for (String path : paths) {
                File[] files = new File(path).listFiles(filter);
                if (files.length <= 0) {
                    //如果当前文件夹下没有任何合法的音频文件 , 则跳到下一个文件夹路径
                    continue;
                }

                //循环遍历每一个音频文件
                for (File musicFile : files) {

                    //提取文件名
                    final String fileName = musicFile.getName();

                    //提取文件路径(同时作为媒体数据的 UID、封面ID、封面颜色ID)
                    final String filePath = musicFile.getPath();

                    //媒体文件解析器
                    final MediaMetadataRetriever retriever = new MediaMetadataRetriever();

                    //创建媒体数据构造器
                    final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

                    //设置解析文件路径
                    retriever.setDataSource(filePath);

                    try {
                        final long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        if (duration < AppConfigs.LengthLimited) {
                            //如果歌曲长度小于限制 , 则不继续解析这个文件
                            continue;
                        } else {
                            //音频长度
                            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
                        }
                    } catch (NumberFormatException e) {
                        //如果无法获取到歌曲长度 , 则不解析这个文件
                        continue;
                    }

                    //文件名
                    metadataBuilder.putString(SongItem.SONGITEM_KEY_FILE_NAME, fileName);

                    //文件路径
                    metadataBuilder.putString(SongItem.SONGITEM_KEY_FILE_PATH, filePath);

                    //封面ID
                    metadataBuilder.putString(SongItem.SONGITEM_KEY_COVER_ID, filePath);

                    //媒体ID
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, filePath);

                    //媒体Uri
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, FileProvider.getUriForFile(AppConfigs.ApplicationContext, "FProvider", new File(filePath)).toString());

                    //标题
                    final String musicTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, TextUtils.isEmpty(musicTitle) ? fileName : musicTitle);

                    //专辑名
                    final String musicAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, TextUtils.isEmpty(musicAlbum) ? AppConfigs.UNKNOWN : musicAlbum);

                    //作者
                    final String musicArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, TextUtils.isEmpty(musicArtist) ? AppConfigs.UNKNOWN : musicArtist);

                    //检查是否有自定义封面
                    final File customCoverFile = new File(AppConfigs.DownloadCoversFolder + filePath + ".jpg");
                    if (customCoverFile.exists() && customCoverFile.length() > 0) {
                        CoverManager.INSTANCE.setSource(CoverType.CUSTOM, filePath, customCoverFile.getPath(), true);
                    } else {
                        //noinspection ResultOfMethodCallIgnored
                        customCoverFile.delete();
                    }

                    //开始处理所有类型的封面数据
                    albumCoverHandler(CoverType.NORMAL, filePath, retriever.getEmbeddedPicture());
                    albumCoverHandler(CoverType.CUSTOM, filePath, null);

                    songList.add(new SongItem(filePath, metadataBuilder.build()));
                }

            }

            if (songList.size() == 0) {
                return null;
            } else {
                //进行歌曲文件的排序
                switch (AppConfigs.SortType) {
                    case ByDate:
                        Collections.sort(songList, new ComparatorByData());
                        break;
                    case ByName:
                        Collections.sort(songList, new ComparatorByName());
                        break;
                }
                return songList;
            }

        }

    }

    /**
     * 加载上一次搜索记录线程
     */
    final private class LoadCachedData implements Callable<String> {
        final public static String TAG = "LoadCachedData";

        @Override
        public String call() throws Exception {

            ArrayList<SongItem> cachedList = JSONHandler.loadPlaylist(AppConfigs.CACHE_NAME);

            if (cachedList != null) {
                //进行歌曲文件的排序
                switch (AppConfigs.SortType) {
                    case ByDate:
                        Collections.sort(cachedList, new ComparatorByData());
                        break;
                    case ByName:
                        Collections.sort(cachedList, new ComparatorByName());
                        break;
                }
            }

            if (callback == null) {
                cacheDatas(cachedList);
            } else {
                onCompleted(cachedList);
            }

            threadExecutor.removeTag(TAG);
            return null;
        }

        private void onCompleted(@Nullable final ArrayList<SongItem> arrayList) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    callback.onScanCompleted(arrayList, true);
                }
            });
        }

    }

    /**
     * 按文件创建日期排序器
     */
    final private class ComparatorByData implements Comparator<SongItem> {

        ComparatorByData() {
            Logger.warnning("ComparatorByData", "正在按照日期排序");
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
    final private class ComparatorByName implements Comparator<SongItem> {

        ComparatorByName() {
            Logger.warnning("ComparatorByName", "正在按照名称排序");
        }

        @Override
        public int compare(SongItem songItem, SongItem t1) {
            return songItem.getTitle().compareTo(t1.getTitle());
        }

    }

}
