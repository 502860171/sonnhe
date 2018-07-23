package com.sonnhe.voicecommand.voicelib.inService;

import android.annotation.SuppressLint;
import android.os.Environment;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;

public class SpeechService {
    @SuppressLint("StaticFieldLeak")
    private static SpeechService sSpeechService;

    //    private Context mContext;
    private InitListener mInitListener;

    public static SpeechService getInstance() {
        if (sSpeechService == null) {
            sSpeechService = new SpeechService();
        }
        return sSpeechService;
    }

    private SpeechService() {
//        mContext = context;
        super();
    }

    /**
     * 初始化监听器
     */
    public InitListener getInitListener() {
        if (mInitListener == null) {
            mInitListener = new InitListener() {
                @Override
                public void onInit(int code) {
//                    if (code != ErrorCode.SUCCESS) {
//                        ToastUtil.getInstance(mContext).showTip("初始化失败，错误码：" + code);
//                        Log.e("lib->", "初始化失败，错误码：" + code);
//                    }
                }
            };
        }

        return mInitListener;
    }

    public void setTTSParam(SpeechSynthesizer mTts, String mEngineType) {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoqi");
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "50");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "50");
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
//            /**
//             * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
//             * 开发者如需自定义参数，请参考在线合成参数设置
//             */
        }
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }
}
