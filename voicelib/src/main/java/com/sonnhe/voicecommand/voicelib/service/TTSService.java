package com.sonnhe.voicecommand.voicelib.service;

import android.content.Context;
import android.os.Bundle;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.sonnhe.voicecommand.voicelib.inService.SpeechService;

public class TTSService {

    private static TTSService sTTSService;

    private TTSCallback mCallback;

    // 语音合成对象
    private SpeechSynthesizer mTts;

    private TTSService(Context context, TTSCallback callback) {
        super();
        initTTS(context);
        this.mCallback = callback;
    }

    public interface TTSCallback {
        void onCompleted();
    }

    public static TTSService instance(Context context, TTSCallback callback) {
        if (sTTSService == null) {
            sTTSService = new TTSService(context, callback);
        }
        return sTTSService;
    }

    public void startTTS(String value) {
        startTTSThread(value);
    }

    public void stopTTS() {
        mTts.stopSpeaking();
    }

//    public void destroy() {
//
//    }


    private void startTTSThread(String value) {
        mTts.startSpeaking(value, mTtsListener);
//        int code = mTts.startSpeaking(value, mTtsListener);
//        if (code != ErrorCode.SUCCESS) {
//            Log.e("lib->", "语音合成失败,错误码: " + code);
//        }
    }

    private void initTTS(Context context) {
        SpeechUtility.createUtility(context, "appid=5a094a6b");
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(context, SpeechService
                .getInstance().getInitListener());
        SpeechService.getInstance().setTTSParam(mTts, SpeechConstant.TYPE_CLOUD);
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
//            ToastUtil.getInstance(getApplicationContext()).showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
//            ToastUtil.getInstance(getApplicationContext()).showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
//            ToastUtil.getInstance(getApplicationContext()).showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
//            mPercentForBuffering = percent;
//            ToastUtil.getInstance(getApplicationContext()).showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
//            mPercentForPlaying = percent;
//            if (mPercentForPlaying > 95) {
//                Log.i(Constants.TAG, "播放进度:" + mPercentForPlaying);
//                Log.i(Constants.TAG, "endPos:" + endPos);
//            }
//            ToastUtil.getInstance(getApplicationContext()).showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
//            if (error != null) {
//                Log.e("lib->", error.getPlainDescription(true));
//            }
            mCallback.onCompleted();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };
}
