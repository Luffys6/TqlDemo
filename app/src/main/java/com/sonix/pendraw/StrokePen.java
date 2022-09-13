package com.sonix.pendraw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;

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
    public ArrayList<ControllerPoints> mHWPointList = new ArrayList<>();
    public ArrayList<ControllerPoints> mPointList = new ArrayList<>();
    /**
     * 计算出来的线段宽度
     */
    public double mLastWidth;
    public double mLastVel;
    public ControllerPoints mLastPoint = new ControllerPoints(0, 0);

    private ControllerPoints mCurPoint;

    /**
     * 控制点的，
     */
    private ControllerPoints mControl = new ControllerPoints();
    /**
     * 距离
     */
    private ControllerPoints mDestination = new ControllerPoints();
    /**
     * 下一个需要控制点
     */
    private ControllerPoints mNextControl = new ControllerPoints();
    /**
     * 资源的点
     */
    private ControllerPoints mSource = new ControllerPoints();
    /**
     * 转换比率
     */
    private float TRANSFORM_SCALE = 40.00f;

    public StrokePen(Context context) {
        mContext = context;
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor(penColor));
        mPaint.setStrokeWidth(penWidth);
        // mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStyle(Paint.Style.FILL);
        //结束的笔画为圆心
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //连接处元
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setAlpha(0xFF);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeMiter(1.0f);
    }

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
                    this.penColor = String.format("#%06X", 0xFFFFFF & colorValue);
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
                this.penWidth = penWidth;
                mPaint.setStrokeWidth(penWidth);
            }
        }
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
    public void onDown(float pointX, float pointY, int force) {
        //Log.i(TAG, "onDown: pointX=" + pointX + "/pointY" + pointY + "/force" + force);
        float pressure = calculatePressure(force);
        if (mPaint == null) {
            throw new NullPointerException("paint 笔不可能为null哦");
        }
        mPointList.clear();
        //如果在brush字体这里接受到down的事件，把下面的这个集合清空的话，那么绘制的内容会发生改变
        //不清空的话，也不可能
        mHWPointList.clear();
        //记录down的控制点的信息
        ControllerPoints curPoint = new ControllerPoints(pointX, pointY);
        //如果用笔画的画我的屏幕，记录他宽度的和压力值的乘
        mLastWidth = (float) pressure / TRANSFORM_SCALE * penWidth;
        //down下的点的宽度
        curPoint.width = (float) mLastWidth;
        mLastVel = 0;
        mPointList.add(curPoint);
        //记录当前的点
        mLastPoint = curPoint;
    }

    @Override
    public void onMove(float pointX, float pointY, int force) {
        float pressure = calculatePressure(force);
        ControllerPoints curPoint = new ControllerPoints(pointX, pointY);
        double deltaX = curPoint.x - mLastPoint.x;
        double deltaY = curPoint.y - mLastPoint.y;
        //deltaX和deltay平方和的二次方根 想象一个例子 1+1的平方根为1.4 （x²+y²）开根号
        //同理，当滑动的越快的话，deltaX+deltaY的值越大，这个越大的话，curDis也越大
        double curDis = Math.hypot(deltaX, deltaY);

        //我们求出的这个值越小，画的点或者是绘制椭圆形越多，这个值越大的话，绘制的越少，笔就越细，宽度越小
        double curVel = curDis * 0.02f;
        double curWidth;
        //点的集合少，我们得必须改变宽度,每次点击的down的时候，这个事件
        if (mPointList.size() < 2) {
            curWidth = (float) pressure / TRANSFORM_SCALE * penWidth;
            curPoint.width = (float) curWidth;
            initBezier(mLastPoint, curPoint);
        } else {
            mLastVel = curVel;
            curWidth = (float) pressure / TRANSFORM_SCALE * penWidth;
            //Log.i(TAG, "onMove: pressure" + force + "积" + (float) (pressure / TRANSFORM_SCALE));
            curPoint.width = (float) curWidth;
            addNodeBezier(curPoint);
        }
        //每次移动的话，这里赋值新的值
        mLastWidth = curWidth;
        mPointList.add(curPoint);
        moveNeetToDo(curDis);
        mLastPoint = curPoint;
    }

    @Override
    public void onUp(float pointX, float pointY, int force, Canvas canvas) {
        //Log.i(TAG, "onMove: pointX=" + pointX + "/pointY" + pointY);
        mCurPoint = new ControllerPoints(pointX, pointY);
        double deltaX = mCurPoint.x - mLastPoint.x;
        double deltaY = mCurPoint.y - mLastPoint.y;
        double curDis = Math.hypot(deltaX, deltaY);
        mCurPoint.width = 0;
        mPointList.add(mCurPoint);
        addNodeBezier(mCurPoint);
        int steps = 1 + (int) curDis / 10;
        double step = 1.0 / steps;
        for (double t = 0; t < 1.0; t += step) {
            ControllerPoints point = getPointBezier(t);
            mHWPointList.add(point);
        }
        //
        endBezier();
        for (double t = 0; t < 1.0; t += step) {
            ControllerPoints point = getPointBezier(t);
            mHWPointList.add(point);
        }

        draws(canvas);
        //每次抬起手来，就把集合清空，在水彩笔的那个地方，如果啊，我说如果不清空的话，每次抬起手来，
        // 在onDown下去的话，最近画的线的透明度有改变，所以这里clear下线的集合
        clear();
    }

    public void endBezier() {
        mSource.set(mDestination);
        float x = getMid(mNextControl.x, mSource.x);
        float y = getMid(mNextControl.y, mSource.y);
        float w = getMid(mNextControl.width, mSource.width);
        mControl.set(x, y, w);
        mDestination.set(mNextControl);
    }

    @Override
    public void draws(Canvas canvas) {
        //点的集合少 不去绘制
        if (mHWPointList == null || mHWPointList.size() < 1) {
            return;
        }
        //当控制点的集合很少的时候，需要画个小圆，但是需要算法
        if (mHWPointList.size() > 0) {
            if (mHWPointList.size() < 2) {
                ControllerPoints point = mHWPointList.get(0);
                //由于此问题在算法上还没有实现，所以暂时不给他画圆圈
                //canvas.drawCircle(point.x, point.y, point.width, mPaint);
            } else {
                mCurPoint = mHWPointList.get(0);
                drawNeetToDo(canvas);
            }
        }
    }

    @Override
    public void clear() {
        mHWPointList.clear();
        mPointList.clear();
    }


    private void initBezier(ControllerPoints last, ControllerPoints cur) {
        //资源点设置，最后的点的为资源点
        mSource.set(last.x, last.y, last.width);
        float xmid = getMid(last.x, cur.x);
        float ymid = getMid(last.y, cur.y);
        float wmid = getMid(last.width, cur.width);
        //距离点为平均点
        mDestination.set(xmid, ymid, wmid);
        //控制点为当前的距离点
        mControl.set(getMid(last.x, xmid), getMid(last.y, ymid), getMid(last.width, wmid));
        //下个控制点为当前点
        mNextControl.set(cur.x, cur.y, cur.width);
    }

    public void addNodeBezier(ControllerPoints cur) {
        mSource.set(mDestination);
        mControl.set(mNextControl);
        mDestination.set(getMid(mNextControl.x, cur.x), getMid(mNextControl.y, cur.y), getMid(mNextControl.width, cur.width));
        mNextControl.set(cur.x, cur.y, cur.width);
    }

    private void moveNeetToDo(double curDis) {
        int steps = 1 + (int) curDis / 10;
        double step = 1.0 / steps;
        for (double t = 0; t < 1.0; t += step) {
            ControllerPoints point = getPointBezier(t);
            mHWPointList.add(point);
        }
    }

    public ControllerPoints getPointBezier(double t) {
        float x = (float) getX(t);
        float y = (float) getY(t);
        float w = (float) getW(t);
        ControllerPoints point = new ControllerPoints();
        point.set(x, y, w);
        return point;
    }

    private void drawNeetToDo(Canvas canvas) {
        for (int i = 1; i < mHWPointList.size(); i++) {
            ControllerPoints point = mHWPointList.get(i);
            drawToPoint(canvas, point, mPaint);
            mCurPoint = point;
        }
    }

    protected void drawToPoint(Canvas canvas, ControllerPoints point, Paint paint) {
        if ((mCurPoint.x == point.x) && (mCurPoint.y == point.y)) {
            return;
        }
        //水彩笔的效果和钢笔的不太一样，交给自己去实现
        doNeetToDo(canvas, point, paint);
    }

    private void doNeetToDo(Canvas canvas, ControllerPoints point, Paint paint) {
        drawLine(canvas, mCurPoint.x, mCurPoint.y, mCurPoint.width, point.x,
                point.y, point.width, paint);
    }

    private void drawLine(Canvas canvas, double x0, double y0, double w0, double x1, double y1, double w1, Paint paint) {
        //求两个数字的平方根 x的平方+y的平方在开方记得X的平方+y的平方=1，这就是一个园
        double curDis = Math.hypot(x0 - x1, y0 - y1);
        int steps = 1;
        if (paint.getStrokeWidth() <= 6) {
            //Log.i(TAG, "drawLine: " + paint.getStrokeWidth());
            steps = 1 + (int) (curDis);
            //Log.i(TAG, "drawLine:== " + steps);
            //steps = 10 + (int) (curDis);
        } else if (paint.getStrokeWidth() > 60) {
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
        //Log.i(TAG, "drawLine: deltaX=" + deltaX + "//deltaY=" + deltaY);
        for (int i = 0; i < steps; i++) {
            //都是用于表示坐标系中的一块矩形区域，并可以对其做一些简单操作
            //精度不一样。Rect是使用int类型作为数值，RectF是使用float类型作为数值。
            //            Rect rect = new Rect();
            float left, top, right, bottom, centerX, centerY;
            RectF oval = new RectF();

            // oval.set((float) (x - w / 2.0f), (float) (y - w / 2.0f), (float) (x + w / 2.0f), (float) (y + w / 2.0f));
            //oval.set((float) (x + w / 4.0f), (float) (y + w / 2.0f), (float) (x - w / 4.0f), (float) (y - w / 2.0f));
//            if (deltaX < 0.4d && deltaY > 0.6d) {
//                Log.i(TAG, "drawLine: 竖直");
//                left = (float) (x + w / 0.9f);
//                top = (float) (y + w / 2.0f);
//
//                right = (float) (x - w / 0.9f);
//                bottom = (float) (y - w / 2.0f);
//
//            } else {
//                left = (float) (x + w / 2.0f);
//                top = (float) (y + w / 0.9f);
//
//                right = (float) (x - w / 2.0f);
//                bottom = (float) (y - w / 0.9f);
//            }
            left = (float) (x + w / 2.0f);
            top = (float) (y + w / 2.0f);

            right = (float) (x - w / 2.0f);
            bottom = (float) (y - w / 2.0f);

//            left = (float) (x + w / 2.0f);
//            top = (float) (y + w / 0.9f);
//            right = (float) (x - w / 2.0f);
//            bottom = (float) (y - w / 0.9f);

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


    private double getValue(double p0, double p1, double p2, double t) {
        double A = p2 - 2 * p1 + p0;
        double B = 2 * (p1 - p0);
        double C = p0;
        return A * t * t + B * t + C;
    }

    private double getX(double t) {
        return getValue(mSource.x, mControl.x, mDestination.x, t);
    }

    private double getY(double t) {
        return getValue(mSource.y, mControl.y, mDestination.y, t);
    }

    private double getW(double t) {
        return getWidth(mSource.width, mDestination.width, t);
    }

    /**
     * @param x1 一个点的x
     * @param x2 一个点的x
     * @return
     */
    private float getMid(float x1, float x2) {
        return (float) ((x1 + x2) / 2.0);
    }

    private double getWidth(double w0, double w1, double t) {
        return w0 + (w1 - w0) * t;
    }

    /**
     * 区分压力值
     *
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
            pressure = 110;
        } else {
            pressure = 130;
        }
        return pressure;
    }

    private static class ControllerPoints {
        public float x;
        public float y;
        public int type;
        public float width;
        public int alpha = 255;

        public ControllerPoints() {
        }

        public ControllerPoints(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public ControllerPoints(float x, float y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }


        public void set(float x, float y, float w) {
            this.x = x;
            this.y = y;
            this.width = w;
        }

        public void set(float x, float y, float w, int type) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.type = type;
        }


        public void set(ControllerPoints point) {
            this.x = point.x;
            this.y = point.y;
            this.width = point.width;
        }


        @Override
        public String toString() {
            String str = "X = " + x + "; Y = " + y + "; W = " + width;
            return str;
        }
    }
}
