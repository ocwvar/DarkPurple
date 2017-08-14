package com.ocwvar.darkpurple.Units;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.CoverPreviewBean;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.Units.Cover.ColorType;
import com.ocwvar.darkpurple.Units.Cover.CoverType;
import com.ocwvar.darkpurple.Units.MediaLibrary.MediaLibrary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/8/13 22:13
 * Project: DarkPurple
 * JSON 数据处理
 */
@SuppressWarnings("ConstantConditions")
public final class JSONHandler {

    //储存读取与储存使用到的Key
    private final static String[] mediaMetadataKeys = new String[]{
            MediaMetadataCompat.METADATA_KEY_ALBUM,
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            MediaMetadataCompat.METADATA_KEY_TITLE,
            MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
            MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
            MediaMetadataCompat.METADATA_KEY_DURATION,
            SongItem.SONGITEM_KEY_COVER_ID,
            SongItem.SONGITEM_KEY_FILE_NAME,
            SongItem.SONGITEM_KEY_FILE_PATH
    };


    ////////////////////////////////////////
    /////////////音频数据相关处理/////////////
    ////////////////////////////////////////


    /**
     * 以Json方式储存播放列表数据
     *
     * @param name     播放列表名称
     * @param playlist 播放列表音频数据
     * @return 执行结果
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static boolean savePlaylist(final @NonNull String name, final @NonNull ArrayList<SongItem> playlist) {
        final String TAG = "JSON播放列表  储存";
        if (TextUtils.isEmpty(name) || playlist == null || playlist.size() == 0) {
            //如果是无效数据 , 则执行失败
            Logger.error(TAG, "无效请求数据");
            return false;
        } else {

            //创建一个JsonArray用于存放整个数据
            JsonArray jsonArray = new JsonArray();

            for (SongItem singleSong : playlist) {
                final JsonObject object = new JsonObject();

                //遍历所有储存的Key进行写入到 JsonObject
                for (String key : mediaMetadataKeys) {
                    if (key.equals(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                        object.addProperty(key, singleSong.getMediaMetadata().getLong(key));
                    } else {
                        object.addProperty(key, singleSong.getMediaMetadata().getString(key));
                    }
                }

                jsonArray.add(object);
            }

            //进行文件储存
            return json2File(jsonArray, new File(AppConfigs.PlaylistFolder + name + ".pl"));
        }
    }

    /**
     * 从文件读取Json形式保存的播放列表数据
     *
     * @param name 播放列表名称
     * @return 如果读取成功 , 则返回歌曲列表 , 否则返回 NULL
     */
    static
    @Nullable
    ArrayList<SongItem> loadPlaylist(final @NonNull String name) {
        final String TAG = "JSON播放列表  读取";
        if (TextUtils.isEmpty(name)) {

            Logger.error(TAG, "请求数据无效");
            return null;
        } else {

            //获取对应的文件数据
            final JsonElement jsonElement = file2Json(AppConfigs.PlaylistFolder + name + ".pl");
            if (jsonElement == null) {
                //获取数据失败或数据为空
                return null;
            }

            //歌曲数据储存列表容器
            final ArrayList<SongItem> playlist = new ArrayList<>();
            final JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                final JsonObject object = jsonArray.get(i).getAsJsonObject();
                final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

                //逐个Key进行获取
                for (final String key : mediaMetadataKeys) {
                    if (key.equals(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                        metadataBuilder.putLong(key, object.get(key).getAsLong());
                    } else {
                        metadataBuilder.putString(key, object.get(key).getAsString());
                    }
                }

                final MediaMetadataCompat mediaMetadataCompat = metadataBuilder.build();
                playlist.add(new SongItem(mediaMetadataCompat.getString(SongItem.SONGITEM_KEY_FILE_PATH), mediaMetadataCompat));
            }

            if (playlist.size() == 0) {

                Logger.error(TAG, "列表无数据储存");
                return null;
            } else {

                Logger.warning(TAG, "读取播放列表成功");
                return playlist;
            }
        }
    }

