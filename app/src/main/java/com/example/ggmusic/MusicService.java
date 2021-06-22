package com.example.ggmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.io.IOException;


public class MusicService extends Service {
    MediaPlayer mMediaPlayer;
    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "Music channel";
    NotificationManager mNotificationManager;

    //13
    private final IBinder mBinder = new MusicServiceBinder();
    private MusicService mService;
    private boolean mBound = false;


    //以下是自己加的：
//    public static final String title =
//            "com.glriverside.xgqin.ggmusic.TITLE";
//    public static final String artist =
//            "com.glriverside.xgqin.ggmusic.ARTIST";

    @Override
    public void onDestroy() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        super.onDestroy();
    }
    public MusicService() {
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        String data = intent.getStringExtra(MainActivity.DATA_URI);
        Uri dataUri = Uri.parse(data);

        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(
                        getApplicationContext(),
                        dataUri);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                //13 5.3
                Intent musicStartIntent = new Intent(MainActivity.ACTION_MUSIC_START);
                sendBroadcast(musicStartIntent);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//A 可以判断当前系统的API版本，如果其大于Build.VERSION_CODES.O则表明需要构造Notification Channel。
            mNotificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Music Channel", NotificationManager.IMPORTANCE_HIGH);

            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent =
                new Intent(getApplicationContext(),
                        MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0, notificationIntent, 0);

        NotificationCompat.Builder builder =//B 通过NotificationCompat.Builder构造Notification对象。NotificationCompat.Builder提供了构造一个通知所需的所有方法
                new NotificationCompat.Builder(
                        getApplicationContext(),
                        CHANNEL_ID);

        Bundle bundle = intent.getExtras();//接受intent
        String title = bundle.getString("com.glriverside.xgqin.ggmusic.TITLE");//获取信息
        String artist = bundle.getString("com.glriverside.xgqin.ggmusic.ARTIST");//获取信息

        Notification notification = builder
                .setContentTitle(title)//设置title
                .setContentText(artist)//设置artist
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent).build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);//将当前服务以前台服务形式运行
        return super.onStartCommand(intent, flags, startId);
    }

    //13
    public class MusicServiceBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    /** method for clients */
    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public void play() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    public int getDuration() {
        int duration = 0;

        if (mMediaPlayer != null) {
            duration = mMediaPlayer.getDuration();
        }

        return duration;
    }

    public int getCurrentPosition() {
        int position = 0;

        if (mMediaPlayer != null) {
            position = mMediaPlayer.getCurrentPosition();
        }

        return position;
    }

    public boolean isPlaying() {

        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }



}
