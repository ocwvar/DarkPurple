package com.ocwvar.darkpurple.Bean;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Bean
 * Data: 2016/8/12 14:49
 * Project: DarkPurple
 * 播放列表项目
 */
public class PlaylistItem {

    private String name;
    private String firstAudioCoverID;
    private ArrayList<SongItem> playlist = null;
    private int count;

    public PlaylistItem() {
    }

    public PlaylistItem(String name) {
        this.name = name;
    }

    public PlaylistItem(String name, String[] simpleValues) {
        this.name = name;
        for (String value : simpleValues) {
            if (value.startsWith("cID_")) {
                this.firstAudioCoverID = value.replaceFirst("cID_", "");
            } else if (value.startsWith("count_")) {
                this.count = Integer.parseInt(value.replaceFirst("count_", ""));
            }
        }
    }

    /**
     * @return 播放列表名称
     */
    public String getName() {
        return name;
    }

    /**
     * @param name 播放列表名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return 列表第一个媒体资源的封面ID
     */
    public String getFirstAudioCoverID() {
        return firstAudioCoverID;
    }

    /**
     * @param firstAudioCoverID 列表第一个媒体资源的封面ID
     */
    public void setFirstAudioCoverID(String firstAudioCoverID) {
        this.firstAudioCoverID = firstAudioCoverID;
    }

    /**
     * @return 内部数据列表数量
     */
    public int getCounts() {
        if (playlist == null) {
            return this.count;
        } else {
            return this.playlist.size();
        }
    }

    /**
     * @return 播放列表内部媒体资源列表，未初始化则为 NULL
     */
    public ArrayList<SongItem> getPlaylist() {
        return playlist;
    }

    /**
     * 设置列表内的媒体列表数据
     *
     * @param playlist 媒体列表数据
     */
    public void setPlaylist(ArrayList<SongItem> playlist) {
        this.playlist = playlist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return this.hashCode() == o.hashCode();

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
