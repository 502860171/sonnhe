package com.sonnhe.voicecommand.phonehelper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import com.sonnhe.voicecommand.voicelib.model.SemanticResult;
import com.sonnhe.voicecommand.voicelib.service.SonnheTTSService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("OverrideAbstract")
public class    MyNotifiService extends NotificationListenerService {

    private CallBack callback = null;

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    public interface  CallBack{
        void onDataChanged(String data);
    }

//    public MyNotifiService() {
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return new Binder();
//    }

//    /**
//     *创建一个类继承Binder,来进行
//     */
//    public class Binder extends android.os.Binder{
//        public MyNotifiService getService(){
//            return MyNotifiService.this;
//        }
//    }
    private SonnheTTSService mSonnheTTSService;

    private void initTTSService() {
        if (mSonnheTTSService == null) {
            mSonnheTTSService = new SonnheTTSService(new SonnheTTSService.RequestCallback() {
//                @Override
//                public void requestSuccess(Map<String, Object> returnMap) {
//                    byte[] mBytes = (byte[]) returnMap.get("bytes");
//                    if (mBytes != null && mBytes.length > 0) {
//                        mSonnheTTSService.setData(mBytes);
//                        mSonnheTTSService.startPlay();
//                    }
//                }

                @Override
                public void requestError(String error) {
//                    Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
                }

                @Override
                public void playComplete() {
//                    if (ttsIsPlay) {
//                        playUrl();
//                    }
//                    if (guide){
//                        guide = false;
//                        SemanticResult item = resourceUrls.get(0);
//                        mapService.startGuide(item.getStartLoc(), item.getEndLoc());
//                    }
                }

                @Override
                public void setDataError() {
//                    Toast.makeText(mContext, "初始化数据错误", Toast.LENGTH_LONG).show();
                }
            }, this);

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("KEVIN", "Service is started" + "-----");
        initTTSService();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //        super.onNotificationPosted(sbn);
        try {
            //有些通知不能解析出TEXT内容，这里做个信息能判断
            if (sbn.getNotification().tickerText != null) {
//                SharedPreferences sp = getSharedPreferences("msg", MODE_PRIVATE);
//                nMessage = sbn.getNotification().tickerText.toString();
//                Log.e("KEVIN", "Get Message" + "-----" + nMessage);
//                sp.edit().putString("getMsg", nMessage).apply();
//                Message obtain = Message.obtain();
//                obtain.obj = nMessage;
//                mHandler.sendMessage(obtain);
//                init();
//                if (nMessage.contains(data)) {
//                    Message message = handler.obtainMessage();
//                    message.what = 1;
//                    handler.sendMessage(message);
//                    writeData(sdf.format(new Date(System.currentTimeMillis())) + ":" + nMessage);
//                }
                if(sbn.getPackageName().equals("com.tencent.mm")){
//                    Toast.makeText(MyNotifiService.this, sbn.getNotification().tickerText.toString(), Toast.LENGTH_SHORT).show();
                    Log.e("MyNotifiService", sbn.getNotification().tickerText.toString());
                    if(sbn.getNotification().tickerText.toString().contains(":")){
                        String[] parts = sbn.getNotification().tickerText.toString().split(":");
                        String part1 = parts[0]; // name
                        String part2 = parts[1]; // content
                        mSonnheTTSService.requestTTS("收到来自" + part1 + "的消息，" + part2);
                    }

                }
            }else{
                Log.e("MyNotifiService", "内容为空");
            }
        } catch (Exception e) {
            Log.e("MyNotifiService", "不可解析的通知");
        }
    }

//    private void writeData(String str) {
//        try {
////            bw.newLine();
////            bw.write("NOTE");
//            bw.newLine();
//            bw.write(str);
//            bw.newLine();
////            bw.newLine();
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void init() {
//        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        try {
//            FileOutputStream fos = new FileOutputStream(newFile(), true);
//            OutputStreamWriter osw = new OutputStreamWriter(fos);
//            bw = new BufferedWriter(osw);
//        } catch (IOException e) {
//            Log.d("KEVIN", "BufferedWriter Initialization error");
//        }
//        Log.d("KEVIN", "Initialization Successful");
//    }
//
//    private File newFile() {
//        File fileDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "ANotification");
//        fileDir.mkdir();
//        String basePath = Environment.getExternalStorageDirectory() + File.separator + "ANotification" + File.separator + "record.txt";
//        return new File(basePath);
//
//    }
//
//
//    class MyHandler extends Handler {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case 1:
////                    Toast.makeText(MyService.this,"Bingo",Toast.LENGTH_SHORT).show();
//            }
//        }
//
//    }
}
