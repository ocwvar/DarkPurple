package com.ocwvar.darkpurple.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.ocwvar.darkpurple.Activities.SelectMusicActivity;
import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.SongItem;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.CoverImage2File;
import com.ocwvar.darkpurple.Units.Logger;

import java.io.IOException;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/11/5 15:13
 * File Location com.ocwvar.darkpurple.Services
 * 多媒体Notification控制器
 */
class MediaNotification {

    static final int notificationID = 888;
    private static long notificationSwitchSkipTime = 0L;
    private final String TAG = MediaNotification.class.getSimpleName();
    private final Context context;
    private final MediaNotificationReceiver notificationReceiver;
    private MediaSessionCompat sessionCompat;

    MediaNotification(@NonNull Context context) {
        this.notificationReceiver = new MediaNotificationReceiver();
        this.context = context;
        setup();
    }

    /**
     * 获取更新Notification
     *
     * @param songItem    当前在播放序列的对象
     * @param audioStatus 当前的播放状态
     * @return 更新后的Notification
     */
    Notification updateNotification(@Nullable SongItem songItem, @NonNull AudioCore.AudioStatus audioStatus) {

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        final NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle(builder);

        mediaStyle.setShowActionsInCompactView(0, 1);

        mediaStyle.setMediaSession(sessionCompat.getSessionToken());

        //设置风格
        builder.setStyle(mediaStyle);

        //设置背景颜色
        builder.setColor(AppConfigs.Color.DefaultCoverColor);

        //不显示时间
        builder.setShowWhen(false);

        //仅通知一次
        builder.setOnlyAlertOnce(true);

        //设置点击时不取消
        builder.setAutoCancel(false);

        //设置状态栏图标
        builder.setSmallIcon(R.drawable.ic_action_small_icon);

        //设置锁屏是否显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        //设置通知优先度
        builder.setPriority(Notification.PRIORITY_MAX);

        //设置通知类别为服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }

        //点击通知操作
        Intent intent = new Intent(context, SelectMusicActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        //根据播放状态不同 , 设置不同主按钮样式
        builder.addAction(generateAction(R.drawable.ic_media_previous, MediaNotificationReceiver.BUTTON_PREV));
        switch (audioStatus) {
            case Paused:
                builder.addAction(generateAction(R.drawable.ic_media_play, MediaNotificationReceiver.BUTTON_PLAY));
                break;
            case Playing:
                builder.addAction(generateAction(R.drawable.ic_media_pause, MediaNotificationReceiver.BUTTON_PAUSE));
                break;
        }
        builder.addAction(generateAction(R.drawable.ic_media_next, MediaNotificationReceiver.BUTTON_NEXT));

        //builder.addAction(generateAction(R.drawable.ic_media_close,MediaNotificationReceiver.BUTTON_CLOSE));

        Bitmap cover = loadCoverFromMainThread(songItem);
        if (cover == null) {
            cover = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_music_big);
        }

        if (songItem != null) {
            //更新标题
            builder.setContentTitle(songItem.getTitle());
            //更新作者
            builder.setContentText(songItem.getArtist());
            //更新封面
            builder.setLargeIcon(cover);
        } else {
            //更新标题
            builder.setContentTitle(context.getString(R.string.notification_string_empty));
            //更新作者
            builder.setContentText(context.getString(R.string.notification_string_empty));
            //封面设置为空
            builder.setLargeIcon(cover);
        }

        return builder.build();
    }

    /**
     * 生成按钮
     *
     * @param icon         图标资源
     * @param intentAction 按钮产生的广播Action
     * @return 按钮Action
     */
    private NotificationCompat.Action generateAction(int icon, String intentAction) {
        return new NotificationCompat.Action(icon, intentAction, PendingIntent.getBroadcast(context, 0, new Intent(intentAction), PendingIntent.FLAG_CANCEL_CURRENT));
    }

    /**
     * 初始化
     */
    private void setup() {
        sessionCompat = new MediaSessionCompat(this.context.getApplicationContext(), "test", new ComponentName(this.context.getApplicationContext().getPackageName(), HeadsetButtonReceiver.class.getName()), null);
        sessionCompat.setCallback(new MediaSessionCallback());
        sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        sessionCompat.setActive(true);

        context.registerReceiver(notificationReceiver, notificationReceiver.filter);
    }

    /**
     * 结束资源
     */
    void close() {
        sessionCompat.setActive(false);
        sessionCompat.release();
        sessionCompat.setCallback(null);
        context.unregisterReceiver(notificationReceiver);
    }

    /**
     * 读取歌曲封面图像
     *
     * @param songItem 歌曲数据
     * @return 封面图像位图
     */
    private Bitmap loadCoverFromMainThread(@Nullable SongItem songItem) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        if (songItem == null) {
            return null;
        } else if (!TextUtils.isEmpty(songItem.getCustomCoverPath())) {
            return BitmapFactory.decodeFile(songItem.getCustomCoverPath(), options);
        } else if (songItem.getAlbumCoverUri() != null) {
            try {
                return MediaStore.Images.Media.getBitmap(context.getContentResolver(), songItem.getAlbumCoverUri());
            } catch (IOException e) {
                return null;
            }
        } else if (songItem.isHaveCover()) {
            return BitmapFactory.decodeFile(CoverImage2File.getInstance().getNormalCachePath(songItem.getPath()), options);
        } else {
            return null;
        }
    }

    /**
     * Notification 点击接收器
     */
    private class MediaNotificationReceiver extends BroadcastReceiver {

        static final String BUTTON_PLAY = "BUTTON_PLAY";
        static final String BUTTON_PAUSE = "BUTTON_PAUSE";
        static final String BUTTON_NEXT = "BUTTON_NEXT";
        static final String BUTTON_PREV = "BUTTON_PREV";
        static final String BUTTON_CLOSE = "BUTTON_CLOSE";

        public final IntentFilter filter;

        public MediaNotificationReceiver() {
            filter = new IntentFilter();
            filter.addAction(BUTTON_PLAY);
            filter.addAction(BUTTON_PAUSE);
            filter.addAction(BUTTON_NEXT);
            filter.addAction(BUTTON_PREV);
            filter.addAction(BUTTON_CLOSE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            synchronized (this) {
                final long systemTime = System.currentTimeMillis();
                if (systemTime - notificationSwitchSkipTime < 500) {
                    return;
                } else {
                    notificationSwitchSkipTime = System.currentTimeMillis();
                }
            }

            final String action = intent.getAction();
            final Intent toService = new Intent();

            Logger.warnning(TAG, "接收到的事件: " + action);

            switch (action) {
                case BUTTON_PAUSE:
                    toService.setAction(AudioService.NOTIFICATION_PAUSE);
                    break;
                case BUTTON_PLAY:
                    toService.setAction(AudioService.NOTIFICATION_PLAY);
                    break;
                case BUTTON_NEXT:
                    toService.setAction(AudioService.NOTIFICATION_NEXT);
                    break;
                case BUTTON_PREV:
                    toService.setAction(AudioService.NOTIFICATION_PREV);
                    break;
                case BUTTON_CLOSE:
                    toService.setAction(AudioService.NOTIFICATION_CLOSE);
                    break;
            }

            context.sendBroadcast(toService);

        }

    }

    /**
     * MediaSession 回调 (主要用于接收耳机按钮回调)
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

            //接收到耳机事件之后就将事件转发到默认的接收器中

            final Intent intent = new Intent(HeadsetButtonReceiver.fromMediaSession);
            intent.putExtras(mediaButtonEvent.getExtras());
            context.sendBroadcast(intent);
            return true;
        }

    }

}
