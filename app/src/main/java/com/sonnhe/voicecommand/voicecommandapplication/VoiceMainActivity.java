package com.sonnhe.voicecommand.voicecommandapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.sonnhe.voicecommand.voicecommandapplication.model.Msg;
import com.sonnhe.voicecommand.voicecommandapplication.service.adapter.MsgAdapter;

import com.sonnhe.voicecommand.voicelib.model.SemanticResult;
import com.sonnhe.voicecommand.voicelib.model.VoiceResult;
import com.sonnhe.voicecommand.voicelib.service.AudioRecordService;
import com.sonnhe.voicecommand.voicelib.service.MediaPlayerService;
import com.sonnhe.voicecommand.voicelib.service.TTSService;

import java.util.ArrayList;
import java.util.List;

public class VoiceMainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int EXTERNAL_STORAGE_RECORD_AUDIO = 1;
    // 是否已经赋予相应权限
    private boolean isCanRecordAudio = false;

    private Activity mContext;

    private ImageButton mSend;
    private TextView mSendTextView;
    private RecyclerView mMsgRecyclerView;
    private MsgAdapter mAdapter;
    private AnimationDrawable mAnimRecord;

    private List<Msg> mMsgList = new ArrayList<>();
    // 是否正在录音
    private boolean isRecording = false;

    private List<SemanticResult> resourceUrls;
    // tts播放完成后 是否 播放 资源
    private boolean ttsIsPlay = false;
    /**
     * 录音发送service
     */
    private AudioRecordService mRecordService = null;
    private TTSService mTTSService = null;
    private MediaPlayerService mMediaPlayerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_main);
        mContext = VoiceMainActivity.this;
        requestPermissions();
        initView();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mediaPlayerStop();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isCanRecordAudio = true;
                    initMediaPlayerManager();
                    initRecordService();
                    initTTSService();
                } else {
                    isCanRecordAudio = false;
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send_btn:
                sendClick();
                break;
        }
    }

    private void requestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                            EXTERNAL_STORAGE_RECORD_AUDIO);
                } else {
                    isCanRecordAudio = true;
                    initMediaPlayerManager();
                    initRecordService();
                    initTTSService();
                }
            } else {
                isCanRecordAudio = true;
                initMediaPlayerManager();
                initRecordService();
                initTTSService();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        findViewById(R.id.left_btn).setOnClickListener(this);
        findViewById(R.id.left_btn).setVisibility(View.GONE);
        mSend = findViewById(R.id.send_btn);
        mSend.setOnClickListener(this);
        mSendTextView = findViewById(R.id.send_text_view);
        mMsgRecyclerView = findViewById(R.id.msg_recycler_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        mMsgRecyclerView.setLayoutManager(manager);

        mAdapter = new MsgAdapter(mMsgList);
        mMsgRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnClickListener(new MsgAdapter.OnClickCallBack() {
            @Override
            public void callback(int position, String text) {
            }
        });
    }

    private void initTTSService() {
        if (mTTSService == null) {
            mTTSService = TTSService.instance(mContext, new TTSService.TTSCallback() {
                @Override
                public void onCompleted() {
                    Log.e("activity->", "tts播放完成");
                    if (ttsIsPlay) {
                        playUrl();
                    }
                }
            });
        }
    }

    private void initMediaPlayerManager() {
        mMediaPlayerService = new MediaPlayerService();
    }

    private void mediaPlayerStop() {
        if (mMediaPlayerService != null && mMediaPlayerService.isPlaying()) {
            mMediaPlayerService.stop();
        }
    }

    /**
     * 初始化语音服务
     */
    private void initRecordService() {
        if (mRecordService == null) {
            mRecordService = new AudioRecordService(new AudioRecordService.RecordCallback() {
                @Override
                public void startRecordSuccess() {
                    Log.i("activity->", "开始录音");
                }

                @Override
                public void startRecordError() {
                    Log.i("activity->", "开始录音失败");
                    replyMsg("录音失败");
                }

                @Override
                public void savePcmFileError() {
                    Log.i("activity->", "保存录音文件失败");
                    replyMsg("录制语音失败！");
                }

                @Override
                public void vadEnd() {
                    stopRecordAction();
                }

                @Override
                public void sendDataError() {
                    Log.i("activity->", "发送录音文件失败");
                    replyMsg("发送请求失败!");
                }

                @Override
                public void responseAsr(VoiceResult asr) {
                    Log.i("activity->", "收到asr回传:" + asr);
                    addMsg(asr.getDataText());
                    if (TextUtils.isEmpty(asr.getDataSemantic())) {
                        Log.e("activity->", "没有语义");
                    } else {
                        Log.e("activity->", "有语义");
                        StringBuilder result = new StringBuilder();
                        if (asr.getSemanticResults() != null) {
                            resourceUrls = asr.getSemanticResults();
                            if (resourceUrls.size() > 0) {
                                ttsIsPlay = true;
                                if (TextUtils.isEmpty(asr.getSemanticTts())) {
                                    SemanticResult item = resourceUrls.get(0);
                                    result.append(item.getName());
                                    playUrl();
                                } else {
                                    result.append(asr.getSemanticTts());
                                    mediaPlayerStop();
                                    mTTSService.startTTS(asr.getSemanticTts());
                                }
                            }
                        }
                        replyMsg(result.toString());
                    }
                }

                @Override
                public void responseNlp(String nlp) {
                    Log.i("activity->", "收到nlp回传:" + nlp);
                    replyMsg(nlp);
                    mTTSService.startTTS(nlp);
                    ttsIsPlay = false;
                }

                @Override
                public void responseCmd(String cmd, String cmdText) {
                    Log.i("activity->", "指令:" + cmd);
                    Log.e("activity->", "是否正在播放:" + mMediaPlayerService.isPlaying());
                    Log.e("activity->", "url:" + mMediaPlayerService.getVideoUrl());
                    Log.e("activity->", "isPause:" + mMediaPlayerService.isPause());
                    if (resourceUrls != null && resourceUrls.size() > 0 && resourceUrls.get(0) != null && !TextUtils.isEmpty(resourceUrls.get(0).getUrl())) {
                        if (cmd.equalsIgnoreCase("replay")) {
                            if (!TextUtils.isEmpty(mMediaPlayerService.getVideoUrl())
                                    && resourceUrls.get(0).getUrl().equals(mMediaPlayerService.getVideoUrl())
                                    && mMediaPlayerService.isPause()) {
                                mMediaPlayerService.play();
                            } else {
                                playUrl();
                            }
                        } else if (cmd.equalsIgnoreCase("pause")) {
                            if (mMediaPlayerService.isPlaying()) {
                                mMediaPlayerService.pause();
                            }
                        }
                    }
                }

                @Override
                public void responseError(int code, String message) {
                    Log.e("activity->", "错误:" + code + " " + message);
                    replyMsg(code + ":" + message);
                }
            }, mContext);
            mRecordService.setRequestOpenId("123456789");
            mRecordService.setRequestUrl("http://192.168.3.21:8080/speech/api/voice/asr/chineseMedicine/");
//            mRecordService.setRequestUrl("http://www.sonnhe.com:8080/speech/api/voice/asr/chineseMedicine");
        }
    }

    /**
     * 播放 resourceUrls 中的 资源
     */
    private void playUrl() {
        if (resourceUrls != null && resourceUrls.size() > 0) {
            SemanticResult item = resourceUrls.get(0);
            if (item != null && !TextUtils.isEmpty(item.getUrl())) {
                Log.e("activity->", "应播放:" + item.getUrl());
                Log.e("activity->", "现在:" + mMediaPlayerService.getVideoUrl());
                if (mMediaPlayerService.isPlaying()) {
                    mMediaPlayerService.stop();
                    if (!mMediaPlayerService.getVideoUrl().equals(item.getUrl())) {
                        mMediaPlayerService.init(item.getUrl());
                        mMediaPlayerService.play();
                    }
                } else {
                    mMediaPlayerService.init(item.getUrl());
                    mMediaPlayerService.play();
                }
            }

        }
    }

    private void sendClick() {
        if (isCanRecordAudio) {
            Log.e("activity->", "send-click");
            mTTSService.stopTTS();
            if (!isRecording) {
                startRecordAction();
            } else {
                stopRecordAction();
            }
        } else {
            requestPermissions();
        }
    }

    /**
     * 开始录音事件
     */
    private void startRecordAction() {
        isRecording = true;
        playAnimRecord();
        recordAlert();
        startRecord();
    }

    /**
     * 停止录音事件
     */
    private void stopRecordAction() {
        isRecording = false;
        stopAnimRecord();
        stopRecordAlert();
        stopRecord();
    }

    /**
     * 播放录音动画
     */
    private void playAnimRecord() {
        mSend.setBackgroundResource(R.drawable.anim_record);
        Object object = mSend.getBackground();
        mAnimRecord = (AnimationDrawable) object;
        mAnimRecord.stop();
        mAnimRecord.start();
    }

    /**
     * 停止录音动画
     */
    private void stopAnimRecord() {
        mSend.setBackgroundResource(R.drawable.play_00);
        if (mAnimRecord == null) {
            return;
        }
        mAnimRecord.stop();
    }

    /**
     * 录音中提示（关闭)
     */
    private void recordAlert() {
        mSendTextView.setText(getString(R.string.stop));
    }

    /**
     * 准备开始录音提示(开始)
     */
    private void stopRecordAlert() {
        mSendTextView.setText(getString(R.string.send));
    }

    /**
     * 开始录音
     */
    private void startRecord() {
        mRecordService.startRecord();
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        mRecordService.stopRecord();
    }

    /**
     * 添加一条msg(右侧)
     */
    private void replyMsg(String content) {
        if (!TextUtils.isEmpty(content)) {
            Log.e("activity->", "添加一条msg(右侧):" + content);
            Msg msg = new Msg(content, Msg.TYPE_RECEIVED);
            mMsgList.add(msg);
            mAdapter.notifyItemInserted(mMsgList.size() - 1);
            // 将RecyclerView定位到最后一行
            mMsgRecyclerView.scrollToPosition(mMsgList.size() - 1);
        }
    }

    /**
     * 添加一条msg(左侧)
     */
    private void addMsg(String content) {
        if (!TextUtils.isEmpty(content)) {
            Log.e("activity->", "添加一条msg(左侧):" + content);
            Msg msg = new Msg(content, Msg.TYPE_SENT);
            mMsgList.add(msg);
            mAdapter.notifyItemInserted(mMsgList.size() - 1);
            // 将RecyclerView定位到最后一行
            mMsgRecyclerView.scrollToPosition(mMsgList.size() - 1);
        }
    }
}
