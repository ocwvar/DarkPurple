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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstAudioCoverID() {
        return firstAudioCoverID;
    }

    public void setFirstAudioCoverID(String firstAudioCoverID) {
        this.firstAudioCoverID = firstAudioCoverID;
    }

    public int getCounts() {
        if (playlist == null) {
            return count;
        } else {
            return playlist.size();
        }
    }

    public ArrayList<SongItem> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(ArrayList<SongItem> playlist) {
        this.playlist = playlist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlaylistItem that = (PlaylistItem) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
