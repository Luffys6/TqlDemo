package com.sonix.oidbluetooth.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.sonix.oidbluetooth.bean.HanziBean;

public class StrokeOrderViewJavaNew extends View {
    private final int MODE_NORMAL = 0;
    private final int MODE_WRITER = 1;
    private final int MODE_ANIM = 2;
    private int mode = MODE_NORMAL;

    private Paint strokePaint = new Paint();

    private int currIndex = 0;

    private Path animPath = new Path();
    private Paint animPaint = new Paint();

    private int bgColor = Color.BLACK; //背景字颜色
    private int animColor = Color.GREEN;
    private HanziBean hanziBean;
    private AnimatorSet writerAnim;
    private int position = -1;

    private OnAnimStrokeWriterStartListener onAnimStrokeWriterStartListener;

    public StrokeOrderViewJavaNew(Context context) {
        super(context);
    }

    public StrokeOrderViewJavaNew(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(10);
        strokePaint.setStyle(Paint.Style.FILL);


        animPaint.setColor(animColor);
        animPaint.setStrokeWidth(100);
        animPaint.setStyle(Paint.Style.STROKE);
        animPaint.setStrokeCap(Paint.Cap.ROUND);


    }

    public StrokeOrderViewJavaNew(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public StrokeOrderViewJavaNew(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    /**
     * 监听动画的每一笔的开始
     *
     * @param onAnimStrokeWriterStartListener
     */
    public void setOnAnimStrokeWriterStartListener(OnAnimStrokeWriterStartListener onAnimStrokeWriterStartListener) {
        this.onAnimStrokeWriterStartListener = onAnimStrokeWriterStartListener;
    }


    public void setHanziBean(HanziBean hanziBean) {
        this.hanziBean = hanziBean;
        invalidate();
    }

    public void writerHanzi() {
        if (writerAnim != null && writerAnim.isRunning()) {
            writerAnim.cancel();
        }
        mode = MODE_WRITER;
        currIndex = 0;
        invalidate();
    }

    public void setPosition(int position) {
        this.position = position;
        startAnimation(position);
//        writerAnim.play(animators[position]);
//        invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hanziBean != null) {
            hanziBean.initHanzi(getWidth(), getHeight());
            for (int i = 0; i < hanziBean.getStrokePaths().size(); i++) {
//                if (mode == MODE_NORMAL){
//                    strokePaint.setColor(bgColor);
//                }
                if (i < currIndex) {
                    strokePaint.setColor(mode == MODE_WRITER ? bgColor : animColor);
                } else {
                    strokePaint.setColor(bgColor);
                }
                canvas.drawPath(hanziBean.getStrokePaths().get(i), strokePaint);
            }
            if (mode == MODE_ANIM && currIndex < hanziBean.getStrokeCount()) {
                 canvas.clipPath(hanziBean.getStrokePaths().get(currIndex));
                canvas.drawPath(animPath, animPaint);
            }
        }
    }

    public void startAnimation() {
        if (getVisibility() != VISIBLE) {
            return;
        }
        if (writerAnim != null) {
            writerAnim.cancel();
        }
        mode = MODE_ANIM;
        currIndex = -1;
        hanziBean.initHanzi(getWidth(), getHeight());
        writerAnim = new AnimatorSet();
        Animator[] animators = new Animator[hanziBean.getStrokeCount()];
        int index = 0;
        for (Path path : hanziBean.getMedianPaths()) {
            ValueAnimator va = ValueAnimator.ofFloat(0, 1);
            va.setDuration(2000);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                PathMeasure pm;

                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = (float) valueAnimator.getAnimatedValue();
                    if (pm == null) {
                        pm = new PathMeasure(path, false);
                    }
                    float end = pm.getLength() * value;
                    animPath.reset();
                    pm.getSegment(0, end, animPath, true);
                    invalidate();
                }
            });
            va.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    currIndex++;
                    if (onAnimStrokeWriterStartListener != null) {
                        onAnimStrokeWriterStartListener.onStart(currIndex);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            if (index > 0) {
                va.setStartDelay(500);
            }
            animators[index++] = va;
        }
        writerAnim.playSequentially(animators);
        writerAnim.start();

    }

    public void startAnimation(int position) {
        if (getVisibility() != VISIBLE) {
            return;
        }
        if (writerAnim != null) {
            writerAnim.cancel();
        }
        mode = MODE_ANIM;
        currIndex = position;
        hanziBean.initHanzi(getWidth(), getHeight());
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(500);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            PathMeasure pm;
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) valueAnimator.getAnimatedValue();
                if (pm == null) {
                    pm = new PathMeasure(hanziBean.getStrokePaths().get(position), false);
                }
                float end = pm.getLength() * value;
                animPath.reset();
                pm.getSegment(0, end, animPath, true);
                animPaint.setColor(Color.RED);
                invalidate();
            }
        });
        va.start();

    }


    /**
     * 监听动画开始写第几笔
     * index   开始写第几笔
     */
    public interface OnAnimStrokeWriterStartListener {
        void onStart(int index);
    }
}