package com.ocwvar.darkpurple.Units;

import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.CoverPreviewBean;
import com.ocwvar.darkpurple.Bean.SongItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/8/13 22:13
 * Project: DarkPurple
 * JSON 数据处理
 */
public class JSONHandler {

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

    /**
     * 以Json方式储存播放列表数据
     *
     * @param name     播放列表名称
     * @param playlist 播放列表音频数据
     * @return 执行结果
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static boolean savePlaylist(String name, ArrayList<SongItem> playlist) {
        final String TAG = "JSON播放列表  储存";
        if (TextUtils.isEmpty(name) || playlist == null || playlist.size() == 0) {
            //如果是无效数据 , 则执行失败
            Logger.error(TAG, "无效请求数据");
            return false;
        } else {
            File dataFile = new File(AppConfigs.PlaylistFolder);
            dataFile.mkdirs();
            dataFile = new File(AppConfigs.PlaylistFolder + name + ".pl");
            if (!dataFile.exists()) {
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    //文件创建失败 , 则保存失败
                    Logger.error(TAG, "文件创建失败");
                    return false;
                }
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

            return jsonArray2File(jsonArray, dataFile);
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
    ArrayList<SongItem> loadPlaylist(String name) {
        final String TAG = "JSON播放列表  读取";
        if (TextUtils.isEmpty(name)) {
            Logger.error(TAG, "请求数据无效");
            return null;
        } else {
            //创建文件对象
            File dataFile = new File(AppConfigs.PlaylistFolder + name + ".pl");
            if (dataFile.exists() && dataFile.canRead() && dataFile.length() > 0) {
                //如果文件合法 , 则开始读取

                //获取对应的文件数据
                final JsonArray jsonArray = file2JsonArray(dataFile.getPath());
                if (jsonArray == null || jsonArray.size() <= 0) {
                    //获取数据失败或数据为空
                    return null;
                }

                //歌曲数据储存列表容器
                final ArrayList<SongItem> playlist = new ArrayList<>();
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
                    Logger.warnning(TAG, "读取播放列表成功");
                    return playlist;
                }

            } else {
                Logger.error(TAG, "播放列表文件不存在");
                return null;
            }
        }
    }

    /**
     * 缓存搜索记录
     *
     * @param playlist 搜索得到的数据
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void cacheSearchResult(ArrayList<SongItem> playlist) {
        final String TAG = "搜索记录缓存";
        if (playlist == null) {
            Logger.error(TAG, "缓存列表为 NULL , 不进行缓存");
            return;
        }

        File dataFile = new File(AppConfigs.PlaylistFolder);
        dataFile.mkdirs();
        dataFile = new File(AppConfigs.PlaylistFolder + AppConfigs.CACHE_NAME + ".pl");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                //文件创建失败 , 则保存失败
                Logger.error(TAG, "文件创建失败");
                return;
            }
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

        jsonArray2File(jsonArray, dataFile);
    }

    /**
     * 解析出封面预览的数据列表
     *
     * @param jsonData 获取到的Json数据
     * @return 封面数据集合
     */
    public static ArrayList<CoverPreviewBean> loadCoverPreviewList(String jsonData) {
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

    /**
     * 检测要获取的字段是否合法
     *
     * @param object 要检测的JsonObject
     * @param key    要获取的key
     * @return 是否合法
     */
    private static boolean isJsonObjectValid(JsonObject object, String key) {
        return object != null && !TextUtils.isEmpty(key) && object.has(key) && object.get(key) != null;
    }

    /**
     * 将JsonArray存入指定文件中
     *
     * @param jsonArray JsonArray数据
     * @param target    目标文件，文件不存在则进行创建，已存在则覆写
     * @return 执行结果
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean jsonArray2File(final JsonArray jsonArray, final File target) {
        //创建字节缓冲数组
        final byte[] buffer = new byte[512];
        //创建字符串字节数组
        byte[] jsonArrayByteArray;
        try {
            jsonArrayByteArray = jsonArray.toString().getBytes("UTF-8");
            if (!target.exists()) {
                target.createNewFile();
            }
        } catch (UnsupportedEncodingException e) {
            Logger.error("JsonArray2File", "保存搜索结果数据时UTF-8转码出现异常 , 使用默认编码");
            jsonArrayByteArray = jsonArray.toString().getBytes();
        } catch (IOException e) {
            Logger.error("JsonArray2File", "写入文件不存在 且 无法创建写入文件");
            return false;
        }
        //读取的长度
        int length;

        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(target, false);
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(jsonArrayByteArray);
            while ((length = byteArrayInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
            byteArrayInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            Logger.warnning("JsonArray2File", "文件保存成功：" + target.getName());
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
    JsonArray file2JsonArray(final String targetPath) {

        final File target = new File(targetPath);
        if (!target.exists() || !target.canRead()) {
            Logger.error("file2JsonArray", "文件不存在或不可读取：" + targetPath);
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
            final JsonArray jsonArray = new JsonParser().parse(new String(byteArrayOutputStream.toByteArray(), "UTF-8")).getAsJsonArray();

            //清空数据
            byteArrayOutputStream.reset();

            //返回数据
            Logger.warnning("file2JsonArray", "文件读取成功：" + targetPath);
            return jsonArray;
        } catch (Exception e) {
            Logger.error("file2JsonArray", "创建文件输入流 或 转换JsonArray失败\n" + e);
            return null;
        }
    }

}
