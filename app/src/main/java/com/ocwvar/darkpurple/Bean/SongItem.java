package com.ocwvar.darkpurple.Bean;

import android.net.Uri;

import com.ocwvar.darkpurple.AppConfigs;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Bean
 * Data: 2016/7/9 10:36
 * Project: DarkPurple
 * 音乐信息Bean
 */
public class SongItem {

    //歌曲名
    private String title;
    //专辑名
    private String album;
    //专辑ID
    private long albumID = 0L;
    //专辑封面Uri路径
    private Uri albumCoverUri = null;
    //歌曲作者
    private String artist;
    //歌曲长度 单位:ms
    private long length = 0L;
    //歌曲长度单位 [0]小时  [1]分钟  [2]秒数
    private int[] lengthSet;
    //歌曲文件大小
    private String fileSize;
    //歌曲文件名
    private String fileName;
    //歌曲文件路径
    private String path;
    //歌曲封面混合颜色
    private int paletteColor = AppConfigs.DefaultPaletteColor;
    //是否预先缓存了封面图像
    private boolean haveCover = false;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getAlbumID() {
        return albumID;
    }

    public void setAlbumID(long albumID) {
        this.albumID = albumID;
    }

    public Uri getAlbumCoverUri() {
        return albumCoverUri;
    }

    public void setAlbumCoverUri(Uri albumCoverUri) {
        this.albumCoverUri = albumCoverUri;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int[] getLengthSet() {
        return lengthSet;
    }

    public void setLengthSet(int[] lengthSet) {
        this.lengthSet = lengthSet;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPaletteColor() {
        return paletteColor;
    }

    public void setPaletteColor(int paletteColor) {
        this.paletteColor = paletteColor;
    }

    public boolean isHaveCover() {
        return haveCover;
    }

    public void setHaveCover(boolean haveCover) {
        this.haveCover = haveCover;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SongItem songItem = (SongItem) o;

        return title.equals(songItem.title);

    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }

}
