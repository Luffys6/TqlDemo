package com.sonix.surfaceview;

/**
 * @author ljm
 * @date 2020/9/4.
 */
public interface DoodleTouchEvent {
    /**
     * @param x         x轴座标
     * @param y         y轴座标
     * @param type      触摸状态
     * @param touchTime 触摸的时间可以用来转化成rct时间  跟dot数据存在一起
     */
    void onDown(float x, float y, int type, long touchTime);

    void onMove(float x, float y, int type, long touchTime);

    void onUp(float x, float y, int type, long touchTime);
}

