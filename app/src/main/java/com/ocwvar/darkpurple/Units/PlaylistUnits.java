package com.ocwvar.darkpurple.Units;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.PlaylistItem;
import com.ocwvar.darkpurple.Bean.SongItem;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/8/12 17:47
 * Project: DarkPurple
 * 播放列表操作类
 */
public class PlaylistUnits {
    private final String TAG = "PlaylistUnits";

    public static PlaylistUnits playlistUnits;
    private ArrayList<PlaylistItem> playlists;
    private GetPlaylistAudioesThread audioesThread = null;
    final String playlistSPName = "playlists";

    public PlaylistLoadingCallbacks loadingCallbacks;
    public PlaylistChangedCallbacks changedCallbacks;

    public interface PlaylistLoadingCallbacks{

        void onPreLoad();

        void onLoadCompleted(ArrayList<SongItem> data);

        void onLoadFailed();

    }

    public interface PlaylistChangedCallbacks{

        void onPlaylistDataChanged();

    }

    public PlaylistUnits() {
        this.playlists = new ArrayList<>();
    }

    public static PlaylistUnits getInstance() {
        if (playlistUnits == null) {
            playlistUnits = new PlaylistUnits();
        }
        return playlistUnits;
    }

    public void setPlaylistLoadingCallbacks(PlaylistLoadingCallbacks loadingCallbacks) {
        this.loadingCallbacks = loadingCallbacks;
    }

    public void setPlaylistChangedCallbacks(PlaylistChangedCallbacks changedCallbacks) {
        this.changedCallbacks = changedCallbacks;
    }

    /**
     * 从 SharedPreferences 读取播放列表的基本信息
     */
    public void initSPData(){
        Logger.warnning(TAG , "正在获取已储存的播放列表基本数据");
        SharedPreferences sp = AppConfigs.ApplicationContext.getSharedPreferences( playlistSPName , 0 );
        //先获取所有播放列表的名称
        String[] names = loadStringArray(sp , "names" );

        if (names != null){
            for (String playlistName : names) {
                //逐个从SP中获取播放列表数据
                Logger.warnning(TAG , "正在获取基本数据  "+playlistName);
                String[] playlistValues = loadStringArray( sp , playlistName );
                if (playlistValues != null && playlistValues.length == 4){
                    //如果字符集合有效 , 同时数量为4
                    this.playlists.add(new PlaylistItem(playlistValues));
                }
            }
            Logger.warnning(TAG , "基本数据获取完毕.  总计: "+playlists.size());
        }else {
            Logger.error(TAG , "无法获取. 原因: 没有已保存的数据");
        }

    }

