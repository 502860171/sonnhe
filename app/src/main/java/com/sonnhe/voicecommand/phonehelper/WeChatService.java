package com.sonnhe.voicecommand.phonehelper;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.sonnhe.voicecommand.phonehelper.call.Cn2Spell;
import com.sonnhe.voicecommand.phonehelper.model.Constant;
import com.sonnhe.voicecommand.voicelib.service.SonnheTTSService;

import org.w3c.dom.NodeList;

import java.util.Calendar;
import java.util.List;

/**
 * Created by tianbowen on 2019/2/17.
 */
public class WeChatService extends AccessibilityService {
    private final String TAG = "WeChatService_TAG";
    /**
     * 微信版本是否为7.0.3
     */
    private Boolean version = false;

    /**
     * 防止连点主页面的搜索按钮
     */
    private long lastClickTime = 0;
    public static final int MIN_CLICK_DELAY_TIME = 3000;

    /**
     * 是否进行了搜索
     */
    private Boolean search = false;

    /**
     * 是否在进行初始化
     */
    private Boolean initialization = false;
    /**
     * 微信主页面的“搜索”按钮id
     */
    private final String SEARCH_ID = "com.tencent.mm:id/ij";
    /**
     * 7.0.3微信主页面的“搜索”按钮id
     */
    private final String SEARCH_ID703 = "com.tencent.mm:id/iq";

    /**
     * 微信主页面bottom的“微信”按钮id
     */
    private final String WECHAT_ID = "com.tencent.mm:id/d3t";

    /**
     * 7.0.3微信主页面bottom的“微信”按钮id
     */
    private final String WECHAT_ID703 = "com.tencent.mm:id/d7b";

    /**
     * 微信搜索页面的输入框id
     */
    private final String EDIT_TEXT_ID = "com.tencent.mm:id/ka";

    /**
     * 7.0.3微信搜索页面的输入框id
     */
    private final String EDIT_TEXT_ID703 = "com.tencent.mm:id/kh";

    /**
     * 微信搜索页面的用户名id
     */
    private final String  USER_TEXT_ID = "com.tencent.mm:id/pp";

    /**
     * 7.0.3微信搜索页面的用户名id
     */
    private final String  USER_TEXT_ID703 = "com.tencent.mm:id/q0";

    /**
     * 微信搜索页面的列表id
     */
    private final String  USER_LIST_ID = "com.tencent.mm:id/auq";

    /**
     * 7.0.3微信搜索页面的列表id
     */
    private final String  USER_LIST_ID703 = "com.tencent.mm:id/avp";

    /**
     * 微信聊天页面的输入框id
     */
    private final String USER_EDITTEXT_ID = "com.tencent.mm:id/alm";

    /**
     * 7.0.3微信聊天页面的输入框id
     */
    private final String USER_EDITTEXT_ID703 = "com.tencent.mm:id/amb";


    /**
     * 微信聊天页面转换文字语音输入按钮id
     */
    private final String USER_TURN_ID = "com.tencent.mm:id/alk";

    /**
     * 703微信聊天页面转换文字语音输入按钮id
     */
    private final String USER_TURN_ID703 = "com.tencent.mm:id/am_";


    /**
     * 微信主页面ViewPage的id
     */
    private final String VIEW_PAGE_ID = "com.tencent.mm:id/bko";

    /**
     * 7.0.3微信主页面ViewPage的id
     */
    private final String VIEW_PAGE_ID703 = "com.tencent.mm:id/bmn";

    /**
     * 微信主页面活动id
     */
    private String LAUNCHER_ACTIVITY_NAME = "com.tencent.mm.ui.LauncherUI";
    /**
     * 微信主页面活动id
     */
    private String CHATTING_ACTIVITY_NAME = "com.tencent.mm.ui.chatting.ChattingUI";

    /**
     * 微信搜索页面活动id
     */
    private String SEARCH_ACTIVITY_NAME = "com.tencent.mm.plugin.fts.ui.FTSMainUI";
    /**
     * 微信搜索页面活动id
     */
    private String SPLASH_ACTIVITY_NAME = "com.tencent.mm.app.WeChatSplashActivity";

