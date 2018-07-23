package com.sonnhe.voicecommand.voicelib.inService;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class AudioRecordManager {
    private AudioRecord mRecorder = null;
    private DataOutputStream mDataOutputStream;
    private Thread recordThread;
    private boolean isStart = false;
    private static AudioRecordManager mInstance;
    private int bufferSize;
    private VADManager vadManager;
    private AudioCallback mAudioCallback;


    private AudioRecordManager() {
        bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        initRecorder();
        if (vadManager == null) {
            vadManager = new VADManager();
            vadManager.setCallback(new VADManager.Callback() {
                @Override
                public void vadEnd() {
                    Log.e("lib->", "收到vad停止指令");
                    if (mAudioCallback != null) {
                        mAudioCallback.vadEnd();
                    }
                }
            });
        }
    }

    private void initRecorder() {
        if (mRecorder == null) {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2);
        }
    }

    public interface AudioCallback {
        void vadEnd();
    }

    public void setAudioCallback(AudioCallback audioCallback) {
        mAudioCallback = audioCallback;
    }

    /**
     * 获取单例引用
     */
    public static AudioRecordManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioRecordManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioRecordManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 销毁线程方法
     */
    private void destroyThread() {
        try {
            isStart = false;
            if (null != recordThread && Thread.State.RUNNABLE == recordThread.getState()) {
                try {
//                    Thread.sleep(500);
                    recordThread.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                    recordThread = null;
                }
            }
            recordThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recordThread = null;
        }
    }

    /**
     * 启动录音线程
     */
    private void startThread() {
        destroyThread();
        isStart = true;
        if (recordThread == null) {
            Log.i("lib->", "启动录音线程:recordThread == null");
            recordThread = new Thread(recordRunnable);
            recordThread.start();
        }
    }

    /**
     * 录音线程
     */
    private Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                int bytesRecord;
                byte[] tempBuffer = new byte[bufferSize * 2];
                mRecorder.startRecording();
                while (isStart) {
                    if (null != mRecorder) {
                        bytesRecord = mRecorder.read(tempBuffer, 0, bufferSize * 2);
                        if (bytesRecord == AudioRecord.ERROR_INVALID_OPERATION || bytesRecord == AudioRecord.ERROR_BAD_VALUE) {
                            continue;
                        }
                        if (bytesRecord != 0 && bytesRecord != -1) {
                            //在此可以对录制音频的数据进行二次处理 比如变声，压缩，降噪，增益等操作
                            //我们这里直接将pcm音频原数据写入文件 这里可以直接发送至服务器 对方采用AudioTrack进行播放原数据
                            mDataOutputStream.write(tempBuffer, 0, bytesRecord);
//                            Log.i(Constant.TAG, "tempBuffer:" + tempBuffer.length);
//                            Log.i(Constant.TAG, "bytesRecord:" + bytesRecord);
                            vadManager.receiveData(tempBuffer, bytesRecord);
                            // 计算音量
//                            int volumn = 0;
//                            for (int i = 0; i < tempBuffer.length; i++) {
//                                volumn += tempBuffer[i] * tempBuffer[i];
//                            }
//                            Log.d(Constant.TAG, String.valueOf(volumn / (float) bytesRecord));
                        } else {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("lib->", e.getMessage());
                e.printStackTrace();
            }
        }
    };

    /**
     * 保存文件
     */
    private void setPath(String path) throws Exception {
        File file = new File(path);
        boolean isDelete = false;
        if (file.exists()) {
            isDelete = file.delete();
        }
        boolean isCreate = file.createNewFile();
        mDataOutputStream = new DataOutputStream(new FileOutputStream(file, true));
        Log.i("lib->", "保存文件完成:" + path);
        Log.i("lib->", "isCreate:" + isCreate);
        Log.i("lib->", "isDelete:" + isDelete);
    }

    /**
     * 启动录音
     */
    public void startRecord(String path) {
        try {
            Log.i("lib->", "启动录音");
            initRecorder();
            setPath(path);
            startThread();
            Log.e("lib->", "start:" + vadManager.startJni());
        } catch (Exception e) {
            Log.e("lib->", "启动录音:" + e);
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        try {
            Log.i("lib->", "停止录音");
            destroyThread();
            closeRecorder();
            if (mDataOutputStream != null) {
                mDataOutputStream.flush();
                mDataOutputStream.close();
            }
            Log.e("lib->", "stop:" + vadManager.stopJni());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("lib->", "停止录音:" + e);
        }
    }

    public void destroy() {
        destroyThread();
        vadManager.destroy();
    }

    private void closeRecorder() {
        if (mRecorder != null) {
            if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                mRecorder.stop();
            }
            if (mRecorder != null) {
                mRecorder.release();
            }
            mRecorder = null;
        }
    }
}
