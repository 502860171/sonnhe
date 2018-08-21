package com.sonnhe.voicecommand.voicelib.inService;

import cn.thinkit.libtmfe.test.JNI;

public class VADManager {

    static {
        System.loadLibrary("tmfe30");
    }

    //前端引擎
    private JNI mEngine;
    private Callback mCallback;

    private boolean isVadEnd = false;

    public interface Callback {
        void vadEnd();
    }

    VADManager() {
        super();
        mEngine = new JNI();
        int ret = mEngine.mfeSetParam(JNI.PARAM_OFFSET, 1);
        mEngine.mfeSetParam(JNI.PARAM_SPEECH_MODE, 0);
        mEngine.mfeSetParam(JNI.PARAM_SPEECH_END, 80);
        mEngine.mfeSetCallbackDatLen(1280);
        mEngine.mfeInit(16000, JNI.MFE_FORMAT_PCM_16K);
//        if (ret != JNI.MFE_SUCCESS) {
//            Log.e("lib.vad->", "MFE Engine Init failed. Error code is " + ret);
//        } else {
//            ret = mEngine.mfeOpen();
//            if (ret != JNI.MFE_SUCCESS) {
//                Log.e("lib.vad->", "MFE Engine Open failed. Error code is "
//                        + ret);
//            }
//        }
//        Log.e("lib.vad->", "jni init " + ret);
        if (ret == JNI.MFE_SUCCESS) {
            mEngine.mfeOpen();
        }
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void receiveData(byte[] bytes, int length) {
        mEngine.mfeSendDataByte(bytes, length);
        int detect_flag = mEngine.mfeDetect();
//        System.out.println("**********detect_flag is 02:" + detect_flag);
        if (detect_flag >= 2) {
            if (mCallback != null) {
                if (!isVadEnd) {
                    isVadEnd = true;
                    mCallback.vadEnd();
                }
            }
        }
    }

    public int startJni() {
        isVadEnd = false;
        return mEngine.mfeStart();
    }

    public int stopJni() {
        isVadEnd = false;
        return mEngine.mfeStop();
    }

    public void destroy() {
        mEngine.mfeStop();
        mEngine.mfeClose();
        mEngine.mfeExit();
    }
}
