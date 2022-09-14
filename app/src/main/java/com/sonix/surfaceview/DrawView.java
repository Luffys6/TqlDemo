package com.sonix.surfaceview;



import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.sonix.oidbluetooth.R;
import com.sonix.util.DotUtils;
import com.sonix.util.LogUtils;
import com.sonix.util.ThreadManager;
import com.tqltech.tqlpencomm.bean.Dot;


/**
 * @author ljm
 * @date 2020/8/22.
 */
public class DrawView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "DrawView";
    private Context mContext;
    private SurfaceHolder mHolder;
    //    private Thread mThread;
//    final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    /**
     * 线程运行的标识，用于控制线程
     */
    private boolean mIsDrawing;
    private Bitmap mBitmap;
    /**
     * 笔记本实际规格大小默认B5笔记本
     */
    private double PAPER_WIDTH =  1;
    private double PAPER_HEIGHT =  1;
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
    private int mBookId, mPageId;
    private DoodleTouchEvent mOnTouchEvent;


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
//        生成背景图片
        setBgBitmap(mBgResourceId, imageDpi);
        init(context);
    }

    public boolean isChangLayout() {
        if (LAST_PAPER_WIDTH != PAPER_WIDTH && LAST_PAPER_HEIGHT != PAPER_HEIGHT) {
            return true;
        }
        return false;
    }



    private void init(Context context) {
        this.mContext = context;
        initSurface();
        //初始化页码的画笔
        initTextPaint();
        mPen = new NormalPen(context);
    }

    private void initSurface() {
//        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);
//        setWillNotDraw(false);
        mHolder = getHolder();
        //添加回调
        mHolder.addCallback(this);
        //背景黑色变透明
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.setFocusable(true);
//        this.setFocusableInTouchMode(true);
    }

    private void initTextPaint() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(35);
        mTextPaint.setDither(true);
        mTextPaint.setColor(Color.parseColor("#8ca2d1"));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.i(TAG, "onMeasure: ---------");
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




    public void setOnSizeChangeListener(OnSizeChangeListener listener) {
        this.mOnSizeChangeListener = listener;
    }

    public OnSizeChangeListener mOnSizeChangeListener;

    public interface OnSizeChangeListener {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        BG_WIDTH = w;
        BG_HEIGHT = h;
        LAST_PAPER_WIDTH = PAPER_WIDTH;
        LAST_PAPER_HEIGHT = PAPER_HEIGHT;
        if (BG_HEIGHT<=0||BG_WIDTH<=0){
            return;
        }
        bgWrite = Bitmap.createScaledBitmap(mBitmap, BG_WIDTH, BG_HEIGHT, true);
        initCanvas(bgWrite);

        if (mOnSizeChangeListener!=null){
            mOnSizeChangeListener.onSizeChanged(w,h,oldw,oldh);
            mOnSizeChangeListener=null;
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Log.i(TAG, "surfaceCreated: ");
        //创建线程
        mIsDrawing = true;
//        singleThreadExecutor.execute(new Thread(this));
        ThreadManager.getThreadPool().exeute(new Thread(this));
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Log.i(TAG, "surfaceDestroyed: ");
        mIsDrawing = false;
    }

    public void reset() {
        mPen.clear();
        if (BG_WIDTH<=0||BG_HEIGHT<=0){
            return;
        }
        bgWrite = Bitmap.createScaledBitmap(mBitmap, BG_WIDTH, BG_HEIGHT, true);
        if (mCanvas != null) {
            mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        initCanvas(bgWrite);
    }



    /**
     * 绘制数据
     *    历史 回访
     * @param dot
     */
    public void processDot(Dot dot) {
        if (currentPenMode != TYPE_DOODLE_PEN) {
            int bookId = dot.BookID;
            int pageId = dot.PageID;

            float x = DotUtils.joiningTogether(dot.x, dot.fx);
            float y = DotUtils.joiningTogether(dot.y, dot.fy);
            // TODO: 2022/1/17 出血位
//            x =   Math.max(x-12,0);
//            y =   Math.max(y-15,0);
            pointX = DotUtils.getPoint(x, BG_WIDTH, PAPER_WIDTH, DotUtils.getDistPerunit());
            pointY = DotUtils.getPoint(y, BG_HEIGHT, PAPER_HEIGHT, DotUtils.getDistPerunit());

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
    }

    /**
     * @param pointX  屏幕x轴座标
     * @param pointY  屏幕y轴座标
     * @param force   压力值
     * @param dotType 笔迹状态，down move up
     */
    public void processDot(float pointX, float pointY, int force, int dotType) {
//        Log.i(TAG, "pointX: " + pointX + "---pointY:" + pointY + "---force:" + force + "---dotType:" + dotType);

        switch (dotType) {
            case 0:
//                drawPageNum(sCanvas);
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
     * 底图id转化为bitmap,并算出点码规格
     *
     * @param bgResourceId
     * @param imageDpi
     */
    private void setBgBitmap(int bgResourceId, int imageDpi) {
//        InputStream is = this.getResources().openRawResource(bgResourceId);
//        mBitmap = BitmapFactory.decodeStream(is);
        mBitmap = BitmapFactory.decodeResource(getResources(), bgResourceId).copy(Bitmap.Config.ARGB_4444, true);
        Double[] doubles = DotUtils.calculateBookSize(mBitmap, imageDpi);
        //Log.i(TAG, "底图width=" + mBitmap.getWidth() + "//height=" + mBitmap.getHeight() + "换算之后的宽高；" + Arrays.toString(doubles));
        PAPER_WIDTH = doubles[0];
        PAPER_HEIGHT = doubles[1];
//                PAPER_WIDTH = 260;
//        PAPER_HEIGHT = 375;
        Log.e(TAG, "setBgBitmap: PAPER_WIDTH=" + PAPER_WIDTH + "//PAPER_HEIGHT=" + PAPER_HEIGHT);
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
//        if (mDrawThread != null && !mDrawThread.pause) {
//            mDrawThread.pauseThread();
//        }
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
//        bgWrite = Bitmap.createScaledBitmap(mBitmap, BG_WIDTH, BG_HEIGHT, true);
//
//        initCanvas(bgWrite);

        //重新调用一遍measure（）onLayout() onDraw()方法
        requestLayout();
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
        if (penMode == TYPE_NORMAL_PEN) {
            //初始化无笔锋类
            if (mPen == null || !(mPen instanceof NormalPen)) {
                mPen = new NormalPen(mContext);
            }
            //((NormalPen) mPen).switchHandWrite();

            setOnTouchListener(null);
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
            draw();
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }


    private void draw() {

        synchronized (this) {
            if (mIsDrawing) {
                try {
                    mCanvas = mHolder.lockCanvas();
                    if (mCanvas != null) {
                        if (bgWrite != null && !bgWrite.isRecycled()) {
                            //画底图
                            mCanvas.drawBitmap(bgWrite, 0, 0, null);
                        }
                        //画笔记
                        mPen.draws(mCanvas);

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
    }


//    private void drawPageNum(Canvas canvas) {
//        if (mPageId % 2 == 0) {
//            canvas.drawText(String.format("%02d", mPageId + 1), BG_WIDTH - 135, BG_HEIGHT - 72, mTextPaint);
//        } else {
//            canvas.drawText(String.format("%02d", mPageId + 1), 135, BG_HEIGHT - 72, mTextPaint);
//        }
//    }

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
                    //Log.i(TAG, "onTouch: 涂鸦down");
                    mPen.onDown(x, y, pressure, sCanvas);
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onDown(x, y, 0, touchTime);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    //Log.i(TAG, "onTouch: 涂鸦move");
                    mPen.onMove(x, y, pressure, sCanvas);
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onMove(x, y, 1, touchTime);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //Log.i(TAG, "onTouch: 涂鸦up");
                    mPen.onUp(x, y, pressure, sCanvas);
                    if (mOnTouchEvent != null) {
                        mOnTouchEvent.onUp(x, y, 2, touchTime);
                    }
                    break;
            }
            return true;
        }
    };

    public void processDotNew(Dot dot) {
        float x = DotUtils.joiningTogether(dot.x, dot.fx);
        float y = DotUtils.joiningTogether(dot.y, dot.fy);
        LogUtils.e("dbj", "BG_WIDTH=" + BG_WIDTH + ",BG_HEIGHT=" + BG_HEIGHT);
        pointX = DotUtils.getPoint(x, BG_WIDTH, PAPER_WIDTH, DotUtils.getDistPerunit());
        pointY = DotUtils.getPoint(y, BG_HEIGHT, PAPER_HEIGHT, DotUtils.getDistPerunit());
//        pointX = x * BG_WIDTH/182;
//        pointY = y*BG_HEIGHT/256;
        LogUtils.e("dbj", "pointX=" + pointX + ",pointY=" + pointY);

        switch (dot.type) {
            case PEN_DOWN:
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

}
