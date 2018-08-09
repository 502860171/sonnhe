package com.sonnhe.voicecommand.voicelib.service;

import android.content.Context;

import com.sonnhe.voicecommand.voicelib.inService.AudioTrackHandlerThread;
import com.sonnhe.voicecommand.voicelib.inService.RequestTTSService;

import java.util.Map;

public class SonnheTTSService {

    private RequestCallback mRequestCallback;

    private Context mContext;


    private RequestTTSService mRequestTTSService = null;

    private AudioTrackHandlerThread mAudioTrackHandlerThread = null;

    public interface RequestCallback {
//        void requestSuccess(Map<String, Object> returnMap);

        void requestError(String error);

        void playComplete();

        void setDataError();
    }


    public SonnheTTSService(RequestCallback requestCallback, Context context) {
        mContext = context;
        mRequestCallback = requestCallback;
        initTrackService();
        initTTSService();
    }


    private void initTTSService() {
        if (mRequestTTSService == null) {
            mRequestTTSService = new RequestTTSService(new RequestTTSService.RequestCallback() {
                @Override
                public void requestSuccess(Map<String, Object> returnMap) {
                    byte[] mBytes = (byte[]) returnMap.get("bytes");
                    if (mBytes != null && mBytes.length > 0) {
                        mAudioTrackHandlerThread.setData(mBytes);
                        mAudioTrackHandlerThread.startPlay();
                    }
//                    mRequestCallback.requestSuccess(returnMap);
                }

                @Override
                public void requestError(String error) {
                    mRequestCallback.requestError(error);
                }
            }, mContext);
        }
        mRequestTTSService.getLooper();
        mRequestTTSService.start();
    }

    private void initTrackService() {
        if (mAudioTrackHandlerThread == null) {
            mAudioTrackHandlerThread = new AudioTrackHandlerThread(new AudioTrackHandlerThread.AudioTrackCallback() {

                @Override
                public void playComplete() {
                    mRequestCallback.playComplete();
                }

                @Override
                public void setDataError() {
                    mRequestCallback.setDataError();
                }
            });

        }
        mAudioTrackHandlerThread.getLooper();
        mAudioTrackHandlerThread.start();

    }

    public void requestTTS(String text) {
        mRequestTTSService.requestTTS(text);
    }

//    public void setData(byte[] bytes) {
//        mAudioTrackHandlerThread.setData(bytes);
//    }
//
//    public void startPlay() {
//        mAudioTrackHandlerThread.startPlay();
//    }

    public void stopPlay() {
        mAudioTrackHandlerThread.stopPlay();
    }
}
