package com.sonnhe.voicecommand.voicelib.service;

import android.content.Context;

import com.sonnhe.voicecommand.voicelib.inService.AudioRecordSemanticHandlerThread;
import com.sonnhe.voicecommand.voicelib.model.VoiceResult;

public class AudioRecordService {

    // 录音及发送的回调
    private RecordCallback mRecordCallback;

    private Context mContext;

    /**
     * 录音发送service
     */
    private AudioRecordSemanticHandlerThread mAudioRecordSemanticHandlerThread = null;

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

    public AudioRecordService(RecordCallback recordCallback, Context context) {
        mRecordCallback = recordCallback;
        mContext = context;
        initRecordService();
    }

    public void setRequestUrl(String requestUrl) {
        mAudioRecordSemanticHandlerThread.setRequestUrl(requestUrl);
    }

    public void setRequestOpenId(String requestOpenId) {
        mAudioRecordSemanticHandlerThread.setRequestOpenId(requestOpenId);
    }

    private void initRecordService() {
        if (mAudioRecordSemanticHandlerThread == null) {
            mAudioRecordSemanticHandlerThread = new AudioRecordSemanticHandlerThread(new AudioRecordSemanticHandlerThread.RecordCallback() {
                @Override
                public void startRecordSuccess() {
                    mRecordCallback.startRecordSuccess();
                }

                @Override
                public void startRecordError() {
                    mRecordCallback.startRecordError();
                }

                @Override
                public void savePcmFileError() {
                    mRecordCallback.savePcmFileError();
                }

                @Override
                public void vadEnd() {
                    mRecordCallback.vadEnd();
                }

                @Override
                public void sendDataError() {
                    mRecordCallback.sendDataError();
                }

                @Override
                public void responseAsr(VoiceResult asr) {
                    mRecordCallback.responseAsr(asr);
                }

                @Override
                public void responseNlp(String nlp) {
                    mRecordCallback.responseNlp(nlp);
                }

                @Override
                public void responseCmd(String cmd, String cmdText) {
                    mRecordCallback.responseCmd(cmd, cmdText);
                }

                @Override
                public void responseError(int code, String message) {
                    mRecordCallback.responseError(code, message);
                }
            }, mContext);
            mAudioRecordSemanticHandlerThread.getLooper();
            mAudioRecordSemanticHandlerThread.start();
        }
    }

    public void startRecord() {
        mAudioRecordSemanticHandlerThread.startRecord();
    }

    public void stopRecord() {
        mAudioRecordSemanticHandlerThread.stopRecord();
    }
}
