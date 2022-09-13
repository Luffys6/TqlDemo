package com.sonix.oidbluetooth.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sonix.oidbluetooth.R;

/**
 * 图片和文字自定义view
 */
public class MyImageTextView extends LinearLayout {

    private Context mContext;
    private TextView mTextView;
    private ImageView mImageView;

    public MyImageTextView(Context context) {
        this(context, null);
    }

    public MyImageTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyImageTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @SuppressLint("NewApi")
    private void initView(Context context, @Nullable AttributeSet attrs) {
        mContext = context;
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(15,15,15,15);

        mTextView = new TextView(context);
        mImageView = new ImageView(context);

//        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//        mTextView.setLayoutParams(params);
//        mImageView.setLayoutParams(params);
//
//        mImageView.setImageResource(R.mipmap.icon_width);
//        mTextView.setText(R.string.width);
//        this.addView(mImageView);
//        this.addView(mTextView);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyImageTextView);

            Drawable mDrawable = typedArray.getDrawable(R.styleable.MyImageTextView_img_src);
            float img_width = typedArray.getDimension(R.styleable.MyImageTextView_img_width, 0);
            float img_height = typedArray.getDimension(R.styleable.MyImageTextView_img_height, 0);

            String mText = typedArray.getString(R.styleable.MyImageTextView_text);
            float mTextSize = typedArray.getDimension(R.styleable.MyImageTextView_text_size, 12);
            ColorStateList mTextColor = typedArray.getColorStateList(R.styleable.MyImageTextView_text_color);
            float mTextMargin = typedArray.getDimension(R.styleable.MyImageTextView_text_margin, 10);
            String mTextDirection = typedArray.getString(R.styleable.MyImageTextView_text_direction);

            typedArray.recycle();

            LayoutParams imgParams = null;
            if (img_width == 0 || img_height == 0) {
                imgParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            } else {
                imgParams = new LayoutParams((int) img_width, (int) img_height);
            }
            mImageView.setLayoutParams(imgParams);
            if (mDrawable != null) {
                mImageView.setImageDrawable(mDrawable);
            }

            mTextView.setText(mText);
            mTextView.setTextSize(mTextSize);
            mTextView.setTextColor(mTextColor);
            mTextView.setGravity(Gravity.VERTICAL_GRAVITY_MASK);
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            if (TextDirection.LEFT.equals(mTextDirection)) {
                this.setOrientation(HORIZONTAL);
                params.rightMargin = (int) mTextMargin;
                mTextView.setLayoutParams(params);
                this.addView(mTextView);
                this.addView(mImageView);
            } else if (TextDirection.RIGHT.equals(mTextDirection)) {
                this.setOrientation(HORIZONTAL);
                params.leftMargin = (int) mTextMargin;
                mTextView.setLayoutParams(params);
                this.addView(mImageView);
                this.addView(mTextView);
            } else if (TextDirection.TOP.equals(mTextDirection)) {
                this.setOrientation(VERTICAL);
                params.bottomMargin = (int) mTextMargin;
                mTextView.setLayoutParams(params);
                this.addView(mTextView);
                this.addView(mImageView);
            } else if (TextDirection.BOTTOM.equals(mTextDirection)) {
                this.setOrientation(VERTICAL);
                params.topMargin = (int) mTextMargin;
                mTextView.setLayoutParams(params);
                this.addView(mImageView);
                this.addView(mTextView);
            } else {
                this.setOrientation(VERTICAL);
                params.topMargin = (int) mTextMargin;
                mTextView.setLayoutParams(params);
                this.addView(mImageView);
                this.addView(mTextView);
            }
        }
    }

    public class TextDirection {
        public static final String LEFT = "left";
        public static final String TOP = "top";
        public static final String RIGHT = "right";
        public static final String BOTTOM = "bottom";
    }
}
