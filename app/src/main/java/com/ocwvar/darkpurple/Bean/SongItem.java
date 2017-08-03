package com.ocwvar.darkpurple.Bean;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import com.ocwvar.darkpurple.AppConfigs;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Bean
 * Data: 2016/7/9 10:36
 * Project: DarkPurple
 * 音乐信息Bean
 */
public class SongItem implements Parcelable {

    public static final Creator<SongItem> CREATOR = new Creator<SongItem>() {
        @Override
        public SongItem createFromParcel(Parcel in) {
            return new SongItem(in);
        }

        @Override
        public SongItem[] newArray(int size) {
            return new SongItem[size];
        }
    };

    //自定义Key
    public static final String SONGITEM_KEY_FILE_NAME = "kfn";
    public static final String SONGITEM_KEY_FILE_PATH = "kfp";
    public static final String SONGITEM_KEY_COVER_ID = "kci";

    //此音频数据唯一标识
    private final String UID;

    //媒体数据
    private final MediaMetadataCompat mediaMetadataCompat;

    public SongItem(final String UID, final MediaMetadataCompat mediaMetadataCompat) {
        this.UID = UID;
        this.mediaMetadataCompat = mediaMetadataCompat;
    }

    protected SongItem(Parcel in) {
        this.UID = in.readString();
        this.mediaMetadataCompat = in.readParcelable(MediaMetadataCompat.class.getClassLoader());
    }

    /**
     * 毫秒转 小时,分钟,秒数
     *
     * @param length 毫秒长度
     * @return 数据数组 [0]小时  [1]分钟  [2]秒数
     */
    private int[] getTimes(long length) {
        if (length < 1000) {
            return null;
        }

        //用于存放数据的数组
        int[] time = new int[3];
        //总毫秒转秒数
        length = length / 1000;

        if ((length / 60) > 60) {
            // 有小时数
            int hours = (int) (length / 60 / 60);
            time[0] = hours;
            //求减去已统计的小时数的毫秒数
            length -= hours * 60 * 60;
        } else {
            //如果没有则设为 0
            time[0] = 0;
        }

        if ((length / 60) > 0) {
            //有分钟数
            int mins = (int) (length / 60);
            time[1] = mins;
            //求减去已统计的小时数的毫秒数
            length -= mins * 60;
        } else {
            time[1] = 0;
        }

        time[2] = (int) (length);

        return time;
    }

    public final
    @NonNull
    MediaMetadataCompat getMediaMetadata() {
        if (this.mediaMetadataCompat == null) {
            return new MediaMetadataCompat.Builder()
                    .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                    .putText(MediaMetadataCompat.METADATA_KEY_TITLE, AppConfigs.UNKNOWN)
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, AppConfigs.UNKNOWN)
                    .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, Uri.EMPTY.toString())
                    .build();
        } else {
            return mediaMetadataCompat;
        }
    }

    public
    @NonNull
    String getTitle() {
        final String result = this.mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        return TextUtils.isEmpty(result) ? AppConfigs.UNKNOWN : result;
    }

    public
    @NonNull
    String getAlbum() {
        final String result = this.mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        return TextUtils.isEmpty(result) ? AppConfigs.UNKNOWN : result;
    }

    public
    @NonNull
    String getArtist() {
        final String result = this.mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        return TextUtils.isEmpty(result) ? AppConfigs.UNKNOWN : result;
    }

    public long getDuration() {
        return this.mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
    }

    public
    @NonNull
    String getFileName() {
        final String result = this.mediaMetadataCompat.getString(SONGITEM_KEY_FILE_NAME);
        return TextUtils.isEmpty(result) ? AppConfigs.UNKNOWN : result;
    }

    public
    @NonNull
    String getPath() {
        final String result = this.mediaMetadataCompat.getString(SONGITEM_KEY_FILE_PATH);
        return TextUtils.isEmpty(result) ? AppConfigs.UNKNOWN : result;
    }

    public
    @NonNull
    String getCoverID() {
        final String result = this.mediaMetadataCompat.getString(SONGITEM_KEY_COVER_ID);
        return TextUtils.isEmpty(result) ? "" : result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return this.UID.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.UID);
        parcel.writeParcelable(this.mediaMetadataCompat, 0);
    }
}
