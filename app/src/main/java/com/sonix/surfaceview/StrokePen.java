package com.sonix.surfaceview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ljm 带笔锋的绘制
 * @date 2020/8/31.
 */
public class StrokePen extends BasePen {

    private static final String TAG = "StrokePen";
    private Paint mPaint;
    private Context mContext;
    /**
     * 笔迹颜色
     */
    private String penColor = "#000000";
    /**
     * 笔迹宽度
     */
    private float penWidth = 1.0f;
    public List<ControllerPoint> mHWPointList = Collections.synchronizedList(new ArrayList<ControllerPoint>());
    /**
     * 计算出来的线段宽度
     */
    public double mLastWidth;
    public ControllerPoint mLastPoint = new ControllerPoint(0, 0);

    private ControllerPoint mCurPoint;
    private Bezier mBezier = new Bezier();

    /**
     * 转换比率
     */
    private float TRANSFORM_SCALE = 40.00f;

    public StrokePen(Context context) {
        mContext = context;
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor(penColor));
        penWidth = transformWidth(1.0f);
        mPaint.setStrokeWidth(penWidth);
        mPaint.setStyle(Paint.Style.FILL);
        //结束的笔画为圆心
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //连接处元
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setDither(true);
        mPaint.setAlpha(0xFF);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeMiter(1.0f);
    }

    /**
     * 根据像素密度动态计算笔迹宽度
     *
     * @param width
     * @return
     */
    private float transformWidth(float width) {
        float density = mContext.getResources().getDisplayMetrics().density;
        Log.i(TAG, "transformWidth: width=" + width + "///density=" + density);
        return width * density * 0.5f;
    }
