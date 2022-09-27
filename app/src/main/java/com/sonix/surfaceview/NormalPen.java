package com.sonix.surfaceview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.widget.Toast;

import com.sonix.oidbluetooth.MainActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 涂鸦笔
 *
 * @author ljm
 * @date 2020/8/29
 */
public class NormalPen extends BasePen {
    private static final String TAG = "NormalPen";
    /**
     * 保存涂鸦轨迹的集合
     */
    private List<DrawPath> mPathList = Collections.synchronizedList(new ArrayList<DrawPath>());

    /**
     * 保存撤销笔迹
     */
    private List<DrawPath> mSavePathList = new ArrayList<>();
    /**
     * 当前的涂鸦轨迹
     */
    private DrawPath mCurrentPath;

    /**
     * 画笔
     */
    private Paint sPaint;

    /**
     * 笔迹颜色
     */
    private String penColor = "#000000";

    /**
     * 涂鸦颜色
     */
    private String doodleColor = "#ff000000";
    /**
     * 笔迹宽度
     */
    private float penWidth = 2.53125f;
    /**
     * 涂鸦宽度
     */
    private float doodleWith = 20.0f;

    private static int lastListSize;
    private List<DrawPath> mWeakReferencePathList = new ArrayList<>();

    private float mLastX, mLastY;
    private Context mContext;
    //是否是涂鸦模式
    private boolean isDoodle = false;
    private int bgWidth, bgHeight;

    public NormalPen(Context context) {
        this.mContext = context;
        sPaint = new Paint();
        sPaint.setColor(Color.parseColor(penColor));
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setStrokeWidth(transformWidth(1.5f));
        sPaint.setAntiAlias(true);
        sPaint.setStrokeCap(Paint.Cap.ROUND);
        sPaint.setStrokeMiter(1.0f);
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
        return width * density * 0.75f;
    }


    /**
     * 切换手写
     */
    public void switchHandWrite() {
        isDoodle = false;
        sPaint.setStrokeCap(Paint.Cap.ROUND);
        // sPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        sPaint.setStrokeWidth(penWidth);


    }

    /**
     * 切换涂鸦
     */
    public void switchDoodle() {
        isDoodle = true;
        sPaint.setStrokeCap(Paint.Cap.SQUARE);
        //在笔迹的下面绘制
        sPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
        sPaint.setStrokeWidth(doodleWith);
    }


