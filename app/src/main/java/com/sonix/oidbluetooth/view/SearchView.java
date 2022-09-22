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
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SearchView extends View {

    private Path mCirclePath;
    private Path mSearchPath;
    private PathMeasure mMeasure;
    private ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private float mAnimatorValue;
    private long defaultDuration = 800L;
    private ValueAnimator mStartingAnimator;
    private ValueAnimator mSearchingAnimator;
    private ValueAnimator mEndingAnimator;
    private State mCurrentState = State.NONE;
    /**
     * 画笔
     */
    private Paint sPaint;
    private String penColor = "#000000";


    public SearchView(Context context) {
        super(context);
    }

    public SearchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        sPaint = new Paint();
        sPaint.setColor(Color.parseColor(penColor));
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setStrokeWidth(1.5F);
        sPaint.setAntiAlias(true);
        sPaint.setStrokeCap(Paint.Cap.ROUND);
        sPaint.setStrokeMiter(1.0f);

        mCirclePath = new Path();
        mSearchPath = new Path();

        mMeasure = new PathMeasure();

        RectF oval1 = new RectF(-50, -50, 50, 50);
        mSearchPath.addArc(oval1, 45, 358);

        RectF oval2 = new RectF(-100, -100, 100, 100);
        mCirclePath.addArc(oval2, 45, -358);

        float[] pos = new float[2];
        mMeasure.setPath(mCirclePath, false);
        mMeasure.getPosTan(0, pos, null);

        mSearchPath.lineTo(pos[0], pos[1]);

        mUpdateListener = animation -> {
            mAnimatorValue = (float) animation.getAnimatedValue();
            invalidate();
        };
        mStartingAnimator = ValueAnimator.ofFloat(0, 1).setDuration(defaultDuration);
        mSearchingAnimator = ValueAnimator.ofFloat(0, 1).setDuration(defaultDuration);
        mEndingAnimator = ValueAnimator.ofFloat(1, 0).setDuration(defaultDuration);

        mStartingAnimator.addUpdateListener(mUpdateListener);
        mSearchingAnimator.addUpdateListener(mUpdateListener);
        mEndingAnimator.addUpdateListener(mUpdateListener);


        mStartingAnimator.addListener(mAnimatorListener);
        mSearchingAnimator.addListener(mAnimatorListener);
        mSearchingAnimator.setRepeatCount(1);
        mEndingAnimator.addListener(mAnimatorListener);

        // 进入开始动画
        mCurrentState = State.STARING;
        mStartingAnimator.start();
    }

    private Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mCurrentState = State.STARING;
            mCurrentState = State.SEARCHING;
            mStartingAnimator.start();
            mSearchingAnimator.start();
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            mCurrentState = State.STARING;
            mCurrentState = State.SEARCHING;
            mStartingAnimator.start();
            mSearchingAnimator.start();
        }
    };
    private Path path = new Path();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        sPaint.setColor(Color.WHITE);
        canvas.translate(getMeasuredWidth() / 2, getMeasuredHeight() / 2);
        canvas.drawColor(Color.parseColor("#0082D7"));
        switch (mCurrentState) {
            case NONE:
                canvas.drawPath(mSearchPath, sPaint);
                 break;
            case STARING:
            case ENDING:
                mMeasure.setPath(mSearchPath, false);
                path.reset();
                mMeasure.getSegment(mMeasure.getLength() * mAnimatorValue, mMeasure.getLength(), path, true);
                break;
            case SEARCHING:
                mMeasure.setPath(mCirclePath, false);
                path.reset();
                float stop = mMeasure.getLength() * mAnimatorValue;
                float start = (float) (stop - ((0.5 - Math.abs(mAnimatorValue - 0.5)) * 200f));
                mMeasure.getSegment(start, stop, path, true);
                break;
        }
        canvas.drawPath(path, sPaint);
    }

    public enum State {
        NONE,
        STARING,
        SEARCHING,
        ENDING
    }
}
