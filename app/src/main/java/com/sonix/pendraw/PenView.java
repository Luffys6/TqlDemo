package com.sonix.pendraw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;

import androidx.annotation.Nullable;

import com.sonix.util.DotUtils;
import com.sonix.oidbluetooth.R;
import com.tqltech.tqlpencomm.bean.Dot;


/**
 * @author ljm
 * @date 2020/5/12.darwLine
 */
@SuppressLint("AppCompatCustomView")
public class PenView extends View {
    private static final String TAG = "PenView";
    private Context mContext;

    /**
     * 绘制页码的画笔
     */
    private Paint mTextPaint;

    /**
     * 自身的宽高
     */
    private int BG_WIDTH, BG_HEIGHT;
    private Bitmap mBitmap;
    private Bitmap bgWrite;
    private Canvas mCanvas;
    private OnFluorescentPenTouchEvent mOnTouchEvent;
    private TimeListener mTimeListener;
    /**
     * 笔记本实际规格大小默认B5笔记本
     */
    private double PAPER_WIDTH = -1;
    private double PAPER_HEIGHT = -1;
    //笔锋类型
    public static final int TYPE_STROKE_PEN = 0;
    //普通类型
    public static final int TYPE_NORMAL_PEN = 1;
    //涂鸦类型
    public static final int TYPE_DOODLE_PEN = 2;
    //当前笔的类型
    private int currentPenMode = TYPE_NORMAL_PEN;
    //横屏模式
    public static final int HORIZONTAL_MODE = 100;
    //竖屏模式
    public static final int VERTICAL_MODE = 101;
    //屏幕方向
    private int screenOrientation = VERTICAL_MODE;
    //是否在绘制
    private boolean mIsCanvasDraw;

    private int mBackColor = Color.TRANSPARENT;
    private int imageDpi;
    private int mBookId, mPageId;
    private int mBgResourceId;
    private ScaleGestureDetector mScaleGestureDetector;
    private float minScale = 1.0f;
    private float maxScale = 3.0f;
    //图片矩阵用于放大
    private Matrix bitmapMatrix = new Matrix();
    //用于笔锋绘制倾斜度
    private Matrix ovalMatrix = new Matrix();
    private int mActivePointerId;
    private VelocityTracker mVelocityTracker;
    private float mLastTouchX = 0f;
    private float mLastTouchY = 0f;
    private boolean isDragging = false;
    private int INVALID_POINTER_ID = -1;
    private int mActivePointerIndex;
    private float mTouchSlop;
    private float mMinimumVelocity;
    private float PAGER_MAX_X, PAGER_MAX_Y;
    private BasePen mPen;
    private float pointX, pointY;

    private int lastBookId, lastPageId;

    public PenView(Context context) {
        this(context, null);
    }

