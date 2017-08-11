package com.ocwvar.darkpurple.Services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import com.ocwvar.darkpurple.Activities.MainFrameworkActivity
import com.ocwvar.darkpurple.Bean.SongItem.SONGITEM_KEY_COVER_ID
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.Cover.CoverManager

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 17-8-3 下午12:43
 * File Location com.ocwvar.darkpurple.Services
 * This file use to :   基于MediaSession的MediaStyle Notification
 */
class MediaNotification {

    val NOTIFICATION_ID: Int = 1451

    object ACTIONS {

        /**
         * Notification 动作 ：暂停
         */
        val NOTIFICATION_ACTION_PAUSE: String = "na1"

        /**
         * Notification 动作 ：播放
         */
        val NOTIFICATION_ACTION_PLAY: String = "na2"

        /**
         * Notification 动作 ：上一个媒体数据
         */
        val NOTIFICATION_ACTION_PREVIOUS: String = "na3"

        /**
         * Notification 动作 ：下一个媒体数据
         */
        val NOTIFICATION_ACTION_NEXT: String = "na4"

        /**
         * Notification 动作 ：关闭
         */
        val NOTIFICATION_ACTION_CLOSE: String = "na5"
    }

    fun createNotification(mediaSession: MediaSessionCompat, appContext: Context): Notification? {

        val controller: MediaControllerCompat? = mediaSession.controller
        val playbackState: PlaybackStateCompat? = controller?.playbackState
        val mediaMetadata: MediaMetadataCompat? = controller?.metadata

        if (controller == null || playbackState == null || mediaMetadata == null) {
            return null
        }

        //先检测当前的状态
        val state: Int = playbackState.state
        if (state == PlaybackStateCompat.STATE_ERROR || state == PlaybackStateCompat.STATE_NONE) {
            //不支持的状态
            return null
        }

        //创建Notification构造器
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(appContext)
        builder
                //设置MediaSession
                .setStyle(NotificationCompat.MediaStyle()
                        //设置MediaSession的Token
                        .setMediaSession(mediaSession.sessionToken)
                        //设置显示的Action数量
                        .setShowActionsInCompactView(2)
                )
                //不显示Notification时间
                .setShowWhen(false)
                //状态栏上的图标
                .setSmallIcon(R.drawable.ic_action_small_icon)
                //设置最低隐私程度，允许在锁屏界面操作
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //设置默认优先度
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //设置当Notification被移除时的广播（歌曲暂停时可以被移除）
                .setDeleteIntent(PendingIntent.getBroadcast(appContext, 0, Intent(ACTIONS.NOTIFICATION_ACTION_CLOSE), PendingIntent.FLAG_CANCEL_CURRENT))
                //开始显示时状态栏显示的文字： 艺术家 - 歌曲标题
                .setTicker(String.format("%s - %s", mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST), mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)))
                //主标题
                .setContentTitle(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                //副标题
                .setContentText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                //点击打开主界面
                .setContentIntent(PendingIntent.getActivity(appContext, 0, Intent(appContext, MainFrameworkActivity::class.java).let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    it
                }, PendingIntent.FLAG_CANCEL_CURRENT))
                //添加 上一个 媒体数据按钮
                .addAction(android.R.drawable.ic_media_previous, "Previous", PendingIntent.getBroadcast(appContext, 0, Intent(ACTIONS.NOTIFICATION_ACTION_PREVIOUS), PendingIntent.FLAG_CANCEL_CURRENT))

        if (state == PlaybackStateCompat.STATE_PLAYING) {

            //当前是播放状态，添加 暂停播放 媒体按钮
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", PendingIntent.getBroadcast(appContext, 0, Intent(ACTIONS.NOTIFICATION_ACTION_PAUSE), PendingIntent.FLAG_CANCEL_CURRENT))
        } else if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_STOPPED) {

            //当前是暂停或停止状态，添加 开始播放 媒体按钮
            builder.addAction(android.R.drawable.ic_media_play, "Play", PendingIntent.getBroadcast(appContext, 0, Intent(ACTIONS.NOTIFICATION_ACTION_PLAY), PendingIntent.FLAG_CANCEL_CURRENT))
        }
        //添加 下一个 媒体数据按钮
        builder.addAction(android.R.drawable.ic_media_next, "Next", PendingIntent.getBroadcast(appContext, 0, Intent(ACTIONS.NOTIFICATION_ACTION_NEXT), PendingIntent.FLAG_CANCEL_CURRENT))

        //尝试获取有效封面图像
        BitmapFactory.decodeFile(CoverManager.getValidSource(mediaMetadata.getString(SONGITEM_KEY_COVER_ID)))?.let {
            builder.setLargeIcon(it)
        }


        return builder.build()
    }

}