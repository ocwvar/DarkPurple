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
    private static PlaylistUnits playlistUnits;
    private final String playlistSPName = "playlists";
    private final String TAG = "PlaylistUnits";
    private PlaylistLoadingCallbacks loadingCallbacks;
    private PlaylistChangedCallbacks changedCallbacks;
    private ArrayList<PlaylistItem> playlists;
    private GetPlaylistAudiosThread audioesThread = null;

    private PlaylistUnits() {
        this.playlists = new ArrayList<>();
    }

    public static PlaylistUnits getInstance() {
        if (playlistUnits == null) {
            playlistUnits = new PlaylistUnits();
        }
        return playlistUnits;
    }

    /**
     * 设置播放列表读取回调
     *
     * @param loadingCallbacks 回调接口
     */
    public void setPlaylistLoadingCallbacks(PlaylistLoadingCallbacks loadingCallbacks) {
        this.loadingCallbacks = loadingCallbacks;
    }

    /**
     * 设置播放列表变动回调
     *
     * @param changedCallbacks 回调接口
     */
    public void setPlaylistChangedCallbacks(PlaylistChangedCallbacks changedCallbacks) {
        this.changedCallbacks = changedCallbacks;
    }

    /**
     * 从 SharedPreferences 读取播放列表的基本信息
     */
    void initSPData() {
        Logger.warnning(TAG, "正在获取已储存的播放列表基本数据");
        SharedPreferences sp = AppConfigs.ApplicationContext.getSharedPreferences(playlistSPName, 0);
        //先获取所有播放列表的名称
        String[] names = loadStringArray(sp, "names");

        if (names != null) {
            for (String playlistName : names) {
                //逐个从SP中获取播放列表数据
                Logger.warnning(TAG, "正在获取基本数据  " + playlistName);
                String[] playlistValues = loadStringArray(sp, playlistName);
                if (playlistValues != null && playlistValues.length == 3) {
                    //如果字符集合有效 , 同时数量为3
                    this.playlists.add(new PlaylistItem(playlistName, playlistValues));
                }
            }
            Logger.warnning(TAG, "基本数据获取完毕.  总计: " + playlists.size());
        } else {
            Logger.error(TAG, "无法获取. 原因: 没有已保存的数据");
        }

    }

    /**
     * 储存一个播放列表 (覆盖旧数据)
     *
     * @param name     播放列表名字
     * @param playlist 播放列表音频数据
     * @return 创建是否成功
     */
    @SuppressLint("CommitPrefEdits")
    public boolean savePlaylist(@NonNull final String name, @NonNull final ArrayList<SongItem> playlist) {
        if (playlist.size() <= 0) {
            return false;
        }

        //在储存到播放列表List中
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setName(name);
        playlistItem.setColor(playlist.get(0).getPaletteColor());
        playlistItem.setFirstAudioPath(playlist.get(0).getPath());
        playlistItem.setPlaylist(playlist);
        //移除旧的数据 , 添加新的数据
        this.playlists.remove(playlistItem);
        this.playlists.add(playlistItem);

        //回调更新数据
        if (changedCallbacks != null) {
            changedCallbacks.onPlaylistDataChanged();
        }

        //异步储存基本数据到 SharedPreferences 中
        SharedPreferences sharedPreferences = AppConfigs.ApplicationContext.getSharedPreferences(playlistSPName, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //保存名字索引
        final Set<String> keys = sharedPreferences.getStringSet("names", new LinkedHashSet<String>());
        if (!keys.contains(name)) {
            keys.add(name);
        }
        //保存基本数据
        Set<String> values = new LinkedHashSet<>();
        values.add("fap_" + playlistItem.getFirstAudioPath());     //储存播放列表第一个对象的路径
        values.add("color_" + Integer.toString(playlistItem.getColor()));      //储存第一个对象的颜色
        values.add("count_" + Integer.toString(playlist.size()));      //储存列表的总体大小
        editor.remove(name).putStringSet(name, values).putStringSet("names", keys).commit();    //异步操作 : 删除旧的数据 , 添加新的数据

        //异步储存到本地文件
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONHandler.savePlaylist(name, playlist);
            }
        }).start();

        return true;
    }

    /**
     * 移除播放列表
     *
     * @param playlistItem 播放列表对象
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CommitPrefEdits")
    public void removePlaylist(@NonNull PlaylistItem playlistItem) {
        SharedPreferences sharedPreferences = AppConfigs.ApplicationContext.getSharedPreferences(playlistSPName, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //移除列表内的数据对象
        if (playlists.contains(playlistItem)) {
            playlists.remove(playlistItem);
        }

        //回调更新数据
        if (changedCallbacks != null) {
            changedCallbacks.onPlaylistDataChanged();
        }

        //获取播放列表名称合集 , 移除请求的关键字
        Set<String> names = sharedPreferences.getStringSet("names", new LinkedHashSet<String>());
        if (names.contains(playlistItem.getName())) {
            names.remove(playlistItem.getName());
        }
        //移除请求的关键字下的基础信息
        editor.remove(playlistItem.getName());
        editor.putStringSet("names", names);
        //异步执行
        editor.commit();
        //移除播放列表Json数据文件
        new File(JSONHandler.folderPath + playlistItem.getName() + ".pl").delete();
    }

    /**
     * 更改播放列表名字
     *
     * @param oldName 旧的名字
     * @param newName 新的名字
     * @return 执行结果
     */
    @SuppressLint("CommitPrefEdits")
    public boolean renamePlaylist(@NonNull String oldName, @NonNull final String newName) {
        if (playlists.contains(new PlaylistItem(oldName)) && !playlists.contains(new PlaylistItem(newName))) {
            //如果旧的列表的确存在 同时不存在与新名字相同的列表 , 则可以执行
            //获取名字集合 , 更改后重新保存到SP中
            final SharedPreferences sp = AppConfigs.ApplicationContext.getSharedPreferences(playlistSPName, 0);
            final Set<String> newKeys = new LinkedHashSet<>();
            String[] names = loadStringArray(sp, "names");
            if (names != null && names.length >= 1) {
                for (String name : names) {
                    if (name.equals(oldName)) {
                        name = newName;
                    }
                    newKeys.add(name);
                }
                names = null;
                sp.edit().putStringSet("names", newKeys).commit();
            } else {
                Logger.error(TAG, "当前SP内没有播放列表数据 , 无法更改不存在的数据");
                return false;
            }

            //修改旧的基础数据的名称
            final Set<String> newValues = sp.getStringSet(oldName, null);
            if (newValues != null) {
                sp.edit().remove(oldName).putStringSet(newName, newValues).commit();
            } else {
                Logger.error(TAG, "原始播放列表基本数据丢失 , 修改失败");
                return false;
            }

            //获取要改名的对象 , 更改名字
            final PlaylistItem playlistItem = playlists.get(playlists.indexOf(new PlaylistItem(oldName)));
            playlistItem.setName(newName);

            //回调更新数据
            if (changedCallbacks != null) {
                changedCallbacks.onPlaylistDataChanged();
            }

            //更改本地播放列表音频数据储存文件
            File plFile = new File(JSONHandler.folderPath + oldName + ".pl");
            if (plFile.exists()) {
                //先删除与新名字相同的残留文件
                if (new File(JSONHandler.folderPath + newName + ".pl").delete()) {
                    Logger.warnning(TAG, "发现残留文件 , 已删除");
                }
                //如果文件存在 , 则进行重命名操作
                boolean result = plFile.renameTo(new File(JSONHandler.folderPath + newName + ".pl"));
                plFile = null;
                if (result) {
                    Logger.warnning(TAG, "播放列表名称成功修改.  " + oldName + " --> " + newName);
                    return true;
                } else {
                    Logger.error(TAG, "播放列表名称修改失败.  无法重命名本地数据文件.");
                    return false;
                }
            } else {
                //如果文件不存在 , 则创建新文件
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONHandler.savePlaylist(newName, playlistItem.getPlaylist());
                    }
                }).start();
                Logger.warnning(TAG, "播放列表数据原始文件不存在 , 重新创建数据列表.");
                return true;
            }
        } else {
            Logger.error(TAG, "播放列表名称修改失败.  没有找到需求改名的数据");
            return false;
        }
    }

    /**
     * 添加歌曲到播放列表中
     *
     * @param playlistItem 要添加到的播放列表
     * @param songItem     要添加的歌曲对象
     * @return 执行结果
     */
    public boolean addAudio(@NonNull PlaylistItem playlistItem, @NonNull SongItem songItem) {
        if (playlistItem.getPlaylist() == null) {
            //列表没有初始化过
            Logger.error(TAG, "播放列表没有初始化过 , 无法添加");
            return false;
        } else if (playlistItem.getPlaylist().contains(songItem)) {
            //列表内存在相同路径的音频文件
            return false;
        } else {
            playlistItem.getPlaylist().add(songItem);
            return savePlaylist(playlistItem.getName(), playlistItem.getPlaylist());
        }
    }

    /**
     * 异步读取播放列表内的音频数据列表
     *
     * @param callbacks    读取回调
     * @param playlistItem 播放列表对象
     */
    public void loadPlaylistAudiosData(PlaylistLoadingCallbacks callbacks, PlaylistItem playlistItem) {
        if (audioesThread != null && audioesThread.getStatus() != AsyncTask.Status.FINISHED) {
            audioesThread.cancel(true);
            audioesThread = null;
        }
        audioesThread = new GetPlaylistAudiosThread(playlistItem, callbacks);
        audioesThread.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * 检测是否已经存在相同的播放列表
     *
     * @param name 播放列表名字
     * @return 是否存在
     */
    public boolean isPlaylistExisted(String name) {
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setName(name);
        boolean result = playlists.contains(playlistItem);
        playlistItem = null;
        return result;
    }

    /**
     * 单独操作从 SharedPreferences 中获取数组基本数据
     *
     * @param sp  SharedPreferences 对象
     * @param key 要获取的字符段
     * @return 成功时返回数据 , 失败时返回 NULL
     */
    private
    @Nullable
    String[] loadStringArray(@NonNull SharedPreferences sp, @NonNull String key) {
        Set<String> set = sp.getStringSet(key, null);
        if (set != null) {
            try {
                return set.toArray(new String[set.size()]);
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public ArrayList<PlaylistItem> getPlaylists() {
        return playlists;
    }

    public PlaylistItem getPlaylistItem(int position) {
        if (playlists != null && position >= 0 && position < playlists.size()) {
            return playlists.get(position);
        } else {
            return null;
        }
    }

    public int indexOfPlaylistItem(PlaylistItem playlistItem) {
        return playlists.indexOf(playlistItem);
    }

    public interface PlaylistLoadingCallbacks {

        void onPreLoad();

        void onLoadCompleted(PlaylistItem playlistItem, ArrayList<SongItem> data);

        void onLoadFailed();

    }

    public interface PlaylistChangedCallbacks {

        void onPlaylistDataChanged();

    }

    private final class GetPlaylistAudiosThread extends AsyncTask<Integer, Void, ArrayList<SongItem>> {

        private PlaylistItem playlistItem;
        private PlaylistLoadingCallbacks callbacks;

        GetPlaylistAudiosThread(PlaylistItem playlistItem, PlaylistLoadingCallbacks callbacks) {
            this.playlistItem = playlistItem;
            this.callbacks = callbacks;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callbacks != null) {
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
            if (callbacks != null) {
                if (songItems == null) {
                    callbacks.onLoadFailed();
                } else {
                    playlistItem.setPlaylist(songItems);
                    callbacks.onLoadCompleted(playlistItem, songItems);
                }
            }
        }

    }


}
