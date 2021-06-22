package com.example.ggmusic;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MusicService mService;
    private boolean mBound=false;
    private Boolean mPlayStatus=true;
    private ImageView ivPlay;
    private ContentResolver mContentResolver;
    private ListView mPlaylist;
    private MediaCursorAdapter mCursorAdapter;
    public static final int UPDATE_PROGRESS=1;
    private ProgressBar pbProgress;
    public static final String ACTION_MUSIC_STOP="com.glriverside.xgqin.ggmusic.ACTION_MUSIC_STOP";

    public static final String ACTION_MUSIC_START="com.glriverside.xgqin.ggmusic.ACTION_MUSIC_START";
    private MusicReceiver musicReceiver;
    public static final String TITLE="com.com.glriverside.xgqin.ggmusic.ARTIST";
    public static final String ARTIST="com.com.glriverside.xgqin.ggmusic.TITLE";
    public static String DATA_URI="com.glriverside.xgqin.ggmusic.DATA_URI";
    private final String SELECTION= MediaStore.Audio.Media.IS_MUSIC+" = ?"+" AND "+MediaStore.Audio.Media.MIME_TYPE+" LIKE ? ";
    private final String[] SELECTION_ARGS={
            Integer.toString(1),
            "audio/mpeg"
    };

    //第十一个项目
    private static final String TAG = "MainActivity";//调试log
    private BottomNavigationView navigation;
    private TextView tvBottomTitle;//底部title
    private TextView tvBottomArtist;
    private ImageView ivAlbumThumbnail;//缩略图


    private MediaPlayer mMediaPlayer=null;

    /*2.1定义所需申请权限数组，
                定义所需申请的权限数组以及requestCode，这两个参数将在requestPermissions方法中使用*/
    private final int REQUEST_EXTERNAL_STORAGE=1;
    private static String[] PERMISSIONS_STORAGE={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public class MusicReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context,Intent intent){
            if(mService!=null){

                pbProgress.setMax(mService.getDuration());
                new Thread(new MusicProgressRunnable()).start();
            }
        }
    }
    private Handler mHandler=new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE_PROGRESS:
                    int position=msg.arg1;
                    pbProgress.setProgress(position);
                    break;
                default:
                    break;
            }
        }
    };
    private class MusicProgressRunnable implements Runnable{
        public MusicProgressRunnable(){
        }
        @Override
        public void run(){
            boolean mThreadWorking =true;
            while(mThreadWorking){
                try{
                    if(mService!=null){
                        int position=mService.getCurrentPosition();
                        Message message=new Message();
                        message.what=UPDATE_PROGRESS;
                        message.arg1=position;
                        mHandler.sendMessage(message);
                    }
                    mThreadWorking=mService.isPlaying();
                    Thread.sleep(100);
                }catch(InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }
    }


    private ServiceConnection mConn =new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder){
            MusicService.MusicServiceBinder binder=(MusicService.MusicServiceBinder)iBinder;
            mService=binder.getService();
            mBound=true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName){
            mService=null;
            mBound=false;
        }
    };

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent=new Intent(MainActivity.this,MusicService.class);
        bindService(intent,mConn,Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop(){
        unbindService(mConn);
        mBound=false;
        super.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        navigation=findViewById(R.id.navigation);
        LayoutInflater.from(MainActivity.this).inflate(R.layout.bottom_media_toolbar,navigation,true);
        ivPlay=navigation.findViewById(R.id.iv_play);
        tvBottomTitle=navigation.findViewById(R.id.tv_bottom_title);
        tvBottomArtist=navigation.findViewById(R.id.tv_bottom_artist);
        ivAlbumThumbnail=navigation.findViewById(R.id.iv_thumbnail);
        pbProgress=navigation.findViewById(R.id.progress);

        if(ivPlay!=null)
        {
            ivPlay.setOnClickListener(MainActivity.this);
        }
        mPlaylist=findViewById(R.id.lv_playlist);
        mPlaylist.setOnItemClickListener(itemClickListener);
        navigation.setVisibility(View.GONE);// 意思是不可见的，不占用原来的布局空间

        //步骤5适配器
        mContentResolver=getContentResolver();
        mCursorAdapter=new MediaCursorAdapter(MainActivity.this);
        mPlaylist.setAdapter(mCursorAdapter);
        //2.2检测所需权限申请
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            //app没有权限，需要申请
            //如果App之前请求过此权限但用户拒绝了请求，此方法将返回true；如果用户在过去拒绝了权限请求，
            // 并在权限请求系统对话框中选择了【Don’t ask again】选项，此方法将返回false。
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)) {}else{
                requestPermissions(PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        }else{
            initPlaylist();//App已经获得权限，可直接进行相应操作
        }

        musicReceiver=new MusicReceiver();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(ACTION_MUSIC_START);
        intentFilter.addAction(ACTION_MUSIC_STOP);
        registerReceiver(musicReceiver,intentFilter);

    }
    @Override
    protected void onDestroy(){
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    private void initPlaylist(){
        //5.3使用ContentResolver查询数据, 该方法是在权限检测及权限申请时两种情况下被调用的
        Cursor mCursor=mContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                SELECTION,
                SELECTION_ARGS,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
        mCursorAdapter.swapCursor(mCursor);
        mCursorAdapter.notifyDataSetChanged();
    }

    //2.3 在MainActivity中处理权限申请结果
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {

        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPlaylist();
                }
                break;
            default:
                break;
        }
    }
    private ListView.OnItemClickListener itemClickListener=new ListView.OnItemClickListener(){
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onItemClick(AdapterView<?> adapterView,View view,int i,long l){
            Cursor cursor=mCursorAdapter.getCursor();//所需获取的多媒体音乐信息在Cursor对象中
            if(cursor!=null&&cursor.moveToPosition(i)){
                int titleIndex=cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int artistIndex=cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumIdIndex=cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

                int dataIndex=cursor.getColumnIndex(MediaStore.Audio.Media.DATA);



                String title=cursor.getString(titleIndex);
                String artist=cursor.getString(artistIndex);
                Long albumId=cursor.getLong(albumIdIndex);
                String data=cursor.getString(dataIndex);
                navigation.setVisibility(View.VISIBLE);
                Uri dataUri=Uri.parse(data);//获取URI，然后开始使用MediaPlayer播放

                //第十二个项目
                Intent serviceIntent=new Intent(MainActivity.this,MusicService.class);
                //通过Intent对象传递当前播放的歌曲名、歌手名等信息给MusicService
                serviceIntent.putExtra(MainActivity.DATA_URI,data);
                serviceIntent.putExtra(MainActivity.TITLE,title);
                serviceIntent.putExtra(MainActivity.ARTIST,artist);
                startForegroundService(serviceIntent);
                if(tvBottomTitle!=null){
                    tvBottomTitle.setText(title);
                }
                if(tvBottomArtist!=null){
                    tvBottomArtist.setText(artist);
                }
                //构造albumURI
                Uri albumUri= ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,albumId);
                Cursor albumCursor=mContentResolver.query(//获取albumCursor游标对象
                        albumUri,
                        null,
                        null,
                        null,
                        null
                );

//                if(albumCursor!=null&&albumCursor.getCount()>0) {
//                    albumCursor.moveToFirst();
//                    int albumArtIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
//                    String albumArt = albumCursor.getString(albumArtIndex);
//                    //Log.d(TAG, albumArt == null ? "null" : albumArt);
//                    Glide.with(MainActivity.this).load(albumArt).into(ivAlbumThumbnail);
//                    albumCursor.close();
//                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    try {
                        int width = (int) MainActivity.this.getResources().getDisplayMetrics().density * 40;
                        int height = (int) MainActivity.this.getResources().getDisplayMetrics().density * 40;

                        Bitmap albumBitmap = mContentResolver.loadThumbnail(albumUri, new Size(width, height), null);
                        Glide.with(MainActivity.this).load(albumBitmap).into(ivAlbumThumbnail);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                else {
                    cursor = mContentResolver.query(
                            albumUri,
                            null,
                            null,
                            null,
                            null
                    );

                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int albumArtIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);//专辑封面图
                        String albumArt = cursor.getString(albumArtIndex);

                        Log.d(TAG, "albumArt: " + albumArt);

                        Glide.with(MainActivity.this).load(albumArt).into(ivAlbumThumbnail);//通过Glide图形加载库进行加载
                        cursor.close();
                    }
                }



                if(mMediaPlayer!=null){
                    try{
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(MainActivity.this,dataUri);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    }catch(IOException ex){
                        ex.printStackTrace();
                    }
                }
            }
        }
    };


    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.iv_play){
            mPlayStatus=!mPlayStatus;
            if(mPlayStatus==true){
                mService.play();
                ivPlay.setImageResource(R.drawable.ic_pause_circle_outline_24dp);
            }else{
                mService.pause();
                ivPlay.setImageResource(R.drawable.ic_play_circle_outline_24dp);
            }
        }
    }
}