    /**
     * 缓存搜索记录
     *
     * @param playlist 搜索得到的数据
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void cacheSearchResult(final @NonNull ArrayList<SongItem> playlist) {
        final String TAG = "搜索记录缓存";
        if (playlist == null) {
            Logger.error(TAG, "缓存列表为 NULL , 不进行缓存");
            return;
        }

        //创建一个JsonArray用于存放整个数据
        JsonArray jsonArray = new JsonArray();

        for (SongItem singleSong : playlist) {
            final JsonObject object = new JsonObject();

            //遍历所有储存的Key进行写入到 JsonObject
            for (String key : mediaMetadataKeys) {
                if (key.equals(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                    object.addProperty(key, singleSong.getMediaMetadata().getLong(key));
                } else {
                    object.addProperty(key, singleSong.getMediaMetadata().getString(key));
                }
            }

            jsonArray.add(object);
        }

        //进行文件储存
        json2File(jsonArray, new File(AppConfigs.PlaylistFolder + AppConfigs.CACHE_NAME + ".pl"));
    }


    ////////////////////////////////////////
    ///////////////在线封面解析///////////////
    ////////////////////////////////////////


    /**
     * 解析出封面预览的数据列表
     *
     * @param jsonData 获取到的Json数据
     * @return 封面数据集合
     */
    public static
    @Nullable
    ArrayList<CoverPreviewBean> loadCoverPreviewList(final @NonNull String jsonData) {
        final String TAG = "解析封面Json数据";

        if (TextUtils.isEmpty(jsonData)) {
            return null;
        }

        JsonObject jsonObject;

        try {
            jsonObject = new JsonParser().parse(jsonData).getAsJsonObject();
        } catch (Exception e) {
            Logger.error(TAG, "数据解析失败");
            return null;
        }

        if (isJsonObjectValid(jsonObject, "results")) {
            JsonArray jsonArray = jsonObject.get("results").getAsJsonArray();
            if (jsonArray.size() > 0) {
                ArrayList<CoverPreviewBean> previewBeen = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject object = jsonArray.get(i).getAsJsonObject();
                    CoverPreviewBean bean = new CoverPreviewBean();

                    if (isJsonObjectValid(object, "collectionName")) {
                        bean.setAlbumName(object.get("collectionName").getAsString());
                    }

                    if (isJsonObjectValid(object, "artworkUrl60")) {
                        bean.setArtworkUrl60(object.get("artworkUrl60").getAsString());
                    }

                    if (isJsonObjectValid(object, "artworkUrl100")) {
                        bean.setArtworkUrl100(object.get("artworkUrl100").getAsString());
                    }

                    previewBeen.add(bean);

                }

                return previewBeen;
            } else {
                Logger.error(TAG, "无封面数据");
                return null;
            }
        } else {
            Logger.error(TAG, "无封面数据");
            return null;
        }

    }


    ////////////////////////////////////////
    ///////////最后播放音频数据处理////////////
    ////////////////////////////////////////


    /**
     * 储存最近的媒体状态：使用的媒体库、媒体路径，到本地文件
     */
    public static void saveLastMediaState() {
        final String usingLibraryTAG = MediaLibrary.INSTANCE.getUsingLibraryTAG();
        final SongItem mediaItem = MediaLibrary.INSTANCE.getUsingMedia();

        if (TextUtils.isEmpty(usingLibraryTAG) || mediaItem == null) {
            //不可用的数据
            return;
        }

        final String mediaPath = mediaItem.getPath();
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("usingLibraryTAG", usingLibraryTAG);
        jsonObject.addProperty("mediaPath", mediaPath);

        json2File(jsonObject, new File(AppConfigs.DataFolder + "LastState.data"));
    }

    /**
     * 删除最近的媒体状态储存文件
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void removeLastMediaState() {
        new File(AppConfigs.DataFolder + "LastState.data").delete();
    }

    /**
     * 获取最近的媒体状态：使用的媒体库、媒体路径
     *
     * @return 保存的数据，不存在则返回 NULL。 [0]：最后使用的媒体库TAG  [1]：歌曲路径
     */
    public static
    @Nullable
    String[] getLastMediaState() {
        final JsonElement jsonElement = file2Json(AppConfigs.DataFolder + "LastState.data");
        if (jsonElement == null) {
            return null;
        }
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!isJsonObjectValid(jsonObject, "usingLibraryTAG") || !isJsonObjectValid(jsonObject, "mediaPath")) {
            //获取到的数据不合法
            return null;
        }

        return new String[]{
                jsonObject.get("usingLibraryTAG").getAsString(),
                jsonObject.get("mediaPath").getAsString()
        };
    }


    ////////////////////////////////////////
    /////////封面&混合色  数据相关处理/////////
    ////////////////////////////////////////


    /**
     * 获取封面库
     *
     * @param coverType 封面类型
     * @return 封面库的Map，获取失败返回 NULL
     */
    public static synchronized
    @Nullable
    LinkedHashMap<String, String> getCoverLibrary(final @NonNull CoverType coverType) {
        final JsonElement jsonElement = file2Json(AppConfigs.DataFolder + "CoverLibrary_" + coverType.name() + ".data");
        if (jsonElement == null) {
            return null;
        }

        final JsonArray jsonArray = jsonElement.getAsJsonArray();
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        final Iterator<JsonElement> objects = jsonArray.iterator();
        while (objects.hasNext()) {
            final JsonObject jsonObject = objects.next().getAsJsonObject();
            final String key = jsonObject.get("Key").getAsString();
            final String value = jsonObject.get("Value").getAsString();
            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 获取封面颜色库
     *
     * @param colorType 颜色类型
     * @return 封面颜色库的Map，获取失败返回 NULL
     */
    public static synchronized
    @Nullable
    LinkedHashMap<String, Integer> getColorLibrary(final @NonNull ColorType colorType) {
        final JsonElement jsonElement = file2Json(AppConfigs.DataFolder + "ColorLibrary_" + colorType.name() + ".data");
        if (jsonElement == null) {
            return null;
        }

        final JsonArray jsonArray = jsonElement.getAsJsonArray();
        final LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        final Iterator<JsonElement> objects = jsonArray.iterator();

        while (objects.hasNext()) {
            final JsonObject jsonObject = objects.next().getAsJsonObject();
            final String key = jsonObject.get("Key").getAsString();
            final int value = jsonObject.get("Value").getAsInt();
            if (!TextUtils.isEmpty(key)) {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 保存封面库
     *
     * @param library   封面库
     * @param coverType 封面类型
     */
    public synchronized static void saveCoverLibrary(final @NonNull LinkedHashMap<String, String> library, final @NonNull CoverType coverType) {
        if (library != null && library.size() <= 0) {
            //库为空，直接删除数据文件
            new File(AppConfigs.DataFolder + "CoverLibrary_" + coverType.name() + ".data").delete();
            return;
        }

        final JsonArray jsonArray = new JsonArray();
        final Iterator<String> keys = library.keySet().iterator();
        final Iterator<String> values = library.values().iterator();

        while (keys.hasNext() && values.hasNext()) {
            final JsonObject jsonObject = new JsonObject();
            final String value = values.next();
            final String key = keys.next();

            jsonObject.addProperty("Key", key);
            jsonObject.addProperty("Value", value);
            jsonArray.add(jsonObject);
        }

        json2File(jsonArray, new File(AppConfigs.DataFolder + "CoverLibrary_" + coverType.name() + ".data"));
    }

    /**
     * 保存封面颜色库
     *
     * @param colorType 颜色类型
     */
    public synchronized static void saveColorLibrary(final @NonNull LinkedHashMap<String, Integer> library, final @NonNull ColorType colorType) {
        if (library != null && library.size() <= 0) {
            //库为空，直接删除数据文件
            new File(AppConfigs.DataFolder + "ColorLibrary_" + colorType.name() + ".data").delete();
            return;
        }

        final JsonArray jsonArray = new JsonArray();
        final Iterator<String> keys = library.keySet().iterator();
        final Iterator<Integer> values = library.values().iterator();

        while (keys.hasNext() && values.hasNext()) {
            final JsonObject jsonObject = new JsonObject();
            final int value = values.next();
            final String key = keys.next();

            jsonObject.addProperty("Key", key);
            jsonObject.addProperty("Value", value);
            jsonArray.add(jsonObject);
        }

        json2File(jsonArray, new File(AppConfigs.DataFolder + "ColorLibrary_" + colorType.name() + ".data"));
    }


    ////////////////////////////////////////
    //////////////均衡器数据处理//////////////
    ////////////////////////////////////////

    /**
     * 读取所有均衡器配置文件
     *
     * @return 已储存的数据，< 配置名称 , 配置数据 >，读取失败或无储存数据，返回 NULL
     */
    public static
    @Nullable
    LinkedHashMap<String, short[]> loadSavedEqualizerArgs() {

        //获取所有 *.eqz 文件
        final File[] eqFiles = new File(AppConfigs.DataFolder + "Equalizer/").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.length() > 5 && s.endsWith(".eqz");
            }
        });

        final LinkedHashMap<String, short[]> result = new LinkedHashMap<>();
        for (final File eqFile : eqFiles) {

            //获取文件的Json数据
            final JsonElement jsonElement = file2Json(eqFile.getPath());
            if (jsonElement == null) {
                break;
            }

            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (isJsonObjectValid(jsonObject, "name") && isJsonObjectValid(jsonObject, "data")) {
                //如果Json数据合法，则开始解析

                //获取均衡器数据
                final JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();
                final short[] data = new short[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    data[i] = jsonArray.get(i).getAsShort();
                }

                //获取频谱名称
                final String name = jsonObject.get("name").getAsString();

                result.put(name, data);
            }

        }

        if (result.size() <= 0) {
            return null;
        }
        return result;
    }

    /**
     * 储存均衡器配置到本地文件
     *
     * @param name 配置名称
     * @param data 配置数据
     */
    public static void saveEqualizerArgs(final @NonNull String name, final @NonNull short[] data) {
        if (TextUtils.isEmpty(name) || data.length <= 0) {
            //无效数据，不进行保存
            return;
        }

        //创建JsonObject主体，以及设置配置名称
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", name);

        //储存配置数据
        final JsonArray jsonArray = new JsonArray();
        for (final short number : data) {
            jsonArray.add(number);
        }
        jsonObject.add("data", jsonArray);

        //进行文件储存
        json2File(jsonObject, new File(AppConfigs.DataFolder + "Equalizer/"));
    }

    ////////////////////////////////////////
    ////////////////工具方法/////////////////
    ////////////////////////////////////////


    /**
     * 检测要获取的字段是否合法
     *
     * @param object 要检测的JsonObject
     * @param key    要获取的key
     * @return 是否合法
     */
    private static boolean isJsonObjectValid(final @NonNull JsonObject object, final @NonNull String key) {
        return object != null && !TextUtils.isEmpty(key) && object.has(key) && object.get(key) != null;
    }

    /**
     * 将JsonArray存入指定文件中
     *
     * @param jsonElement Json数据
     * @param target      目标文件，文件不存在则进行创建，已存在则覆写
     * @return 执行结果
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean json2File(final @NonNull JsonElement jsonElement, final @NonNull File target) {
        //创建字节缓冲数组
        final byte[] buffer = new byte[512];
        //创建字符串字节数组
        byte[] jsonElementBytes;
        try {
            //开始检查文件目录的可用性
            final String parentDirPath = target.getParent();

            //生成文件的父目录路径存在
            if (!TextUtils.isEmpty(parentDirPath)) {

                final File parentDir = new File(parentDirPath);

                if (parentDir.exists() || parentDir.mkdirs()) {
                    //只有当父目录存在或创建成功才进行下一步
                    if (!target.exists()) {
                        //如果要储存的文件不存在，则创建一个空文件
                        target.createNewFile();
                    }

                    //所有需求都满足后读取 JsonArray 的字节
                    //默认采取 UTF8 编码
                    jsonElementBytes = jsonElement.toString().getBytes("UTF-8");

                } else {
                    Logger.error("JsonArray2File", "文件目录不可用");
                    return false;
                }
            } else {
                Logger.error("JsonArray2File", "文件目录不可用");
                return false;
            }
        } catch (UnsupportedEncodingException e) {

            Logger.error("JsonArray2File", "保存搜索结果数据时UTF-8转码出现异常 , 使用默认编码");
            jsonElementBytes = jsonElement.toString().getBytes();
        } catch (IOException e) {

            Logger.error("JsonArray2File", "写入文件不存在 且 无法创建写入文件");
            return false;
        }
        //读取的长度
        int length;

        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(target, false);
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(jsonElementBytes);
            while ((length = byteArrayInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
            byteArrayInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            Logger.warning("JsonArray2File", "文件保存成功：" + target.getName());
            return true;
        } catch (Exception e) {
            //无法创建数据输出流
            Logger.error("JsonArray2File", "创建文件输出流失败");
            return false;
        }
    }

    /**
     * 将文件内容读取成JsonArray
     *
     * @param targetPath 目标文件路径
     * @return 结果JsonArray，如果提取失败，则返回 NULL
     */
    private static
    @Nullable
    JsonElement file2Json(final @NonNull String targetPath) {

        final File target = new File(targetPath);
        if (!target.exists() || !target.canRead()) {
            Logger.error("file2Json", "文件不存在或不可读取：" + targetPath);
            return null;
        }

        try {
            final FileInputStream fileInputStream = new FileInputStream(target);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final byte[] buffer = new byte[512];

            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            //关闭流
            byteArrayOutputStream.close();
            fileInputStream.close();

            //将字节数组转换为字符串再将转换到的数据转化为JsonArray对象
            final JsonElement jsonElement = new JsonParser().parse(new String(byteArrayOutputStream.toByteArray(), "UTF-8"));

            //清空数据
            byteArrayOutputStream.reset();

            //返回数据
            Logger.warning("file2Json", "文件读取成功：" + targetPath);
            return jsonElement;
        } catch (Exception e) {
            Logger.error("file2Json", "创建文件输入流 或 转换JsonElement失败\n" + e);
            return null;
        }
    }

}
