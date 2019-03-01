package com.sonnhe.voicecommand.phonehelper;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sonnhe.recordlib.AudioRecordManager;
import com.sonnhe.voicecommand.phonehelper.call.CallService;
import com.sonnhe.voicecommand.phonehelper.model.Msg;
import com.sonnhe.voicecommand.phonehelper.service.PermissionsActivity;
import com.sonnhe.voicecommand.phonehelper.service.adapter.MsgAdapter;
import com.sonnhe.voicecommand.voicelib.model.SemanticResult;
import com.sonnhe.voicecommand.voicelib.model.VoiceResult;
import com.sonnhe.voicecommand.voicelib.service.AudioRecordService;
import com.sonnhe.voicecommand.voicelib.service.MediaPlayerService;
import com.sonnhe.voicecommand.voicelib.service.SonnheTTSService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoiceMainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int EXTERNAL_STORAGE_RECORD_AUDIO = 1;
    // 是否已经赋予相应权限
    private boolean isCanRecordAudio = false;

    private Activity mContext;

    private ImageButton mSend;
    private TextView mSendTextView;
    private RecyclerView mMsgRecyclerView;
    private MsgAdapter mAdapter;
    private AnimationDrawable mAnimRecord;
//    private MapService mapService;
//    private CallService callService;
//

    private List<Msg> mMsgList = new ArrayList<>();
    // 是否正在录音
    private boolean isRecording = false;

//    private List<SemanticResult> resourceUrls;
//    // tts播放完成后是否播放资源
//    private boolean ttsIsPlay = false;
//    // tts播放完成后是否进行导航
//    private boolean guide = false;

//    private AudioManager mAudioManager;
//    /**
//     * 录音发送service
//     */
//    private AudioRecordService mRecordService = null;
//    private SonnheTTSService mSonnheTTSService;
//    private MediaPlayerService mMediaPlayerService;
//    private AudioRecordManager mAudioRecordManager;

    private RecordService recordService;

    private PermissionsChecker mPermissionsChecker; // 权限检测器

    private static final int REQUEST_CODE = 0; // 请求码

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    //申请两个权限，录音和文件读写
    //1、首先声明一个数组permissions，将需要的权限都放在里面
    String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    //2、创建一个mPermissionList，逐个判断哪些权限未授予，未授予的权限存储到mPerrrmissionList中
    List<String> mPermissionList = new ArrayList<>();

    private final int mRequestCode = 100;//权限请求码


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_main);
        mContext = VoiceMainActivity.this;
//        requestPermissions();

//        mapService = new MapService(this);
//        callService = new CallService(this);
        ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(
                this,
                MusicIntentReceiver.class));
//        initBlueToothHeadset();
//        if(mAudioManager == null){
//            mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        }
        if (Build.VERSION.SDK_INT >= 23) {//6.0才用动态权限
            initPermission();
        }else{
            Intent intent1 = new Intent(VoiceMainActivity.this, MyNotifiService.class);//启动服务
            startService(intent1);

            Intent intent2 = new Intent(VoiceMainActivity.this, RecordService.class);
            bindService(intent2, conn, Context.BIND_AUTO_CREATE);
            initView();
        }



