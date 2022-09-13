package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sonix.oidbluetooth.R;

/**
 * 灵敏度自定义view
 */
public class SelectorView extends RelativeLayout implements Checkable {

    private ImageView mImageView;
    private TextView mTextView;

    private Drawable mDrawable;
    private int mColor;

    private Drawable mSelectorDrawable;
    private int mSelectorTextColor;
    private boolean isChecked;

    public SelectorView(Context context) {
        this(context, null);
    }

    public SelectorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View view = LayoutInflater.from(context).inflate(R.layout.view_selector, this);
        mImageView = view.findViewById(R.id.iv_background);
        mTextView = view.findViewById(R.id.tv_txt);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SelectorView);
        String value = typedArray.getString(R.styleable.SelectorView_selected_text);

        mDrawable = typedArray.getDrawable(R.styleable.SelectorView_normal_src);
        mColor = typedArray.getColor(R.styleable.SelectorView_normal_text_color, Color.BLACK);

        mSelectorDrawable = typedArray.getDrawable(R.styleable.SelectorView_selected__src);
        mSelectorTextColor = typedArray.getColor(R.styleable.SelectorView_selected_text_color, Color.WHITE);
        isChecked = typedArray.getBoolean(R.styleable.SelectorView_selected_view_checked, false);

        setChecked(isChecked);
        mTextView.setText(value);
        if (isChecked) {
            if (mSelectorDrawable != null) {
                mImageView.setImageDrawable(mSelectorDrawable);
            }
            mTextView.setTextColor(mSelectorTextColor);
        } else {
            if (mDrawable != null)
                mImageView.setImageDrawable(mDrawable);
            mTextView.setTextColor(mSelectorTextColor);
        }
        typedArray.recycle();
    }


    @Override
    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        if (isChecked()) {
            mImageView.setImageDrawable(mSelectorDrawable);
            mTextView.setTextColor(mSelectorTextColor);
        } else {
            mImageView.setImageDrawable(mDrawable);
            mTextView.setTextColor(mColor);
        }
    }

    public void toggle(boolean checked) {
        /**外部通过调用此方法传入checked参数，然后把值传入给setChecked（）方法改变当前的选中状态*/
        setChecked(checked);
        toggle();
    }
}
