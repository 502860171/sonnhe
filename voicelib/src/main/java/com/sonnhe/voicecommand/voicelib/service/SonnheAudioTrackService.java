package com.sonnhe.voicecommand.voicelib.service;

import com.sonnhe.voicecommand.voicelib.inService.AudioTrackHandlerThread;

public class SonnheAudioTrackService {

    private AudioTrackCallback mAudioTrackCallback;

    private AudioTrackHandlerThread mAudioTrackHandlerThread = null;

    public interface AudioTrackCallback {
        void playComplete();

        void setDataError();
    }

    public SonnheAudioTrackService(AudioTrackCallback audioTrackCallback){
        mAudioTrackCallback = audioTrackCallback;
        initTrackService();
    }

    private void initTrackService(){
        if (mAudioTrackHandlerThread == null){
            mAudioTrackHandlerThread = new AudioTrackHandlerThread(new AudioTrackHandlerThread.AudioTrackCallback(){

                @Override
                public void playComplete() {
                    mAudioTrackCallback.playComplete();
                }

                @Override
                public void setDataError() {
                    mAudioTrackCallback.setDataError();
                }
            });

        }
        mAudioTrackHandlerThread.getLooper();
        mAudioTrackHandlerThread.start();

    }

    public void setData(byte[] bytes){
        mAudioTrackHandlerThread.setData(bytes);
    }

    public void startPlay(){
        mAudioTrackHandlerThread.startPlay();
    }
}
