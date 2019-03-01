package com.sonnhe.voicecommand.phonehelper;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.sonnhe.recordlib.AudioRecordManager;
import com.sonnhe.voicecommand.phonehelper.call.CallService;
import com.sonnhe.voicecommand.phonehelper.call.Cn2Spell;
import com.sonnhe.voicecommand.phonehelper.model.Constant;
import com.sonnhe.voicecommand.voicelib.model.SemanticResult;
import com.sonnhe.voicecommand.voicelib.model.VoiceResult;
import com.sonnhe.voicecommand.voicelib.service.AudioRecordService;
import com.sonnhe.voicecommand.voicelib.service.MediaPlayerService;
import com.sonnhe.voicecommand.voicelib.service.SonnheTTSService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RecordService extends Service{

    private static final String TAG = "RecordService";
    private boolean isRecording = false;

    private AudioRecordService mRecordService = null;
    private MediaPlayerService mMediaPlayerService;
    private AudioRecordManager mAudioRecordManager;
    // tts播放完成后是否进行导航
    private boolean guide = false;
    private boolean telephone = false;
    private String name;
    private MapService mapService;
    private CallService callService;
    private Cn2Spell mSpell;
    // 语音合成对象
    private SonnheTTSService mSonnheTTSService;

    private List<SemanticResult> resourceUrls;

    private RecordCallBack callBack;
    // tts播放完成后 是否 播放 资源
    private boolean ttsIsPlay = false;
    // 用于监听蓝牙按键
    private AudioManager mAudioManager;
    private AutoExceptMsgReceiver autoExcepMstReceiver;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    public class RecordBinder extends Binder{

        public RecordService getService(){
            return RecordService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.e("onCreate","onCreate");
        super.onCreate();
        //注册媒体按键广播接收器
        autoExcepMstReceiver = new AutoExceptMsgReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("deomo.demo.demoaction");
        registerReceiver(autoExcepMstReceiver, filter);
        //初始化
        init();
    }

    @Override
    public void onDestroy() {
        mMediaPlayerService.release();
        mSonnheTTSService.release();
        mAudioRecordManager.stopRecord();
        mAudioRecordManager.destroy();
        super.onDestroy();

    }

    public void init(){
        Log.e("init", "init");
        if(mAudioManager == null){
            mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        }
        initReceiver();
        initBlueToothHeadset();
        initBlueToothA2DP();
        initMediaPlayerManager();
        initRecordManager();
        initTTSService();
        initRecordService();
        mapService = new MapService(this);
        callService = new CallService(this);
        mSpell = new Cn2Spell();


    }


    public int onStartCommand(Intent intent, int flags, int startId) {
//        timeCounts = new TimeCounts(5000, 1000);
        Log.e("onStartCommand","onStartCommand");

        return START_STICKY;
    }

    public void setCallBack(RecordCallBack callBack){
        this.callBack = callBack;
    }

    public void sendClick(){
        mSonnheTTSService.stopPlay();
        if (!isRecording) {
            startRecordAction();
            Log.e("startRecordAction","startRecordAction");
            mediaPlayerStop();
        } else {
            Log.e("stopRecordAction","stopRecordAction");
            stopRecordAction();
        }
    }
    public void connect(){
        Log.e("A2DP", "isBluetoothA2dpOn"+mAudioManager.isBluetoothA2dpOn());
        Log.e("A2DP", "getConnectionState"+mBluetoothA2dp.getConnectionState(bluetoothDevice));
    }

    private BluetoothHeadset bluetoothHeadset;
    private BluetoothDevice bluetoothDevice;
    private List<BluetoothDevice> devices;
    BluetoothProfile.ServiceListener blueHeadsetListener=new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i("blueHeadsetListener", "onServiceDisconnected:"+profile);
            if(profile== BluetoothProfile.HEADSET){
                bluetoothHeadset=null;
            }
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.i("blueHeadsetListener", "onServiceConnected:"+profile);
            if(profile==BluetoothProfile.HEADSET){
                bluetoothHeadset=(BluetoothHeadset) proxy;
                devices = bluetoothHeadset.getConnectedDevices();
                if (devices.size()>0){
                    bluetoothDevice = devices.get(0);
                    int state = bluetoothHeadset.getConnectionState(bluetoothDevice);
                    Log.e("==============","headset state:"+state);
                    if (state==BluetoothHeadset.STATE_CONNECTED){
                        Log.e("=================","bluetooth headset connected");
                    }
//                    if (mAudioManager != null) {
//                        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//                    }
//                    bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
//                    setMediaButtonEvent();
                }else{
                    Toast.makeText(RecordService.this, "未检测到蓝牙设备，请确认连接后重启APP", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothProfile.ServiceListener mListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            if(profile == BluetoothProfile.A2DP){
                mBluetoothA2dp = null;
            }
        }
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if(profile == BluetoothProfile.A2DP){
                mBluetoothA2dp = (BluetoothA2dp) proxy; //转换
            }
        }
    };

    private void initBlueToothHeadset(){
        BluetoothAdapter adapter;
        if(android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){//android4.3之前直接用BluetoothAdapter.getDefaultAdapter()就能得到BluetoothAdapter
            adapter=BluetoothAdapter.getDefaultAdapter();
        }
        else{
            BluetoothManager bm=(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            adapter=bm.getAdapter();
        }
        adapter.getProfileProxy(RecordService.this, blueHeadsetListener, BluetoothProfile.HEADSET);
    }

    private void initBlueToothA2DP(){
        BluetoothAdapter mBtAdapter;
        if(android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){//android4.3之前直接用BluetoothAdapter.getDefaultAdapter()就能得到BluetoothAdapter
            mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        }
        else{
            BluetoothManager bm=(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBtAdapter=bm.getAdapter();
        }
        mBtAdapter.getProfileProxy(RecordService.this, mListener, BluetoothProfile.A2DP);
    }
    //连接到A2DP
    private void connectA2dp(BluetoothDevice device){
        setPriority(bluetoothDevice, 100); //设置priority
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
            Method connectMethod =BluetoothA2dp.class.getMethod("connect",
                    BluetoothDevice.class);
            connectMethod.invoke(mBluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void disConnectA2dp(BluetoothDevice device){
        setPriority(bluetoothDevice, 0);
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），断开连接。
            Method connectMethod =BluetoothA2dp.class.getMethod("disconnect",
                    BluetoothDevice.class);
            connectMethod.invoke(mBluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //设置优先级
    public void setPriority(BluetoothDevice device, int priority) {
        if (mBluetoothA2dp == null) return;
        try {//通过反射获取BluetoothA2dp中setPriority方法（hide的），设置优先级
            Method connectMethod =BluetoothA2dp.class.getMethod("setPriority",
                    BluetoothDevice.class,int.class);
            connectMethod.invoke(mBluetoothA2dp, device, priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private MediaSession mSession;
    private long buttonTime = 0;
    @TargetApi(21)
    private void setMediaButtonEvent()
    {
        if (this.mSession == null)
        {
            this.mSession = new MediaSession(RecordService.this, VoiceMainActivity.class.getSimpleName());
//            PendingIntent localPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, MediaButtonReceiver.class), 0);
//            this.mSession.setMediaButtonReceiver(localPendingIntent);
            this.mSession.setCallback(new MediaSession.Callback()
            {
                public boolean onMediaButtonEvent(@NonNull Intent paramAnonymousIntent)
                {
                    Log.e("test", paramAnonymousIntent.getAction());
//                    KeyEvent a = paramAnonymousIntent.getParcelableExtra("android.intent.extra.KEY_EVENT");
//                    Log.e("test", "event.getKeyCode()=" + a.getKeyCode());
                    if ((System.currentTimeMillis() - buttonTime) > 2000) {
                        //弹出提示，可以有多种方式
                        buttonTime = System.currentTimeMillis();
                    } else {

                        sendClick();
                        callBack.sendMediaButton();
                    }

                    return true;
                }
            });
            this.mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
            this.mSession.setActive(true);
        }
    }

    /**
     * 开始录音事件
     */
    public void startRecordAction() {
        play("hinttts.mp3");
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(800);//休眠
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isRecording = true;

                if(bluetoothHeadset != null) {
                    if (devices.size()>0) {
                        if (!mAudioManager.isBluetoothScoOn()) {
                            bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
                            IntentFilter audioStateFilter = new IntentFilter();
                            audioStateFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                            audioStateFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
                            registerReceiver(new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    String action = intent.getAction();
                                    if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)){
                                        int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,-1);
                                        if (state==BluetoothHeadset.STATE_AUDIO_CONNECTED){
                                            Log.e("ScoOn()==false", "startRecordAction");

                                            mAudioRecordManager.startRecord();
                                            unregisterReceiver(this); // 别遗漏
                                        }
                                    }else{
                                        Log.e("ScoOn()==false", "发生了意料之外的错误");
                                    }
                                }
                            }, audioStateFilter);
                        } else {
                            mAudioRecordManager.startRecord();
                            Log.e("ScoOn()==true", "startRecordAction");
                        }
                    }else{
                        mAudioRecordManager.startRecord();
                        Log.e("bluetoothDevice==null", "startRecordAction");
                    }
                }else{
                    mAudioRecordManager.startRecord();
                    Log.e("bluetoothHeadset!=null", "startRecordAction");
                }
            }
        }.start();





    }

    public void stopRecordAction() {
        isRecording = false;
        mAudioRecordManager.stopRecord();
        mRecordService.stopRecord();
        if(bluetoothHeadset != null){
            bluetoothHeadset.stopVoiceRecognition(bluetoothDevice);
        }
//        disConnectA2dp(bluetoothDevice);
//        connectA2dp(bluetoothDevice);
//        mAudioManager.setSpeakerphoneOn(false);


    }

    public void mDestory(){
        if(bluetoothHeadset != null) {
            bluetoothHeadset.stopVoiceRecognition(bluetoothDevice);
        }
    }

    private class AutoExceptMsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {

                Log.e("ServiceBroadcast", "onButtonReceive");

                sendClick();
                callBack.sendMediaButton();

            }
        }
    }

    //播放assets下的mp3文件
    private void play(String filename) {
        try {
            AssetManager assetManager = this.getAssets();   ////获得该应用的AssetManager
            AssetFileDescriptor afd = assetManager.openFd(filename);   //根据文件名找到文件
            //对mediaPlayer进行实例化
            MediaPlayer mediaPlayer = new MediaPlayer();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.reset();    //如果正在播放，则重置为初始状态
            }
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());     //设置资源目录
            mediaPlayer.prepare();//缓冲
            mediaPlayer.start();//开始或恢复播放
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initMediaPlayerManager() {
        mMediaPlayerService = new MediaPlayerService();
    }


    private void initRecordManager() {
        if(mAudioRecordManager == null){
            mAudioRecordManager = AudioRecordManager.getInstance(new AudioRecordManager.RecordCallBack() {
                @Override
                public void getRecord(byte[] tempBuffer, int bytesRecord) {
                    if(isRecording){
                        mRecordService.startRecord(tempBuffer, bytesRecord);
                    }

                }
            });
        }

    }

    private void initTTSService() {
        if (mSonnheTTSService == null) {
            mSonnheTTSService = new SonnheTTSService(new SonnheTTSService.RequestCallback() {

                @Override
                public void requestError(String error) {
                    Toast.makeText(RecordService.this, error, Toast.LENGTH_LONG).show();
                }

                @Override
                public void playComplete() {
                    if (ttsIsPlay) {
                        playUrl();
                    }
                    if (guide){
                        guide = false;
                        SemanticResult item = resourceUrls.get(0);
                        mapService.startGuide(item.getStartLoc(), item.getEndLoc());
                    }
                    if(telephone){
                        telephone = false;
                        callService.nameNumberCall(name);
                    }

                }

                @Override
                public void setDataError() {
                    Toast.makeText(RecordService.this, "初始化数据错误", Toast.LENGTH_LONG).show();
                }
            }, RecordService.this);

        }
    }
    /**
     * 初始化语音服务
     */
    private void initRecordService() {
        if (mRecordService == null) {
            mRecordService = new AudioRecordService(new AudioRecordService.RecordCallback() {
                @Override
                public void startRecordSuccess() {
//                    Log.i("activity->", "开始录音");
                }

                @Override
                public void startRecordError() {
                    Log.i("activity->", "开始录音失败");
                    callBack.callBackReplyMsg("录音失败");
                }

                @Override
                public void savePcmFileError() {
                    Log.i("activity->", "保存录音文件失败");
                    callBack.callBackReplyMsg("录制语音失败！");
                }

                @Override
                public void vadEnd() {
                    Log.e("vadEnd", "vadEnd");
                    stopRecordAction();
                    callBack.sendMediaButton();
                }

                @Override
                public void sendDataError() {
                    Log.i("activity->", "发送录音文件失败");
                    callBack.callBackReplyMsg("发送请求失败!");
                }

                @Override
                public void responseAsr(VoiceResult asr) {
                    Log.i("activity->", "收到asr回传:" + asr);
                    callBack.callBackAddMsg(asr.getDataText());
                    if (TextUtils.isEmpty(asr.getDataSemantic())) {
                        Log.e("activity->", "没有语义");
                    } else {
                        Log.e("activity->", "有语义");
                        StringBuilder result = new StringBuilder();
                        if (asr.getSemanticResults() != null) {
                            resourceUrls = asr.getSemanticResults();
                            if (resourceUrls.size() > 0) {
                                SemanticResult item = resourceUrls.get(0);
                                if(asr.getDataSemantic().equals("mapU")){
                                    guide = true;
                                    result.append(asr.getSemanticTts());
                                    mSonnheTTSService.requestTTS(asr.getSemanticTts());
                                }else if(asr.getDataSemantic().equals("telephone")){

                                    name = callService.queryPhonetic(item.getValue());
                                    if(name == null){
                                        result.append("没有找到您要拨打的联系人");
                                        mSonnheTTSService.requestTTS("没有找到您要拨打的联系人");
                                    }else{
                                        telephone = true;
                                        result.append("好的，即将为您呼叫" + name);
                                        mSonnheTTSService.requestTTS("好的，即将为您呼叫" + name);
                                    }
                                }else if(asr.getDataSemantic().equals("weixin")){
                                    if(item.getValue().equals("null")){
                                        result.append("请将发送内容一起告诉我哦");
                                        mSonnheTTSService.requestTTS("请将发送内容一起告诉我哦");
                                    }else{
                                        //判断是否开启辅助服务
                                        if (!isAccessibilitySettingsOn(RecordService.this)) {
                                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            mSonnheTTSService.requestTTS("请先打开智能语音助手辅助功能");
                                            Toast.makeText(RecordService.this, "请先打开智能语音助手辅助功能", Toast.LENGTH_SHORT).show();

                                        }else{
                                            Intent intent = new Intent(Intent.ACTION_MAIN);
                                            ComponentName cmp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
                                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.setComponent(cmp);
                                            startActivity(intent);
                                            Constant.flag = 1;
                                            Constant.back = 1;
                                            Constant.wechatId = mSpell.getPinYin(item.getName());
                                            Constant.wechatContext = item.getValue();
                                        }

                                    }
                                }else{
                                    ttsIsPlay = true;
                                    if (TextUtils.isEmpty(asr.getSemanticTts())) {
                                        result.append(item.getName());
                                        playUrl();
                                    } else {
                                        result.append(asr.getSemanticTts());
                                        mediaPlayerStop();
                                        mSonnheTTSService.requestTTS(asr.getSemanticTts());
                                    }
                                }

                            }
                        }
                        callBack.callBackReplyMsg(result.toString());
                    }
                }

                @Override
                public void responseNlp(String nlp) {
                    Log.i("activity->", "收到nlp回传:" + nlp);
                    callBack.callBackReplyMsg(nlp);
                    mSonnheTTSService.requestTTS(nlp);
                    ttsIsPlay = false;
                }

                @Override
                public void responseNlpJson(String json) {
                    Log.i("activity->", "json:" + json);
                }

                @Override
                public void responseCmd(String cmd, String cmdText) {
                    Log.i("activity->", "指令:" + cmd);
                    Log.e("activity->", "是否正在播放:" + mMediaPlayerService.isPlaying());
                    Log.e("activity->", "url:" + mMediaPlayerService.getVideoUrl());
                    Log.e("activity->", "isPause:" + mMediaPlayerService.isPause());
                    if (resourceUrls != null && resourceUrls.size() > 0 && resourceUrls.get(0) != null && !TextUtils.isEmpty(resourceUrls.get(0).getUrl())) {
                        if (cmd.equalsIgnoreCase("replay")) {
                            if (!TextUtils.isEmpty(mMediaPlayerService.getVideoUrl())
                                    && resourceUrls.get(0).getUrl().equals(mMediaPlayerService.getVideoUrl())
                                    && mMediaPlayerService.isPause()) {
                                mMediaPlayerService.play();
                            } else {
                                playUrl();
                            }
                        } else if (cmd.equalsIgnoreCase("pause")) {
                            if (mMediaPlayerService.isPlaying()) {
                                mMediaPlayerService.pause();
                            }
                        }
                    }
                }

                @Override
                public void responseError(int code, String message) {
                    Log.e("activity->", "错误:" + code + " " + message);
                    callBack.callBackReplyMsg(message);
                }
            }, RecordService.this);
            mRecordService.setRequestOpenId("29e08b82&0e56eb43");
            mRecordService.setBase64(false);
            mRecordService.setRequestType(49);
            mRecordService.setRequestUrl("http://ai.sonnhe.com/speech/api/voice/asr");
//            mRecordService.setRequestUrl("http://192.168.3.21:8080/speech/api/voice/asrV2/");
//            mRecordService .setRequestUrl("http://60.205.112.156:8080/xunfeiasr/api/voice/");
        }
    }


    /**
     * 播放 resourceUrls 中的 资源
     */
    private void playUrl() {
        if (resourceUrls != null && resourceUrls.size() > 0) {
            SemanticResult item = resourceUrls.get(0);
            if (item != null && !TextUtils.isEmpty(item.getUrl())) {
                Log.e(Constant.TAG, "应播放:" + item.getUrl());
                Log.e(Constant.TAG, "现在:" + mMediaPlayerService.getVideoUrl());
                if (mMediaPlayerService.isPlaying()) {
                    mMediaPlayerService.stop();
                    if (!mMediaPlayerService.getVideoUrl().equals(item.getUrl())) {
                        mMediaPlayerService.init(item.getUrl());
                        mMediaPlayerService.play();
                    }
                } else {
                    mMediaPlayerService.init(item.getUrl());
                    mMediaPlayerService.play();
                }
            }

        }
    }

    public void mediaPlayerStop() {
        if (mMediaPlayerService != null && mMediaPlayerService.isPlaying()) {
            mMediaPlayerService.stop();
        }
    }

    private void initReceiver(){
        //注册广播接收者监听状态改变
        IntentFilter filter = new IntentFilter(BluetoothA2dp.
                ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("A2DP","onReceive action="+action);
            //A2DP连接状态改变
            if(action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)){
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                Log.i("A2DP","connect state="+state);
            }else if(action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)){
                //A2DP播放状态改变
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                Log.i("A2DP","play state="+state);
            }
        }
    };


    /**
     * 检测辅助功能是否开启
     * @param mContext
     * @return boolean
     */
    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        // TestService为对应的服务
        final String service = getPackageName() + "/" + WeChatService.class.getCanonicalName();
        Log.i(TAG, "service:" + service);
        // com.z.buildingaccessibilityservices/android.accessibilityservice.AccessibilityService
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            // com.z.buildingaccessibilityservices/com.z.buildingaccessibilityservices.TestService
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }

}
