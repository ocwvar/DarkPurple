package com.ocwvar.darkpurple.Units;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocwvar.darkpurple.AppConfigs;
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

    //数据储存位置
    public final static String folderPath = AppConfigs.JsonFilePath+"Playlist/";

    /**
     *  以Json方式储存播放列表数据
     * @param name  播放列表名称
     * @param playlist  播放列表音频数据
     * @return 执行结果
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean savePlaylist(String name , ArrayList<SongItem> playlist){
        final String TAG = "JSON播放列表  储存";
        if (TextUtils.isEmpty( name ) || playlist == null || playlist.size() == 0){
            //如果是无效数据 , 则执行失败
            Logger.error(TAG,"无效请求数据");
            return false;
        }else {
            File dataFile = new File(folderPath);
            dataFile.mkdirs();
            dataFile = new File(folderPath+name+".pl");
            if (!dataFile.exists()){
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    //文件创建失败 , 则保存失败
                    Logger.error(TAG,"文件创建失败");
                    return false;
                }
            }

            //创建一个JsonArray用于存放整个数据
            JsonArray jsonArray = new JsonArray();

            for (SongItem singleSong : playlist) {
                JsonObject object = new JsonObject();
                object.addProperty("name",singleSong.getTitle());
                object.addProperty("path",singleSong.getPath());
                object.addProperty("album",singleSong.getAlbum());
                object.addProperty("artist",singleSong.getArtist());
                object.addProperty("filename",singleSong.getFileName());
                object.addProperty("length",Long.toString(singleSong.getLength()));
                object.addProperty("albumid",Long.toString(singleSong.getAlbumID()));
                object.addProperty("color",Integer.toString(singleSong.getPaletteColor()));
                object.addProperty("ishavecover",singleSong.isHaveCover());
                if (singleSong.getAlbumCoverUri() == null){
                    object.addProperty("albumuri","");
                }else {
                    object.addProperty("albumuri",singleSong.getAlbumCoverUri().toString());
                }

                jsonArray.add(object);
            }

            //创建字节缓冲数组
            byte[] buffer = new byte[512];
            //创建字符串字节数组
            byte[] jsonArrayByteArray;
            try {
                jsonArrayByteArray = jsonArray.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                Logger.error(TAG,"保存播放列表数据时UTF-8转码出现异常 , 使用默认编码");
                jsonArrayByteArray = jsonArray.toString().getBytes();
            }
            //读取的长度
            int length;

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(dataFile,false);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(jsonArrayByteArray);
                while ((length = byteArrayInputStream.read(buffer)) != -1){
                    fileOutputStream.write(buffer,0,length);
                }
                byteArrayInputStream.close();
                fileOutputStream.flush();
                fileOutputStream.close();
                byteArrayInputStream = null;
                fileOutputStream = null;
                dataFile = null;
            } catch (Exception e) {
                //无法创建数据输出流
                jsonArray = null;
                buffer = null;
                dataFile = null;
                Logger.error(TAG,"创建文件输出流失败");
                return false;
            }
            Logger.warnning(TAG,"播放列表保存成功 !  文件名:"+name+".pl");
            return true;
        }
    }

    /**
     * 从文件读取Json形式保存的播放列表数据
     * @param name  播放列表名称
     * @return  如果读取成功 , 则返回歌曲列表 , 否则返回 NULL
     */
    public static @Nullable ArrayList<SongItem> loadPlaylist(String name){
        final String TAG = "JSON播放列表  读取";
        if (TextUtils.isEmpty(name)){
            Logger.error(TAG,"请求数据无效");
            return null;
        }else {
            //创建文件对象
            File dataFile = new File(folderPath+name+".pl");
            if (dataFile.exists() && dataFile.canRead() && dataFile.length() > 0){
                //如果文件合法 , 则开始读取
                //先创建JsonArray对象用于储存数据
                JsonArray jsonArray;

                try {
                    FileInputStream fileInputStream = new FileInputStream(dataFile);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    byte[] buffer = new byte[512];
                    int length;
                    while ((length = fileInputStream.read(buffer)) != -1){
                        byteArrayOutputStream.write(buffer,0,length);
                    }
                    //关闭流
                    byteArrayOutputStream.close();
                    fileInputStream.close();
                    //将字节数组转换为字符串再将转换到的数据转化为JsonArray对象
                    jsonArray = new JsonParser().parse(new String(byteArrayOutputStream.toByteArray(),"UTF-8")).getAsJsonArray();
                    //清空数据
                    buffer = null;
                    length = 0;
                    byteArrayOutputStream.reset();
                } catch (Exception e) {
                    Logger.error(TAG,"创建文件输入流 或 转换JsonArray失败\n" + e);
                    return null;
                }

                ArrayList<SongItem> playlist = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject object = jsonArray.get(i).getAsJsonObject();
                    SongItem songItem = new SongItem();

                    if (isJsonObjectVaild( object , "name" )){
                        songItem.setTitle(object.get("name").getAsString());
                    }else {
                        continue;
                    }

                    if (isJsonObjectVaild( object , "path" )){
                        songItem.setPath(object.get("path").getAsString());
                    }else {
                        continue;
                    }

                    if (isJsonObjectVaild( object , "album" )){
                        songItem.setAlbum(object.get("album").getAsString());
                    }else {
                        continue;
                    }

                    if (isJsonObjectVaild( object , "filename" )){
                        songItem.setFileName(object.get("filename").getAsString());
                    }else {
                        continue;
                    }

                    if (isJsonObjectVaild( object , "length" )){
                        songItem.setLength(object.get("length").getAsLong());
                    }else {
                        continue;
                    }

                    if (isJsonObjectVaild( object , "artist" )){
                        songItem.setArtist(object.get("artist").getAsString());
                    }

                    if (isJsonObjectVaild( object , "albumid" )){
                        songItem.setAlbumID(object.get("albumid").getAsLong());
                    }

                    if (isJsonObjectVaild( object , "color" )){
                        songItem.setPaletteColor(object.get("color").getAsInt());
                    }

                    if (isJsonObjectVaild( object , "ishavecover" )){
                        songItem.setHaveCover(object.get("ishavecover").getAsBoolean());
                    }

                    if (isJsonObjectVaild( object , "albumuri" )){
                        songItem.setAlbumCoverUri(Uri.parse(object.get("albumuri").getAsString()));
                    }else {
                        songItem.setAlbumCoverUri(null);
                    }

                    playlist.add(songItem);
                }

                if (playlist.size() == 0){
                    Logger.error(TAG,"列表无数据储存");
                    return null;
                }else {
                    Logger.warnning(TAG,"读取播放列表成功");
                    return playlist;
                }

            }else {
                Logger.error(TAG,"播放列表文件不存在");
                return null;
            }
        }
    }

    /**
     * 检测要获取的字段是否合法
     * @param object    要检测的JsonObject
     * @param key 要获取的key
     * @return  是否合法
     */
    private static boolean isJsonObjectVaild(JsonObject object , String key){
        return object != null && !TextUtils.isEmpty(key) && object.has(key) && object.get(key) != null;
    }

}