    /**
     * 储存一个播放列表 (覆盖旧数据)
     * @param name  播放列表名字
     * @param playlist  播放列表音频数据
     * @return 创建是否成功
     */
    @SuppressLint("CommitPrefEdits")
    public boolean savePlaylist(@NonNull final String name  , @NonNull final ArrayList<SongItem> playlist){
        if (playlist.size() <= 0){
            return false;
        }

        //在储存到播放列表List中
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setName(name);
        playlistItem.setColor(playlist.get(0).getPaletteColor());
        playlistItem.setFirstAudioPath(playlist.get(0).getPath());
        playlistItem.setPlaylist(playlist);
        this.playlists.add(playlistItem);

        //回调更新数据
        if (changedCallbacks != null){
            changedCallbacks.onPlaylistDataChanged();
        }

        //异步储存基本数据到 SharedPreferences 中
        SharedPreferences sharedPreferences = AppConfigs.ApplicationContext.getSharedPreferences(playlistSPName , 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //保存名字索引
        Set<String> keys = sharedPreferences.getStringSet("names",new LinkedHashSet<String>());
        keys.add(name);
        //保存基本数据
        Set<String> values = new LinkedHashSet<>();
        values.add("name_"+playlistItem.getName());    //储存播放列表名字
        values.add("fap_"+playlistItem.getFirstAudioPath());     //储存播放列表第一个对象的路径
        values.add("color_"+Integer.toString(playlistItem.getColor()));      //储存第一个对象的颜色
        values.add("count_"+Integer.toString(playlist.size()));      //储存列表的总体大小
        editor.putStringSet(name , values).putStringSet("names",keys).commit();    //异步储存

        //异步储存到本地文件
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONHandler.savePlaylist(name , playlist);
            }
        }).start();

        return true;
    }

    /**
     * 移除播放列表
     * @param playlistName  播放列表名称
     */
    @SuppressLint("CommitPrefEdits")
    public void removePlaylist(@NonNull String playlistName){
        SharedPreferences sharedPreferences = AppConfigs.ApplicationContext.getSharedPreferences(playlistSPName , 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //获取播放列表名称合集 , 移除请求的关键字
        Set<String> names = sharedPreferences.getStringSet("names",new LinkedHashSet<String>());
        if (names.contains(playlistName)){
            names.remove(playlistName);
        }
        //移除请求的关键字下的基础信息
        editor.remove(playlistName);
        editor.putStringSet("names",names);
        //异步执行
        editor.commit();
        //移除播放列表Json数据文件
        new File(AppConfigs.JsonFilePath+playlistName+".pl").delete();
    }

    /**
     * 异步读取播放列表内的音频数据列表
     * @param callbacks 读取回调
     * @param playlistItem  播放列表对象
     */
    public void loadPlaylistAudioesData(PlaylistLoadingCallbacks callbacks , PlaylistItem playlistItem){
        if (audioesThread != null && audioesThread.getStatus() != AsyncTask.Status.FINISHED){
            audioesThread.cancel(true);
            audioesThread = null;
        }
        audioesThread = new GetPlaylistAudioesThread(playlistItem , callbacks);
        audioesThread.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * 检测是否已经存在相同的播放列表
     * @param name  播放列表名字
     * @return  是否存在
     */
    public boolean isPlaylistExisted(String name){
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setName(name);
        boolean result = playlists.contains(playlistItem);
        playlistItem = null;
        return result;
    }

    /**
     * 单独操作从 SharedPreferences 中获取数组基本数据
     * @param sp    SharedPreferences 对象
     * @param key   要获取的字符段
     * @return  成功时返回数据 , 失败时返回 NULL
     */
    private @Nullable String[] loadStringArray(@NonNull SharedPreferences sp , @NonNull String key){
        Set<String> set = sp.getStringSet(key , null);
        if (set != null){
            try {
                return set.toArray(new String[set.size()]);
            } catch (Exception e) {
                return null;
            }
        }else {
            return null;
        }
    }

    public ArrayList<PlaylistItem> getPlaylists() {
        return playlists;
    }

    public PlaylistItem getPlaylistIten(int position){
        if (playlists != null && position >= 0 && position < playlists.size()){
            return playlists.get(position);
        }else {
            return null;
        }
    }

    public int indexOfPlaylistItem(PlaylistItem playlistItem){
        return playlists.indexOf(playlistItem);
    }

    final class GetPlaylistAudioesThread extends AsyncTask<Integer , Void , ArrayList<SongItem>>{

        private PlaylistItem playlistItem;
        private PlaylistLoadingCallbacks callbacks;

        public GetPlaylistAudioesThread(PlaylistItem playlistItem, PlaylistLoadingCallbacks callbacks) {
            this.playlistItem = playlistItem;
            this.callbacks = callbacks;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callbacks != null){
                callbacks.onPreLoad();
            }
        }

        @Override
        protected ArrayList<SongItem> doInBackground(Integer... integers) {
            return JSONHandler.loadPlaylist(playlistItem.getName());
        }

        @Override
        protected void onPostExecute(ArrayList<SongItem> songItems) {
            super.onPostExecute(songItems);
            if (callbacks != null){
                if (songItems == null){
                    callbacks.onLoadFailed();
                }else {
                    playlistItem.setPlaylist(songItems);
                    callbacks.onLoadCompleted(songItems);
                }
            }
        }

    }

}
