package com.sonix.surfaceview;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.sonix.oidbluetooth.R;
import com.sonix.util.DotUtils;
import com.tqltech.tqlpencomm.Constants;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.pen.PenUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ljm
 * @date 2020/8/22.
 */
public class DrawView extends SurfaceView implements SurfaceHolder.Callback, Runnable, OnGestureListener {
    private static final String TAG = "DrawView";
    private Context mContext;
    private SurfaceHolder mHolder;
    private Thread mThread;
    /**
     * 线程运行的标识，用于控制线程
     */
    private boolean mIsDrawing;

    private Bitmap mBitmap;
    /**
     * 笔记本实际规格大小默认B5笔记本
     */
    private double PAPER_WIDTH = -1;
    private double PAPER_HEIGHT = -1;

    private double LAST_PAPER_WIDTH = -1;
    private double LAST_PAPER_HEIGHT = -1;

    private Canvas mCanvas;
    private Canvas sCanvas;
    /**
     * 绘制页码的画笔
     */
    private Paint mTextPaint;
    /**
     * 自身的宽高
     */
    private int BG_WIDTH, BG_HEIGHT;
    private float pointX, pointY;
    /**
     * 绘制类
     */
    private BasePen mPen;
    private Bitmap bgWrite;
    private int imageDpi;
    private int mBgResourceId;
    //笔锋类型
    public static final int TYPE_STROKE_PEN = 0;
    //普通类型
    public static final int TYPE_NORMAL_PEN = 1;
    //涂鸦类型
    public static final int TYPE_DOODLE_PEN = 2;
    //当前笔的类型
    private int currentPenMode = TYPE_NORMAL_PEN;
    private int mBookId, mLastBookId, mPageId;
    private DoodleTouchEvent mOnTouchEvent;


    private ScaleDragDetector scaleDragDetector;
    private boolean openScale;

    // 最小缩放倍数
    private final Float minScale = 1f;
    // 最大缩放倍数
    private final Float maxScale = 10f;

    private Matrix canvasMatrix = new Matrix();

    private boolean rotateView;

    public void setRotateView(boolean rotateView) {
        this.rotateView = rotateView;
    }

