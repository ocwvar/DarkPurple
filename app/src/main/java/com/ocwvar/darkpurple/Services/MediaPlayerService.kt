package com.ocwvar.darkpurple.Services

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-2 下午11:07
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   播放器媒体服务
 */
class MediaPlayerService : MediaBrowserServiceCompat() {

    private val ROOT_ID_OK: String = "_1"
    private val ROOT_ID_DENIED: String = "no"

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if (clientPackageName == this.packageName) {
            return BrowserRoot(this.ROOT_ID_OK, null)
        } else {
            return BrowserRoot(this.ROOT_ID_DENIED, null)
        }
    }
}