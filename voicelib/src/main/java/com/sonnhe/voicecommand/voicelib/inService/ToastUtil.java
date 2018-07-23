package com.sonnhe.voicecommand.voicelib.inService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

public class ToastUtil {
    private static ToastUtil sToastUtil;

    private Toast mToast;

    public static ToastUtil getInstance(Context context) {
        if (sToastUtil == null) {
            sToastUtil = new ToastUtil(context.getApplicationContext());
        }
        return sToastUtil;
    }

    @SuppressLint("ShowToast")
    private ToastUtil(Context context) {
        mToast = Toast.makeText(context.getApplicationContext(), "", Toast.LENGTH_SHORT);
    }

    public void showTip(final String content) {
        if (!TextUtils.isEmpty(content)) {
            mToast.setDuration(Toast.LENGTH_SHORT);
            mToast.setText(content);
            mToast.show();
        }
    }

    public void showTipLong(final String content) {
        if (!TextUtils.isEmpty(content)) {
            mToast.setDuration(Toast.LENGTH_LONG);
            mToast.setText(content);
            mToast.show();
        }
    }
}
