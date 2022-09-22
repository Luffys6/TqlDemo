package com.sonix.oidbluetooth.view;

import static android.graphics.Region.Op.DIFFERENCE;
import static android.graphics.Region.Op.INTERSECT;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

public class ClipView extends View {
    private Paint mPaint = new Paint();
    private RectF rectF;
    private int paddingLeft, paddingRight, paddingTop, paddingBottom;

    public ClipView(Context context) {
        super(context);
    }

    public ClipView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        rectF = new RectF(100, 400, 400, 700);
        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();
        paddingTop = getPaddingTop();
        paddingBottom = getPaddingBottom();
    }

    public ClipView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
         canvas.drawRect(rectF, mPaint);
        canvas.drawCircle(400, 450, 150, mPaint);
        canvas.clipRect(rectF,INTERSECT);   // 1.
        Path path = new Path();
        path.addCircle(400, 450, 150, Path.Direction.CCW);
        canvas.drawPath(path,mPaint);
        canvas.clipPath(path, Region.Op.INTERSECT); // 2.
        canvas.drawColor(Color.GREEN);  // 3.

//        mPaint.setAntiAlias(true);
//        mPaint.setColor(Color.RED);
//        RectF rectF = new RectF(100, 100, 400, 400);
//        canvas.drawColor(Color.BLUE);
//        canvas.drawRect(rectF, mPaint);
//        canvas.drawCircle(400, 250, 150, mPaint);
//        canvas.clipRect(rectF);   // 1.
//        Path path = new Path();
//        path.addCircle(400, 250, 150, Path.Direction.CCW);
//        canvas.clipPath(path, Region.Op.INTERSECT); // 2.
//        canvas.drawColor(Color.GREEN);  // 3.
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 获取宽-测量规则的模式和大小
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        // 获取高-测量规则的模式和大小
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // 设置wrap_content的默认宽 / 高值
        // 默认宽/高的设定并无固定依据,根据需要灵活设置
        // 类似TextView,ImageView等针对wrap_content均在onMeasure()对设置默认宽 / 高值有特殊处理,具体读者可以自行查看
        int mWidth = 400;
        int mHeight = 400;

        // 当布局参数设置为wrap_content时，设置默认值
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mWidth, mHeight);
            // 宽 / 高任意一个布局参数为= wrap_content时，都设置默认值
        } else if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mWidth, heightSize);
        } else if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(widthSize, mHeight);
        }
    }
}
