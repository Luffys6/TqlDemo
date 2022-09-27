package com.sonix.surfaceview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * 绘制基类
 *
 * @author ljm
 * @date 2020/8/29.
 */
public abstract class BasePen {
    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */
    public abstract void setPenColor(String colorValue);

    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */
    public abstract void setPenColor(int colorValue);

    /**
     * 设置笔迹宽度
     *
     * @param penWidth
     */
    public abstract void setPenWidth(float penWidth);


    /**
     *
     *
     * @param bgWidth
     */
    public abstract void BGSize(int bgWidth,int bgHeight);




    /**
     * @return 获取笔迹颜色
     */
    public abstract String getPenColor();


    /**
     * @return 获取笔迹宽度
     */
    public abstract float getPenWidth();

    /**
     * @return 获取笔迹图
     */
    public abstract Bitmap getBitmap();

    /**
     * down状态
     *
     * @param x
     * @param y
     * @param force 压力值
     */
    public abstract void onDown(float x, float y, int force, Canvas canvas);

    /**
     * move状态
     *
     * @param x
     * @param y
     * @param force 压力值
     */
    public abstract void onMove(float x, float y, int force, Canvas canvas);


    /**
     * up状态
     *
     * @param x
     * @param y
     * @param force  压力值
     * @param canvas 画布
     */
    public abstract void onUp(float x, float y, int force, Canvas canvas);


    /**
     * 绘制
     *
     * @param canvas
     */
    public abstract void draws(Canvas canvas);

    /**
     * 清除数据
     */
    public abstract void clear();
}
