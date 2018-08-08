package com.sonnhe.voicecommand.voicelib.service;

import android.content.Context;

import com.sonnhe.voicecommand.voicelib.inService.RequestTTSService;

import java.util.Map;

public class SonnheTTSService {



    private RequestCallback mRequestCallback;

    private Context mContext;


    private RequestTTSService mRequestTTSService = null;



    public interface RequestCallback {
        void requestSuccess(Map<String, Object> returnMap);

        void requestError(String error);
    }



    public SonnheTTSService(RequestCallback requestCallback, Context context){
        mContext = context;
        mRequestCallback = requestCallback;
        initTTSService();
    }



    private void initTTSService(){
        if(mRequestTTSService == null){
            mRequestTTSService = new RequestTTSService(new RequestTTSService.RequestCallback(){
                @Override
                public void requestSuccess(Map<String, Object> returnMap) {
                    mRequestCallback.requestSuccess(returnMap);
                }

                @Override
                public void requestError(String error) {
                    mRequestCallback.requestError(error);
                }
            },mContext);
        }
        mRequestTTSService.getLooper();
        mRequestTTSService.start();
    }

    public void requestTTS(String text){
        mRequestTTSService.requestTTS(text);
    }





}
