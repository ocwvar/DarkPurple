package com.ocwvar.darkpurple.Callbacks;

import com.ocwvar.darkpurple.Bean.SongItem;

import java.util.ArrayList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Callbacks
 * Data: 2016/7/9 10:51
 * Project: DarkPurple
 * 音乐扫描器回调接口
 */
public interface MediaScannerCallback {

    void onScanCompleted(ArrayList<SongItem> songItems);

}
