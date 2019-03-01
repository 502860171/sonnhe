package com.sonnhe.recordlib;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;


public class AudioRecordManager {
    private int bytesRecord;
    private byte[] tempBuffer;
    private RecordCallBack mRecordCallBack;
    private AudioRecord mRecorder = null;
    private Handler mMainHandler;
    private Thread recordThread;
    private boolean isStart = false;
    private static AudioRecordManager mInstance;
    private int bufferSize;

    public interface RecordCallBack{

        void getRecord(byte[] tempBuffer, int bytesRecord);

    }


    private AudioRecordManager(RecordCallBack recordCallBack) {
        mRecordCallBack = recordCallBack;
        mMainHandler = new Handler(Looper.getMainLooper());
        bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        tempBuffer = new byte[8000];
        initRecorder();
    }

    private void initRecorder() {
        if (mRecorder == null) {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, 8000);
        }
    }
//
//    public interface AudioCallback {
//        void vadEnd();
//    }
//
//    public void setAudioCallback(AudioCallback audioCallback) {
//        mAudioCallback = audioCallback;
//    }

    /**
     * 获取单例引用
     */
    public static AudioRecordManager getInstance(RecordCallBack recordCallBack) {
        if (mInstance == null) {
            synchronized (AudioRecordManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioRecordManager(recordCallBack);
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
//        recBufSize = AudioRecord.getMinBufferSize(frequency,
//                AudioFormat.CHANNEL_IN_MONO, audioEncoding);
//
//        playBufSize=AudioTrack.getMinBufferSize(frequency,
//                AudioFormat.CHANNEL_OUT_MONO, audioEncoding);
//        // -----------------------------------------
//
////        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
////                AudioFormat.CHANNEL_OUT_MONO, audioEncoding,
////                playBufSize, AudioTrack.MODE_STREAM);
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
//                AudioFormat.CHANNEL_IN_MONO, audioEncoding, 8000);
//
//        recordThread = new RecordPlayThread();
//        recordThread.start();
    }

    boolean isRecording = false;//是否录放的标记
    static final int frequency = 16000;//44100;
    static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int recBufSize,playBufSize;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
    int bufferReadResult;
    private byte[] buffer;
    class RecordPlayThread extends Thread {
        public void run() {
            try {

                buffer = new byte[8000];
                audioRecord.startRecording();//开始录制
//                audioTrack.play();//开始播放
                Log.e("recording", "startRecord");
                if (null != audioRecord) {
                    while (isStart) {
                        //从MIC保存数据到缓冲区
                        bufferReadResult = audioRecord.read(buffer, 0,
                                8000);
                        Log.e("recording", Arrays.toString(buffer));
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mRecordCallBack.getRecord(buffer, bufferReadResult);
                            }
                        });
//                        byte[] tmpBuf = new byte[bufferReadResult];
//
//                        System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
//                        //写入数据即播放
//                        audioTrack.write(tmpBuf, 0, tmpBuf.length);


                    }
                    audioRecord.stop();
                }else{
                    Log.e("err", "null == audioRecord");
                }
            } catch (Throwable t) {
                Log.e("RecordService", "t.getMessage()");
            }
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

                mRecorder.startRecording();
                while (isStart) {
                    if (null != mRecorder) {
                        bytesRecord = mRecorder.read(tempBuffer, 0, 8000);
                        Log.e("RecordBuffer", Arrays.toString(tempBuffer));
                        if (bytesRecord == AudioRecord.ERROR_INVALID_OPERATION || bytesRecord == AudioRecord.ERROR_BAD_VALUE) {
                            Log.e("err", "run: ");
                            continue;
                        }
                        if (bytesRecord != 0 && bytesRecord != -1) {
                            //在此可以对录制音频的数据进行二次处理 比如变声，压缩，降噪，增益等操作
                            //我们这里直接将pcm音频原数据写入文件 这里可以直接发送至服务器 对方采用AudioTrack进行播放原数据
                            //mDataOutputStream.write(tempBuffer, 0, bytesRecord);
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mRecordCallBack.getRecord(tempBuffer, bytesRecord);
                                }
                            });
                        } else {
                            Log.e("err", String.valueOf(bytesRecord));
                            break;
                        }
                    }else{
                        Log.e("err", "null == mRecorder");
                    }
                }
            } catch (Exception e) {
                Log.e("lib->", "error");
                e.printStackTrace();
            }
        }
    };

//    /**
//     * 保存文件
//     */
//    private void setPath(String path) throws Exception {
//        File file = new File(path);
//        boolean isDelete = false;
//        if (file.exists()) {
//            isDelete = file.delete();
//        }
//        boolean isCreate = file.createNewFile();
//        mDataOutputStream = new DataOutputStream(new FileOutputStream(file, true));
//        Log.i("lib->", "保存文件完成:" + path);
//        Log.i("lib->", "isCreate:" + isCreate);
//        Log.i("lib->", "isDelete:" + isDelete);
//    }

    /**
     * 启动录音
     */
    public void startRecord() {
        try {
            Log.i("lib->", "启动录音");
            initRecorder();
//            setPath(path);
            startThread();
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
//            if (mDataOutputStream != null) {
//                mDataOutputStream.flush();
//                mDataOutputStream.close();
//            }
//            Log.e("lib->", "stop:" + vadManager.stopJni());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("lib->", "停止录音:" + e);
        }
    }

    public void destroy() {
        destroyThread();
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