//        bindService(new Intent(this,MyNotifiService.class),this, Context.BIND_AUTO_CREATE);
    }


    //权限判断和申请
    private void initPermission() {
        Log.e("initPermission", "initPermission");
        mPermissionList.clear();//清空没有通过的权限

        //逐个判断你要的权限是否已经通过
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);//添加还未授予的权限
            }
        }
        Log.e("mPermissionList", String.valueOf(mPermissionList));
        //申请权限
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, permissions, mRequestCode);
        }else{
            //说明权限都已经通过，可以做你想做的事情去
            Intent intent1 = new Intent(VoiceMainActivity.this, MyNotifiService.class);//启动服务
            startService(intent1);

            Intent intent2 = new Intent(VoiceMainActivity.this, RecordService.class);
            bindService(intent2, conn, Context.BIND_AUTO_CREATE);
            initView();
        }
    }


    //请求权限后回调的方法
    //参数： requestCode  是我们自己定义的权限请求码
    //参数： permissions  是我们请求的权限名称数组
    //参数： grantResults 是我们在弹出页面后是否允许权限的标识数组，数组的长度对应的是权限名称数组的长度，数组的数据0表示允许权限，-1表示我们点击了禁止权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (mRequestCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                showPermissionDialog();//跳转到系统设置权限页面，或者直接关闭页面，不让他继续访问
            }else{
                //全部权限通过，可以进行下一步操作。。。
                Intent intent1 = new Intent(VoiceMainActivity.this, MyNotifiService.class);//启动服务
                startService(intent1);

                Intent intent2 = new Intent(VoiceMainActivity.this, RecordService.class);
                bindService(intent2, conn, Context.BIND_AUTO_CREATE);
                initView();
            }
        }

    }


    /**
     * 不再提示权限时的展示对话框
     */
    AlertDialog mPermissionDialog;
    String mPackName = "com.huawei.liwenzhi.weixinasr";

    private void showPermissionDialog() {
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(this)
                    .setMessage("已禁用权限，请手动授予")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();

                            Uri packageURI = Uri.parse("package:" + mPackName);
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关闭页面或者做其他操作
                            cancelPermissionDialog();

                        }
                    })
                    .create();
        }
        mPermissionDialog.show();
    }

    //关闭对话框
    private void cancelPermissionDialog() {
        mPermissionDialog.cancel();
    }


//
//    private void startPermissionsActivity() {
//        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
//    }
//
//    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
//        if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
//            finish();
//        }else{
//            Intent intent1 = new Intent(VoiceMainActivity.this, MyNotifiService.class);//启动服务
//            startService(intent1);
//
//            Intent intent2 = new Intent(VoiceMainActivity.this, RecordService.class);
//            bindService(intent2, conn, Context.BIND_AUTO_CREATE);
//        }
//    }


    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            recordService = ((RecordService.RecordBinder) service).getService();

            recordService.setCallBack(new RecordCallBack() {
                @Override
                public void callBackReplyMsg(String content) {

                    replyMsg(content);
                }
                @Override
                public void callBackAddMsg(String content) {
                    addMsg(content);
                }
                @Override
                public void sendMediaButton() {
                    if (!isRecording) {
                        startRecordAction();
                    } else {
                        stopRecordAction();
                    }
                }
            });
//            requestPermissions();
        }
    };

    @Override
    protected void onDestroy() {
//        mMediaPlayerService.release();
//        mSonnheTTSService.release();
//        mAudioRecordManager.stopRecord();
//        mAudioRecordManager.destroy();
//        bluetoothHeadset.stopVoiceRecognition(bluetoothDevice);
        recordService.mDestory();
        Intent  intent = new Intent(VoiceMainActivity.this, MyNotifiService.class);
        stopService(intent);
        intent.setClass(VoiceMainActivity.this, RecordService.class);
        stopService(intent);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
//        if(bluetoothHeadset != null){
//            bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
//        }
        super.onResume();
//        Log.e("Permissions", String.valueOf(mPermissionsChecker.lacksPermissions(PERMISSIONS)));
//        if (mPermissionsChecker.lacksPermissions(PERMISSIONS)) {
//            startPermissionsActivity();
//        }
    }

    @Override
    protected void onPause() {
//        if(bluetoothHeadset != null){
//            bluetoothHeadset.stopVoiceRecognition(bluetoothDevice);
//        }
        super.onPause();
    }

    private long exitTime = 0;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                //弹出提示，可以有多种方式
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
//        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){
            Log.e("KeyEvent", String.valueOf(keyCode));
//        }

        return super.onKeyDown(keyCode, event);
    }

