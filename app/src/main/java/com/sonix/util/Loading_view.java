package com.sonix.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import com.sonix.oidbluetooth.R;


/**
 * Created by Administrator on 2017/12/1 0001.
 */

public class Loading_view extends ProgressDialog {
    /**
     * 构造函数
     * @param context 上下文
     */
    public Loading_view(Context context) {
        super(context);
    }
    public Loading_view(Context context, int theme) {
        super(context, theme);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(getContext());
    }

    /**
     * 初始化加载框
     * @param context 上下文
     */
    private void init(Context context) {
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.newloading);//loading的xml文件
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);
    }
    @Override
    /**
     * 开始loading
     */
    public void show() {//开启
        super.show();
    }
    @Override
    /**
     * 关闭loading
     */
    public void dismiss() {//关闭
        super.dismiss();
    }
}