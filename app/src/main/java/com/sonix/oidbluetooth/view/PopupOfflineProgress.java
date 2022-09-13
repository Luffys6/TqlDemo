package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sonix.oidbluetooth.R;

/**
 * 离线下载进度弹出框
 */
public class PopupOfflineProgress extends PopupWindow implements View.OnClickListener {

    private Context mContext;
    private View view;
    private int popupWidth;//测量后的宽度
    private int popupHeight;//测量后的高度
    private int progress;
    private boolean isPause = true;

    private LinearLayout ll_progress;
    private RoundProgressBar progressBar;
    private TextView tv_pause_continue;
    private TextView tv_pause_stop;

    public PopupOfflineProgress(Context context) {
        super(context);
        mContext = context;
        view = LayoutInflater.from(mContext).inflate(R.layout.popup_progress, null);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        setContentView(view);

        initView();

        // 设置SelectPicPopupWindow弹出窗体可点击
        setFocusable(false);
        setOutsideTouchable(false);

        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0000000000);
        // 点back键和其他地方使其消失,设置了这个才能触发OnDismisslistener ，设置其他控件变化等操作
        setBackgroundDrawable(dw);
        // 设置弹出窗体动画效果
        setAnimationStyle(android.R.style.Animation_Dialog);
    }

    private void initView() {
        popupWidth = view.getMeasuredWidth();    //获取测量后的宽度
        popupHeight = view.getMeasuredHeight();  //获取测量后的高度

        ll_progress = view.findViewById(R.id.ll_progress);
        progressBar = view.findViewById(R.id.progressBar);
        tv_pause_continue = view.findViewById(R.id.tv_pause_continue);
        tv_pause_stop = view.findViewById(R.id.tv_pause_stop);

        tv_pause_continue.setOnClickListener(this);
        tv_pause_stop.setOnClickListener(this);
    }

    public int getPopupWidth() {
        return popupWidth;
    }

    public int getPopupHeight() {
        return popupHeight;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        progressBar.setProgress(progress);
    }

    public boolean isPause() {
        return isPause;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_pause_continue:
                if (isPause) {
                    isPause = false;
                    tv_pause_continue.setText(mContext.getResources().getString(R.string.jx));
                } else {
                    isPause = true;
                    tv_pause_continue.setText(mContext.getResources().getString(R.string.zt));
                }
                break;
            case R.id.tv_pause_stop:
                break;
        }
        listener.click(view.getId());
    }

    private PopupListener listener;

    public void setListener(PopupListener listener) {
        this.listener = listener;
    }
}
