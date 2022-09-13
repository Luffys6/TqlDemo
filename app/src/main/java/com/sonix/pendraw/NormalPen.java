package com.sonix.pendraw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
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
    private List<DrawPath> mPathList = new ArrayList<>();
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
    private float penWidth = 2.0f;
    /**
     * 涂鸦宽度
     */
    private float doodleWith = 20.0f;

    private float mLastX, mLastY;
    private Context mContext;
    //是否是涂鸦模式
    private boolean isDoodle = false;
    private Canvas mCanvas;

    public NormalPen(Context context) {
        this.mContext = context;
        sPaint = new Paint();
        sPaint.setColor(Color.parseColor(penColor));
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setStrokeWidth(penWidth);
        sPaint.setAntiAlias(true);
        sPaint.setStrokeCap(Paint.Cap.ROUND);
        sPaint.setStrokeMiter(1.0f);
        initCanvas();
    }

    private void initCanvas() {
        if (mCanvas == null) {
            mCanvas = new Canvas();
        }
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
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
        if (sPaint != null) {
            if (this.penWidth != width) {
                Log.i(TAG, "setPenWidth: " + width);
                sPaint.setStrokeWidth(width);
                if (isDoodle) {
                    doodleWith = width;
                } else {
                    penWidth = width;
                }
            }
        }
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
    public void onDown(float x, float y, int force) {
        mCurrentPath = new DrawPath();
        mCurrentPath.color = isDoodle ? doodleColor : penColor;
        Log.i(TAG, "onDown: " + mCurrentPath.color);
        mCurrentPath.width = isDoodle ? doodleWith : penWidth;
        mCurrentPath.path = new Path();
        mPathList.add(mCurrentPath);
        mCurrentPath.path.moveTo(x, y);
        mLastX = x;
        mLastY = y;
    }

    @Override
    public void onMove(float x, float y, int force) {
        //mLastX落笔x
        //mLastY落笔Y
        mCurrentPath.path.quadTo(mLastX,
                mLastY,
                (x + mLastX) / 2,
                (y + mLastY) / 2);
        mLastX = x;
        mLastY = y;
    }

    @Override
    public void onUp(float x, float y, int force, Canvas canvas) {
        mCurrentPath.path.quadTo(mLastX,
                mLastY,
                (x + mLastX) / 2,
                (y + mLastY) / 2);
        canvas.drawPath(mCurrentPath.path,sPaint);
        mPathList.clear();
        mCurrentPath = null;
    }

    @Override
    public void draws(Canvas canvas) {
        if (mPathList != null && mPathList.size() > 0) {
            Log.i(TAG, "draws: " + mPathList.size());
            for (DrawPath path : mPathList) {
                sPaint.setColor(Color.parseColor(path.color));
                sPaint.setStrokeWidth(path.width);
                canvas.drawPath(path.path, sPaint);
            }
        }
    }

    @Override
    public void clear() {
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
        } else {
            Toast.makeText(mContext, "无可撤销数据", Toast.LENGTH_SHORT).show();
        }
    }

    private static class DrawPath {
        public Path path;
        public String color;
        public float width;
    }
}
