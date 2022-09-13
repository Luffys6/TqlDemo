package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import com.sonix.oidbluetooth.R;
import com.tqltech.tqlpencomm.Constants;

/**
 * 更多弹出框
 */
public class PopupMore extends PopupWindow implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private Context mContext;
    private View view;
    private int popupWidth;//测量后的宽度
    private int popupHeight;//测量后的高度

    private LinearLayout ll_recognition;
    private LinearLayout ll_offline;
    private LinearLayout ll_setting;
    private LinearLayout ll_about;
    private LinearLayout ll_ota;
    private LinearLayout ll_txt;
    private LinearLayout ll_bt_log;

    private LinearLayout ll_bit_error;
    private LinearLayout ll_mcu;
    private LinearLayout ll_mcu_ota;
    private TextView tv_beiSaiEr;
    private TextView tv_lineTo;
    private TextView tv_Migration_close;
    private TextView tv_Migration_open;
    private TextView tvOpne_8clockAlgorithm;
    private TextView tvClose_8clockAlgorithm;
    private Switch sw_save_log;
    private Switch sw_draw_stroke;
    private Switch sw_filter_algorithm,sw_invalid_code_value,sw_practise_calligraphy;
    private boolean isChecked;
    private Switch sw_hand_code;
    private Switch sw_invalid_code;


    public PopupMore(Context context) {
        super(context);
        mContext = context;
        view = LayoutInflater.from(mContext).inflate(R.layout.popup_more, null);
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

        ll_recognition = view.findViewById(R.id.ll_recognition);
        ll_offline = view.findViewById(R.id.ll_offline);
        ll_setting = view.findViewById(R.id.ll_setting);
        ll_about = view.findViewById(R.id.ll_about);
        ll_ota = view.findViewById(R.id.ll_bt);
        ll_mcu = view.findViewById(R.id.ll_mcu);
        ll_bt_log = view.findViewById(R.id.ll_bt_log);
        ll_mcu_ota = view.findViewById(R.id.ll_mcu_ota);
        ll_txt = view.findViewById(R.id.ll_txt);
        sw_save_log = view.findViewById(R.id.sw_save_log);
        sw_draw_stroke = view.findViewById(R.id.sw_draw_stroke);
        sw_filter_algorithm = view.findViewById(R.id.sw_filter_algorithm);
        sw_hand_code = view.findViewById(R.id.sw_hand_code);
        sw_invalid_code = view.findViewById(R.id.sw_invalid_code);
        tv_beiSaiEr = view.findViewById(R.id.tv_beiSaiEr);
        tv_lineTo = view.findViewById(R.id.tv_lineTo);
        tv_Migration_close = view.findViewById(R.id.tv_Migration_close);
        tv_Migration_open = view.findViewById(R.id.tv_Migration_open);
        ll_bit_error = view.findViewById(R.id.ll_bit_error);
        tvOpne_8clockAlgorithm = view.findViewById(R.id.tv_opne_8clockAlgorithm);
        tvClose_8clockAlgorithm = view.findViewById(R.id.tv_close_8clockAlgorithm);

        sw_invalid_code_value = view.findViewById(R.id.sw_invalid_code_value);
        sw_invalid_code_value.setChecked(Constants.isLargeXdistPerunit);

        sw_practise_calligraphy = view.findViewById(R.id.sw_practise_calligraphy);

        ll_bit_error.setOnClickListener(this);
        ll_recognition.setOnClickListener(this);
        ll_offline.setOnClickListener(this);
        ll_setting.setOnClickListener(this);
        ll_about.setOnClickListener(this);
        ll_ota.setOnClickListener(this);
        ll_mcu_ota.setOnClickListener(this);
        ll_txt.setOnClickListener(this);
        tv_beiSaiEr.setOnClickListener(this);
        tv_lineTo.setOnClickListener(this);
        tv_Migration_open.setOnClickListener(this);
        tv_Migration_close.setOnClickListener(this);
        tvOpne_8clockAlgorithm.setOnClickListener(this);
        tvClose_8clockAlgorithm.setOnClickListener(this);
        ll_bt_log.setOnClickListener(this);
        ll_mcu.setOnClickListener(this);
        sw_save_log.setOnCheckedChangeListener(this);
        sw_draw_stroke.setOnCheckedChangeListener(this);
        sw_filter_algorithm.setOnCheckedChangeListener(this);
        sw_hand_code.setOnCheckedChangeListener(this);
        sw_invalid_code.setOnCheckedChangeListener(this);
        sw_practise_calligraphy.setOnCheckedChangeListener(this);
        sw_invalid_code_value.setOnCheckedChangeListener(this);
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
        dismiss();
    }

    private PopupListener listener;
    private PopupCheckListener listener2;

    public void setListener(PopupListener listener) {
        this.listener = listener;
    }

    public void setCheckedListener(PopupCheckListener listener2) {
        this.listener2 = listener2;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        listener2.checkedChanged(buttonView,isChecked);
    }


}