    @Override
    public void setPenColor(String colorValue) {
        if (sPaint != null) {
            if (!this.penColor.equals(colorValue)) {
                try {
                    sPaint.setColor(Color.parseColor(colorValue));
                    if (isDoodle) {
                        doodleColor = colorValue;
                    } else {
                        penColor = colorValue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sPaint.setColor(Color.BLACK);
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
        if (sPaint != null) {
            if (!this.penColor.equals(colorValue)) {
                try {
                    sPaint.setColor(colorValue);
                    String color = String.format("#%08X", 0xFFFFFFFF & colorValue);
                    if (isDoodle) {
                        doodleColor = color;
                    } else {
                        penColor = color;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sPaint.setColor(Color.BLACK);
                }
            }
        }
    }

    @Override
    public void setPenWidth(float width) {
        if (width == 1.0) {
            width = 1.5f;
        }
        Log.i(TAG, "setPenWidth width: " + width);
        if (sPaint != null) {
            if (this.penWidth != width) {
                float transformWidth = transformWidth(width);
                Log.i(TAG, "setPenWidth: " + transformWidth);
                sPaint.setStrokeWidth(transformWidth);
                if (isDoodle) {
                    doodleWith = transformWidth;
                } else {
                    penWidth = transformWidth;
                }
            }
        }
    }

    @Override
    public void BGSize(int bgWidth, int bgHeight) {
        this.bgHeight = bgHeight;
        this.bgWidth = bgWidth;
    }

    @Override
    public String getPenColor() {
        return isDoodle ? doodleColor : penColor;
    }

    @Override
    public float getPenWidth() {
        return isDoodle ? doodleWith : penWidth;
    }

    @Override
    public void onDown(float x, float y, int force, Canvas canvas) {
        mCurrentPath = new DrawPath();
        mCurrentPath.color = isDoodle ? doodleColor : penColor;
        mCurrentPath.width = isDoodle ? doodleWith : penWidth;
        mCurrentPath.path = new Path();
        mCurrentPath.path.moveTo(x, y);
        mPathList.add(mCurrentPath);

        mLastX = x;
        mLastY = y;
        moveNumber = 0;
    }


    private int moveNumber;

    @Override
    public void onMove(float x, float y, int force, Canvas canvas) {

        if (mCurrentPath != null && mCurrentPath.path != null) {

            mCurrentPath.path.quadTo(mLastX,
                    mLastY,
                    (x + mLastX) / 2,
                    (y + mLastY) / 2);

//        mCurrentPath.path.lineTo(x, y);

            moveNumber++;
            if (moveNumber == 300) {
                //也就是300个move的时候
//                Log.e(TAG,"超过300个了  重新计算");
                onUp(x, y, 0, canvas);//设置一个up点
                onDown(x, y, force, canvas);//再同时设置一个Down点  让mCurrentPath清空一下   避免mCurrentPath太大造成的卡顿
            }

            mLastX = x;
            mLastY = y;


        }
    }

    @Override
    public void onUp(float x, float y, int force, Canvas canvas) {
        if (mCurrentPath != null && mCurrentPath.path != null) {
            mCurrentPath.path.quadTo(mLastX,
                    mLastY,
                    (x + mLastX) / 2,
                    (y + mLastY) / 2);


//            bitmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
//            Canvas canvas1 = new Canvas(bitmap);
//            draws(canvas1);

            draws(canvas);

            mLastX = 0;//清0
            mLastY = 0;


            moveNumber = 0;
            mPathList.clear();

        }
    }

    private void drawPathCanvas(Canvas canvas) {
        if (sPaint.getStrokeWidth() == getPenWidth() && sPaint.getColor() == Color.parseColor(getPenColor())) {
            //宽度相等的情况下去绘制,  偶尔会出现不同宽度 不同颜色的异常情况
            //直接排除这一笔画
            canvas.drawPath(mCurrentPath.path, sPaint);
        }
    }

    @Override
    public void draws(Canvas canvas) {
        if (mPathList == null || mPathList.size() < 1) {
            return;
        }
        doPreDraw(canvas);
    }

    private Bitmap bitmap;
    private Canvas canvas1;

    private synchronized void doPreDraw(Canvas canvas) {
        mWeakReferencePathList.clear();
        mWeakReferencePathList.addAll(mPathList);//用一个新的集合来遍历
        //避免遍历的时候  mPathList在其他地方做了增删操作  导致异常java.util.ConcurrentModificationException异常
        for (DrawPath path : mWeakReferencePathList) {
            //用mPathList循环会出现界面闪烁  笔画断一点或者颜色异常
            sPaint.setColor(Color.parseColor(path.color));
            sPaint.setStrokeWidth(path.width);
            canvas.drawPath(path.path, sPaint);
        }

//        lastListSize = mPathList.size();

    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public void clear() {
        lastListSize = 0;
        mPathList.clear();
        mSavePathList.clear();
    }

    /**
     * 反撤销
     */
    public void redo() {
        if (mSavePathList != null && mSavePathList.size() > 0) {
            mPathList.add(mSavePathList.get(0));
            mSavePathList.remove(0);
        }
    }

    /**
     * 撤销
     */
    public void undo() {
        if (mPathList != null && mPathList.size() > 0) {
            Log.i(TAG, "undo: " + mSavePathList.size());
            //删除最后一位数据，并保存撤销笔迹
            DrawPath drawPath = mPathList.get(mPathList.size() - 1);
            mSavePathList.add(drawPath);
            mPathList.remove(drawPath);
            if (lastListSize > 0) {
                lastListSize -= 1;
            }
        } else {
            Toast.makeText(mContext, "无可撤销数据", Toast.LENGTH_SHORT).show();
        }
    }

    private static class DrawPath {
        public Path path;
        public String color;
        public float width;
    }

    float preX = 0f;
    float preY = 0f;
    Path mDrawPath = new Path();

    private void drawQuadTo(int ntype, float pointX, float pointY, int force, Canvas canvas) {
        float curX = pointX;
        float curY = pointY;
        sPaint.setAntiAlias(true);
        sPaint.setDither(true);

        if (ntype == 0) {
            mDrawPath.reset();
            mDrawPath.moveTo(curX, curY);
        } else {
            //mDrawPath.quadTo(curX, curY, (preX + curX) / 2, (preY + curY) / 2);
            mDrawPath.lineTo(curX, curY);
        }
        canvas.drawPath(mDrawPath, sPaint);

        preX = curX;
        preY = curY;
    }
}
