package com.sonix.oidbluetooth.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.*;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.PathParser;

import com.sonix.util.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class StrokeOrderViewJava extends View {

    static float SVG_STROKE_WIDTH = 1024F;
    static float SVG_STROKE_HEIGHT = 1024F;

    private ArrayList<Path> strokePaths = new ArrayList<Path>();
    private ArrayList<Path> medians = new ArrayList<Path>();
    private Paint strokePaint = new Paint();
    private Paint medianPaint = new Paint();
    private ArrayList<PathMeasure> medianMeasures = new ArrayList<PathMeasure>();
    private Path tempPath = new Path();
    private float progress = 0F;
    private int currIndex = 0;
    private ArrayList<Point> points = new ArrayList<Point>();

    Bitmap srcBmp = null;
    Canvas srcCanvas = null;
    Paint srcPaint = new Paint();

    Bitmap dstBmp = null;
    Canvas dstCanvas = null;
    Paint dstPaint = new Paint();

    PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    PorterDuffXfermode srcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

    public StrokeOrderViewJava(Context context) {
        super(context);
    }

    public StrokeOrderViewJava(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.FILL);
        strokePaint.setColor(Color.RED);

        medianPaint.setAntiAlias(true);
        medianPaint.setStyle(Paint.Style.FILL);
        medianPaint.setColor(Color.BLACK);

        srcPaint.setAntiAlias(true);
        srcPaint.setStrokeWidth(100f);
        srcPaint.setStyle(Paint.Style.STROKE);
        srcPaint.setColor(Color.BLACK);

        dstPaint.setAntiAlias(true);
        dstPaint.setStyle(Paint.Style.FILL);
        dstPaint.setColor(Color.BLACK);
    }

    public StrokeOrderViewJava(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public StrokeOrderViewJava(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setStrokesBySvg(String svgJson){
        strokePaths.clear();
        medians.clear();
        medianMeasures.clear();
        points.clear();
        ArrayList<String> strokes = new ArrayList<String>();
        parseSvgJson(svgJson, strokes, medians, points);
        for(int i=0; i<strokes.size(); i++){
            strokePaths.add(PathParser.createPathFromPathData(strokes.get(i)));
        }
        for(int i=0; i<medians.size(); i++){
            medianMeasures.add(new PathMeasure(medians.get(i), false));
        }
         startAnimation();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (0 == currIndex) {
            dstPaint.setXfermode(clearMode);
            if(dstCanvas != null){
                dstCanvas.drawPaint(dstPaint);
            }
            srcPaint.setXfermode(clearMode);
            if(srcCanvas != null){
                srcCanvas.drawPaint(srcPaint);
            }
        }
        float xTmp = getMeasuredWidth() / SVG_STROKE_WIDTH;
        float yTmp = getMeasuredHeight() / SVG_STROKE_HEIGHT;

        int restore = canvas.save();
        // 1. ?????? y ?????????
        // 2. ??? View ???????????????????????????????????????????????????????????????????????? 1/8
        // 3. ????????????????????????????????????????????????(0, 0)?????????????????????????????????
        canvas.scale(1F, -1F);
        canvas.translate(0F, -SVG_STROKE_HEIGHT * 7 / 8); // -1024 + 1024/8
        canvas.scale(xTmp, yTmp, 0F, SVG_STROKE_HEIGHT * 7 / 8); // 1024 - 1024/8
        for(int i=0; i<strokePaths.size(); i++){
            canvas.drawPath(strokePaths.get(i), strokePaint);
        }
        canvas.restoreToCount(restore);

        float w = getMeasuredWidth();
        float h = getMeasuredHeight();
        int layer = canvas.saveLayer(0F, 0F, w, h, null);
        // ??????Bitmap

        if (dstBmp == null) {
            dstBmp = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
            dstCanvas = new Canvas(dstBmp);
        }
        dstPaint.setXfermode(srcMode);// ????????? srcBmp ??? alpha ??? color ?????????????????????????????????

        int c1 = dstCanvas.save();
        dstCanvas.scale(1F, -1F);
        dstCanvas.translate(0F, -SVG_STROKE_HEIGHT * 7 / 8);
        dstCanvas.scale(xTmp, yTmp, 0F, SVG_STROKE_HEIGHT * 7 / 8);
        for(int i = strokePaths.size() - 1; i>=0; i--){
            if(i <= currIndex && currIndex < strokePaths.size()){
                dstCanvas.drawPath(strokePaths.get(i), dstPaint);
            }
        }

        dstCanvas.restoreToCount(c1);
        dstPaint.setXfermode(null);
        canvas.drawBitmap(dstBmp, 0F, 0F, medianPaint);

        // ???????????????????????????????????????
        medianPaint.setXfermode(porterDuffXfermode);
        // src bitmap
        if (srcBmp == null) {
            srcBmp = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
            srcCanvas = new Canvas(srcBmp);
        }
        srcPaint.setXfermode(srcMode);// ????????? srcBmp ??? alpha ??? color ?????????????????????????????????

        int c2 = srcCanvas.save();
        srcCanvas.scale(1F, -1F);
        srcCanvas.translate(0F, -SVG_STROKE_HEIGHT * 7 / 8);
        srcCanvas.scale(xTmp, yTmp, 0F, SVG_STROKE_HEIGHT * 7 / 8);
        if (!medianMeasures.isEmpty()) { // ??????????????????????????????
            // ???????????? ????????????????????????????????????
            drawBackbonePointCircle(currIndex * 2, 20F);
            if (progress > 0.99) {
                drawBackbonePointCircle(currIndex * 2 + 1, 30F);
            }
            tempPath.reset();
            PathMeasure m = medianMeasures.get(currIndex);
            LogUtils.e("dbj",m.getLength() * progress+"-----------");
            m.getSegment(0F, m.getLength() * progress, tempPath, true);
            srcCanvas.drawPath(tempPath, srcPaint);
        }
        srcCanvas.restoreToCount(c2);
        srcPaint.setXfermode(null);
        canvas.drawBitmap(srcBmp, 0F, 0F, medianPaint);
        medianPaint.setXfermode(null);

        canvas.restoreToCount(layer);
    }

    void parseSvgJson(String json, ArrayList<String> list, ArrayList<Path> paths, ArrayList<Point> points){
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray array = obj.getJSONArray("strokes");
            for(int i=0; i<array.length(); i++){
                list.add(array.getString(i));
            }

            JSONArray array2 = obj.getJSONArray("medians");
            for(int i=0; i<array2.length(); i++){
                JSONArray array3 = array2.getJSONArray(i);
                Path path = new Path();
                for(int j=0; j<array3.length(); j++){
                    JSONArray array4 = array3.getJSONArray(j);
                    int x = array4.getInt(0);
                    int y = array4.getInt(1);
                    if(0 == j){
                        path.moveTo(x,y);
                        points.add(new Point(x,y));
                    }else{
                        path.lineTo(x,y);
                    }
                    if (array3.length() - 1 == j) { // ???????????????
                        points.add(new Point(x, y));
                    }
                }
                paths.add(path);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void drawBackbonePointCircle(int index, float radius) {
        srcPaint.setStyle(Paint.Style.FILL);
        // ??????points ???????????????????????????size = 2 * medianPaint
        srcCanvas.drawCircle((float)points.get(index).x, (float)points.get(index).y, radius, srcPaint);
        srcPaint.setStyle(Paint.Style.STROKE);
    }

    private AnimatorSet createAnimation(){
        AnimatorSet set = new AnimatorSet();
        ArrayList<Animator> animators = new ArrayList<Animator>();
        for(int i=0; i<medians.size(); i++){
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            final int index = i;
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    currIndex = index;
                    progress = (float)valueAnimator.getAnimatedValue();
                    if(progress <= 1){
                        sleepAnimation();
                        postInvalidate();
                    }
                }
            });
            animator.setDuration(1000);
            animators.add(animator);
        }
        set.playSequentially(animators);
        return set;
    }

    private void sleepAnimation() {
        try {
            Thread.sleep(15);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAnimation(){
        AnimatorSet animator = createAnimation();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                progress = 0F;
                currIndex = 0;
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                progress = 0F;
                currIndex = 0;
            }
        });
        animator.start();
    }

}