    public void setOpenScale(boolean openScale) {
        this.openScale = openScale;
        if (openScale) {
            scaleDragDetector = new ScaleDragDetector(mContext, this);
        } else {
            scaleDragDetector = null;
        }
    }


    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DrawView);
        //底图dpi
        imageDpi = typedArray.getInt(R.styleable.DrawView_resources_dpi, 300);
        //底图背景资源
        mBgResourceId = typedArray.getResourceId(R.styleable.DrawView_background_resources, R.drawable.pager_positive);
        //生成背景图片
        setBgBitmap(mBgResourceId, imageDpi);
        init(context);
        setOpenScale(true);
    }

    private void init(Context context) {
        this.mContext = context;
        initSurface();
        //初始化页码的画笔
        initTextPaint();
        mPen = new NormalPen(context);
    }

    private void initSurface() {
        setZOrderMediaOverlay(true);
        // setWillNotDraw(false);
        mHolder = getHolder();
        //添加回调
        mHolder.addCallback(this);
        //背景黑色变透明
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
    }


    private void initTextPaint() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(35);
        mTextPaint.setColor(Color.parseColor("#8ca2d1"));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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

    public boolean isChangLayout() {
        if (LAST_PAPER_WIDTH != PAPER_WIDTH && LAST_PAPER_HEIGHT != PAPER_HEIGHT) {
            return true;
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
//        Log.i(TAG, "  / ID /  setBgBitmap 切换大小图层成功 444444444  "  );
        BG_WIDTH = w;
        BG_HEIGHT = h;
        LAST_PAPER_WIDTH = PAPER_WIDTH;
        LAST_PAPER_HEIGHT = PAPER_HEIGHT;

        bgWrite = Bitmap.createScaledBitmap(mBitmap, w, h, true);

        initCanvas(bgWrite);

//        Log.i(TAG, "onSizeChanged:BG_WIDTH=" + BG_WIDTH + "/BG_HEIGHT=" + BG_HEIGHT);
        if (mOnSizeChangeListener != null) {

//            if (rotateView || (w - h > 0 && oldw - oldh < 0) || (w - h < 0 && oldw - oldh > 0)) {
//                rotateView = false;
//                return; //切换背景的时候   如果还切换了横屏  会进来两次  如果不切换横竖屏   就不能进这个...
//                //但是横竖屏在fragment是手动加上去的...这导致了这个判断会出现一些不确定性
//            }

            mOnSizeChangeListener.onSizeChanged(w, h, oldw, oldh);//切换后的Down点等待绘制
            mOnSizeChangeListener = null;
        }
//        Log.i(TAG, "  / ID /  setBgBitmap 切换大小图层彻底结束  "  );
    }

    public void setOnSizeChangeListener(OnSizeChangeListener listener) {
        this.mOnSizeChangeListener = listener;
    }

    public OnSizeChangeListener mOnSizeChangeListener;

    public interface OnSizeChangeListener {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Log.i(TAG, "surfaceChanged: ");

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Log.i(TAG, "surfaceCreated: ");
        //创建线程
        mIsDrawing = true;
        mThread = new Thread(this);
        mThread.start();

    }

    private float getCurScaleFactor() {
        float[] values = new float[9];
        canvasMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    // 缩放
    private boolean onScaleAction(Float scaleFactor, Float focusX, Float focusY) {
        float curScaleFactor = getCurScaleFactor();
        if (curScaleFactor < maxScale && scaleFactor > 1.0f || curScaleFactor > minScale && scaleFactor < 1.0f) {
            float resultScaleFactor = scaleFactor;
            if (curScaleFactor * scaleFactor < minScale) {
                resultScaleFactor = minScale / curScaleFactor;
            }
            if (curScaleFactor * scaleFactor > maxScale) {
                resultScaleFactor = maxScale / curScaleFactor;
            }
            // 以手指所在地方进行缩放
            canvasMatrix.postScale(resultScaleFactor, resultScaleFactor,
                    focusX, focusY);
            checkMatrixBounds("Scale", canvasMatrix);
//            invalidate();
        }
        return false;
    }

    // 拖动
    private boolean onDragAction(Float dx, Float dy) {
        canvasMatrix.postTranslate(dx, dy);
        checkMatrixBounds("drag", canvasMatrix);
//        invalidate();
        return false;
    }

    @Override
    public void onDrag(Float dx, Float dy) {
        onDragAction(dx, dy);
    }

    @Override
    public void onFling(Float startX, Float startY, Float velocityX, Float velocityY) {

    }

    @Override
    public void onScale(Float scaleFactor, Float focusX, Float focusY) {
        onScaleAction(scaleFactor, focusX, focusY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Log.i(TAG, "surfaceDestroyed: ");
        mIsDrawing = false;
    }

    public void reset() {
        //Log.i(TAG, "clear: ");
        mPen.clear();

        if (BG_WIDTH > 0) {
            bgWrite = Bitmap.createScaledBitmap(mBitmap, BG_WIDTH, BG_HEIGHT, true);
        }

//        if (mCanvas != null) {
//            mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
//        }

        canvasMatrix = new Matrix();//初始化
        initCanvas(bgWrite);

    }

    /**
     * 绘制数据
     *
     * @param dot
     */
    public void processDot(Dot dot) {

//        executorService.execute(new Runnable() {
//            @Override
//            public void run() {
//        Log.e(TAG, currentPenMode + "  =  2 /     H绘制的 " +dot.toString());
        if (currentPenMode != TYPE_DOODLE_PEN) {
            int bookId = dot.BookID;
            int pageId = dot.PageID;


            float x = DotUtils.joiningTogether(dot.x, dot.fx);
            float y = DotUtils.joiningTogether(dot.y, dot.fy);


            if (PenUtils.penDotType == 18  || PenUtils.penDotType == 19 || PenUtils.penDotType == 51 ) {
                pointX = DotUtils.getPoint(dot.ab_x, BG_WIDTH, PAPER_WIDTH, DotUtils.getDistPerunit());
                pointY = DotUtils.getPoint(dot.ab_y, BG_HEIGHT, PAPER_HEIGHT, DotUtils.getDistPerunit());
            } else {
                pointX = DotUtils.getPoint(x, BG_WIDTH, PAPER_WIDTH, DotUtils.getDistPerunit());
                pointY = DotUtils.getPoint(y, BG_HEIGHT, PAPER_HEIGHT, DotUtils.getDistPerunit());
            }


            switch (dot.type) {
                case PEN_DOWN:
                    if (mBookId != bookId || mPageId != pageId) {
                        mBookId = bookId;
                        mPageId = pageId;
                    }
                    mPen.onDown(pointX, pointY, dot.force, sCanvas);
                    break;
                case PEN_MOVE:
                    mPen.onMove(pointX, pointY, dot.force, sCanvas);
                    break;
                case PEN_UP:
                    mPen.onUp(pointX, pointY, 1, sCanvas);
                    break;
            }
        }

//            }
//        });

    }

    /**
     * @param pointX  屏幕x轴座标
     * @param pointY  屏幕y轴座标
     * @param force   压力值
     * @param dotType 笔迹状态，down move up
     */


    public void processDot(float pointX, float pointY, int force, int dotType) {
        //Log.i(TAG, "pointX: " + pointX + "pointY:" + pointY + "force:" + force + "dotType:" + dotType);
        //涂鸦是禁止接收蓝牙穿过来的数据
        //获取离线和正常书写都走这个方法

//        executorService.execute(new Runnable() {
//            @Override
//            public void run() {
//        Log.e("最终获得", dotType +" 最终获 画了pointX =" + pointX + "/ pointY =" +  pointY);
        if (currentPenMode != TYPE_DOODLE_PEN) {
            switch (dotType) {
                case 0:
                    drawPageNum(sCanvas);
                    mPen.onDown(pointX, pointY, force, sCanvas);
                    break;
                case 1:
                    mPen.onMove(pointX, pointY, force, sCanvas);
                    break;
                case 2:
                    mPen.onUp(pointX, pointY, 1, sCanvas);
                    break;
            }
        }
//            }
//        });

    }

    /**
     * 获取自身的宽度
     *
     * @return
     */
    public int getBG_WIDTH() {
//        Log.i("最终 ", "getBG_WIDTH = " + BG_WIDTH);
        return BG_WIDTH;
    }

    /**
     * 获取自身的高度
     *
     * @return
     */
    public int getBG_HEIGHT() {
//        Log.i("最终 ", "getBG_HEIGHT = " + BG_HEIGHT);
        return BG_HEIGHT;
    }

    /**
     * 获取点码纸的宽度
     *
     * @return
     */
    public double getPAPER_WIDTH() {
//        Log.i("最终 ", "getPAPER_WIDTH = " + PAPER_WIDTH);
        return PAPER_WIDTH;
    }

    /**
     * 获取点码纸的高度
     *
     * @return
     */
    public double getPAPER_HEIGHT() {
//        Log.i("最终 ", "getPAPER_HEIGHT = " + PAPER_HEIGHT);
        return PAPER_HEIGHT;
    }


    /**
     * 底图id转化为bitmap,并算出点码规格
     *
     * @param bgResourceId
     * @param imageDpi
     */
    private void setBgBitmap(int bgResourceId, int imageDpi) {

//        if (mBitmap == null || mBookId != mLastBookId) {//等于null肯定要进来  第一次    不等于null,bookId没变化 不改变图片
        //同本切换页面不需要重绘

        InputStream is = this.getResources().openRawResource(bgResourceId);
        mBitmap = BitmapFactory.decodeStream(is);

//        mBitmap = BitmapFactory.decodeResource(getResources(), bgResourceId).copy(Bitmap.Config.ARGB_4444, true);

        Double[] doubles = DotUtils.calculateBookSize(mBitmap, imageDpi);
        Log.i(TAG, "底图width=" + mBitmap.getWidth() + "//height=" + mBitmap.getHeight() + "换算之后的宽高；" + Arrays.toString(doubles));
        PAPER_WIDTH = doubles[0];
        PAPER_HEIGHT = doubles[1];
        Log.i(TAG, "setBgBitmap: PAPER_WIDTH=" + PAPER_WIDTH + "//PAPER_HEIGHT=" + PAPER_HEIGHT + ",imageDpi:" + imageDpi);

//        }

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * 设置涂鸦触摸接口
     */
    public void setDoodleTouchEventListener(DoodleTouchEvent listener) {
        this.mOnTouchEvent = listener;
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
     * @param resId 图片id
     * @param dpi   底图的dpi
     */
    public void replaceBackgroundImage2(int resId, int dpi) {
        imageDpi = dpi;
        //setBgBitmap(resId, dpi);

        mBitmap = Bitmap.createBitmap(2800, 2100, Bitmap.Config.ARGB_8888);
        Double[] doubles = DotUtils.calculateBookSize(mBitmap, 150);
        //Log.i(TAG, "底图width=" + mBitmap.getWidth() + "//height=" + mBitmap.getHeight() + "换算之后的宽高；" + Arrays.toString(doubles));
        PAPER_WIDTH = doubles[0];
        PAPER_HEIGHT = doubles[1];
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


    public void setLastBookId() {
        this.mLastBookId = mBookId;
    }

    public void setNoteParameter(int bookId, int pageId) {
        mBookId = bookId;
        mPageId = pageId;
    }


    private void initCanvas(Bitmap bitmap) {
        if (sCanvas == null) {
            sCanvas = new Canvas();
        }
        //canvas抗锯齿
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        sCanvas.setBitmap(bitmap);
    }

    /**
     * 获取view截图
     *
     * @return
     */
    public Bitmap getScreenshot() {
        return bgWrite;
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
     * 根据像素密度动态计算笔迹宽度
     *
     * @param width
     * @return
     */
    private float transformWidth(float width) {
        float density = mContext.getResources().getDisplayMetrics().density;
        //Log.i(TAG, "transformWidth: width=" + width + "///density=" + density);
        return width * density * 0.5f;
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
            if (mPen == null || !(mPen instanceof StrokePen)) {
                mPen = new StrokePen(mContext);
            }
            setOnTouchListener(mOnTouchListener);
        } else if (penMode == TYPE_NORMAL_PEN) {
            //初始化无笔锋类
            if (mPen == null || !(mPen instanceof NormalPen)) {
                mPen = new NormalPen(mContext);
            }
            //((NormalPen) mPen).switchHandWrite();
            setOnTouchListener(mOnTouchListener);
        } else if (penMode == TYPE_DOODLE_PEN) {
            //初始化涂鸦类
            if (mPen == null || !(mPen instanceof NormalPen)) {
                mPen = new NormalPen(mContext);
            }
            ((NormalPen) mPen).switchDoodle();
            setOnTouchListener(mOnTouchListener);
        }
    }

    /**
     * 反撤销笔迹
     */
    public void redo() {
        if (currentPenMode == TYPE_DOODLE_PEN) {
            if (mPen instanceof NormalPen) {
                NormalPen doodlePen = (NormalPen) mPen;
                doodlePen.redo();
            }
        }
    }

    /**
     * 撤销笔迹
     */
    public void undo() {
        if (currentPenMode == TYPE_DOODLE_PEN) {
            if (mPen instanceof NormalPen) {
                NormalPen doodlePen = (NormalPen) mPen;
                doodlePen.undo();
            }
        }
    }


    @Override
    public void run() {
        while (mIsDrawing) {
//            long start = System.currentTimeMillis();
            try {
                mCanvas = mHolder.lockCanvas();
                //处理绘制
                if (mCanvas != null) {
                    if (bgWrite != null && !bgWrite.isRecycled()) {
//                        mCanvas.save();
                        mCanvas.setMatrix(canvasMatrix);

                        //画底图
                        mCanvas.drawBitmap(bgWrite, 0, 0, null);
//                      mCanvas.drawBitmap(bgWrite, canvasMatrix, null);

                        //画笔记
                        mPen.draws(mCanvas);
                        drawPageNum(mCanvas);
//                        mCanvas.restore();

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mCanvas != null) {
                    mHolder.unlockCanvasAndPost(mCanvas);
                }
            }
        }
    }

    private void drawPageNum(Canvas canvas) {
//        String bookId = "BookID=" + mBookId;
//        String pageId = "\tPageID=" + String.format("%02d", mPageId + 1);
//        canvas.drawText(bookId + pageId, BG_WIDTH / 2, BG_HEIGHT - 72, mTextPaint);
        if (mBookId == 0) {
            return;
        }
        if (mBookId != 168 && mPageId != 212) {
            if (mPageId % 2 == 0) {
                canvas.drawText(String.format("%02d", mPageId + 1), BG_WIDTH - 135, BG_HEIGHT - 72, mTextPaint);
            } else {
                canvas.drawText(String.format("%02d", mPageId + 1), 135, BG_HEIGHT - 72, mTextPaint);
            }
        }
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            //Log.i(TAG, "onTouch: x=" + x + "//y=" + y);
            MotionEvent motionEvent = MotionEvent.obtain(event);
            long touchTime = System.currentTimeMillis();
            int pressure = (int) motionEvent.getPressure();
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (currentPenMode == TYPE_DOODLE_PEN) {
                        mPen.onDown(x, y, pressure, sCanvas);
                        if (mOnTouchEvent != null) {
                            mOnTouchEvent.onDown(x, y, 0, touchTime);
                        }
                    } else {
                        if (openScale) {
                            scaleDragDetector.onTouchEvent(event);
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:

                    if (currentPenMode == TYPE_DOODLE_PEN) {
                        mPen.onMove(x, y, pressure, sCanvas);
                        if (mOnTouchEvent != null) {
                            mOnTouchEvent.onMove(x, y, 1, touchTime);
                        }
                    } else {
                        if (openScale && (scaleDragDetector.isScaling() || scaleDragDetector.isDragging() || event.getPointerCount() >= 1)) {
                            // 已经在拖动、缩放状态  或者  多指触摸  这种情况下 Touch事件都交由scaleDragDetector处理
                            scaleDragDetector.onTouchEvent(event);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:

                    if (currentPenMode == TYPE_DOODLE_PEN) {
                        mPen.onUp(x, y, pressure, sCanvas);
                        if (mOnTouchEvent != null) {
                            mOnTouchEvent.onUp(x, y, 2, touchTime);
                        }
                    } else {
                        if (openScale && (scaleDragDetector.isScaling() || scaleDragDetector.isDragging() || event.getPointerCount() >= 1)) {
                            //parentView.getEditMode() != StrokeView.EditMode.ERASER //非橡皮擦模式
                            scaleDragDetector.onTouchEvent(event);
                        }
                    }
                    break;
                default:
                    //其他状态
                    if (openScale && (scaleDragDetector.isScaling() || scaleDragDetector.isDragging() || event.getPointerCount() >= 1)) {
                        scaleDragDetector.onTouchEvent(event);
                    }
                    break;
            }
            return true;
        }
    };


    private boolean checkMatrixBounds(String source, Matrix matrix) {
        RectF rect = getDisplayRect(matrix);
        float height = rect.height();
        float width = rect.width();
        float deltaX = 0f;
        float deltaY = 0f;

        float viewHeight = getBG_HEIGHT();
        if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }
        float viewWidth = getBG_WIDTH();
        if (width <= viewWidth) {
            deltaX = -rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
        }
        matrix.postTranslate(deltaX, deltaY);
        RectF mRectDes = getDisplayRect(matrix);
        return true;
    }

    private RectF getDisplayRect(Matrix matrix) {
        RectF mDisplayRect = new RectF();
        mDisplayRect.set(0, 0, Float.valueOf(getBG_WIDTH()), Float.valueOf(getBG_HEIGHT()));
        matrix.mapRect(mDisplayRect);
        return mDisplayRect;
    }
}
