package com.sonix.oidbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.RequiresApi;

import com.google.common.collect.ArrayListMultimap;
import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.base.BindEventBus;
import com.sonix.pendraw.PenView;
import com.sonix.surfaceview.DrawView;
import com.sonix.util.ColorUtil;
import com.sonix.util.DataHolder;
import com.sonix.util.DotUtils;
import com.sonix.util.Events;
import com.sonix.util.FileUtils;
import com.sonix.util.LogUtils;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.pen.PenUtils;
import com.tqltech.tqlpencomm.util.BLEFileUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * 液晶板主界面
 */
@BindEventBus
public class BoardActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = BoardActivity.class.getSimpleName();

    private static final String LOG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TQL/"; //绘制数据保存目录

    private static final int MSG_REPLAY = 0;
    private static final boolean isSaveLog = false;

    private DrawView mPenView;

    private PenCommAgent penCommAgent;

    private boolean hasMeasured = false;
    private double A5_WIDTH;                                    //本子宽
    private double A5_HEIGHT;                                   //本子高
    private int BG_WIDTH;                                       //显示背景图宽
    private int BG_HEIGHT;                                      //显示背景图高
    private int gCurPageID = -1;                                //当前PageID
    private int gCurBookID = -1;                                //当前BookID
    private int gColor = 1;                                     //笔迹颜色
    private int gWidth = 1;                                     //笔迹粗细
    private int gSpeed = 3;                                     //笔迹回放速度
    private int gProgress = 0;                                  //离线数据下载进度

    private boolean startOffline;                               //开始下载离线标记
    private boolean showProgress;                               //显示离线下载进度标记
    private boolean bIsReplay;                                  //回放标记

    private int gReplayTotalNumber = 0;                         //回放数据总量
    private int gReplayCurrentNumber = 0;                       //当前回放数据位置标识

    private ArrayListMultimap<Integer, Dot> dot_number = ArrayListMultimap.create();    //每页笔迹数据
    private HashMap<Integer, ArrayListMultimap> map = new HashMap<>();                  //每本笔记数据

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm：");
    private String tmpLog;
    private float pointX;
    private float pointY;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_board;
    }

    @Override
    protected void initView() {
        mPenView = findViewById(R.id.penview);
        mPenView.getViewTreeObserver().addOnGlobalLayoutListener(viewLayoutListener);
    }

    @Override
    protected void initData() {
        penCommAgent = PenCommAgent.GetInstance(getApplication());
        gCurBookID = getIntent().getExtras().getInt("bookID",-1);
        gCurPageID = getIntent().getExtras().getInt("pageID",-1);
        int sectionID = getIntent().getExtras().getInt("sectionID",-1);
        int ownerID = getIntent().getExtras().getInt("ownerID",-1);

        if(gCurBookID == 0){
            calculateBookSize(R.drawable.lgt_game0, 300);
        }else if(gCurBookID == 168){
            calculateBookSize(R.drawable.board, 300);
        }

        setBackgroundImage(sectionID, ownerID, gCurBookID, gCurPageID);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (App.isDrawStoke) {
            mPenView.setPenMode(PenView.TYPE_STROKE_PEN);
        } else {
            mPenView.setPenMode(PenView.TYPE_NORMAL_PEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (App.getInstance().isDeviceConnected()) {
            App.getInstance().deviceDisConnect();
        }*/
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /*case R.id.tv_pen_status:
                showPenStatusDialog();
                break;
            case R.id.itv_width:
                showWidthPopup();
                break;
            case R.id.itv_color:
                showColorPopup();
                break;
            case R.id.itv_clear:
                showClearDialog();
                break;
            case R.id.itv_replay:
                showReplayPopup();
                break;
            case R.id.itv_more:
                showMorePopup();
                break;*/

            default:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FileUtils.GET_FILEPATH_SUCCESS_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    LogUtils.i(TAG, "onActivityResult: path=" + uri + ",path2=" + uri.getPath().toString());
                    if (uri != null) {
                        final String path = FileUtils.getRealPathFromURI(mContext, uri);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                penCommAgent.readTestData(path);
                            }
                        }).start();
                    }
                }
                break;
        }
    }

    /**
     * 背景图位置监听
     */
    private ViewTreeObserver.OnGlobalLayoutListener viewLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            measurePenView();
            mPenView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    };


    /**
     * 测量背景图
     */
    private void measurePenView() {
        if (!hasMeasured) {
            hasMeasured = true;

            BG_WIDTH = mPenView.getMeasuredWidth();
            BG_HEIGHT = mPenView.getMeasuredHeight();
            LogUtils.i(TAG, "measureImgView: BG_WIDTH=" + BG_WIDTH + ",BG_HEIGHT=" + BG_HEIGHT);
        }
    }

    /**
     * 计算本子
     */
    private void calculateBookSize(int resId, int dpi) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        LogUtils.i(TAG, "calculateBookSize: width=" + bitmap.getWidth() + ",height=" + bitmap.getHeight());
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        double bookWidth = ((double) bmpWidth / dpi) * Constants.IN_SIZE;
        double bookHeight = ((double) bmpHeight / dpi) * Constants.IN_SIZE;
        A5_WIDTH = bookWidth;
        A5_HEIGHT = bookHeight;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveOfflineNumber(Events.ReceiveOffline events) {
        LogUtils.i(TAG, "receiveOfflineNumber : " + events.offlineNum);
//        showOfflineNumberDialog(events.offlineNum);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveOfflineProgress(Events.ReceiveOfflineProgress events) {
        LogUtils.i(TAG, "receiveOfflineProgress : " + events.progress + "," + events.finished);
        gProgress = events.progress;
//        showOfflineProgressPopup(events.progress);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveOfflineDeleteStatus(Events.ReceiveOfflineDeleteStatus events) {
        LogUtils.i(TAG, "receiveOfflineDeleteStatus : " + events.isSucceed);
        if (events.isSucceed) {
            showToast(getString(R.string.offline_delete_success));
        } else {
            showToast(getString(R.string.offline_delete_failed));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true, priority = 2)
    public void receiveDot(Events.ReceiveDot receiveDot) {
        Dot dot = receiveDot.dot;

        if(dot.BookID != 168 && dot.BookID != 0){
            finish();//非手写板 回主界面
            return;
        }

        if (dot.type == Dot.DotType.PEN_DOWN && dot.BookID == 168 ) {
            if (dot.PageID == 212) {
                if (dot.x >= 17 && dot.x <= 25 && dot.y >= 6 && dot.y <= 10) {
                    gColor = 1;
                    mPenView.setPenColor(Color.RED);
                } else if (dot.x >= 33 && dot.x <= 42 && dot.y >= 6 && dot.y <= 10) {
                    gColor = 5;
                    mPenView.setPenColor(Color.BLUE);
                } else if (dot.x >= 48 && dot.x <= 57 && dot.y >= 6 && dot.y <= 10) {
                    dot.setColor(6);
                    mPenView.setPenColor(Color.WHITE);
                } else if (dot.x >= 69 && dot.x <= 74 && dot.y >= 6 && dot.y <= 10) {
                    mPenView.setPenWidth(1);
                } else if (dot.x >= 85 && dot.x <= 91 && dot.y >= 6 && dot.y <= 10) {
                    mPenView.setPenWidth(3);
                } else if (dot.x >= 103 && dot.x <= 108 && dot.y >= 6 && dot.y <= 10) {
                    mPenView.setPenWidth(5);
                } else if (dot.x >= 118 && dot.x <= 125 && dot.y >= 6 && dot.y <= 10) {
                    //二维码识别
                    if (gCurBookID == -1 && gCurPageID == -1) {
                        showToast(getResources().getString(R.string.no_data_recognition));
                        return;
                    }
                    Intent intentRecon = new Intent(mContext, RecognitionActivity.class);
                    List<Dot> list = getRecognitionData(gCurBookID, gCurPageID);
                    DataHolder.getInstance().setData("value", list);
                    startActivity(intentRecon);
                } else if (dot.x >= 134 && dot.x <= 141 && dot.y >= 6 && dot.y <= 10) {
                } else if (dot.x >= 150 && dot.x <= 158 && dot.y >= 6 && dot.y <= 10) {
                    //上一页
                } else if (dot.x >= 167 && dot.x <= 174 && dot.y >= 6 && dot.y <= 10) {
                    //下一页
                }
            } else {
                if (dot.x >= 22 && dot.x <= 33 && dot.y >= 17 && dot.y <= 22) {
                    gColor = 1;
                    mPenView.setPenColor(Color.RED);
                } else if (dot.x >= 35 && dot.x <= 46 && dot.y >= 17 && dot.y <= 22) {
                    gColor = 5;
                    mPenView.setPenColor(Color.BLUE);
                } else if (dot.x >= 48 && dot.x <= 59 && dot.y >= 17 && dot.y <= 22) {
                    dot.setColor(6);
                    mPenView.setPenColor(Color.WHITE);
                } else if (dot.x >= 144 && dot.x <= 169 && dot.y >= 17 && dot.y <= 22) {
                    mPenView.reset();
                }
            }

        }

        processDots(dot);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceDisconnected events) {
        LogUtils.i(TAG, "receivePenStatus Disconnected: " + events);
        if (!App.getInstance().isDeviceConnected()) {
            //tv_pen_status.setText(getString(R.string.not_connect_pen));
        }
    }

    /**
     * 处理数据（笔记回放时，接收数据不处理）
     *
     * @param dot
     */
    private void processDots(Dot dot) {
        // 回放模式，不接受点
        if (bIsReplay) {
            return;
        }

        processEachDot(dot);
    }

    /**
     * 处理绘制数据
     *
     * @param dot
     */
    private void processEachDot(Dot dot) {
        if (dot.force < 0) {
            return;
        }

        int BookID = dot.BookID;
        int PageID = dot.PageID;
        if (PageID < 0 || BookID < 0) {
            // 谨防笔连接不切页的情况
            return;
        }
        //LogUtils.i(TAG, "processEachDot 1: " + dot);
        if (dot.SectionID == 2 && dot.OwnerID == 200) {
            if (dot.x > 405 && dot.y > 20 && dot.x < 615 && dot.y < 175) {
                LogUtils.i(TAG, "processEachDot: 小尺寸板");
                dot.x = dot.x - 405;
                dot.y = dot.y - 20;
            } else if (dot.x > 0 && dot.y > 200 && dot.x < 260 && dot.y < 390) {
                LogUtils.i(TAG, "processEachDot: 大尺寸板");
                dot.x = dot.x - 0;
                dot.y = dot.y - 200;
            } else {
                return;
            }
        }
        //LogUtils.i(TAG, "processEachDot 2: " + dot);

        if (PageID != gCurPageID || BookID != gCurBookID && dot.type == Dot.DotType.PEN_DOWN) {
            setBackgroundImage(dot.SectionID, dot.OwnerID, BookID, PageID);
            mPenView.setNoteParameter(BookID, PageID);
            drawExistingStroke(BookID, PageID);

            gCurPageID = PageID;
            gCurBookID = BookID;
        }

        //拼接dot点
        float x = DotUtils.joiningTogether(dot.x, dot.fx);
        float y = DotUtils.joiningTogether(dot.y, dot.fy);

        if (PenUtils.penDotType == 18 || PenUtils.penDotType == 19 || PenUtils.penDotType == 51  ) {
            pointX = DotUtils.getPoint(dot.ab_x, mPenView.getBG_WIDTH(), mPenView.getPAPER_WIDTH(), DotUtils.getDistPerunit());
            pointY = DotUtils.getPoint(dot.ab_y, mPenView.getBG_HEIGHT(), mPenView.getPAPER_HEIGHT(), DotUtils.getDistPerunit());
        } else {
            pointX = DotUtils.getPoint(x, mPenView.getBG_WIDTH(), mPenView.getPAPER_WIDTH(), DotUtils.getDistPerunit());
            pointY = DotUtils.getPoint(y, mPenView.getBG_HEIGHT(), mPenView.getPAPER_HEIGHT(), DotUtils.getDistPerunit());
        }

        int pointZ = dot.force;

        if (pointZ > 0) {
            if (dot.type == Dot.DotType.PEN_DOWN) {
                LogUtils.i(TAG, "PEN_DOWN");
                if (PageID < 0 || BookID < 0) {
                    // 谨防笔连接不切页的情况
                    return;
                }

                mPenView.processDot(pointX, pointY, pointZ, 0);
            }

            if (dot.type == Dot.DotType.PEN_MOVE) {
                LogUtils.i(TAG, "PEN_MOVE");

                mPenView.processDot(pointX, pointY, pointZ, 1);
            }
        } else if (dot.type == Dot.DotType.PEN_UP) {
            LogUtils.i(TAG, "PEN_UP");

            mPenView.processDot(pointX, pointY, pointZ, 2);
        }

        saveData(dot, gColor, gWidth);
    }

    /**
     * 设置背景图
     *
     * @param sectionID
     * @param ownerID
     * @param bookID
     * @param pageID
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private void setBackgroundImage(int sectionID, int ownerID, int bookID, int pageID) {
        mPenView.reset();//盖一个背景  应该在replaceBackgroundImage之前调用  否则时间延迟可能会背景覆盖
        if (bookID == 168 && pageID == 212) {
            mPenView.replaceBackgroundImage(R.drawable.pager7, 300);
        } else if (bookID == 0 && sectionID == 0 && ownerID ==0) {
            switch (pageID){
                case 0:
                    mPenView.replaceBackgroundImage(R.drawable.lgt_game0, 300);
                    break;
                case 1:
                    mPenView.replaceBackgroundImage(R.drawable.lgt_game1, 300);
                    break;
                case 2:
                    mPenView.replaceBackgroundImage(R.drawable.lgt_game2, 300);
                    break;
                case 3:
                    mPenView.replaceBackgroundImage(R.drawable.lgt_game3, 300);
                    break;
                case 4:
                    mPenView.replaceBackgroundImage(R.drawable.lgt_game4, 300);
                    break;
            }
        } else {
            mPenView.replaceBackgroundImage(R.drawable.board, 300);
        }
    }

    /**
     * 绘制当前页历史数据
     *
     * @param bookID
     * @param pageID
     */
    private void drawExistingStroke(int bookID, int pageID) {
        ArrayListMultimap<Integer, Dot> dot_number4 = map.get(bookID);

        if (dot_number4 == null || dot_number4.isEmpty()) {
            return;
        }

        Set<Integer> keys = dot_number4.keySet();
        for (int key : keys) {
            if (key == pageID) {
                List<Dot> dots = dot_number4.get(key);
                LogUtils.i(TAG, "DrawExistingStroke: " + dots.size());
                for (int i = 0; i < dots.size(); i++) {
                    Dot dot = dots.get(i);

                    setPenColor(dot.color);
                    mPenView.setNoteParameter(dot.BookID, dot.PageID);
                    mPenView.processDot(dot);
                }
            }
        }
    }

    /**
     * 回放数据处理
     *
     * @param BookID
     * @param PageID
     * @param SpeedID
     */
    private void ReplayCurrentPage(int BookID, int PageID, int SpeedID) {
        ArrayListMultimap<Integer, Dot> dot_number4 = map.get(BookID);

        if (dot_number4 == null || dot_number4.isEmpty()) {
            bIsReplay = false;
            return;
        }


        Set<Integer> keys = dot_number4.keySet();
        for (int key : keys) {
            bIsReplay = true;
            if (key == PageID) {
                List<Dot> dots = dot_number4.get(key);
                gReplayTotalNumber = dots.size();
                gReplayCurrentNumber = 0;
                for (final Dot dot : dots) {
                    //笔锋绘制方法
                    if (bIsReplay) {
                        setPenColor(dot.color);
                        mPenView.processDot(dot);
                        gReplayCurrentNumber++;
                        /*if (popup instanceof PopupReplay && popup.isShowing()) {
                            gSpeed = ((PopupReplay) popup).getSpeed();
                        }
                        SystemClock.sleep(gSpeed * 10);
                        mHandle.sendEmptyMessage(MSG_REPLAY);*/
                    }
                }
            }
        }

        bIsReplay = false;
        /*if (popup instanceof PopupReplay) {
            ((PopupReplay) popup).setStart(false);
        }*/
    }

    /**
     * 回放
     */
    public void RunReplay() {
        if (gCurPageID < 0) {
            bIsReplay = false;
            return;
        }
        mPenView.reset();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReplayCurrentPage(gCurBookID, gCurPageID, gSpeed);
            }
        }).start();
    }

    /**
     * 内存存储数据
     *
     * @param dot
     * @param color
     * @param width
     */
    private void saveData(Dot dot, int color, int width) {
        dot_number.put(dot.PageID, dot);
        if (dot.BookID != gCurBookID) {
            map.put(dot.BookID, dot_number);
            dot_number.clear();
        } else {
            map.put(dot.BookID, dot_number);
        }
        LogUtils.i(TAG, "saveData: " + map.size());

        saveOutDotLog(dot);
    }

    /**
     * 储存日志
     *
     * @param dot
     */
    private void saveOutDotLog(Dot dot) {
        Date curDate = new Date(System.currentTimeMillis());
        String str = formatter.format(curDate);
        String logName = str.substring(0, 13);
        boolean bLogStart = false;

        if (logName.equals(tmpLog)) {
            bLogStart = false;
        } else {
            bLogStart = true;
            tmpLog = logName;
        }
        if (isSaveLog) {
            String fileName = str.substring(0, 10) + ".log";
            if (bLogStart) {
                BLEFileUtil.writeTxtToFile("-------------------------TQL SmartPen LOG--------------------------", LOG_PATH, fileName);
            }
            BLEFileUtil.writeTxtToFile(dot.toString(), LOG_PATH, fileName);
        }

    }


    /**
     * 设置笔记颜色
     *
     * @param ColorIndex
     */
    private void setPenColor(int ColorIndex) {
        switch (ColorIndex) {
            case 0:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.GRAY));
                break;
            case 1:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.RED));
                break;
            case 2:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.rgb(192, 192, 0)));
                break;
            case 3:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.rgb(0, 128, 0)));
                break;
            case 4:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.rgb(0, 0, 192)));
                break;
            case 5:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.BLUE));
                break;
            case 6:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.BLACK));
                break;
            case 7:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.MAGENTA));
                break;
            case 8:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.CYAN));
                break;
        }
    }

    /**
     * 获取本页手写识别数据
     *
     * @param BookID
     * @param PageID
     * @return
     */
    public List<Dot> getRecognitionData(int BookID, int PageID) {
        List<Dot> dotsList = new ArrayList<>();
        ArrayListMultimap<Integer, Dot> dot_number = map.get(BookID);

        Set<Integer> keys = dot_number.keySet();
        for (int key : keys) {
            if (key == PageID) {
                List<Dot> list = dot_number.get(key);

                for (Dot dot : list) {
                    dotsList.add(dot);
                }
            }
        }

        return dotsList;
    }

    @Override
    protected void onStop() {
        super.onStop();
        App.isNoGoToMain = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
