package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.sonix.oidbluetooth.R;

/**
 * 离线弹出框
 */
public class PopupOffline extends PopupWindow implements View.OnClickListener {

    private Context mContext;
    private View view;
    private int popupWidth;//测量后的宽度
    private int popupHeight;//测量后的高度

    private RelativeLayout ll_back;
    private LinearLayout ll_start_offline;
    private LinearLayout ll_stop_offline;
    private LinearLayout ll_delete_offline;
    private LinearLayout ll_offline_number;
    private LinearLayout ll_breakpoint_offline;

    public PopupOffline(Context context) {
        super(context);
        mContext = context;
        view = LayoutInflater.from(mContext).inflate(R.layout.popup_offline, null);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        setContentView(view);

        initView();

        // 设置SelectPicPopupWindow弹出窗体可点击
        setFocusable(true);
        setOutsideTouchable(true);

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

        ll_back = view.findViewById(R.id.ll_offline_back);
        ll_start_offline = view.findViewById(R.id.ll_start_offline);
        ll_stop_offline = view.findViewById(R.id.ll_stop_offline);
        ll_delete_offline = view.findViewById(R.id.ll_delete_offline);
        ll_offline_number = view.findViewById(R.id.ll_offline_number);
        ll_breakpoint_offline = view.findViewById(R.id.ll_breakpoint_offline);


        ll_back.setOnClickListener(this);
        ll_start_offline.setOnClickListener(this);
        ll_stop_offline.setOnClickListener(this);
        ll_delete_offline.setOnClickListener(this);
        ll_breakpoint_offline.setOnClickListener(this);
        ll_offline_number.setOnClickListener(this);
    }

    public int getPopupWidth() {
        return popupWidth;
    }

    public int getPopupHeight() {
        return popupHeight;
    }

    @Override
    public void onClick(View view) {
        listener.click(view.getId());
        if (view.getId() == R.id.ll_offline_back) {
            dismiss();
        }
    }

    private PopupListener listener;

    public void setListener(PopupListener listener) {
        this.listener = listener;
    }
}
