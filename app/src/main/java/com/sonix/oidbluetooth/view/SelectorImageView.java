package com.sonix.oidbluetooth.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.sonix.oidbluetooth.R;

/**
 * 颜色和线宽选择自定义view
 */
@SuppressLint("AppCompatCustomView")
public class SelectorImageView extends ImageView implements Checkable {

    private Drawable mSelectorDrawable;
    private boolean isChecked;

    private Drawable mDrawable;

    public SelectorImageView(Context context) {
        this(context, null);
    }

    public SelectorImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectorImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDrawable = getDrawable();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SelectorImageView);
        mSelectorDrawable = typedArray.getDrawable(R.styleable.SelectorImageView_selector_src);
        isChecked = typedArray.getBoolean(R.styleable.SelectorImageView_checked, false);

        setChecked(isChecked);
        if (mSelectorDrawable != null && isChecked) {
            setImageDrawable(mSelectorDrawable);
        }
        typedArray.recycle();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
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
            setImageDrawable(mSelectorDrawable);
        } else {
            setImageDrawable(mDrawable);
        }
    }

    public void toggle(boolean checked) {
        /**外部通过调用此方法传入checked参数，然后把值传入给setChecked（）方法改变当前的选中状态*/
        setChecked(checked);
        toggle();
    }
}
