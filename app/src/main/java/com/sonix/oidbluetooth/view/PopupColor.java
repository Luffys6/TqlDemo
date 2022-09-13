package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.sonix.oidbluetooth.R;

/**
 * 颜色弹出框
 */
public class PopupColor extends PopupWindow implements View.OnClickListener {

    private Context mContext;
    private View view;
    private int mWidth;
    private int popupWidth;//测量后的宽度
    private int popupHeight;//测量后的高度

    private SelectorImageView iv_color_1;
    private SelectorImageView iv_color_2;
    private SelectorImageView iv_color_3;
    private SelectorImageView iv_color_4;
    private SelectorImageView iv_color_5;
    private SelectorImageView iv_color_6;

    public PopupColor(Context context) {
        super(context);
        mContext = context;
        view = LayoutInflater.from(mContext).inflate(R.layout.popup_color, null);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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

        iv_color_1 = view.findViewById(R.id.iv_color_black);
        iv_color_2 = view.findViewById(R.id.iv_color_red);
        iv_color_3 = view.findViewById(R.id.iv_color_blue);
        iv_color_4 = view.findViewById(R.id.iv_color_green);
        iv_color_5 = view.findViewById(R.id.iv_color_yellow);
        iv_color_6 = view.findViewById(R.id.iv_color_orange);

        iv_color_1.setOnClickListener(this);
        iv_color_2.setOnClickListener(this);
        iv_color_3.setOnClickListener(this);
        iv_color_4.setOnClickListener(this);
        iv_color_5.setOnClickListener(this);
        iv_color_6.setOnClickListener(this);
    }

    public int getPopupWidth() {
        return popupWidth;
    }

    public int getPopupHeight() {
        return popupHeight;
    }

    /**
     * 设置PopupWindow的大小
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        mWidth = metrics.widthPixels;
        //设置高度为全屏高度的70%
        //mHeight= (int) (metrics.heightPixels*0.7);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_color_black:
                iv_color_1.toggle(true);
                iv_color_2.toggle(false);
                iv_color_3.toggle(false);
                iv_color_4.toggle(false);
                iv_color_5.toggle(false);
                iv_color_6.toggle(false);
                break;
            case R.id.iv_color_red:
                iv_color_1.toggle(false);
                iv_color_2.toggle(true);
                iv_color_3.toggle(false);
                iv_color_4.toggle(false);
                iv_color_5.toggle(false);
                iv_color_6.toggle(false);
                break;
            case R.id.iv_color_blue:
                iv_color_1.toggle(false);
                iv_color_2.toggle(false);
                iv_color_3.toggle(true);
                iv_color_4.toggle(false);
                iv_color_5.toggle(false);
                iv_color_6.toggle(false);
                break;
            case R.id.iv_color_green:
                iv_color_1.toggle(false);
                iv_color_2.toggle(false);
                iv_color_3.toggle(false);
                iv_color_4.toggle(true);
                iv_color_5.toggle(false);
                iv_color_6.toggle(false);
                break;
            case R.id.iv_color_yellow:
                iv_color_1.toggle(false);
                iv_color_2.toggle(false);
                iv_color_3.toggle(false);
                iv_color_4.toggle(false);
                iv_color_5.toggle(true);
                iv_color_6.toggle(false);
                break;
            case R.id.iv_color_orange:
                iv_color_1.toggle(false);
                iv_color_2.toggle(false);
                iv_color_3.toggle(false);
                iv_color_4.toggle(false);
                iv_color_5.toggle(false);
                iv_color_6.toggle(true);
                break;
        }
        listener.click(view.getId());
    }

    public void setSelectIndex(int color) {
        switch (color) {
            case 1:
                onClick(iv_color_1);
                break;
            case 2:
                onClick(iv_color_2);
                break;
            case 3:
                onClick(iv_color_3);
                break;
            case 4:
                onClick(iv_color_4);
                break;
            case 5:
                onClick(iv_color_5);
                break;
            case 6:
                onClick(iv_color_6);
                break;
        }
    }

    private PopupListener listener;

    public void setListener(PopupListener listener) {
        this.listener = listener;
    }
}
