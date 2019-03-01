package com.sonnhe.voicecommand.phonehelper.model;

public class Msg {
    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;

    private String mContent;

    private int mType;

    public Msg(String content, int type) {
        mContent = content;
        mType = type;
    }

    public String getContent() {
        return mContent;
    }

    public int getType() {
        return mType;
    }
}
