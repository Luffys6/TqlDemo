package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.webkit.WebView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sonix.oidbluetooth.R;

/**
 * 协议弹出框
 */
public class ProtocolPopu extends PopupWindow implements View.OnClickListener {

    private Context mContext;
    private WebView wv_protocol;
    private TextView tvAgree;

    public ProtocolPopu(Context context, int type) {
        this.mContext = context;
//        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
//        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        //沉侵式状态栏下全屏
        setWidth(mContext.getResources().getDisplayMetrics().widthPixels);
        setHeight(mContext.getResources().getDisplayMetrics().heightPixels);
        ColorDrawable dw = new ColorDrawable(Color.WHITE);
        setBackgroundDrawable(dw);
        setFocusable(true);
        setClippingEnabled(false);

        View root = View.inflate(context, R.layout.popup_protocol, null);
        setContentView(root);

        wv_protocol = root.findViewById(R.id.wv_protocol);
        tvAgree = root.findViewById(R.id.tv_agree);

        /*wv_protocol.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });*/

        tvAgree.setOnClickListener(this);
    }

    public void setType(int type) {
        if (type == 1) {  //用户协议
            wv_protocol.loadUrl("file:///android_asset/userProtocol.html");
            tvAgree.setText(mContext.getResources().getString(R.string.already_read));
        } else if (type == 2) { //用户隐私
            wv_protocol.loadUrl("file:///android_asset/privacy.html");
            tvAgree.setText(mContext.getResources().getString(R.string.already_read_privacy));
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_agree:
                if (popListener != null) {
                    popListener.onResult(true);
                }
                dismiss();
                break;
        }
    }

    private PopListener popListener;

    public void setPopListener(PopListener popListener) {
        this.popListener = popListener;
    }

    public interface PopListener {
        void onResult(boolean flag);
    }
}
