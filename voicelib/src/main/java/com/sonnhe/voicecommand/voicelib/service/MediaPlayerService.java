package com.sonnhe.voicecommand.voicelib.service;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerService implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    private MediaPlayer mediaPlayer;
    private String videoUrl;
    private boolean isPause = false;
    // 是否正在播放
    private boolean playing = false;

    public MediaPlayerService() {
        super();
    }

    public void init(String url) {
        Log.e("lib->", "mediaPlayer-init");
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            videoUrl = url;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            // prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            playing = false;
        }
    }

    public boolean isPause() {
        return isPause;
    }

    public boolean isPlaying() {
        return playing;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int bufferingProgress) {
//        skbProgress.setSecondaryProgress(bufferingProgress);
//        int currentProgress = skbProgress.getMax()
//                * mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration();
//        Log.e(currentProgress + "% play", bufferingProgress + "% buffer");
        Log.e("lib->", "bufferingProgress:" + bufferingProgress);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.e("lib->", "onCompletion");
        playing = false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        Log.e("mediaPlayer", "onPrepared");
    }

//    // 通过定时器和Handler来更新进度条
//    TimerTask mTimerTask = new TimerTask() {
//        @Override
//        public void run() {
//            if (mediaPlayer == null)
//                return;
//            if (mediaPlayer.isPlaying() && !skbProgress.isPressed()) {
//                handleProgress.sendEmptyMessage(0);
//            }
//        }
//    };
//    Handler handleProgress = new Handler() {
//        public void handleMessage(Message msg) {
//            int position = mediaPlayer.getCurrentPosition();
//            int duration = mediaPlayer.getDuration();
//            if (duration > 0) {
//                long pos = skbProgress.getMax() * position / duration;
//                skbProgress.setProgress((int) pos);
//            }
//        }
//
//        ;
//    };

//    /**
//     * 来电话了
//     */
//    public void callIsComing() {
//        if (mediaPlayer.isPlaying()) {
//            playPosition = mediaPlayer.getCurrentPosition();// 获得当前播放位置
//            mediaPlayer.stop();
//        }
//    }

//    /**
//     * 通话结束
//     */
//    public void callIsDown() {
//        if (playPosition > 0) {
//            playNet(playPosition);
//            playPosition = 0;
//        }
//    }

    /**
     * 播放
     */
    public void play() {
        playNet(mediaPlayer.getCurrentPosition());
        playing = true;
    }
    /**
     * 释放
     */
    public void release(){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    /**
     * 重播
     */
    public void replay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);// 从开始位置开始播放音乐
        } else {
            playNet(0);
        }
        playing = true;
    }

    /**
     * 暂停
     */
    public void pause() {
        if (mediaPlayer.isPlaying()) {// 如果正在播放
            mediaPlayer.pause();// 暂停
            isPause = true;
            playing = false;
        }
    }

    /**
     * 继续播放
     */
    public void keepOn() {
        if (!mediaPlayer.isPlaying()) {
            if (isPause) {// 如果处于暂停状态
                mediaPlayer.start();// 继续播放
                isPause = false;
                playing = true;
            }
        }
    }

    /**
     * 停止
     */
    public void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        playing = false;
    }

    /**
     * 播放音乐
     */
    private void playNet(int playPosition) {
        try {
            mediaPlayer.reset();// 把各项参数恢复到初始状态
            mediaPlayer.setDataSource(videoUrl);
            mediaPlayer.prepare();// 进行缓冲
            mediaPlayer.setOnPreparedListener(new MyPreparedListener(
                    playPosition));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final class MyPreparedListener implements
            android.media.MediaPlayer.OnPreparedListener {
        private int playPosition;

        MyPreparedListener(int playPosition) {
            this.playPosition = playPosition;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start();// 开始播放
            if (playPosition > 0) {
                mediaPlayer.seekTo(playPosition);
            }
        }
    }
}
