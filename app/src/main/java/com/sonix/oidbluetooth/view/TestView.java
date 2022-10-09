package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class TestView extends View {
    private Paint paint;
    private Paint mPaint;
    public TestView(Context context) {
        super(context);
    }

    public TestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
//        paint.setColor(Color.BLUE);
//        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        int[] colors = {Color.RED, Color.GREEN, Color.BLUE};
//        float[] positions = {0f, 0.5f, 1f};
        float[] positions = {0.25f, 0.5f, 0.75f};
        LinearGradient linearGradient = new LinearGradient(100, 100, 650, 650, colors, positions, Shader.TileMode.CLAMP);
        paint.setShader(linearGradient);
        paint.setStrokeWidth(5F);
        paint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5F);
        mPaint.setTextSize(64);

//        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawRect(100F,100F,650F,650F,paint);
//        canvas.translate(0,800);
//        canvas.drawRect(100F,100F,650F,650F,paint);
//        canvas.drawLine(100,100,500,500,paint);
//        canvas.drawLines(new float[]{
//                400,500,1000,500,
//                400,600,1000,600,
//                400,800,1000,800
//        },mPaint);

//        RectF rectF = new RectF(100,100,800,400);
//        canvas.drawRoundRect(rectF,30,30,paint);

        // 以下示例：绘制两个起始角度为0度、扫过90度的圆弧
// 两者的唯一区别就是是否使用了中心点

        // 绘制圆弧1(无使用中心)
//        RectF rectF = new RectF(100, 100, 800,400);
//        // 绘制背景矩形
//        canvas.drawRect(rectF, paint);
//        // 绘制圆弧
//        canvas.drawArc(rectF, 0, 90, false, mPaint);
//
//        // 绘制圆弧2(使用中心)
//        RectF rectF2 = new RectF(300,600,800,900);
//        // 绘制背景矩形
//        canvas.drawRect(rectF2, paint);
//        // 绘制圆弧
//        canvas.drawArc(rectF2,0,90,true,mPaint);

        // 1.创建路径对象
        Path path = new Path();
        // 2. 设置路径轨迹
        path.cubicTo(540, 750, 640, 450, 840, 600);
        // 3. 画路径
        canvas.drawPath(path,paint);
        // 4. 画出在路径上的字
        canvas.drawTextOnPath("在Path上写的字:Carson_Ho", path, 100, -20, mPaint);



    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
     }
}
