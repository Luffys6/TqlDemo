package com.sonix.util;

import android.graphics.Bitmap;

import com.tqltech.tqlpencomm.Constants;

/**
 * @author ljm
 * @date 2020/5/18.
 */
public class DotUtils {
    /**
     * (本子铺码) 一英寸里大小 1in=2.54cm=25.40mm
     */
    public static final float IN_SIZE = 25.40f;

    /**
     * 码点规格
     */
    private double DIST_PERUNIT;


    /**
     * OID3S点码规格
     */
    public static final double DIST_PERUNIT_3S = 1.27d;

    public static double getDistPerunit() {
        return Constants.getXdistPerunit();
    }

    /**
     * 计算本子宽高
     *
     * @param bitmap 底图的bitmap
     * @param dpi    打印的dpi
     * @return
     */



    public static Double[] calculateBookSize(Bitmap bitmap, int dpi) {
        Double[] pars = new Double[2];
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        double bookWidth = ((double) bmpWidth / dpi) * IN_SIZE;
        double bookHeight = ((double) bmpHeight / dpi) * IN_SIZE;
        pars[0] = bookWidth;
        pars[1] = bookHeight;
        return pars;
    }

    /**
     * 拼接 x或者y 的dot点
     *
     * @param x  dot整数位
     * @param fx dot小数位
     * @return
     */
    public static float
    joiningTogether(int x, int fx) {
        float point;
        int temporary = x;
        point = fx;
        point /= 100.0;
        point += temporary;
        return point;
    }

    /**
     * 根据 X点  或者 Y点的dot坐标 转换屏幕坐标
     *
     * @param dotPoint         拼接好的dot座标
     * @param bgSize           绘制view的宽或高
     * @param pageSize         点码纸的宽或高
     * @param dotSpecification 码点规格
     * @return
     */

    public static float
    getPoint(float dotPoint, int bgSize, double pageSize, double dotSpecification) {
//        LogUtils.e("dbj","dotPoint="+dotPoint);
//        LogUtils.e("dbj","bgSize="+bgSize);
//        LogUtils.e("dbj","pageSize="+pageSize);
//        LogUtils.e("dbj","dotSpecification="+dotSpecification);
        dotPoint *= (bgSize);
        float ax = (float) (pageSize / dotSpecification);
        dotPoint /= ax;
        return dotPoint;
    }
}
