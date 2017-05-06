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
import android.media.AudioManager;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.ocwvar.darkpurple.Activities.SelectMusicActivity;
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
 * 多媒体Notification控制器  （非 MediaSession方式）
 */
class MediaNotificationCompact {

    private static long notificationSwitchSkipTime = 0L;
    private final String TAG = MediaNotificationCompact.class.getSimpleName();
    private final Context context;
    private final MediaNotificationReceiver notificationReceiver;
    private final RemoteViews bigRemoteViews, normalRemoteViews;
    private final AudioManager audioManager;

    MediaNotificationCompact(@NonNull Context context) {
        this.notificationReceiver = new MediaNotificationReceiver();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.context = context;
        this.bigRemoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_mediastyle_big);
        this.normalRemoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_mediastyle_normal);
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

        //设置View
        builder.setCustomContentView(normalRemoteViews);
        builder.setCustomBigContentView(bigRemoteViews);

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

        Bitmap cover = loadCoverFromMainThread(songItem);
        if (cover == null) {
            cover = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_music_big);
        }

        switch (audioStatus) {
            case Paused:
            case Stopped:
                bigRemoteViews.setImageViewResource(R.id.notification_main, R.drawable.ic_media_play);
                normalRemoteViews.setImageViewResource(R.id.notification_main, R.drawable.ic_media_play);
                break;
            case Playing:
                bigRemoteViews.setImageViewResource(R.id.notification_main, R.drawable.ic_media_pause);
                normalRemoteViews.setImageViewResource(R.id.notification_main, R.drawable.ic_media_pause);
                break;
        }

        //更新封面
        bigRemoteViews.setImageViewBitmap(R.id.notification_cover, cover);
        normalRemoteViews.setImageViewBitmap(R.id.notification_cover, cover);

        if (songItem != null) {
            //更新标题
            bigRemoteViews.setTextViewText(R.id.notification_title, songItem.getTitle());
            normalRemoteViews.setTextViewText(R.id.notification_title, songItem.getTitle());
            //更新作者
            bigRemoteViews.setTextViewText(R.id.notification_artist, songItem.getArtist());
            normalRemoteViews.setTextViewText(R.id.notification_artist, songItem.getArtist());

        } else {
            final String emptyMsg = context.getString(R.string.notification_string_empty);
            //更新标题
            bigRemoteViews.setTextViewText(R.id.notification_title, emptyMsg);
            normalRemoteViews.setTextViewText(R.id.notification_title, emptyMsg);
            //更新作者
            bigRemoteViews.setTextViewText(R.id.notification_artist, "");
            normalRemoteViews.setTextViewText(R.id.notification_artist, "");
        }

        return builder.build();
    }

    /**
     * 初始化，设置Notification广播接收器和RemoteViews的点击事件
     */
    @SuppressWarnings("deprecation")
    private void setup() {
        context.registerReceiver(notificationReceiver, notificationReceiver.filter);
        //使用旧版耳机按钮监听方法
        audioManager.registerMediaButtonEventReceiver(new ComponentName(context.getPackageName(), HeadsetButtonReceiver.class.getName()));


        bigRemoteViews.setOnClickPendingIntent(R.id.notification_previous, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_PREV), 0));
        bigRemoteViews.setOnClickPendingIntent(R.id.notification_main, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_MAIN), 0));
        bigRemoteViews.setOnClickPendingIntent(R.id.notification_next, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_NEXT), 0));
        bigRemoteViews.setOnClickPendingIntent(R.id.notification_cancel, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_CLOSE), 0));


        normalRemoteViews.setOnClickPendingIntent(R.id.notification_previous, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_PREV_N), 0));
        normalRemoteViews.setOnClickPendingIntent(R.id.notification_main, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_MAIN_N), 0));
        normalRemoteViews.setOnClickPendingIntent(R.id.notification_next, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_NEXT_N), 0));
        normalRemoteViews.setOnClickPendingIntent(R.id.notification_cancel, PendingIntent.getBroadcast(context, 0, new Intent(MediaNotificationReceiver.BUTTON_CLOSE_N), 0));
    }

    /**
     * 结束资源
     */
    void close() {
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
            return BitmapFactory.decodeFile(songItem.getCustomCoverPath().subSequence(7, songItem.getCustomCoverPath().length()).toString(), options);
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

        //带 “_N” 的是给Normal样式的RemoteViews用的广播Action
        static final String BUTTON_MAIN_N = "BUTTON_MAIN_N";
        static final String BUTTON_MAIN = "BUTTON_MAIN";
        static final String BUTTON_NEXT_N = "BUTTON_NEXT_N";
        static final String BUTTON_NEXT = "BUTTON_NEXT";
        static final String BUTTON_PREV_N = "BUTTON_PREV_N";
        static final String BUTTON_PREV = "BUTTON_PREV";
        static final String BUTTON_CLOSE_N = "BUTTON_CLOSE_N";
        static final String BUTTON_CLOSE = "BUTTON_CLOSE";

        public final IntentFilter filter;

        public MediaNotificationReceiver() {
            filter = new IntentFilter();
            filter.addAction(BUTTON_MAIN_N);
            filter.addAction(BUTTON_MAIN);
            filter.addAction(BUTTON_NEXT_N);
            filter.addAction(BUTTON_NEXT);
            filter.addAction(BUTTON_PREV_N);
            filter.addAction(BUTTON_PREV);
            filter.addAction(BUTTON_CLOSE_N);
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
                case BUTTON_MAIN_N:
                case BUTTON_MAIN:
                    final AudioCore.AudioStatus audioStatus = ServiceHolder.getInstance().getService().getAudioStatus();
                    switch (audioStatus) {
                        case Paused:
                        case Stopped:
                            toService.setAction(AudioService.NOTIFICATION_PLAY);
                            break;
                        case Playing:
                            toService.setAction(AudioService.NOTIFICATION_PAUSE);
                            break;
                    }
                    break;
                case BUTTON_NEXT_N:
                case BUTTON_NEXT:
                    toService.setAction(AudioService.NOTIFICATION_NEXT);
                    break;
                case BUTTON_PREV_N:
                case BUTTON_PREV:
                    toService.setAction(AudioService.NOTIFICATION_PREV);
                    break;
                case BUTTON_CLOSE_N:
                case BUTTON_CLOSE:
                    toService.setAction(AudioService.NOTIFICATION_CLOSE);
                    break;
            }

            context.sendBroadcast(toService);

        }

    }

}
