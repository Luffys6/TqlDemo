package com.sonix.oidbluetooth;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.sonix.oidbluetooth.view.ProtocolPopu;
import com.sonix.util.SPUtils;
import com.sonix.util.statusbar.ImmersiveUtils;
import com.tqltech.tqlpencomm.spp.SppUtil;


public class SplashActivity extends AppCompatActivity  {
    private static final String TAG = SplashActivity.class.getSimpleName();
    //权限组
    private Handler mHandler = new Handler();
    private Dialog mServiceDialog;
    private ProtocolPopu popu;
    private int type;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {

            finish();
            return;
        }
        setContentView(R.layout.activity_splash);
        //沉浸式状态栏
        ImmersiveUtils.setStatusBar(this, false, false);
        ImmersiveUtils.setStatusTextColor(false, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //检查权限
        initIsSealSinglePageDialog();
        gotoMainActivity();
    }

    private void gotoMainActivity() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isFirstOpen = (boolean) SPUtils.get(SplashActivity.this, Constants.FIRST_OPEN, true);
                boolean isAgree = (boolean) SPUtils.get(SplashActivity.this, Constants.FIRST_ISAGREE, false);
                if (isFirstOpen) {
                    if (!isAgree){
                        if(mServiceDialog!=null){
                            mServiceDialog.show();
                        }
                    }else {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                } else {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, 3000);
    }

    private void initIsSealSinglePageDialog() {
        mServiceDialog = new Dialog(this, R.style.customDialog);
        LayoutInflater mInflater = LayoutInflater.from(this);
        View view = mInflater.inflate(R.layout.dialog_service_agreement, null);
        mServiceDialog.setContentView(view);
        mServiceDialog.setCancelable(false);
        TextView tv_msg = (TextView) view.findViewById(R.id.tv_msg);
        TextView tvConfirm = (TextView) view.findViewById(R.id.tv_confirm);
        SpannableString spannableString = new SpannableString(getResources().getString(R.string.service_guide));
        final int start = spannableString.toString().indexOf("《");//第一个出现的位置设置颜色及点击
        final int second = spannableString.toString().indexOf("和《");//第一个出现的位置设置颜色及点击
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
           //  startActivity(new Intent(SplashActivity.this,ServiceAgreementActivity.class));
                showUserProtocol(1);
                if (mServiceDialog != null && mServiceDialog.isShowing()) {
                    mServiceDialog.dismiss();
                }
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.blue));
            }
        };
        ClickableSpan clickableSpan2 = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                //  startActivity(new Intent(SplashActivity.this,ServiceAgreementActivity.class));
                showUserProtocol(2);
                if (mServiceDialog != null && mServiceDialog.isShowing()) {
                    mServiceDialog.dismiss();
                }
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.blue));
            }
        };
        spannableString.setSpan(clickableSpan,start, start+6, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(clickableSpan2,second+1, second+7, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        tv_msg.setMovementMethod(LinkMovementMethod.getInstance());
        tv_msg.setText(spannableString);
        tv_msg.setHighlightColor(Color.parseColor("#00000000"));
        TextView tv_confirm_agree = (TextView) view.findViewById(R.id.tv_confirm_agree);
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mServiceDialog.dismiss();
                System.exit(0);

            }
        });
        ImageView tv_cancel = (ImageView) view.findViewById(R.id.tv_cancel);
        tv_cancel.setVisibility(View.INVISIBLE);
        tv_confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAgree();
            }
        });
    }

    private void confirmAgree() {
        SPUtils.put(SplashActivity.this, Constants.FIRST_ISAGREE, false);
        SPUtils.put(SplashActivity.this, Constants.FIRST_OPEN, false);
        mServiceDialog.dismiss();
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }


    /**
     * 协议弹出框
     *
     * @param type
     */
    private void showUserProtocol(int type) {
        if (popu == null)
            popu = new ProtocolPopu(this, type);
        this.type = type;
        popu.setType(type);
        popu.setPopListener(popListener);
        View contentView = LayoutInflater.from(this).inflate(R.layout.activity_splash, null);
        popu.showAsDropDown(contentView);
        popu.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mServiceDialog == null) {
                    initIsSealSinglePageDialog();
                }
                mServiceDialog.show();
            }
        });
    }

    /**
     * 点击事件
     */
    ProtocolPopu.PopListener popListener = new ProtocolPopu.PopListener() {
        @Override
        public void onResult(boolean flag) {
            if (type == 1) {
                SPUtils.put(SplashActivity.this, "UserProtocol", flag);
            } else {
                SPUtils.put(SplashActivity.this, "UserPrivacyPolicy", flag);
            }
            boolean ProtocolBoolean = (boolean) SPUtils.get(SplashActivity.this, "UserProtocol", false);
            boolean PrivacyPolicyBoolean = (boolean) SPUtils.get(SplashActivity.this, "UserPrivacyPolicy", false);
            if (ProtocolBoolean && PrivacyPolicyBoolean) {
                confirmAgree();
            } else {
                if (mServiceDialog == null) {
                    initIsSealSinglePageDialog();
                }
                mServiceDialog.show();
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        getWindow().setBackgroundDrawable(null);
    }
}