    public PenView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PenView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PenView);
        //底图dpi
        imageDpi = typedArray.getInt(R.styleable.PenView_image_dpi, 300);
        //底图背景资源
        mBgResourceId = typedArray.getResourceId(R.styleable.PenView_background_image, R.drawable.pager_positive);
        setBgBitmap(mBgResourceId, imageDpi);
        init(context);
    }

    /**
     * 底图id转化为bitmap,并算出点码规格
     *
     * @param bgResourceId
     * @param imageDpi
     */
    private void setBgBitmap(int bgResourceId, int imageDpi) {
        mBitmap = BitmapFactory.decodeResource(getResources(), bgResourceId).copy(Bitmap.Config.ARGB_4444, true);
        Double[] doubles = DotUtils.calculateBookSize(mBitmap, imageDpi);
        PAPER_WIDTH = doubles[0];
        PAPER_HEIGHT = doubles[1];
        PAGER_MAX_X = (float) (PAPER_WIDTH / DotUtils.getDistPerunit());
        PAGER_MAX_Y = (float) (PAPER_HEIGHT / DotUtils.getDistPerunit());
    }

    public Bitmap getBitmap() {
        return bgWrite;
    }

    private void init(Context context) {
        //setLayerType(LAYER_TYPE_HARDWARE, null);
        mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);
        mContext = context;
        //页码画笔
        initTextPaint();
        //初始化画笔对象
        mPen = new NormalPen(context);
    }

    /**
     * 设置横竖屏模式
     *
     * @param orientation
     */
    public void setScreenOrientation(int orientation) {
        screenOrientation = orientation;
    }

    private ScaleGestureDetector.SimpleOnScaleGestureListener mOnScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float curScaleFactor = getCurScaleFactor();
            float scaleFactor = detector.getScaleFactor();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            if (curScaleFactor < maxScale && scaleFactor > 1.0f || curScaleFactor > minScale && scaleFactor < 1.0f) {
                float resultScaleFactor = scaleFactor;
                if (curScaleFactor * scaleFactor < minScale) {
                    resultScaleFactor = minScale / curScaleFactor;
                }
                if (curScaleFactor * scaleFactor > maxScale) {
                    resultScaleFactor = maxScale / curScaleFactor;
                }
                // 以手指所在地方进行缩放
                bitmapMatrix.postScale(resultScaleFactor, resultScaleFactor,
                        focusX, focusY);
                checkMatrixBounds("Scale", bitmapMatrix);
                invalidate();
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    };

    private boolean checkMatrixBounds(String scale, Matrix matrix) {
        RectF rect = getDisplayRect(matrix);
        if (rect == null) {
            return false;
        }
        float height = rect.height();
        float width = rect.width();
        float deltaX = 0f;
        float deltaY = 0f;
        int viewHeight = BG_HEIGHT;
        if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }
        float viewWidth = BG_WIDTH;
        if (width <= viewWidth) {
            deltaX = -rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
        }
        matrix.postTranslate(deltaX, deltaY);
        RectF rectAfter = getDisplayRect(matrix);
        if (rectAfter == null) {
            return false;
        }
        return true;
    }

    private RectF getDisplayRect(Matrix matrix) {
        RectF mDisplayRect = new RectF();
        mDisplayRect.set(0f, 0f, BG_WIDTH, BG_HEIGHT);
        matrix.mapRect(mDisplayRect);
        return mDisplayRect;
    }

    private float getCurScaleFactor() {
        float[] values = new float[9];
        bitmapMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /**
     * 获取自身的宽度
     *
     * @return
     */
    public int getBG_WIDTH() {
        return BG_WIDTH;
    }

    /**
     * 获取自身的高度
     *
     * @return
     */
    public int getBG_HEIGHT() {
        return BG_HEIGHT;
    }

    /**
     * 获取点码纸的宽度
     *
     * @return
     */
    public double getPAPER_WIDTH() {
        return PAPER_WIDTH;
    }

    /**
     * 获取点码纸的高度
     *
     * @return
     */
    public double getPAPER_HEIGHT() {
        return PAPER_HEIGHT;
    }


    /**
     * 设置荧光笔触摸的接口
     *
     * @param eventListener
     */
    public void setOnFluorescentPenTouchEventListener(OnFluorescentPenTouchEvent eventListener) {
        this.mOnTouchEvent = eventListener;
    }

    /**
     * 设置下笔时间监听回调
     */
    public void setTimeListener(TimeListener listener) {
        this.mTimeListener = listener;
    }

    /**
     * 获取荧光笔触摸的接口
     *
     * @return
     */
    public OnFluorescentPenTouchEvent getOnFluorescentPenTouchEventListener() {
        return mOnTouchEvent;
    }


    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */
    public void setPenColor(String colorValue) {
        mPen.setPenColor(colorValue);
    }

    /**
     * 设置笔迹颜色
     *
     * @param colorValue
     */
    public void setPenColor(int colorValue) {
        mPen.setPenColor(colorValue);
    }


    /**
     * 获取笔迹颜色
     *
     * @return
     */
    public String getPenColor() {
        return mPen.getPenColor();
    }

    /**
     * 设置笔迹宽度
     *
     * @param penWidth
     */
    public void setPenWidth(float penWidth) {
        mPen.setPenWidth(penWidth);
    }

    /**
     * 切换背景图
     *
     * @param resId 图片id
     * @param dpi   底图的dpi
     */
    public void replaceBackgroundImage(int resId, int dpi) {
        imageDpi = dpi;
        setBgBitmap(resId, dpi);
        //重新调用一遍measure（）onLayout() onDraw()方法
        requestLayout();
    }

    /**
     * 切换背景图
     *
     * @param bitmap bitmap
     * @param dpi    底图的dpi
     */
    public void replaceBackgroundImage(Bitmap bitmap, int dpi) {
        imageDpi = dpi;
        mBitmap = bitmap;
        Double[] doubles = DotUtils.calculateBookSize(bitmap, imageDpi);
        PAPER_WIDTH = doubles[0];
        PAPER_HEIGHT = doubles[1];
        //重新调用一遍measure（）onLayout() onDraw()方法
        requestLayout();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mScaleGestureDetector.onTouchEvent(event);
            return processTouchEvent(event);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean processTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mVelocityTracker = VelocityTracker.obtain();
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(ev);
                }
                mLastTouchX = getActiveX(ev);
                mLastTouchY = getActiveY(ev);
                isDragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = getActiveX(ev);
                float y = getActiveY(ev);
                float dx = x - mLastTouchX;
                float dy = y - mLastTouchY;

                if (!isDragging) {
                    // Use Pythagoras to see if drag length is larger than
                    // touch slop
                    isDragging = Math.sqrt((double) (dx * dx + dy * dy)) >= mTouchSlop;
                }
                if (isDragging) {
                    onDrag(dx, dy);
                    mLastTouchX = x;
                    mLastTouchY = y;
                    if (null != mVelocityTracker) {
                        mVelocityTracker.addMovement(ev);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                // Recycle Velocity Tracker
                if (null != mVelocityTracker) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                if (isDragging) {
                    if (null != mVelocityTracker) {
                        mLastTouchX = getActiveX(ev);
                        mLastTouchY = getActiveY(ev);

                        // Compute velocity within the last 1000ms
                        //加速度决定up点绘制 时间越长补点越短
                        mVelocityTracker.addMovement(ev);
                        mVelocityTracker.computeCurrentVelocity(500);

                        float vX = mVelocityTracker.getXVelocity();

                        float vY = mVelocityTracker.getYVelocity();

                        // If the velocity is greater than minVelocity, call
                        // listener
                        if (Math.max(Math.abs(vX), Math.abs(vY)) >= mMinimumVelocity) {
                            onFling(mLastTouchX, mLastTouchY, -vX, -vY);
                        }
                    }
                }

                // Recycle Velocity Tracker
                if (null != mVelocityTracker) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = getPointerIndex(ev.getAction());
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                }
                break;
        }
        mActivePointerIndex = ev.findPointerIndex(mActivePointerId != INVALID_POINTER_ID ? mActivePointerId : 0);
        return true;
    }

    private int getPointerIndex(int action) {
        return action & MotionEvent.ACTION_POINTER_INDEX_MASK >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    }

    private void onFling(float lastTouchX, float lastTouchY, float v, float v1) {

    }

    private void onDrag(float dx, float dy) {
        bitmapMatrix.postTranslate(dx, dy);
        checkMatrixBounds("drag", bitmapMatrix);
        invalidate();
    }

    private float getActiveY(MotionEvent ev) {
        try {
            return ev.getY(mActivePointerIndex);
        } catch (Exception e) {
            return ev.getY();
        }
    }

    private float getActiveX(MotionEvent ev) {
        try {
            return ev.getX(mActivePointerIndex);
        } catch (Exception e) {
            return ev.getX();
        }
    }

    /**
     * 获取笔迹宽度
     *
     * @return
     */
    public float getPenWidth() {
        return mPen.getPenWidth();
    }

    /**
     * 设置笔的类型
     *
     * @param penMode
     */
    public void setPenMode(int penMode) {
        currentPenMode = penMode;
        if (penMode == TYPE_STROKE_PEN) {
            //初始化笔锋类
            mPen = new StrokePen(mContext);
            setOnTouchListener(null);
        } else if (penMode == TYPE_NORMAL_PEN) {
            //初始化无笔锋类
            if (!(mPen instanceof NormalPen)) {
                mPen = new NormalPen(mContext);
            }
            ((NormalPen) mPen).switchHandWrite();
            setOnTouchListener(null);
        } else if (penMode == TYPE_DOODLE_PEN) {
            //初始化涂鸦类
            if (!(mPen instanceof NormalPen)) {
                mPen = new NormalPen(mContext);
            }
            ((NormalPen) mPen).switchDoodle();
            setOnTouchListener(mOnTouchListener);
        }
    }

    /**
     * 获取当前笔的类型
     *
     * @return
     */
    public int getPenMode() {
        return currentPenMode;
    }

    /**
     * @return 判断是否有绘制内容在画布上
     */
    public boolean getHasDraw() {
        return mIsCanvasDraw;
    }

    public void setNoteParameter(int bookId, int pageId) {
        mBookId = bookId;
        mPageId = pageId;
    }

    /**
     * 清除画布，记得清除点的集合
     */
    public void reset() {
        clear();
        bgWrite = Bitmap.createScaledBitmap(mBitmap, BG_WIDTH, BG_HEIGHT, true);
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        initCanvas(bgWrite);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bgWrite != null) {
            Log.i(TAG, "onDraw: bgWrite != null");
            canvas.drawBitmap(bgWrite, bitmapMatrix, null);
        }
        //画页码
        if (lastBookId != mBookId || lastPageId != mPageId) {
            drawPageNum(mCanvas);
            lastBookId = mBookId;
            lastPageId = mPageId;
        }
        //drawPageNum(mCanvas);
        //画笔迹
        //mPen.draws(canvas);
    }


    public void redo() {
        if (currentPenMode == TYPE_DOODLE_PEN) {
            if (mPen instanceof NormalPen) {
                NormalPen doodlePen = (NormalPen) mPen;
                doodlePen.redo();
                invalidate();
            }
        }
    }

    public void undo() {
        if (currentPenMode == TYPE_DOODLE_PEN) {
            if (mPen instanceof NormalPen) {
                NormalPen doodlePen = (NormalPen) mPen;
                doodlePen.undo();
                invalidate();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "onMeasure: ");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int boardHeight = getBoardHeight(widthSize);
        if (boardHeight > heightSize) {
            setMeasuredDimension(getBoardWidth(heightSize), heightSize);
        } else {
            setMeasuredDimension(widthSize, boardHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "onSizeChanged: w=" + w + "/h=" + h + "/oldw=" + oldw + "/oldh=" + oldh);
        BG_WIDTH = w;
        BG_HEIGHT = h;
        Log.i(TAG, "onLayout:BG_WIDTH=" + BG_WIDTH + "/BG_HEIGHT=" + BG_HEIGHT);
        bgWrite = Bitmap.createScaledBitmap(mBitmap, BG_WIDTH, BG_HEIGHT, true);
        initCanvas(bgWrite);
    }

    /**
     * 按比例动态设置高度
     *
     * @param width
     * @return
     */
    private int getBoardHeight(int width) {
        return (int) ((float) width / PAPER_WIDTH * PAPER_HEIGHT);
    }

    /**
     * 按比例动态设置宽度
     *
     * @param height
     * @return
     */
    private int getBoardWidth(int height) {
        return (int) ((float) height / PAPER_HEIGHT * PAPER_WIDTH);
    }


    private void drawPageNum(Canvas canvas) {
        if (mPageId % 2 == 0) {
            canvas.drawText(String.format("%02d", mPageId + 1), BG_WIDTH - 135, BG_HEIGHT - 72, mTextPaint);
        } else {
            canvas.drawText(String.format("%02d", mPageId + 1), 135, BG_HEIGHT - 72, mTextPaint);
        }
    }


    private void clear() {
        mPen.clear();
    }


    private void initTextPaint() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(35);
        mTextPaint.setColor(Color.parseColor("#8ca2d1"));
    }


    private void initCanvas(Bitmap bitmap) {
        if (mCanvas == null) {
            mCanvas = new Canvas();
        }
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        mCanvas.setBitmap(bitmap);
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            Log.i(TAG, "onTouch: x=" + x + "//y=" + y);
            MotionEvent motionEvent = MotionEvent.obtain(event);
            int pressure = (int) motionEvent.getPressure();
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onTouch: 荧光笔down");
                    //onFluorescentPenDown(x, y);
                    mPen.onDown(x, y, pressure);
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onDown(x, y, 0);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onTouch: 荧光笔move");
                    // onFluorescentPenMove(x, y);
                    mPen.onMove(x, y, pressure);
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onMove(x, y, 1);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch: 荧光笔up");
                    //onFluorescentPenUp(x, y);
                    mPen.onUp(x, y, pressure, mCanvas);
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onUp(x, y, 2);
                    }
                    break;
            }
            invalidate();
            return true;
        }
    };


    /**
     * 荧光笔回调
     */

    public interface OnFluorescentPenTouchEvent {
        void onDown(float x, float y, int type);

        void onMove(float x, float y, int type);

        void onUp(float x, float y, int type);
    }

    /**
     * 下笔时间的回调
     */
    public interface TimeListener {
        void getTime(long l);

        void stopTime();
    }

    /**
     * @param pointX  屏幕x轴座标
     * @param pointY  屏幕y轴座标
     * @param force   压力值
     * @param dotType 笔迹状态，down move up
     */
    public void processDot(float pointX, float pointY, int force, int dotType) {
        Log.i(TAG, "pointX: " + pointX + "pointY:" + pointY + "force:" + force + "dotType:" + dotType);
        if (currentPenMode != TYPE_DOODLE_PEN) {
            mIsCanvasDraw = true;
            switch (dotType) {
                case 0:
                    if (mTimeListener != null) {
                        mTimeListener.stopTime();
                    }
                    mPen.onDown(pointX, pointY, force);
                    break;
                case 1:
                    if (mTimeListener != null) {
                        mTimeListener.stopTime();
                    }
                    mPen.onMove(pointX, pointY, force);
                    break;
                case 2:
                    if (mTimeListener != null) {
                        long time = System.currentTimeMillis();
                        mTimeListener.getTime(time);
                    }
                    mPen.onUp(pointX, pointY, 1, mCanvas);
                    break;
            }
            invalidate();
        }
    }

    public void processDot(Dot dot) {
        if (currentPenMode != TYPE_DOODLE_PEN) {
            Log.i(TAG, "processDot: " + dot.toString());
            int bookId = dot.BookID;
            int pageId = dot.PageID;

            float x = DotUtils.joiningTogether(dot.x, dot.fx);
            float y = DotUtils.joiningTogether(dot.y, dot.fy);

            if (screenOrientation == HORIZONTAL_MODE) {
                y -= PAGER_MAX_X;
                x -= 0;
                pointX = (y * BG_WIDTH) / (0 - PAGER_MAX_X);
                pointY = (x * BG_HEIGHT) / (PAGER_MAX_Y - 0);
            } else if (screenOrientation == VERTICAL_MODE) {
                pointX = DotUtils.getPoint(x, BG_WIDTH, PAPER_WIDTH, DotUtils.getDistPerunit());
                pointY = DotUtils.getPoint(y, BG_HEIGHT, PAPER_HEIGHT, DotUtils.getDistPerunit());
            }
            switch (dot.type) {
                case PEN_DOWN:
                    if (mTimeListener != null) {
                        mTimeListener.stopTime();
                    }
                    if (mBookId != bookId || mPageId != pageId) {
                        mBookId = bookId;
                        mPageId = pageId;
                    }
                    mPen.onDown(pointX, pointY, dot.force);
                    break;
                case PEN_MOVE:
                    if (mTimeListener != null) {
                        mTimeListener.stopTime();
                    }
                    mPen.onMove(pointX, pointY, dot.force);
                    break;
                case PEN_UP:
                    if (mTimeListener != null) {
                        long time = System.currentTimeMillis();
                        mTimeListener.getTime(time);
                    }
                    mPen.onUp(pointX, pointY, 1, mCanvas);
                    break;
            }
            invalidate();
        }
    }

    /**
     * 逐行扫描 清楚边界空白。功能是生成一张bitmap位于正中间，不是位于顶部，此关键的是我们画布需要
     * 成透明色才能生效
     *
     * @param blank 边距留多少个像素
     * @return tks github E-signature
     */
    public Bitmap clearBlank(int blank) {
        if (mBitmap != null) {
            int HEIGHT = mBitmap.getHeight();//1794
            int WIDTH = mBitmap.getWidth();//1080
            int top = 0, left = 0, right = 0, bottom = 0;
            int[] pixs = new int[WIDTH];
            boolean isStop;
            for (int y = 0; y < HEIGHT; y++) {
                mBitmap.getPixels(pixs, 0, WIDTH, 0, y, WIDTH, 1);
                isStop = false;
                for (int pix : pixs) {
                    if (pix != mBackColor) {
                        top = y;
                        isStop = true;
                        break;
                    }
                }
                if (isStop) {
                    break;
                }
            }
            for (int y = HEIGHT - 1; y >= 0; y--) {
                mBitmap.getPixels(pixs, 0, WIDTH, 0, y, WIDTH, 1);
                isStop = false;
                for (int pix : pixs) {
                    if (pix != mBackColor) {
                        bottom = y;
                        isStop = true;
                        break;
                    }
                }
                if (isStop) {
                    break;
                }
            }
            pixs = new int[HEIGHT];
            for (int x = 0; x < WIDTH; x++) {
                mBitmap.getPixels(pixs, 0, 1, x, 0, 1, HEIGHT);
                isStop = false;
                for (int pix : pixs) {
                    if (pix != mBackColor) {
                        left = x;
                        isStop = true;
                        break;
                    }
                }
                if (isStop) {
                    break;
                }
            }
            for (int x = WIDTH - 1; x > 0; x--) {
                mBitmap.getPixels(pixs, 0, 1, x, 0, 1, HEIGHT);
                isStop = false;
                for (int pix : pixs) {
                    if (pix != mBackColor) {
                        right = x;
                        isStop = true;
                        break;
                    }
                }
                if (isStop) {
                    break;
                }
            }
            if (blank < 0) {
                blank = 0;
            }
            left = left - blank > 0 ? left - blank : 0;
            top = top - blank > 0 ? top - blank : 0;
            right = right + blank > WIDTH - 1 ? WIDTH - 1 : right + blank;
            bottom = bottom + blank > HEIGHT - 1 ? HEIGHT - 1 : bottom + blank;
            return Bitmap.createBitmap(mBitmap, left, top, right - left, bottom - top);
        } else {
            return null;
        }
    }
}