//    private BluetoothHeadset bluetoothHeadset;
//    private BluetoothDevice bluetoothDevice;
//    BluetoothProfile.ServiceListener blueHeadsetListener=new BluetoothProfile.ServiceListener() {
//
//        @Override
//        public void onServiceDisconnected(int profile) {
//            Log.i("blueHeadsetListener", "onServiceDisconnected:"+profile);
//            if(profile== BluetoothProfile.HEADSET){
//                bluetoothHeadset=null;
//            }
//        }
//
//        @Override
//        public void onServiceConnected(int profile, BluetoothProfile proxy) {
//            Log.i("blueHeadsetListener", "onServiceConnected:"+profile);
//            if(profile==BluetoothProfile.HEADSET){
//                bluetoothHeadset=(BluetoothHeadset) proxy;
//                List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
//                if (devices.size()>0){
//                    bluetoothDevice = devices.get(0);
//                    int state = bluetoothHeadset.getConnectionState(bluetoothDevice);
//                    Log.e("==============","headset state:"+state);
//                    if (state==BluetoothHeadset.STATE_CONNECTED){
//                        Log.e("=================","bluetooth headset connected");
//                    }
//                    if (mAudioManager != null) {
//                        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//                    }
//                    bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
//                    setMediaButtonEvent();
//                }else{
//                    Toast.makeText(mContext, "未检测到蓝牙设备，请确认连接后重启APP", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//    };
//    private void initBlueToothHeadset(){
//        BluetoothAdapter adapter;
//        if(android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){//android4.3之前直接用BluetoothAdapter.getDefaultAdapter()就能得到BluetoothAdapter
//            adapter=BluetoothAdapter.getDefaultAdapter();
//        }
//        else{
//            BluetoothManager bm=(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//            adapter=bm.getAdapter();
//        }
//        adapter.getProfileProxy(this, blueHeadsetListener, BluetoothProfile.HEADSET);
//    }
//
//
//    private MediaSession mSession;
//    private long buttonTime = 0;
//    @TargetApi(21)
//    private void setMediaButtonEvent()
//    {
//        if (this.mSession == null)
//        {
//            this.mSession = new MediaSession(this, VoiceMainActivity.class.getSimpleName());
////            PendingIntent localPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, MediaButtonReceiver.class), 0);
////            this.mSession.setMediaButtonReceiver(localPendingIntent);
//            this.mSession.setCallback(new MediaSession.Callback()
//            {
//                public boolean onMediaButtonEvent(@NonNull Intent paramAnonymousIntent)
//                {
//                    Log.e("test", paramAnonymousIntent.getAction());
////                    KeyEvent a = paramAnonymousIntent.getParcelableExtra("android.intent.extra.KEY_EVENT");
////                    Log.e("test", "event.getKeyCode()=" + a.getKeyCode());
//                    if ((System.currentTimeMillis() - buttonTime) > 2000) {
//                        //弹出提示，可以有多种方式
//                        buttonTime = System.currentTimeMillis();
//                    } else {
//
//                        sendClick();
//                    }
//
//                    return true;
//                }
//            });
//            this.mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
//            this.mSession.setActive(true);
//        }
//    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case EXTERNAL_STORAGE_RECORD_AUDIO:
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    isCanRecordAudio = true;
////                    recordService.init();
//                } else {
//                    isCanRecordAudio = false;
//                }
//                break;
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send_btn:
                sendClick();
//                recordService.startSco();
                break;
        }
        switch (view.getId()) {
            case R.id.info_btn:
//                recBufSize = AudioRecord.getMinBufferSize(frequency,
//                        AudioFormat.CHANNEL_IN_MONO, audioEncoding);
//
//                playBufSize=AudioTrack.getMinBufferSize(frequency,
//                        AudioFormat.CHANNEL_OUT_MONO, audioEncoding);
//                // -----------------------------------------
//
//                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
//                        AudioFormat.CHANNEL_OUT_MONO, audioEncoding,
//                        playBufSize, AudioTrack.MODE_STREAM);
//                new RecordPlayThread().start();


//                recordService.connect();



                Intent intent_s = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent_s);