    /**
     * 微信备注组件id
     */
    private String USERNAME_ID = "com.tencent.mm:id/jw";

    /**
     * 7.0.3微信备注组件id
     */
    private String USERNAME_ID703 = "com.tencent.mm:id/k3";

    private String LIST_VIEW_NAME = "android.widget.ListView";
    private String WECHAT_TEXT_ID = "com.tencent.mm:id/km";

    private Cn2Spell mSpell;

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Constant.flag = 0;
            Constant.wechatId = null;
            Log.e(TAG, "run: flag = 0");
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //判断是否正在进行初始化，如果是则等待5秒
        if(!initialization){
            // 两秒后如果还没有任何的事件，则停止监听
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 2000);
        }


        Log.e(TAG, event.getEventType() + "");
        Log.e(TAG, event.getClassName() + "");

        // 只有从app进入微信才进行监听
        if (Constant.flag == 0) {
            return;
        }

        // 页面改变时需要延迟一段时间进行布局加载
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(event.getSource() == null){
            return;
        }
        //如果初次打开微信，则等待7秒
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && SPLASH_ACTIVITY_NAME.equals(event.getClassName().toString())){
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 7000);
            initialization = true;
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !LAUNCHER_ACTIVITY_NAME.equals(event.getClassName().toString()) && !SEARCH_ACTIVITY_NAME.equals(event.getClassName().toString())) {
            // 如果当前页面不是微信主页面也不是微信搜索页面，就模拟点击返回键
            if(Constant.back == 1){
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }else{
                // 模拟点击之后将暂存值置空，类似于取消监听
                if (fill(event)) {
                    send();
                }else{
                    Constant.flag = 0;
                    Constant.wechatId = null;
                    Constant.wechatContext = null;
                    Toast.makeText(this, "未能填充聊天内容", Toast.LENGTH_SHORT).show();
                }
                Constant.flag = 0;
                Constant.wechatId = null;
                Constant.wechatContext = null;
                Log.e(TAG, "1: flag = 0");
                return;
            }
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && LAUNCHER_ACTIVITY_NAME.equals(event.getClassName().toString())) {
            if(event.getSource() == null){
                Log.e(TAG, "event.getSource() == null");
            }else{
                List<AccessibilityNodeInfo> list = event.getSource().findAccessibilityNodeInfosByViewId(USERNAME_ID);
                if (list.size() > 0) {
                    // 如果是微信主页面，但是是微信聊天页面，则模拟点击返回键
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    return;
                }
                // 7.0.3微信
                List<AccessibilityNodeInfo> list703 = event.getSource().findAccessibilityNodeInfosByViewId(USER_EDITTEXT_ID703);

                if (list703.size() > 0) {
                    version = true;
                    Log.e(TAG, "list703 version = true");
                    // 如果是微信主页面，但是是微信聊天页面，则模拟点击返回键
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    return;
                }

            }

        }else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && CHATTING_ACTIVITY_NAME.equals(event.getClassName().toString())) {
            if(Constant.back == 1){
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }else{
                // 模拟点击之后将暂存值置空，类似于取消监听

                if (fill(event)) {
                    send();
                }else{
                    Constant.flag = 0;
                    Constant.wechatId = null;
                    Constant.wechatContext = null;
                    Toast.makeText(this, "未能填充聊天内容", Toast.LENGTH_SHORT).show();
                }
                Constant.flag = 0;
                Constant.wechatId = null;
                Constant.wechatContext = null;
                Log.e(TAG, "2: flag = 0");
                return;
            }
        }

        //用于判断微信版本
        List<AccessibilityNodeInfo> versionNode = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/r4");
        if(versionNode.size() > 0){
            version = true;
            Log.e(TAG, "version = true");
        }
        Log.e(TAG, String.valueOf(version));

        initialization = false;
        if (version){

            //7.0.3微信
            List<AccessibilityNodeInfo> searchNode703 = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(SEARCH_ID703);
            List<AccessibilityNodeInfo> wechatNode703 = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(WECHAT_ID703);
            List<AccessibilityNodeInfo> viewPageNode703 = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(VIEW_PAGE_ID703);
            if (searchNode703.size() > 1 && viewPageNode703.size() > 0) {
                // 点击“搜索”按钮
                long currentTime = Calendar.getInstance().getTimeInMillis();
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime;
                    if(searchNode703.size() == 10){
                        if (searchNode703.get(8).getParent().isClickable()) {
                            searchNode703.get(8).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, String.valueOf(searchNode703.size()));
                            Log.e(TAG, "点击“搜索”按钮10");
                            return;
                        }
                    }else{
                        if (searchNode703.get(0).getParent().isClickable()) {
                            searchNode703.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, String.valueOf(searchNode703.size()));
                            Log.e(TAG, "点击“搜索”按钮0");
                            return;
                        }
                    }
                }else {
                    Log.e(TAG,"点击过快");
                }



            } else if (searchNode703.size() == 1) {
                // 如果在“我”页面，则进入“微信”页面
                for (AccessibilityNodeInfo info : wechatNode703) {
                    if (info.getText().toString().equals("微信") && !info.isChecked()) {

                        if (info.getParent().isClickable()) {
                            info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, "如果在“我”页面，则进入“微信”页面");
                            return;
                        }
                        break;
                    }
                }
            }


        }else{
            // 用getRootInActiveWindow是为了防止找不到搜索按钮的问题
            List<AccessibilityNodeInfo> searchNode = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(SEARCH_ID);
            List<AccessibilityNodeInfo> wechatNode = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(WECHAT_ID);
            List<AccessibilityNodeInfo> viewPageNode = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(VIEW_PAGE_ID);

            Log.e(TAG, "searchNode:" + searchNode.size());
            Log.e(TAG, "viewPageNode:" + viewPageNode.size());

            // 由于搜索控件在多个页面都有，所以还得判断是否在主页面
            if (searchNode.size() > 1 && viewPageNode.size() > 0) {
                // 点击“搜索”按钮
                long currentTime = Calendar.getInstance().getTimeInMillis();
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime;
                    if (searchNode.get(0).getParent().isClickable()) {
                        searchNode.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return;
                    }
                }else {
                    Log.e(TAG,"点击过快");
                }
            } else if (searchNode.size() == 1) {
                // 如果在“我”页面，则进入“微信”页面
                for (AccessibilityNodeInfo info : wechatNode) {
                    if (info.getText().toString().equals("微信") && !info.isChecked()) {

                        if (info.getParent().isClickable()) {
                            info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            return;
                        }
                        break;
                    }
                }
            }
        }


        // 当前页面是搜索页面
        if (SEARCH_ACTIVITY_NAME.equals(event.getClassName().toString())) {
            //7.0.3微信
            List<AccessibilityNodeInfo> editTextNode703 = event.getSource().findAccessibilityNodeInfosByViewId(EDIT_TEXT_ID703);

            if (editTextNode703.size() > 0) {
                // 输入框内清空
                Bundle clear = new Bundle();
                clear.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
                editTextNode703.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clear);

                // 输入框内输入查询的微信号
                Bundle arguments = new Bundle();
                Log.e(TAG, "onAccessibilityEvent703: "+ Constant.wechatId);
                Log.e(TAG, String.valueOf(editTextNode703.size()));
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Constant.wechatId);
                editTextNode703.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                search = true;

            }else{
                Log.e(TAG, "onAccessibilityEvent703: 搜索框不存在");
            }

            List<AccessibilityNodeInfo> editTextNode = event.getSource().findAccessibilityNodeInfosByViewId(EDIT_TEXT_ID);

            if (editTextNode.size() > 0) {
                // 输入框内清空
                Bundle clear = new Bundle();
                clear.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
                editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clear);

                // 输入框内输入查询的微信号
                Bundle arguments = new Bundle();
                Log.e(TAG, "onAccessibilityEvent: "+ Constant.wechatId);
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Constant.wechatId);
                editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                search = true;

            }




        } else if (LIST_VIEW_NAME.equals(event.getClassName().toString())) {

            if(search){
                // 如果监听到了ListView的内容改变，则找到查询到的人，并点击进入
                if(Constant.back == 1){
                    if(event.getSource() == null){
                        Log.e(TAG, "event.getSource() == null");
                    }else{
                        if(version){
                            //7.0.3微信
                            List<AccessibilityNodeInfo> textList = event.getSource().findAccessibilityNodeInfosByText("查找微信号");
                            List<AccessibilityNodeInfo> userList = event.getSource().findAccessibilityNodeInfosByViewId(USER_TEXT_ID703);
                            List<AccessibilityNodeInfo> listList = event.getSource().findAccessibilityNodeInfosByViewId(USER_LIST_ID703);
                            if(listList.size() > 0) {
                                if (userList.size() > 0) {
                                    Constant.back = 0;
                                    if(mSpell.getPinYin(userList.get(0).getText().toString()).contains(Constant.wechatId) ||
                                            Constant.wechatId.contains(mSpell.getPinYin(userList.get(0).getText().toString()))){
                                        userList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        search = false;
                                    }else{
                                        notFound();
                                    }

                                }else{
                                    notFound();
                                }
                            }else if(textList.size() > 0){
                                notFound();
                            }
                        }else{
                            List<AccessibilityNodeInfo> textList = event.getSource().findAccessibilityNodeInfosByText("查找微信号");
                            List<AccessibilityNodeInfo> userList = event.getSource().findAccessibilityNodeInfosByViewId(USER_TEXT_ID);
                            List<AccessibilityNodeInfo> listList = event.getSource().findAccessibilityNodeInfosByViewId(USER_LIST_ID);
                            if(listList.size() > 0) {
                                if (userList.size() > 0) {
                                    Constant.back = 0;
                                    if(mSpell.getPinYin(userList.get(0).getText().toString()).contains(Constant.wechatId) ||
                                            Constant.wechatId.contains(mSpell.getPinYin(userList.get(0).getText().toString()))){
                                        userList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        search = false;
                                    }else{
                                        notFound();
                                    }

                                }else{
                                    notFound();
                                }
                            }else if(textList.size() > 0){
                                notFound();
                            }

                        }


                    }

                }
            }

        }else if ("android.widget.FrameLayout".equals(event.getClassName().toString())) {
            if(search){
                // 如果监听到了ListView的内容改变，则找到查询到的人，并点击进入
                if(Constant.back == 1){
                    if(event.getSource() == null){
                        Log.e(TAG, "event.getSource() == null");
                    }else{
                        if(version){
                            //7.0.3微信
                            List<AccessibilityNodeInfo> textList = event.getSource().findAccessibilityNodeInfosByText("查找微信号");
                            List<AccessibilityNodeInfo> userList = event.getSource().findAccessibilityNodeInfosByViewId(USER_TEXT_ID703);
                            List<AccessibilityNodeInfo> listList = event.getSource().findAccessibilityNodeInfosByViewId(USER_LIST_ID703);
                            if(listList.size() > 0) {
                                if (userList.size() > 0) {
                                    Constant.back = 0;
                                    if(mSpell.getPinYin(userList.get(0).getText().toString()).contains(Constant.wechatId) ||
                                            Constant.wechatId.contains(mSpell.getPinYin(userList.get(0).getText().toString()))){
                                        userList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        search = false;
                                    }else{
                                        notFound();
                                    }

                                }else{
                                    notFound();
                                }
                            }else if(textList.size() > 0){
                                notFound();
                            }
                        }else{
                            List<AccessibilityNodeInfo> textList = event.getSource().findAccessibilityNodeInfosByText("查找微信号");
                            List<AccessibilityNodeInfo> userList = event.getSource().findAccessibilityNodeInfosByViewId(USER_TEXT_ID);
                            List<AccessibilityNodeInfo> listList = event.getSource().findAccessibilityNodeInfosByViewId(USER_LIST_ID);
                            if(listList.size() > 0) {
                                if (userList.size() > 0) {
                                    Constant.back = 0;
                                    if(mSpell.getPinYin(userList.get(0).getText().toString()).contains(Constant.wechatId) ||
                                            Constant.wechatId.contains(mSpell.getPinYin(userList.get(0).getText().toString()))){
                                        userList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        search = false;
                                    }else{
                                        notFound();
                                    }

                                }else{
                                    notFound();
                                }
                            }else if(textList.size() > 0){
                                notFound();
                            }

                        }


                    }

                }
            }

        }


    }

    private void notFound(){
        Constant.flag = 0;
        Constant.wechatId = null;
        Constant.wechatContext = null;
        Toast.makeText(this, "没有找到联系人", Toast.LENGTH_SHORT).show();
        mSonnheTTSService.requestTTS("没有找到联系人");
        search = false;
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.e(TAG, "connected");
        mSpell = new Cn2Spell();
        initTTSService();
    }


    /**
     * 寻找窗体中的“发送”按钮，并且点击。
     */
    @SuppressLint("NewApi")
    private void send() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("发送");
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    if(n.getClassName().equals("android.widget.Button") && n.isEnabled()){
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        mSonnheTTSService.requestTTS("微信已发送");
                    }
                }

            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo
                        .findAccessibilityNodeInfosByText("Send");
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        if(n.getClassName().equals("android.widget.Button") && n.isEnabled()){
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            mSonnheTTSService.requestTTS("微信已发送");
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private boolean fill(AccessibilityEvent event) {
        if(version){
            return fillEditText(USER_EDITTEXT_ID703, USER_TURN_ID703);
//            List<AccessibilityNodeInfo> editTextNode = event.getSource().findAccessibilityNodeInfosByViewId(USER_EDITTEXT_ID703);
//            if (editTextNode.size() > 0) {
//                // 输入框内清空
//                Bundle clear = new Bundle();
//                clear.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
//                editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clear);
//
//                // 输入框内输入回复信息
//                Bundle arguments = new Bundle();
//                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Constant.wechatContext);
//                editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
//                return true;
//            }
//
//            return false;
        }else{
            return fillEditText(USER_EDITTEXT_ID, USER_TURN_ID);
//            List<AccessibilityNodeInfo> editTextNode = event.getSource().findAccessibilityNodeInfosByViewId(USER_EDITTEXT_ID);
//            if (editTextNode.size() > 0) {
//                // 输入框内清空
//                Bundle clear = new Bundle();
//                clear.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
//                editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clear);
//
//                // 输入框内输入回复信息
//                Bundle arguments = new Bundle();
//                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Constant.wechatContext);
//                editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
//                return true;
//            }
//
//            return false;
        }

    }

    private boolean fillEditText(String textID, String buttonID){
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("按住 说话");
            if (list != null && list.size() > 0) {
                List<AccessibilityNodeInfo> turnButton = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(buttonID);
                if (turnButton.size() > 0) {
                    turnButton.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                }else{
                    Log.e(TAG, "fillEditText: 没有找到转换按钮");
                }

            }else{
                Log.e(TAG, "fillEditText: 没有找到按住说话");
            }
        }
        List<AccessibilityNodeInfo> editTextNode = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(textID);
        if (editTextNode.size() > 0) {
            // 输入框内清空
            Bundle clear = new Bundle();
            clear.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
            editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clear);

            // 输入框内输入回复信息
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Constant.wechatContext);
            editTextNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            return true;
        }

        return false;
    }

    private SonnheTTSService mSonnheTTSService;

    private void initTTSService() {
        if (mSonnheTTSService == null) {
            mSonnheTTSService = new SonnheTTSService(new SonnheTTSService.RequestCallback() {
//                @Override
//                public void requestSuccess(Map<String, Object> returnMap) {
//                    byte[] mBytes = (byte[]) returnMap.get("bytes");
//                    if (mBytes != null && mBytes.length > 0) {
//                        mSonnheTTSService.setData(mBytes);
//                        mSonnheTTSService.startPlay();
//                    }
//                }

                @Override
                public void requestError(String error) {
//                    Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
                }

                @Override
                public void playComplete() {
//                    if (ttsIsPlay) {
//                        playUrl();
//                    }
//                    if (guide){
//                        guide = false;
//                        SemanticResult item = resourceUrls.get(0);
//                        mapService.startGuide(item.getStartLoc(), item.getEndLoc());
//                    }
                }

                @Override
                public void setDataError() {
//                    Toast.makeText(mContext, "初始化数据错误", Toast.LENGTH_LONG).show();
                }
            }, this);

        }
    }


}
