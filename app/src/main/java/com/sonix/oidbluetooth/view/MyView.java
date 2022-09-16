package com.sonix.oidbluetooth.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sonix.oidbluetooth.R;

/**
 * 图片和文字自定义view
 */
public class MyView extends LinearLayout {

    private Context mContext;
    private TextView mTextView;
    private View mView;

    public MyView(Context context) {
        this(context, null);
    }

    public MyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }


    private void initView(Context context, @Nullable AttributeSet attrs) {
        mContext = context;
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(0,0,0,0);

        mTextView = new TextView(context);
        mView = new View(context);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mTextView.setLayoutParams(params);
        mView.setLayoutParams(params);
        mTextView.setText("100分");
        mTextView.setTextSize(10);
        mTextView.setTextColor(getResources().getColor(R.color.green));
        this.addView(mTextView);
        this.addView(mView);

    }


}
