package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tqltech.tqlpencomm.Constants;
import com.tqltech.tqlpencomm.bean.Dot;

import java.util.ArrayList;

/**
 * 自定义绘制view
 */
public class NewDrawView extends View {
    private static final String TAG = "DrawView";
    private Context mContext;
    private int BG_WIDTH;
    private int BG_HEIGHT;
    private Bitmap mBitmap;
    private String penColor = "#000000";
    private float penWidth = 1.5f;
    private Paint mPaint;
    private Canvas mCanvas;
    //码点规格
//    private double XDIST_PERUNIT = Constants.XDIST_PERUNIT;
//    private double YDIST_PERUNIT = Constants.XDIST_PERUNIT;

    //码点规格：16*16
    private double XDIST_PERUNIT = Constants.getXdistPerunit();
    private double YDIST_PERUNIT = Constants.getYdistPerunit();

    //笔记本实际规格大小默认B5笔记本
    private double PAPER_WIDTH = 256;
    private double PAPER_HEIGHT = 181;
    public ArrayList<ControllerPoints> mHWPointList = new ArrayList<>();
    public ArrayList<ControllerPoints> mPointList = new ArrayList<>();
    //计算出来的线段宽度
    public double mLastWidth;
    public double mLastVel;
    public ControllerPoints mLastPoint = new ControllerPoints(0, 0);

    private ControllerPoints mCurPoint;
    private float pointX, pointY;
    //控制点的，
    private ControllerPoints mControl = new ControllerPoints();
    //距离
    private ControllerPoints mDestination = new ControllerPoints();
    //下一个需要控制点
    private ControllerPoints mNextControl = new ControllerPoints();
    //资源的点
    private ControllerPoints mSource = new ControllerPoints();
    //转换比率
    private float TRANSFORM_SCALE = 40.00f;

    //钢笔类型
    public static final int TYPE_STEEL_PEN = 0;
    //荧光笔
    public static final int TYPE_FLUORESCENT_PEN = 1;
    //当前笔的类型
    private int currentPenStyle = TYPE_STEEL_PEN;
    //最后的xy触摸或者笔座标
    private float mLastX, mLastY;
    private Path mPath;
    private Paint sPaint;
    private OnTouchEvent mOnTouchEvent;

    public NewDrawView(Context context) {
        this(context, null);
    }

    public NewDrawView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NewDrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setOnTouchEventListener(OnTouchEvent eventListener) {
        this.mOnTouchEvent = eventListener;
    }


    /**
     * 设置纸张参数
     *
     * @param paperWidth
     * @param paperHeight
     */
    public void setPaperParameter(@NonNull double paperWidth, @NonNull double paperHeight) {
        this.PAPER_WIDTH = paperWidth;
        this.PAPER_HEIGHT = paperHeight;
    }

    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */
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
     * 获取笔迹颜色
     *
     * @return
     */
    public String getPenColor() {
        return penColor;
    }

    /**
     * 设置笔迹宽度
     *
     * @param penWidth
     */
    public void setPenWidth(float penWidth) {
        if (mPaint != null) {
            if (this.penWidth != penWidth) {
                this.penWidth = penWidth;
                mPaint.setStrokeWidth(penWidth);
            }
        }
    }

    /**
     * 获取笔迹宽度
     *
     * @return
     */
    public float getPenWidth() {
        return penWidth;
    }

    /**
     * 获取paint对象
     *
     * @return
     */
    public Paint getPaint() {
        return mPaint;
    }

    /**
     * 获取canvas对象
     *
     * @return
     */
    public Canvas getCanvas() {
        return mCanvas;
    }

