package com.sonnhe.voicecommand.voicelib.inService;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.sonnhe.voicecommand.voicelib.model.SemanticResult;
import com.sonnhe.voicecommand.voicelib.model.VoiceResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AudioRecordSemanticHandlerThread extends HandlerThread implements Handler.Callback {

    private static final String APP_FILE_DIRECTORY = File.separator + "voice";
        private static final String URL = "http://www.sonnhe.com:8080";
//    private static final String URL = "http://192.168.3.21:8080";
    private static final String REQUEST_OPENID = "123456789";

    private static final String REQUEST_HTTP_ASR = URL + "/speech/api/voice/asr/v2/";

    // 开始录音
    private static final int MSG_START_RECORD = 1;
    // 停止录音
    private static final int MSG_STOP_RECORD = 2;

    private Handler mMainHandler;
    private Handler mHandler;
    // 录音及发送的回调
    private RecordCallback mRecordCallback;
    // 录音文件的路径
    private String mFilePath;
    // 是否正在录音
    private boolean isRecording = false;
    private Context mContext;
    private AudioRecordManager mAudioRecordManager;

    public interface RecordCallback {
        void startRecordSuccess();

        void startRecordError();

        void savePcmFileError();

        void vadEnd();

//        void sendDataSuccess();

        void sendDataError();

        void responseAsr(VoiceResult asr);

        void responseNlp(String nlp);

        void responseCmd(String cmd, String cmdText);

        void responseError(int code, String message);
    }

    public AudioRecordSemanticHandlerThread(RecordCallback recordCallback, Context context) {
        super("AudioRecordHandlerThread");
        this.mRecordCallback = recordCallback;
        mMainHandler = new Handler(Looper.getMainLooper());
        mContext = context;
        mAudioRecordManager = AudioRecordManager.getInstance();
        mAudioRecordManager.setAudioCallback(new AudioRecordManager.AudioCallback() {
            @Override
            public void vadEnd() {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecordCallback.vadEnd();
                    }
                });
            }
        });
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        initMHandler();
    }

    @Override
    public boolean quit() {
        mAudioRecordManager.destroy();
        return super.quit();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_START_RECORD:
                startRecordThread();
                break;
            case MSG_STOP_RECORD:
                stopRecordThread();
                break;
        }
        return true;
    }

    public void startRecord() {
        initMHandler();
        mHandler.obtainMessage(MSG_START_RECORD).sendToTarget();
    }

    public void stopRecord() {
        initMHandler();
        mHandler.obtainMessage(MSG_STOP_RECORD).sendToTarget();
    }

    private void initMHandler() {
        if (mHandler == null) {
            mHandler = new Handler(this.getLooper(), this);
        }
    }

    private void startRecordThread() {
        try {
            String parentFilePath = createDataDirectory(mContext);
            if (parentFilePath != null) {
//                mFilePath = parentFilePath + File.separator +
//                        DateUtil.getNowToFileName() + ".pcm";
                mFilePath = parentFilePath + File.separator + "asr.pcm";
                isRecording = true;
                mAudioRecordManager.startRecord(mFilePath);
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecordCallback.startRecordSuccess();
                    }
                });
                return;
            }
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordCallback.startRecordError();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordCallback.startRecordError();
                }
            });
        }
    }

    private void stopRecordThread() {
        mAudioRecordManager.stopRecord();
        isRecording = false;
        try {
            File file = new File(mFilePath);
            if (file.exists()) {
                sendVoiceDataToServer(file.getAbsolutePath(), REQUEST_HTTP_ASR);
            } else {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecordCallback.savePcmFileError();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordCallback.savePcmFileError();
                }
            });
        }
    }

    /**
     * 发送语音文件到server
     */
    private void sendVoiceDataToServer(String filePath, String url) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                String result =
                        requestResolve(url, file, REQUEST_OPENID);
                if (!TextUtils.isEmpty(result)) {
                    analysisResult(result, 0);
                } else {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRecordCallback.sendDataError();
                        }
                    });
                }
            } else {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecordCallback.savePcmFileError();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordCallback.sendDataError();
                }
            });
        }
    }

    /**
     * 解析回传结果
     *
     * @param result server回传的json String
     * @param type   0: text 1:tts
     */
    private void analysisResult(String result, int type) throws JSONException {
        JSONObject object = new JSONObject(result);
        final int code = object.getInt("code");
        final String message = object.getString("message");
        if (code == 200) {
            final JSONObject data = object.getJSONObject("data");
            if (type == 0) {
                final String text = data.getString("text");
                if (!data.isNull("cmd")) {
                    final String cmd = data.getString("cmd");
                    final String cmdText = data.getString("text");
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRecordCallback.responseCmd(cmd, cmdText);
                        }
                    });
                }
                final VoiceResult voiceResult = voiceResultHandler(result);
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecordCallback.responseAsr(voiceResult);
                    }
                });
                if (!data.isNull("tts")) {
                    final String tts = data.getString("tts");
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRecordCallback.responseNlp(tts);
                        }
                    });
                }
            } else if (type == 1) {
                final String tts = data.getString("tts");
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecordCallback.responseNlp(tts);
                    }
                });
            }
        } else {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordCallback.responseError(code, message);
                }
            });
        }
    }

    private VoiceResult voiceResultHandler(String json) throws JSONException {
        VoiceResult result = new VoiceResult();
        JSONObject object = new JSONObject(json);
        int code = object.getInt("code");
        String message = object.getString("message");
        result.setCode(code);
        result.setMessage(message);
        if (!object.isNull("data")) {
            JSONObject data = object.getJSONObject("data");
            result.setDataText(data.getString("text"));
            if (!data.isNull("semantic")) {
                result.setDataSemantic(data.getString("semantic"));
                result.setDataFrom(data.getString("from"));
                result.setSemanticTts(data.getString("semanticTts"));
                if (!data.isNull("semanticResult")) {
                    JSONArray semanticResult = data.getJSONArray("semanticResult");
                    List<SemanticResult> list = new ArrayList<>();
                    SemanticResult item;
                    for (int i = 0; i < semanticResult.length(); i++) {
                        JSONObject obj = semanticResult.getJSONObject(i);
                        item = new SemanticResult();
                        item.setName(obj.getString("name"));
                        item.setUrl(obj.getString("url"));
                        list.add(item);
                    }
                    if (list.size() > 0) {
                        result.setSemanticResults(list);
                    }
                }
            }
        }
        return result;
    }

    /**
     * app 内创建 voice 文件夹
     *
     * @return file path
     */
    private String createDataDirectory(Context context) {
        File file = new File(context.getApplicationContext().getFilesDir().getAbsolutePath() + APP_FILE_DIRECTORY);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return file.getAbsolutePath();
            }
        } else {
            return file.getAbsolutePath();
        }
        return null;
    }

    private String requestResolve(String url, File file, String openId) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"),
                file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "file", fileBody)
                .addFormDataPart("openId", openId)
                .addFormDataPart("type", "1")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        int httpCode = response.code();
        if (httpCode == 200) {
            return response.body().string();
        }
        return null;
    }
}
