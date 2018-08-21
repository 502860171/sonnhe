需要最低android版本 : 4.4
需要权限 :
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

需要依赖 : implementation 'com.squareup.okhttp3:okhttp:3.10.0'

调用:
    语音服务:
        初始化语音服务:
            if (mRecordService == null)
                mRecordService = new AudioRecordService(new AudioRecordService.RecordCallback(){...});
        发送语音指令:
            mRecordService.startRecord();
        停止录音:
            mRecordService.stopRecord();
        语音服务回调接口(RecordCallback):
            startRecordSuccess : 开始录音成功
            startRecordError : 开始录音失败
            savePcmFileError : 开始录音失败 保存录音文件失败
            sendDataError: 开始录音失败 发送数据失败 网络不可用
            vadEnd : 接收到语音尾节点 (可以添加停止录音逻辑)
            responseAsr : 接收到asr数据 (把用户的语音转换为文本:((VoiceResult)asr).getDataText())
            responseNlp : 接收到nlp回传
            responseNlpJson: 返回nlp json
            responseCmd : 收到 asr 指令信息
            responseError : 接收服务器回传错误
        设置asr请求密钥
            mRecordService.setRequestOpenId("商户id&产品id");
        设置传输类型 (是否使用base64传输)
            mRecordService.setBase64(false);
        设置传输的种类 (在此url中生效 http://www.sonnhe.com/speech/api/voice/asr)
            mRecordService.setRequestType(0);
        设置传输url
            mRecordService.setRequestUrl("http://www.sonnhe.com/speech/api/voice/asr/market");
    tts服务:
        初始化tts服务:
            if (mSonnheTTSService == null) {
                mSonnheTTSService = new SonnheTTSService(new SonnheTTSService.RequestCallback({...});
            }
        开始tts：
            mSonnheTTSService.requestTTS(content);; // content 需要转换为语音的文本
        停止tts：
            mSonnheTTSService.stopPlay();
        释放tts语音资源:
            mSonnheTTSService.release();
        tts服务回调接口(SonnheTTSService.RequestCallback)
            requestError : 发送语音数据失败
            setDataError : 初始化tts接口失败
            playComplete : tts语音数据播放完成
    媒体播放服务:
        初始化媒体播放服务：
            mMediaPlayerService = new MediaPlayerService();
        释放媒体播放资源：
            mMediaPlayerService.release();
        判断是否正在播放:
            mMediaPlayerService.isPlaying();
        停止播放:
            mMediaPlayerService.stop();
        获得正在播放的资源的url:
            mMediaPlayerService.getVideoUrl();
        是否处在暂停状态:
            mMediaPlayerService.isPause();
        播放、继续播放:
            mMediaPlayerService.play();
        暂停播放:
            mMediaPlayerService.pause();
        放入媒体资源:
            mMediaPlayerService.init(资源的url);