    private void init(final Context context) {
        mContext = context;
        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor(penColor));
        mPaint.setStrokeWidth(penWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);//结束的笔画为圆心
        mPaint.setStrokeJoin(Paint.Join.ROUND);//连接处元
        mPaint.setAlpha(0xFF);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeMiter(1.0f);
    }

    private void initCanvas() {
        if (mCanvas == null) {
            mCanvas = new Canvas();
        }
        mCanvas.setBitmap(mBitmap);
        mCanvas.drawColor(Color.TRANSPARENT);
    }

    /**
     * 设置笔的类型
     *
     * @param penStyle
     */
    public void setPenStyle(int penStyle) {
        currentPenStyle = penStyle;
        if (penStyle == TYPE_STEEL_PEN) {
            initPaint();
            setOnTouchListener(null);
        } else if (penStyle == TYPE_FLUORESCENT_PEN) {
            sPaint = new Paint();
            sPaint.setColor(Color.GREEN);
            sPaint.setAlpha(8);
            sPaint.setStyle(Paint.Style.STROKE);
            sPaint.setStrokeWidth(40);
            sPaint.setAntiAlias(true);
            sPaint.setStrokeCap(Paint.Cap.SQUARE);
            sPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)); //在笔迹的下面绘制
            setOnTouchListener(mOnTouchListener);
        }
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            MotionEvent motionEvent = MotionEvent.obtain(event);
            float pressure = motionEvent.getPressure();
            // setPenColorAlpha(pressure);
            Log.i(TAG, "onTouch:压力敏感度 " + motionEvent.getPressure());
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = x;
                    mLastY = y;
                    if (mPath == null) {
                        mPath = new Path();
                    }
                    mPath.moveTo(x, y);
                    invalidate();
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onDown(x, y, pressure);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    mPath.quadTo(mLastX,
                            mLastY,
                            (x + mLastX) / 2,
                            (y + mLastY) / 2);
                    mCanvas.drawPath(mPath, sPaint);
                    invalidate();
                    mLastX = x;
                    mLastY = y;
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onMove(x, y, pressure);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    mPath.quadTo(mLastX,
                            mLastY,
                            (x + mLastX) / 2,
                            (y + mLastY) / 2);
                    mCanvas.drawPath(mPath, sPaint);
                    mPath = null;
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onUp(x, y, pressure);
                    }
                    invalidate();
                    break;
            }
            return true;
        }
    };

    /**
     * 获取当前笔的类型
     *
     * @return
     */
    public int getPenStyle() {
        return currentPenStyle;
    }


    /**
     * 清除画布，记得清除点的集合
     */
    public void reset() {
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCanvas.drawPaint(mPaint);
        clear();
        initPaint();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.i(TAG, "onDraw: -------------");
        if (mBitmap != null) {
            //Log.i(TAG, "onDraw: bitmap not null");
            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
        }
        draws(canvas);
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "onMeasure: -----------");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.i(TAG, "onLayout: ----------------");
//        BG_WIDTH = right - left;
//        BG_HEIGHT = bottom - top;
//        mBitmap = Bitmap.createBitmap(BG_WIDTH, BG_HEIGHT, Bitmap.Config.ARGB_8888);
//        initCanvas();
    }

    //设置自定义view的宽高
    public void setLayoutParameter(int layoutWidth, int layoutHeight) {
        BG_WIDTH = layoutWidth;
        BG_HEIGHT = layoutHeight;
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        //mBitmap = Bitmap.createBitmap(BG_WIDTH, BG_HEIGHT, Bitmap.Config.ARGB_8888);
        mBitmap = Bitmap.createBitmap(1900, 2300, Bitmap.Config.ARGB_8888);
        initCanvas();
    }

    public void updateParameter(int layoutWidth, int layoutHeight) {
        BG_WIDTH = layoutWidth;
        BG_HEIGHT = layoutHeight;
    }

    public void processDot(float pointX, float pointY, int force, int dotType) {
        Log.i(TAG, "pointX: " + pointX + "pointY:" + pointY + "force:" + force + "dotType:" + dotType);
        switch (dotType) {
            case 0:
                onDown(pointX, pointY, force);
                break;
            case 1:
                onMove(pointX, pointY, force);
                break;
            case 2:
                onUp(pointX, pointY);
                break;
        }
        invalidate();
    }

    public void processDot(Dot dot) {
        Log.i(TAG, "processDot:压力值 " + dot.force);
        int tmpx = dot.x;
        pointX = dot.fx;
        pointX /= 100.0;
        pointX += tmpx;

        int tmpy = dot.y;
        pointY = dot.fy;
        pointY /= 100.0;
        pointY += tmpy;

        pointX *= (BG_WIDTH);
        float ax = (float) (PAPER_WIDTH / XDIST_PERUNIT);
        pointX /= ax;

        pointY *= (BG_HEIGHT);
        float ay = (float) (PAPER_HEIGHT / YDIST_PERUNIT);
        pointY /= ay;
        switch (dot.type) {
            case PEN_DOWN:
                onDown(pointX, pointY, dot.force);
                break;
            case PEN_MOVE:
                onMove(pointX, pointY, dot.force);
                break;
            case PEN_UP:
                onMove(pointX, pointY, 1);
                onUp(pointX, pointY);
                break;
        }
        invalidate();
    }

    public float getPenWidth(int penWidth, int pointZ) {
        float mPenWidth = 1;
        if (penWidth == 1) {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 0.8;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 1.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 1.2;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 1.4;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 1.6;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 1.8;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 1.9;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 2.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 2.1;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 2.2;
            } else if (pointZ > 800) {
                mPenWidth = (float) 2.4;
            }
        } else if (penWidth == 2) {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 1.6;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 2.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 2.4;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 2.8;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 3.2;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 3.6;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 3.8;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 4.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 4.2;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 4.4;
            } else if (pointZ > 800) {
                mPenWidth = (float) 4.8;
            }
        } else if (penWidth == 3) {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 2.4;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 3.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 3.6;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 4.2;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 4.8;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 5.4;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 5.7;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 6.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 6.3;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 6.6;
            } else if (pointZ > 800) {
                mPenWidth = (float) 7.2;
            }
        } else if (penWidth == 4) {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 3.2;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 4.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 4.8;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 5.6;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 6.4;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 7.2;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 7.6;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 8.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 8.4;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 8.8;
            } else if (pointZ > 800) {
                mPenWidth = (float) 9.6;
            }
        } else if (penWidth == 5) {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 4.0;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 5.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 6.0;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 7.0;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 8.0;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 9.0;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 9.5;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 10.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 10.5;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 11.0;
            } else if (pointZ > 800) {
                mPenWidth = (float) 12.0;
            }
        } else if (penWidth == 6) {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 4.8;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 6.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 7.2;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 8.4;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 9.6;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 10.8;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 11.4;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 12.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 12.6;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 13.2;
            } else if (pointZ > 800) {
                mPenWidth = (float) 14.4;
            }
        } else {
            if (pointZ >= 0 && pointZ <= 50) {
                mPenWidth = (float) 3.2;
            }
            if (pointZ > 50 && pointZ <= 90) {
                mPenWidth = (float) 4.0;
            } else if (pointZ > 90 && pointZ <= 120) {
                mPenWidth = (float) 4.8;
            } else if (pointZ > 120 && pointZ <= 150) {
                mPenWidth = (float) 5.6;
            } else if (pointZ > 150 && pointZ <= 190) {
                mPenWidth = (float) 6.4;
            } else if (pointZ > 190 && pointZ <= 210) {
                mPenWidth = (float) 7.2;
            } else if (pointZ > 210 && pointZ <= 330) {
                mPenWidth = (float) 7.6;
            } else if (pointZ > 330 && pointZ <= 500) {
                mPenWidth = (float) 8.0;
            } else if (pointZ > 500 && pointZ <= 650) {
                mPenWidth = (float) 8.4;
            } else if (pointZ > 650 && pointZ <= 800) {
                mPenWidth = (float) 8.8;
            } else if (pointZ > 800) {
                mPenWidth = (float) 9.6;
            }
        }
        return mPenWidth;
    }

    private void onDown(float pointX, float pointY, int force) {
        int pressure = 1;
//        if (force >= 0 && force <= 100) {
//            pressure = 100;
//        } else if (force > 100 && force <= 160) {
//            pressure = 160;
//        } else if (force > 160 && force <= 220) {
//            pressure = 220;
//        } else {
//            pressure = 220;
//        }
        if (force > 0 && force <= 80) {
            pressure = 80;
        } else if (force > 80 && force <= 110) {
            pressure = 110;
        } else if (force > 110 && force <= 170) {
            pressure = 170;
        } else if (force > 170 && force <= 190) {
            pressure = 190;
        } else if (force > 190 && force <= 210) {
            pressure = 210;
        } else if (force > 210) {
            pressure = 210;
        }

        if (mPaint == null) {
            throw new NullPointerException("paint 笔不可能为null哦");
        }
        mPointList.clear();
        //如果在brush字体这里接受到down的事件，把下面的这个集合清空的话，那么绘制的内容会发生改变
        //不清空的话，也不可能
        mHWPointList.clear();
        //记录down的控制点的信息
        ControllerPoints curPoint = new ControllerPoints(pointX, pointY);
        //如果用笔画的画我的屏幕，记录他宽度的和压力值的乘，但是哇，
        mLastWidth = (float) pressure / TRANSFORM_SCALE * penWidth;
        //down下的点的宽度
        curPoint.width = (float) mLastWidth;
        mLastVel = 0;
        mPointList.add(curPoint);
        //记录当前的点
        mLastPoint = curPoint;
    }

    private void onMove(float pointX, float pointY, int force) {
        int pressure = 1;
//        if (force >= 0 && force <= 100) {
//            pressure = 100;
//        } else if (force > 100 && force <= 160) {
//            pressure = 160;
//        } else if (force > 160 && force <= 220) {
//            pressure = 220;
//        } else {
//            pressure = 220;
//        }
        if (force > 0 && force <= 80) {
            pressure = 80;
        } else if (force > 80 && force <= 110) {
            pressure = 110;
        } else if (force > 110 && force <= 170) {
            pressure = 170;
        } else if (force > 170 && force <= 190) {
            pressure = 190;
        } else if (force > 190 && force <= 210) {
            pressure = 210;
        } else if (force > 210) {
            pressure = 210;
        }
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
            Log.i(TAG, "onMove: pressure" + force + "积" + (float) (pressure / TRANSFORM_SCALE));
            curPoint.width = (float) curWidth;
            addNodeBezier(curPoint);
        }
        //每次移动的话，这里赋值新的值
        mLastWidth = curWidth;
        mPointList.add(curPoint);
        moveNeetToDo(curDis);
        mLastPoint = curPoint;
    }

    public void addNodeBezier(ControllerPoints cur) {
        mSource.set(mDestination);
        mControl.set(mNextControl);
        mDestination.set(getMid(mNextControl.x, cur.x), getMid(mNextControl.y, cur.y), getMid(mNextControl.width, cur.width));
        mNextControl.set(cur.x, cur.y, cur.width);
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

    private void onUp(float pointX, float pointY) {
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

        // 手指up 我画到纸上上
        draws(mCanvas);
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

    private void clear() {
        mHWPointList.clear();
        mPointList.clear();
    }

    public void draws(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        //点的集合少 不去绘制
        if (mHWPointList == null || mHWPointList.size() < 1)
            return;
        //当控制点的集合很少的时候，需要画个小圆，但是需要算法
        if (mHWPointList.size() < 2) {
            ControllerPoints point = mHWPointList.get(0);
            //由于此问题在算法上还没有实现，所以暂时不给他画圆圈
            //canvas.drawCircle(point.x, point.y, point.width, mPaint);
        } else {
            mCurPoint = mHWPointList.get(0);
            drawNeetToDo(canvas);
        }
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
        if (paint.getStrokeWidth() < 6) {
            steps = 1 + (int) (curDis / 2);
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

        for (int i = 0; i < steps; i++) {
            //都是用于表示坐标系中的一块矩形区域，并可以对其做一些简单操作
            //精度不一样。Rect是使用int类型作为数值，RectF是使用float类型作为数值。
            //            Rect rect = new Rect();
            RectF oval = new RectF();
            oval.set((float) (x - w / 4.0f), (float) (y - w / 2.0f), (float) (x + w / 4.0f), (float) (y + w / 2.0f));
            // oval.set((float)(x+w/4.0f), (float)(y+w/4.0f), (float)(x-w/4.0f), (float)(y-w/4.0f));
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

    static class ControllerPoints {
        public float x;
        public float y;

        public float width;
        public int alpha = 255;

        public ControllerPoints() {
        }

        public ControllerPoints(float x, float y) {
            this.x = x;
            this.y = y;
        }


        public void set(float x, float y, float w) {
            this.x = x;
            this.y = y;
            this.width = w;
        }


        public void set(ControllerPoints point) {
            this.x = point.x;
            this.y = point.y;
            this.width = point.width;
        }


        public String toString() {
            String str = "X = " + x + "; Y = " + y + "; W = " + width;
            return str;
        }
    }

    public interface OnTouchEvent {
        void onDown(float x, float y, float pressure);

        void onMove(float x, float y, float pressure);

        void onUp(float x, float y, float pressure);
    }
}
