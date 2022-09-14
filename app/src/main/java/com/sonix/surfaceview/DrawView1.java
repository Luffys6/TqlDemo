package com.sonix.surfaceview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.RectF;
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
import com.tqltech.tqlpencomm.pen.PenUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author ljm
 * @date 2020/8/22.
 */
public class DrawView1 extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "DrawView";
    private Context mContext;
    private SurfaceHolder mHolder;
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


    private Matrix canvasMatrix = new Matrix();


    public DrawView1(Context context) {
        this(context, null);
    }

    public DrawView1(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawView1(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DrawView);
        //底图dpi
        imageDpi = typedArray.getInt(R.styleable.DrawView_resources_dpi, 300);
        //底图背景资源
        mBgResourceId = typedArray.getResourceId(R.styleable.DrawView_background_resources, R.drawable.tzg);
        //生成背景图片
        setBgBitmap(mBgResourceId, imageDpi);
        init(context);

    }


    private void init(Context context) {
        this.mContext = context;
        initSurface();
        //初始化页码的画笔
        initTextPaint();
        mPen = new NormalPen(context);
    }

    private void initSurface() {
//        setZOrderMediaOverlay(true);
        // setWillNotDraw(false);
        mHolder = getHolder();
        //添加回调
        mHolder.addCallback(this);
        //背景黑色变透明
        mHolder.setFormat(PixelFormat.TRANSPARENT);
//        this.setFocusable(true);
//        this.setFocusableInTouchMode(true);
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


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        BG_WIDTH = w;
        BG_HEIGHT = h;
        LAST_PAPER_WIDTH = PAPER_WIDTH;
        LAST_PAPER_HEIGHT = PAPER_HEIGHT;

        bgWrite = Bitmap.createScaledBitmap(mBitmap, w, h, true);

        initCanvas(bgWrite);

        if (mOnSizeChangeListener != null) {
            mOnSizeChangeListener.onSizeChanged(w, h, oldw, oldh);//切换后的Down点等待绘制
            mOnSizeChangeListener = null;
        }
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
//        mThread = new Thread(this);
        ThreadManager.getThreadPool().exeute(new Thread(this));

//        mThread.start();

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
         initCanvas(bgWrite);

    }

    public void processDotNew(Dot dot,int a ,int b) {
        float x = DotUtils.joiningTogether(dot.x, dot.fx);
        float y = DotUtils.joiningTogether(dot.y, dot.fy);
//        LogUtils.e("dbj", "BG_WIDTH=" + BG_WIDTH + ",BG_HEIGHT=" + BG_HEIGHT);
        pointX = DotUtils.getPoint(x, 1080, 182.03333059946698, DotUtils.getDistPerunit());
        pointY = DotUtils.getPoint(y, 1519, 256.03199615478513, DotUtils.getDistPerunit());
//        pointX = x * BG_WIDTH/182;
//        pointY = y*BG_HEIGHT/256;
        pointY = (pointY -b)*2;
        pointX = (pointX -a)*2;
//        LogUtils.e("dbj", "pointX=" + pointX + ",pointY=" + pointY);
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

    /**
     * 底图id转化为bitmap,并算出点码规格
     *
     * @param bgResourceId
     * @param imageDpi
     */
    private void setBgBitmap(int bgResourceId, int imageDpi) {


        InputStream is = this.getResources().openRawResource(bgResourceId);
        mBitmap = BitmapFactory.decodeStream(is);


        Double[] doubles = DotUtils.calculateBookSize(mBitmap, imageDpi);
        Log.i(TAG, "底图width=" + mBitmap.getWidth() + "//height=" + mBitmap.getHeight() + "换算之后的宽高；" + Arrays.toString(doubles));
        PAPER_WIDTH = doubles[0];
        PAPER_HEIGHT = doubles[1];
        Log.i(TAG, "setBgBitmap: PAPER_WIDTH=" + PAPER_WIDTH + "//PAPER_HEIGHT=" + PAPER_HEIGHT + ",imageDpi:" + imageDpi);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

                        //画笔记
                        mPen.draws(mCanvas);

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

}