//    private float transformWidth(float width) {
//        float density = mContext.getResources().getDisplayMetrics().density;
//        return width / density;
//    }

    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */

    @Override
    public void setPenColor(String colorValue) {
        if (mPaint != null) {
            if (!this.penColor.equals(colorValue)) {
                try {
                    mPaint.setColor(Color.parseColor(colorValue));
                    this.penColor = colorValue;
                } catch (Exception e) {
                    e.printStackTrace();
                    mPaint.setColor(Color.BLACK);
                }
            }
        }
    }

    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */
    @Override
    public void setPenColor(int colorValue) {
        if (mPaint != null) {
            if (!this.penColor.equals(colorValue)) {
                try {
                    mPaint.setColor(colorValue);
                    this.penColor = String.format("#%08X", 0xFFFFFFFF & colorValue);
                } catch (Exception e) {
                    e.printStackTrace();
                    mPaint.setColor(Color.BLACK);
                }
            }
        }
    }

    @Override
    public void setPenWidth(float penWidth) {
        if (mPaint != null) {
            if (this.penWidth != penWidth) {
                Log.i(TAG, "setPenWidth: " + penWidth);
                float transformWidth = transformWidth(penWidth);
                this.penWidth = transformWidth;
                mPaint.setStrokeWidth(transformWidth);
            }
        }
    }

    @Override
    public void BGSize(int bgWidth, int bgHeight) {

    }

    @Override
    public String getPenColor() {
        return penColor;
    }

    @Override
    public float getPenWidth() {
        return penWidth;
    }

    @Override
    public Bitmap getBitmap() {
        return null;
    }

    @Override
    public void onDown(float pointX, float pointY, int force, Canvas canvas) {
        //Log.i(TAG, "onDown: pointX=" + pointX + "/pointY" + pointY + "/force" + force + "/penWidth" + penWidth);
        float pressure = calculatePressure(force);
        if (mPaint == null) {
            throw new NullPointerException("paint 笔不可能为null哦");
        }
        mHWPointList.clear();
        //记录down的控制点的信息
        ControllerPoint curPoint = new ControllerPoint(pointX, pointY);
        //如果用笔画的画我的屏幕，记录他宽度的和压力值的乘
        mLastWidth = (float) pressure / TRANSFORM_SCALE * penWidth;
        //down下的点的宽度
        curPoint.width = (float) mLastWidth;
        mHWPointList.add(curPoint);
        //记录当前的点
        mLastPoint = curPoint;


    }

    @Override
    public void onMove(float pointX, float pointY, int force, Canvas canvas) {
        //Log.i(TAG, "onDown: pointX=" + pointX + "/pointY" + pointY + "/force" + force + "/penWidth" + penWidth);
        ControllerPoint curPoint = new ControllerPoint(pointX, pointY);
        float pressure = calculatePressure(force);
        double curWidth = (float) pressure / TRANSFORM_SCALE * penWidth;
        curPoint.width = (float) curWidth;
        double deltaX = curPoint.x - mLastPoint.x;
        double deltaY = curPoint.y - mLastPoint.y;
        //deltaX和deltay平方和的二次方根 想象一个例子 1+1的平方根为1.4 （x²+y²）开根号
        //同理，当滑动的越快的话，deltaX+deltaY的值越大，这个越大的话，curDis也越大
        double curDis = Math.hypot(deltaX, deltaY);
        //点的集合少，我们得必须改变宽度,每次点击的down的时候，这个事件
        if (mHWPointList.size() < 2) {
            mBezier.init(mLastPoint, curPoint);
        } else {
            mBezier.addNode(curPoint);
        }
        //每次移动的话，这里赋值新的值
        mLastWidth = curWidth;
        doMove(curDis);
        mLastPoint = curPoint;
    }

    @Override
    public void onUp(float pointX, float pointY, int force, Canvas canvas) {
        //Log.i(TAG, "onMove: pointX=" + pointX + "/pointY" + pointY);
        if (mHWPointList.size() == 0) {
            return;
        }
        mCurPoint = new ControllerPoint(pointX, pointY);
        double deltaX = mCurPoint.x - mLastPoint.x;
        double deltaY = mCurPoint.y - mLastPoint.y;
        double curDis = Math.hypot(deltaX, deltaY);
        mCurPoint.width = 0;
        mBezier.addNode(mCurPoint);
        int steps = 1 + (int) curDis / 10;
        double step = 1.0 / steps;
        for (double t = 0; t < 1.0; t += step) {
            ControllerPoint point = mBezier.getPoint(t);
            mHWPointList.add(point);
        }
        mBezier.end();
        for (double t = 0; t < 1.0; t += step) {
            ControllerPoint point = mBezier.getPoint(t);
            mHWPointList.add(point);
        }

        draws(canvas);//区别是只有在这里的时候才去绘制画布...画布得到后才会刷新在另一个画布
        // 在onDown下去的话，最近画的线的透明度有改变，所以这里clear下线的集合
        clear();
    }

    @Override
    public void draws(Canvas canvas) {
        //点的集合少 不去绘制
        if (mHWPointList == null || mHWPointList.size() < 1) {
            return;
        }
        doPreDraw(canvas);
    }

    private synchronized void doPreDraw(Canvas canvas) {
        mCurPoint = mHWPointList.get(0);
        int size = mHWPointList.size();
        for (int i = 1; i < size; i++) {
            ControllerPoint point = mHWPointList.get(i);
            drawToPoint(canvas, point, mPaint);
            mCurPoint = point;
        }
    }

    @Override
    public void clear() {
        mHWPointList.clear();
    }


    private void doMove(double curDis) {
        int steps = 1 + (int) curDis / 10;
        double step = 1.0 / steps;
        for (double t = 0; t < 1.0; t += step) {
            ControllerPoint point = mBezier.getPoint(t);
            mHWPointList.add(point);
        }
    }

    private void drawToPoint(Canvas canvas, ControllerPoint point, Paint paint) {
        if ((mCurPoint.x == point.x) && (mCurPoint.y == point.y)) {
            return;
        }
        doDraw(canvas, point, paint);
    }

    private void doDraw(Canvas canvas, ControllerPoint point, Paint paint) {
        drawLine(canvas, mCurPoint.x, mCurPoint.y, mCurPoint.width, point.x,
                point.y, point.width, paint);
    }


    private void drawLine(Canvas canvas, double x0, double y0, double w0, double x1, double y1, double w1, Paint paint) {
        //求两个数字的平方根 x的平方+y的平方在开方记得X的平方+y的平方=1，这就是一个园
        double curDis = Math.hypot(x0 - x1, y0 - y1);
        int steps = 1;
        if (penWidth <= 6) {
            steps = 1 + (int) (curDis);
        } else if (penWidth > 60) {
            steps = 1 + (int) (curDis / 4);
        } else {
            steps = 1 + (int) (curDis / 3);
        }
        double deltaX = (x1 - x0) / steps;
        double deltaY = (y1 - y0) / steps;
        double deltaW = (w1 - w0) / steps;
        double x = x0;
        double y = y0;
        double w = w0;
        for (int i = 0; i < steps; i++) {
            //都是用于表示坐标系中的一块矩形区域，并可以对其做一些简单操作
            //精度不一样。Rect是使用int类型作为数值，RectF是使用float类型作为数值。
            float left, top, right, bottom;
            RectF oval = new RectF();
            left = (float) (x + w / 2.0f);
            top = (float) (y + w / 2.0f);
            right = (float) (x - w / 2.0f);
            bottom = (float) (y - w / 2.0f);
            oval.set(left, top, right, bottom);
            //Log.i(TAG, "drawLine: left=" + left + "/top=" + top + "/right=" + right + "/bottom=" + bottom);
            //Log.i(TAG, "drawLine: width=" + Math.abs(left - right) + "/height" + Math.abs(top - bottom));
            //最基本的实现，通过点控制线，绘制椭圆
            canvas.drawOval(oval, paint);
            x += deltaX;
            y += deltaY;
            w += deltaW;
        }
    }
    /**
     * 区分压力值
     * @param force
     * @return
     */
    private int calculatePressure(int force) {
        int pressure = 1;
        if (force >= 0 && force <= 20) {
            pressure = 40;
        } else if (force > 20 && force <= 40) {
            pressure = 60;
        } else if (force > 40 && force <= 60) {
            pressure = 80;
        } else if (force > 60 && force <= 90) {
            pressure = 100;
        } else if (force > 90 && force <= 150) {
            pressure = 120;
        } else {
            pressure = 130;
        }
        return pressure;
    }
}