//                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                if (mAudioManager != null) {
//                    mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//                }
//                bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
//                setMediaButtonEvent();
                break;
        }
    }

    private void requestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                            },
                            EXTERNAL_STORAGE_RECORD_AUDIO);
                } else {
                    isCanRecordAudio = true;
//                    recordService.init();
                }
            } else {
                isCanRecordAudio = true;
//                recordService.init();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        findViewById(R.id.info_btn).setOnClickListener(this);
        findViewById(R.id.left_btn).setOnClickListener(this);
        findViewById(R.id.left_btn).setVisibility(View.GONE);
        mSend = findViewById(R.id.send_btn);
        mSend.setOnClickListener(this);
        mSendTextView = findViewById(R.id.send_text_view);
        mMsgRecyclerView = findViewById(R.id.msg_recycler_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        mMsgRecyclerView.setLayoutManager(manager);

        mAdapter = new MsgAdapter(mMsgList);
        mMsgRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnClickListener(new MsgAdapter.OnClickCallBack() {
            @Override
            public void callback(int position, String text) {

                recordService.mediaPlayerStop();
            }
        });
    }

//    private void initRecordManager() {
//        if(mAudioRecordManager == null){
//            mAudioRecordManager = AudioRecordManager.getInstance(new AudioRecordManager.RecordCallBack() {
//                @Override
//                public void getRecord(byte[] tempBuffer, int bytesRecord) {
//                    if(isRecording){
//                        mRecordService.startRecord(tempBuffer, bytesRecord);
//                    }
//
//                }
//            });
//        }
//
//    }
//
//
//    private void initTTSService() {
//        if (mSonnheTTSService == null) {
//            mSonnheTTSService = new SonnheTTSService(new SonnheTTSService.RequestCallback() {
////                @Override
////                public void requestSuccess(Map<String, Object> returnMap) {
////                    byte[] mBytes = (byte[]) returnMap.get("bytes");
////                    if (mBytes != null && mBytes.length > 0) {
////                        mSonnheTTSService.setData(mBytes);
////                        mSonnheTTSService.startPlay();
////                    }
////                }
//
//                @Override
//                public void requestError(String error) {
//                    Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
//                }
//
//                @Override
//                public void playComplete() {
//                    if (ttsIsPlay) {
//                        playUrl();
//                    }
//                    if (guide){
//                        guide = false;
//                        SemanticResult item = resourceUrls.get(0);
//                        mapService.startGuide(item.getStartLoc(), item.getEndLoc());
//                    }
//                }
//
//                @Override
//                public void setDataError() {
//                    Toast.makeText(mContext, "初始化数据错误", Toast.LENGTH_LONG).show();
//                }
//            }, mContext);
//
//        }
//    }
//
//    private void initMediaPlayerManager() {
//        mMediaPlayerService = new MediaPlayerService();
//    }
//
//    private void mediaPlayerStop() {
//        if (mMediaPlayerService != null && mMediaPlayerService.isPlaying()) {
//            mMediaPlayerService.stop();
//        }
//    }
//
//    /**
//     * 初始化语音服务
//     */
//    private void initRecordService() {
//        if (mRecordService == null) {
//            mRecordService = new AudioRecordService(new AudioRecordService.RecordCallback() {
//                @Override
//                public void startRecordSuccess() {
//                    Log.i("activity->", "开始录音");
//                }
//
//                @Override
//                public void startRecordError() {
//                    Log.i("activity->", "开始录音失败");
//                    replyMsg("录音失败");
//                }
//
//                @Override
//                public void savePcmFileError() {
//                    Log.i("activity->", "保存录音文件失败");
//                    replyMsg("录制语音失败！");
//                }
//
//                @Override
//                public void vadEnd() {
//                    stopRecordAction();
//                }
//
//                @Override
//                public void sendDataError() {
//                    Log.i("activity->", "发送录音文件失败");
//                    replyMsg("发送请求失败!");
//                }
//
//                @Override
//                public void responseAsr(VoiceResult asr) {
//                    Log.i("activity->", "收到asr回传:" + asr);
//                    addMsg(asr.getDataText());
//                    if (TextUtils.isEmpty(asr.getDataSemantic())) {
//                        Log.e("activity->", "没有语义");
//                    } else {
//                        Log.e("activity->", "有语义");
//                        StringBuilder result = new StringBuilder();
//                        if (asr.getSemanticResults() != null) {
//                            resourceUrls = asr.getSemanticResults();
//                            if (resourceUrls.size() > 0) {
//                                SemanticResult item = resourceUrls.get(0);
//                                if(asr.getDataSemantic().equals("mapU")){
//                                    guide = true;
//                                    result.append(asr.getSemanticTts());
//                                    mSonnheTTSService.requestTTS(asr.getSemanticTts());
//                                }else if(asr.getDataSemantic().equals("telephone")){
//                                    if(!callService.queryPhonetic(item.getValue())){
//                                        result.append("没有找到您要拨打的联系人");
//                                        mSonnheTTSService.requestTTS("没有找到您要拨打的联系人");
//                                    }
//                                }else{
//                                    ttsIsPlay = true;
//                                    if (TextUtils.isEmpty(asr.getSemanticTts())) {
//                                        result.append(item.getName());
//                                        playUrl();
//                                    } else {
//                                        result.append(asr.getSemanticTts());
//                                        mediaPlayerStop();
//                                        mSonnheTTSService.requestTTS(asr.getSemanticTts());
//                                    }
//                                }
//
//                            }
//                        }
//                        replyMsg(result.toString());
//                    }
//                }
//
//                @Override
//                public void responseNlp(String nlp) {
//                    Log.i("activity->", "收到nlp回传:" + nlp);
//                    replyMsg(nlp);
//                    mSonnheTTSService.requestTTS(nlp);
//                    ttsIsPlay = false;
//                }
//
//                @Override
//                public void responseNlpJson(String json) {
//                    Log.i("activity->", "json:" + json);
//                }
//
//                @Override
//                public void responseCmd(String cmd, String cmdText) {
//                    Log.i("activity->", "指令:" + cmd);
//                    Log.e("activity->", "是否正在播放:" + mMediaPlayerService.isPlaying());
//                    Log.e("activity->", "url:" + mMediaPlayerService.getVideoUrl());
//                    Log.e("activity->", "isPause:" + mMediaPlayerService.isPause());
//                    if (resourceUrls != null && resourceUrls.size() > 0 && resourceUrls.get(0) != null && !TextUtils.isEmpty(resourceUrls.get(0).getUrl())) {
//                        if (cmd.equalsIgnoreCase("replay")) {
//                            if (!TextUtils.isEmpty(mMediaPlayerService.getVideoUrl())
//                                    && resourceUrls.get(0).getUrl().equals(mMediaPlayerService.getVideoUrl())
//                                    && mMediaPlayerService.isPause()) {
//                                mMediaPlayerService.play();
//                            } else {
//                                playUrl();
//                            }
//                        } else if (cmd.equalsIgnoreCase("pause")) {
//                            if (mMediaPlayerService.isPlaying()) {
//                                mMediaPlayerService.pause();
//                            }
//                        }
//                    }
//                }
//
//                @Override
//                public void responseError(int code, String message) {
//                    Log.e("activity->", "错误:" + code + " " + message);
//                    replyMsg(message);
//                }
//            }, mContext);
//            mRecordService.setRequestOpenId("29e08b82&0e56eb43");
//            mRecordService.setBase64(false);
//            mRecordService.setRequestType(49);
//            mRecordService.setRequestUrl("http://ai.sonnhe.com/speech/api/voice/asr");
////            mRecordService.setRequestUrl("http://192.168.3.21:8080/speech/api/voice/asrV2/");
////            mRecordService .setRequestUrl("http://60.205.112.156:8080/xunfeiasr/api/voice/");
//        }
//    }

//    /**
//     * 播放 resourceUrls 中的 资源
//     */
//    private void playUrl() {
//        if (resourceUrls != null && resourceUrls.size() > 0) {
//            SemanticResult item = resourceUrls.get(0);
//            if (item != null && !TextUtils.isEmpty(item.getUrl())) {
//                Log.e("activity->", "应播放:" + item.getUrl());
//                Log.e("activity->", "现在:" + mMediaPlayerService.getVideoUrl());
//                Log.e("activity->", "这里002:" + mMediaPlayerService.isPlaying());
//                if (mMediaPlayerService.isPlaying()) {
//                    mMediaPlayerService.stop();
//                    if (!mMediaPlayerService.getVideoUrl().equals(item.getUrl())) {
//                        mMediaPlayerService.init(item.getUrl());
//                        mMediaPlayerService.play();
//                    }
//                } else {
//                    mMediaPlayerService.init(item.getUrl());
//                    mMediaPlayerService.play();
//                }
//            }
//
//        }
//    }

    private void sendClick() {
//        if (isCanRecordAudio) {
            Log.e("activity->", "send-click");
            recordService.sendClick();
            if (!isRecording) {
                startRecordAction();
            } else {
                stopRecordAction();
            }
//        } else {
//            requestPermissions();
//        }

    }

//    private MyNotifiService.Binder binder = null;
//
//    @Override
//    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//        Log.e("onServiceConnected", "onServiceConnected: ");
//        binder = (MyNotifiService.Binder) iBinder;
//
//        binder.getService().setCallback(new MyNotifiService.CallBack() {
//            @Override
//            public void onDataChanged(String data) {//因为在Service里面赋值data是在Thread中进行的，所以我们不能直接在这里将返回的值展示在TextView上。
//                mSonnheTTSService.requestTTS(data);
//            }
//        });
//    }
//
//    @Override
//    public void onServiceDisconnected(ComponentName componentName) {
//
//    }


    /**
     * 开始录音事件
     */
    private void startRecordAction() {
        isRecording = true;
        playAnimRecord();
        recordAlert();
//        startRecord();
//        mediaPlayerStop();
//        mAudioRecordManager.startRecord();
    }

    /**
     * 停止录音事件
     */
    private void stopRecordAction() {
        isRecording = false;
        stopAnimRecord();
        stopRecordAlert();
//        stopRecord();
//        mAudioRecordManager.stopRecord();
    }

    /**
     * 播放录音动画
     */
    private void playAnimRecord() {
        mSend.setBackgroundResource(R.drawable.anim_record);
        Object object = mSend.getBackground();
        mAnimRecord = (AnimationDrawable) object;
        mAnimRecord.stop();
        mAnimRecord.start();
    }

    /**
     * 停止录音动画
     */
    private void stopAnimRecord() {
        mSend.setBackgroundResource(R.drawable.play_00);
        if (mAnimRecord == null) {
            return;
        }
        mAnimRecord.stop();
    }

    /**
     * 录音中提示（关闭)
     */
    private void recordAlert() {
        mSendTextView.setText(getString(R.string.stop));
    }

    /**
     * 准备开始录音提示(开始)
     */
    private void stopRecordAlert() {
        mSendTextView.setText(getString(R.string.send));
    }

//    /**
//     * 开始录音
//     */
//    private void startRecord() {
//        mRecordService.startRecord();
//    }
//
//    /**
//     * 停止录音
//     */
//    private void stopRecord() {
//        mRecordService.stopRecord();
//    }

    /**
     * 添加一条msg(右侧)
     */
    private void replyMsg(String content) {
        if (!TextUtils.isEmpty(content)) {
            Log.e("activity->", "添加一条msg(右侧):" + content);
            Msg msg = new Msg(content, Msg.TYPE_RECEIVED);
            mMsgList.add(msg);
            mAdapter.notifyItemInserted(mMsgList.size() - 1);
            // 将RecyclerView定位到最后一行
            mMsgRecyclerView.scrollToPosition(mMsgList.size() - 1);
        }
    }

    /**
     * 添加一条msg(左侧)
     */
    private void addMsg(String content) {
        if (!TextUtils.isEmpty(content)) {
            Log.e("activity->", "添加一条msg(左侧):" + content);
            Msg msg = new Msg(content, Msg.TYPE_SENT);
            mMsgList.add(msg);
            mAdapter.notifyItemInserted(mMsgList.size() - 1);
            // 将RecyclerView定位到最后一行
            mMsgRecyclerView.scrollToPosition(mMsgList.size() - 1);
        }
    }

}
