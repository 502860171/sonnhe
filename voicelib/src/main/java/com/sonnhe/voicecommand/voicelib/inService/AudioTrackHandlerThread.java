package com.sonnhe.voicecommand.voicelib.inService;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.LinkedBlockingDeque;

public class AudioTrackHandlerThread extends HandlerThread implements Handler.Callback {

    // 开始播放
    private static final int MSG_START_PLAY = 1;
    // 停止播放
    private static final int MSG_STOP_PLAY = 2;
    private Handler mMainHandler;
    private Handler mHandler;
    private AudioTrackCallback mCallback;

    private AudioTrack track;// 录音文件播放对象
    private boolean isPlaying = false;// 标记是否正在录音中
    private LinkedBlockingDeque<Object> dataQueue = new LinkedBlockingDeque<>();


    public AudioTrackHandlerThread(AudioTrackCallback mCallback) {
        super("AudioTrackHandlerThread");
        this.mCallback = mCallback;
        mMainHandler = new Handler(Looper.getMainLooper());
        initTrack();
    }

    public interface AudioTrackCallback {
        void playComplete();

        void setDataError();
    }

    @Override
    public boolean quit() {
        release();
        return super.quit();
    }

    public void release() {
        mMainHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        if (track != null) {
            track.release();
            track = null;
        }
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        initHandler();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_START_PLAY:
                startPlayThread();
                break;
            case MSG_STOP_PLAY:
                stopPlayThread();
                break;
        }
        return true;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setData(byte[] bytes) {
        try {
            if (bytes == null || bytes.length <= 0) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.setDataError();
                    }
                });
                return;
            }
            dataQueue.putLast(bytes);
        } catch (InterruptedException e) {
            e.printStackTrace();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.setDataError();
                }
            });
        }
    }

    public void startPlay() {
        initHandler();
        mHandler.obtainMessage(MSG_START_PLAY).sendToTarget();
    }

    public void stopPlay() {
        Log.e("lib->", "stopPlay");
        stopPlayThread();
//        initHandler();
//        mHandler.obtainMessage(MSG_STOP_PLAY).sendToTarget();
    }

    private void initHandler() {
        if (mHandler == null) {
            mHandler = new Handler(this.getLooper(), this);
        }
    }

    private void initTrack() {
        // 获取缓冲 大小
        int frequence = 16000;
        int channelInConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(frequence, channelInConfig, audioEncoding);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, frequence,
                channelInConfig, audioEncoding, bufferSize, AudioTrack.MODE_STREAM);
        track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
    }

    private void startPlayThread() {
        if (track != null && !isPlaying) {
            track.play();
        }
        while (true) {
            if (dataQueue.size() > 0) {
                isPlaying = true;
                byte[] data = (byte[]) dataQueue.pollFirst();
                track.write(data, 0, data.length);
            } else {
                Log.e("lib->", "播放完成");
                if (isPlaying) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.playComplete();
                        }
                    });
                }
                isPlaying = false;
                return;
            }
        }

    }

    private void stopPlayThread() {
        isPlaying = false;
        Log.e("lib->", "stopPlayThread");
        if (track != null) {
            track.stop();
        }
    }
}
