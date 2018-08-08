package com.sonnhe.voicecommand.voicelib.inService;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestTTSService extends HandlerThread implements Handler.Callback {

    private static final int MSG_REQUEST = 1;

    private Handler mMainHandler;
    private Handler mHandler;
    private RequestCallback mCallback;
    private Context mContext;


    public RequestTTSService(RequestCallback callback, Context context) {
        super("RequestTTSService");
        this.mCallback = callback;
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public interface RequestCallback {
        void requestSuccess(Map<String, Object> returnMap);

        void requestError(String error);
    }

    @Override
    protected void onLooperPrepared() {
        initMHandler();
        super.onLooperPrepared();
    }

    @Override
    public boolean quit() {
        return super.quit();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_REQUEST:
                String text = message.getData().getString("tts");
                requestTTSThread(text);
                break;
        }
        return true;
    }


    public void requestTTS(String text) {
        initMHandler();
        Message message = new Message();
        message.what = MSG_REQUEST;
        Bundle bundle = new Bundle();
        bundle.putString("tts", text);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private void initMHandler() {
        if (mHandler == null) {
            mHandler = new Handler(this.getLooper(), this);
        }
    }

    private void requestTTSThread(String text) {
        if (TextUtils.isEmpty(text)) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.requestError("tts内容为空");
                }
            });
            return;
        }
        try {
            String responseBody = requestTTSToServer(text);
            if (TextUtils.isEmpty(responseBody)) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.requestError("发送tts失败");
                    }
                });
            }
            Log.e("lib->", "responseBody:" + responseBody);
            analysisResult(responseBody);
        } catch (IOException e) {
            e.printStackTrace();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.requestError("发送tts失败");
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.requestError("解析文档失败");
                }
            });
        }
    }

    private String requestTTSToServer(String text) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        RequestBody requestBody = new FormBody.Builder()
                .add("text", text)
                .add("openId", "123456789")
                .build();
        Request request = new Request.Builder()
                .url("http://www.sonnhe.com/ttsParse/api/translate/")
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        int httpCode = response.code();
        if (httpCode == 200) {
            return Objects.requireNonNull(response.body()).string();
        }
        return null;
    }

    private void analysisResult(String result) throws JSONException, IOException {
        JSONObject object = new JSONObject(result);
        int code = object.getInt("code");
        final String message = object.getString("message");
        Log.i("lib->", "code:" + code);
        Log.i("lib->", "message:" + message);
        if (code == 200) {
            final Map<String, Object> returnMap = new HashMap<>();
            JSONObject data = object.getJSONObject("data");
            String text = data.getString("text");
            String base64Text = data.getString("base64Text");
            byte[] bytes = null;
            if (!TextUtils.isEmpty(base64Text)) {
                bytes = Base64.decode(base64Text, Base64.DEFAULT);
            }
            if (bytes == null || bytes.length == 0) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.requestError("base64转换错误");
                    }
                });
                return;
            }
            if (!createTTSFile(bytes)) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.requestError("创建本地文件失败");
                    }
                });
                return;
            }
            returnMap.put("code", code);
            returnMap.put("message", message);
            returnMap.put("text", text);
            returnMap.put("bytes", bytes);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.requestSuccess(returnMap);
                }
            });
        } else {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.requestError(message);
                }
            });
        }
    }

    private boolean createTTSFile(byte[] bytes) throws IOException {
        if (!TextUtils.isEmpty(String.valueOf(mContext.getFilesDir()))) {
            File file = new File(String.valueOf(mContext.getFilesDir()) + "export.wav");
            Log.e("lib->", String.valueOf(mContext.getFilesDir()) + "export.wav");
            OutputStream output = new FileOutputStream(file);
            BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
            bufferedOutput.write(bytes);
            return true;
        }
        return false;
    }

//    /**
//     * 创建sd卡，sonnhe根目录
//     */
//    private String createSDParentPath() throws IOException {
//        String sdDir = getSDPath();
//        if (!TextUtils.isEmpty(sdDir)) {
//            File sdFile = new File(sdDir + File.separator + "sonnhe" +
//                    File.separator + "tts");
//            if (!sdFile.exists()) {
//                if (!sdFile.mkdirs()) {
//                    return null;
//                }
//                return sdFile.getCanonicalPath();
//            } else {
//                return sdFile.getCanonicalPath();
//            }
//        }
//        return null;
//    }

//    /**
//     * 获得sd根目录 path
//     */
//    private String getSDPath() throws IOException {
//        File sdDir;
//        // 判断sd卡是否存在
//        boolean sdExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
//        if (sdExist) {
//            // 获取根目录
//            sdDir = Environment.getExternalStorageDirectory();
//            return sdDir.getCanonicalPath();
//        }
//        return "";
//    }
}
