package com.sonix.oidbluetooth.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import okhttp3.Call;
import okhttp3.Response;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sonix.app.App;
import com.sonix.bean.PaperBean;
import com.sonix.bean.PointsBean;
import com.sonix.network.LoadCallBack;
import com.sonix.network.OkHttpManager;
import com.sonix.oidbluetooth.AboutActivity;
import com.sonix.oidbluetooth.BitErrorActivity;
import com.sonix.oidbluetooth.Constants;
import com.sonix.oidbluetooth.ParameterActivity;
import com.sonix.oidbluetooth.R;
import com.sonix.oidbluetooth.RecognitionActivity;
import com.sonix.oidbluetooth.SearchActivity;
import com.sonix.oidbluetooth.StrokeOrderActivity;
import com.sonix.oidbluetooth.TestActivity;
import com.sonix.oidbluetooth.bean.CalligraphyResult;
import com.sonix.oidbluetooth.bean.JudgeBean;
import com.sonix.oidbluetooth.view.MyImageTextView;
import com.sonix.oidbluetooth.view.PopupCheckListener;
import com.sonix.oidbluetooth.view.PopupColor;
import com.sonix.oidbluetooth.view.PopupListener;
import com.sonix.oidbluetooth.view.PopupMore;
import com.sonix.oidbluetooth.view.PopupOffline;
import com.sonix.oidbluetooth.view.PopupOfflineProgress;
import com.sonix.oidbluetooth.view.PopupReplay;
import com.sonix.oidbluetooth.view.PopupWidth;
import com.sonix.oidbluetooth.view.StrokeOrderView;
import com.sonix.oidbluetooth.view.ZoomView;
import com.sonix.ota.BtMcuActivity;
import com.sonix.ota.McuActivity;
import com.sonix.ota.OTAActivity;
import com.sonix.surfaceview.DrawView;
import com.sonix.surfaceview.DrawView1;
import com.sonix.util.ColorUtil;
import com.sonix.util.DataHolder;
import com.sonix.util.DotUtils;
import com.sonix.util.Events;
import com.sonix.util.FileUtils;
import com.sonix.util.LogUtils;
import com.sonix.util.SPUtils;

import com.sonix.util.ThreadManager;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.listener.ExportLogListener;

import com.tqltech.tqlpencomm.pen.PenData;
import com.tqltech.tqlpencomm.pen.PenUtils;
import com.tqltech.tqlpencomm.util.BLEByteUtil;
import com.tqltech.tqlpencomm.util.BLEFileUtil;
import com.tqltech.tqlpencomm.util.BLELogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HandCodeFragment extends Fragment implements View.OnClickListener {//App.ReceiveDotListener
    private static final String TAG = "HandCodeFragment";
    protected static final int PERMISSION_REQUEST = 40;

    private static final String LOG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TQL/"; //????????????????????????

    private static final int MSG_REPLAY = 0;
    private static final boolean isSaveLog = false;

    private Switch swSaveLog;
    private Switch swDrawStroke, swCodeValue;
    private Switch swFilterAlgorithm;
    private DrawView mPenView;
    private LinearLayout ll_bottom;

    private LinearLayout itv_width;
    private LinearLayout itv_color;
    private LinearLayout itv_clear;
    private LinearLayout itv_replay;
    private LinearLayout itv_more;
    private LinearLayout itv_submit;

    private PopupWindow popup, movePopup;

    private PenCommAgent penCommAgent;

    private boolean hasMeasured = false, isActiveActivity;
    private double A5_WIDTH;                                    //?????????
    private double A5_HEIGHT;                                   //?????????
    private int BG_WIDTH;                                       //??????????????????
    private int BG_HEIGHT;                                      //??????????????????
    private int gCurPageID = -1;                                //??????PageID
    private int gCurBookID = 100;                               //??????BookID
    private int gColor = 1;                                     //????????????
    private int gWidth = 1;                                     //????????????
    private int gSpeed = 1;                                     //??????????????????
    private int gProgress = 0;                                  //????????????????????????

    private boolean startOffline;                               //????????????????????????
    private boolean showProgress;                               //??????????????????????????????
    private boolean bIsReplay = false;                                  //????????????
    private boolean endOffline;                                 //????????????????????????

    private int gReplayTotalNumber = 0;                         //??????????????????
    private int gReplayCurrentNumber = 0;                       //??????????????????????????????

    private ArrayListMultimap<Integer, Dot> dot_number = ArrayListMultimap.create();    //??????????????????
    private HashMap<Integer, ArrayListMultimap> map = new HashMap<>();                  //??????????????????


    private ArrayListMultimap<Integer, Dot> dot_word = ArrayListMultimap.create();    //??????????????????

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm???");
    private String tmpLog;

    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REPLAY:
                    if (popup != null && popup instanceof PopupReplay) {
                        ((PopupReplay) popup).setTotalNumber(gReplayTotalNumber);
                        ((PopupReplay) popup).setCurrentNumber(gReplayCurrentNumber);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private int offlineNumber;
    private Dialog dialog;
    public static boolean isBessel = true;
    private float pointX;
    private float pointY;
    private Toast mToast;
    private Activity activity;
    private boolean isRequesting;
    private int index;
    private RelativeLayout llShowHandCode;
    private TextView tvForceHand;
    private TextView tvYhand;
    private TextView tvXhand;
    private TextView tvBidHand;
    private TextView tvOidHand;
    private TextView tvSidHand;
    private TextView tvPidHand;
    private Dialog dialogData;
    private Dialog dialogInvalid;
    private TextView tvInvalidCode;
    private ArrayList<Point> movePoints;
    private ArrayList<PointsBean> pointsBeans = new ArrayList<>();

    private PointsBean pointsBean;
    private ZoomView myLayout;
    private List<PaperBean.DataDTO.PosListDTO> posListBeans;
    private int widths, heights;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().setFormat(PixelFormat.TRANSLUCENT);

        View view = inflater.inflate(R.layout.fragment_hand, container, false);

        mPenView = view.findViewById(R.id.penview);
        mPenView.getViewTreeObserver().addOnGlobalLayoutListener(viewLayoutListener);
        gWidth = 1;
        mPenView.setPenWidth(gWidth);

        ll_bottom = view.findViewById(R.id.ll_bottom);
        itv_width = view.findViewById(R.id.itv_width);
        itv_color = view.findViewById(R.id.itv_color);
        itv_clear = view.findViewById(R.id.itv_clear);
        itv_replay = view.findViewById(R.id.itv_replay);
        itv_more = view.findViewById(R.id.itv_more);
        itv_submit = view.findViewById(R.id.itv_submit);
        itv_width.setOnClickListener(this);
        itv_color.setOnClickListener(this);
        itv_clear.setOnClickListener(this);
        itv_replay.setOnClickListener(this);
        itv_more.setOnClickListener(this);
        itv_submit.setOnClickListener(this);
        myLayout = view.findViewById(R.id.myLayout);
//        App.getInstance().setmReceiveDotListener(this);
        MyImageTextView clip = view.findViewById(R.id.clip);
        clip.setOnClickListener(view1 -> {
            startActivity(new Intent(getActivity(), TestActivity.class));
        });
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();

    }

    private void initData() {
        LogUtils.i(TAG, "initData: ");
        EventBus.getDefault().register(this);
        calculateBookSize(R.drawable.pager_positive, 300);
        getPos();

    }

    public void getPos() {
        Map<String, String> params = new HashMap<>();
        OkHttpManager.getInstance().postRequest(getActivity(), "http://192.168.6.162:8031/tea/get_word_pos/", new LoadCallBack<String>(getActivity(), false) {
            @Override
            protected void onSuccess(Call call, Response response, String s) {
                Gson gson = new Gson();
                PaperBean paperBean = gson.fromJson(s, PaperBean.class);
                if (paperBean.getResponse().equals("ok")) {
                    posListBeans = paperBean.getData().getPos_list();
                    widths = paperBean.getData().getW();
                    heights = paperBean.getData().getH();
                    addView();
                }
            }

            @Override
            protected void onError(Call call, int statusCode, Exception e) {
//                showShortToast("?????????????????????????????????");
                showToast("?????????????????????????????????");
            }
        }, params);

    }


//    private void addView() {
//
//        myLayout.post(() -> {
//            int width = myLayout.getWidth();
//            int height = myLayout.getHeight();
//            if (posListBeans == null) {
//                showToast("???????????????????????????");
//                return;
//            }
//            for (int i1 = 0; i1 < posListBeans.size(); i1++) {
//
//                MyView view = new MyView(getActivity());
//                myLayout.addView(view);
//
//                ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
////                ViewGroup.MarginLayoutParams margin1 = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
//
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(margin);
//                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(margin);
//                FrameLayout.LayoutParams ViewParams = new FrameLayout.LayoutParams(margin);
//
////                float xRatio1 = (float) width / (float) widths;
////                float yRatio1 = (float) height / (float) heights;
////                    thanH = yRatio1;
////                    thanW = xRatio1;
////                LogUtils.e("dbj????????????",width+"------"+height);
////                LogUtils.e("dbj????????????",xRatio1+"------"+yRatio1);
//                float xRatio1 = 1.0F;
//                float yRatio1 = 1.0F;
//                int x = (int) ((float) (posListBeans.get(i1).getX()) * xRatio1);
//                int y = (int) ((float) (posListBeans.get(i1).getY()) * yRatio1);
//                int ax = (int) ((float) (posListBeans.get(i1).getAx()) * xRatio1);
//                int ay = (int) ((float) (posListBeans.get(i1).getAy()) * yRatio1);
//                int w = ax - x;
//                int h = ay - y;
//                int left = x;
//                int top = y;
//
//                layoutParams.width = w;
//                layoutParams.height = h;
//                layoutParams.leftMargin = 0;
//                layoutParams.topMargin = 6;
//
//                params.width = w;
//                params.height = 32;
//                params.leftMargin = 0;
//                params.topMargin = 0;
//
//                ViewParams.width = w;
//                ViewParams.height = h+40;
//                ViewParams.leftMargin = left;
//                ViewParams.topMargin = top-32;
//                 view.getChildAt(1).setBackgroundResource(R.drawable.shape_black_border);
//                int finalI = i1;
//                view.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
////                        showReplayDialog(finalI);
//                        if (pointsBeans != null && pointsBeans.size() > 0) {
//                            onSave();
//                            showReplayDialog(finalI, 1);
//                        } else {
//                            GetResult(finalI);
//                        }
//                    }
//                });
//                view.setLayoutParams(ViewParams);
//                view.getChildAt(1).setLayoutParams(layoutParams);
//                view.getChildAt(0).setLayoutParams(params);
//            }
//        });
//    }


    private void addView() {
        myLayout.post(() -> {
            int width = myLayout.getWidth();
            int height = myLayout.getHeight();
            if (posListBeans == null) {
                showToast("???????????????????????????");
                return;
            }
            for (int i1 = 0; i1 < posListBeans.size(); i1++) {

                View view = new View(getActivity());
                myLayout.addView(view);

                ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(margin);

//                float xRatio1 = (float) width / (float) widths;
//                float yRatio1 = (float) height / (float) heights;
//                    thanH = yRatio1;
//                    thanW = xRatio1;
//                LogUtils.e("dbj????????????",width+"------"+height);
//                LogUtils.e("dbj????????????",xRatio1+"------"+yRatio1);
                float xRatio1 = 1.0F;
                float yRatio1 = 1.0F;
                int x = (int) ((float) (posListBeans.get(i1).getX()) * xRatio1);
                int y = (int) ((float) (posListBeans.get(i1).getY()) * yRatio1);
                int ax = (int) ((float) (posListBeans.get(i1).getAx()) * xRatio1);
                int ay = (int) ((float) (posListBeans.get(i1).getAy()) * yRatio1);
                int w = ax - x;
                int h = ay - y;
                int left = x;
                int top = y;
                layoutParams.width = w;
                layoutParams.height = h;
                layoutParams.leftMargin = left;
                layoutParams.topMargin = top;
                view.setBackgroundResource(R.drawable.shape_black_border);
                int finalI = i1;
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        showReplayDialog(finalI);
                        LogUtils.e("??????", "area=" + area + ",finalI=" + finalI);
                        if (pointsBeans != null && pointsBeans.size() > 0 && area == finalI) {
                            onSave();
                            showReplayDialog(finalI, 1);
                        } else {
                            GetResult(finalI);
                        }
                    }
                });
                view.setLayoutParams(layoutParams);
            }
        });
    }

    /**
     * ??????
     */
    private int area = -1;

    private void isRect(int x, int y, Dot dot) {
        if (posListBeans == null) {
            return;
        }
        for (int i = 0; i < posListBeans.size(); i++) {
            if (x >= posListBeans.get(i).getX() && y >= posListBeans.get(i).getY() && x <= posListBeans.get(i).getAx() && y <= posListBeans.get(i).getAy()) {
                int pointZ = dot.force;

                if (pointZ > 0) {
                    if (dot.type == Dot.DotType.PEN_DOWN) {
                        movePoints = new ArrayList<>();
                        pointsBean = new PointsBean();
                        movePoints.add(new Point((int) pointX, (int) pointY));
                        mPenView.processDot(pointX, pointY, pointZ, 0);
                    }
                    if (dot.type == Dot.DotType.PEN_MOVE) {
                        movePoints.add(new Point((int) pointX, (int) pointY));
                        mPenView.processDot(pointX, pointY, pointZ, 1);
                    }
                } else if (dot.type == Dot.DotType.PEN_UP) {
                    if (movePoints == null) {
                        return;
                    }
                    movePoints.add(new Point((int) pointX, (int) pointY));
                    if (movePoints.size() == 0) {
                        return;
                    }
                    pointsBean.setMovePoint(movePoints);
                    pointsBean.setIndex(i);
                    pointsBeans.add(pointsBean);
                    mPenView.processDot(pointX, pointY, pointZ, 2);
                }
                dot_word.put(i, dot);
                if (area != i) {
                    area = i;
                    //??????
                    if (pointsBeans != null && pointsBeans.size() > 0) {
                        onSave();
                    }
                }

            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();
        isActiveActivity = true;//??????????????????
        if (penCommAgent == null) {
            Log.i(TAG, "onResume penCommAgent==null");
            penCommAgent = PenCommAgent.GetInstance(activity.getApplication());
        }
        boolean connect = penCommAgent.isConnect();
        Log.i(TAG, "onResume isConnect:" + connect);
        LogUtils.i(TAG, "onResume: ");


        if (App.isDrawStoke) {
            //????????????
            mPenView.setPenMode(DrawView.TYPE_STROKE_PEN);
        } else {
            //????????????
            mPenView.setPenMode(DrawView.TYPE_NORMAL_PEN);
        }

    }

    public void onPause() {
        super.onPause();
        LogUtils.i(TAG, "onPause: ");

        isActiveActivity = false;  //???????????????
    }

    public void onStop() {
        super.onStop();
        LogUtils.i(TAG, "onStop:");


    }

    public void onDestroy() {
        super.onDestroy();
        LogUtils.i(TAG, "onDestroy: ");
        EventBus.getDefault().unregister(this);
        mHandle.removeCallbacksAndMessages(null);

        if (movePopup != null && movePopup.isShowing()) { // ??????????????????stop???????????????
            movePopup.dismiss();
        }
        if (popup != null && popup.isShowing()) {//&& !(popup instanceof PopupOfflineProgress)
            popup.dismiss();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        if (penCommAgent != null) {
            penCommAgent = null;
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.itv_width:
                if (isDeviceConnected()) return;
                showWidthPopup();
                break;
            case R.id.itv_color:
                if (isDeviceConnected()) return;
                showColorPopup();
                break;
            case R.id.itv_clear:
//                if (isDeviceConnected()) return;
                PenData.firstDot = false;
                showClearDialog();
                break;
            case R.id.itv_replay:
                if (isDeviceConnected()) return;
                ArrayListMultimap<Integer, Dot> dot_number4 = map.get(gCurBookID);
                if (dot_number4 == null || dot_number4.isEmpty()) {
                    showToast(getResources().getString(R.string.no_data));
                    return;
                }
                showReplayPopup();
                break;
            case R.id.itv_more:
                showMorePopup();
                break;
            case R.id.itv_submit:
                startActivity(new Intent(getActivity(), StrokeOrderActivity.class));
                break;
            default:
                break;
        }
    }


    private boolean isDeviceConnected() {
        if (!App.getInstance().isDeviceConnected()) {
            showGoConnectDialog();
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FileUtils.GET_FILEPATH_SUCCESS_CODE:
                LogUtils.i(TAG, "onActivityResult: resultCode=" + resultCode);
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    if (penCommAgent == null) {
                        penCommAgent = PenCommAgent.GetInstance(activity.getApplication());
                    }
                    LogUtils.i(TAG, "onActivityResult: path=" + uri + ",path2=" + uri.getPath().toString());
                    if (uri != null) {
                        final String path = FileUtils.getRealPathFromURI(activity, uri);
                        if (mPenView != null) {
                            mPenView.reset();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                penCommAgent.readTestData(path);
                            }
                        }).start();
                    }
                }
                break;
            default:
                break;
        }
    }


    /**
     * ?????????????????????
     */
    private ViewTreeObserver.OnGlobalLayoutListener viewLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            measurePenView();
            mPenView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    };


    /**
     * ???????????????
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
     * ????????????
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
        offlineNumber = events.offlineNum;
        penCommAgent.RemoveOfflineData();
//        showOfflineNumberDialog(events.offlineNum);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveOfflineProgress(Events.ReceiveOfflineProgress events) {
        LogUtils.i(TAG, "receiveOfflineProgress : " + events.progress + "," + events.finished);
        gProgress = events.progress;
        if (!endOffline) {
            endOffline = events.finished;//false????????????  true????????????????????????
            //startOffline???????????????  ??????????????????false
        }

        if (startOffline) {
//            showOfflineProgressPopup(events.progress);
            penCommAgent.RemoveOfflineData();
        } else {
            if (popup != null && popup.isShowing()) {
                popup.dismiss();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveOfflineProgress(Events.ReceiveOfflineDataTransferResponse events) {
        BLELogUtil.i(TAG, "receiveOfflineProgress : " + events.isSucceed);
        if (events.isSucceed) {
            showToast(getString(R.string.ztcg));
        } else {
            showToast(getString(R.string.ztsb));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveOfflineDeleteStatus(Events.ReceiveOfflineDeleteStatus events) {
        BLELogUtil.i(TAG, "receiveOfflineDeleteStatus : " + events.isSucceed);
        if (events.isSucceed) {
            showToast(getString(R.string.offline_delete_success));
            offlineNumber = 0;
        } else {
            showToast(getString(R.string.offline_delete_failed));
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void receiveDot(Events.ReceiveDot receiveDot) {

        Dot dot = receiveDot.dot;
        if ((dot.OwnerID == 201 && dot.BookID == 63) || dot.BookID == 168 || dot.BookID == 0) {
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            }
        } else {
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            }
        }
        processDots(dot);

    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param dot
     */
    private void processDots(Dot dot) {

        //???????????????
        if (dot.type == Dot.DotType.PEN_DOWN && dot.BookID == 168) {
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
                    //???????????????
                    if (gCurBookID == -1 && gCurPageID == -1) {
                        showToast(getResources().getString(R.string.no_data_recognition));
                        return;
                    }
                    Intent intentRecon = new Intent(getActivity(), RecognitionActivity.class);
                    List<Dot> list = getRecognitionData(gCurBookID, gCurPageID);
                    DataHolder.getInstance().setData("value", list);
                    startActivity(intentRecon);
                } else if (dot.x >= 134 && dot.x <= 141 && dot.y >= 6 && dot.y <= 10) {
                } else if (dot.x >= 150 && dot.x <= 158 && dot.y >= 6 && dot.y <= 10) {
                    //?????????
                } else if (dot.x >= 167 && dot.x <= 174 && dot.y >= 6 && dot.y <= 10) {
                    //?????????
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

        // ???????????????????????????
        if (bIsReplay) {
            return;
        }
        processEachDot(dot);
    }

    /**
     * ??????????????????
     *
     * @param dot
     */
    private void processEachDot(Dot dot) {
//        BLELogUtil.i(TAG, "processEachDot: " + dot.toString());
//        Log.i(TAG, "processEachDot: " + dot.toString());
        if (dot.force < 0) {
            return;
        }
        if (dot.type == null) {
            return;
        }

        int BookID = dot.BookID;
        int PageID = dot.PageID;
        if (PageID < 0 || BookID < 0) {
            // ?????????????????????????????????
            return;
        }
        if (dot.SectionID == 2 && dot.OwnerID == 200) {
            if (dot.x > 405 && dot.y > 20 && dot.x < 615 && dot.y < 175) {
                LogUtils.i(TAG, "processEachDot: ????????????");
                dot.x = dot.x - 405;
                dot.y = dot.y - 20;
            } else if (dot.x > 0 && dot.y > 200 && dot.x < 260 && dot.y < 390) {
                LogUtils.i(TAG, "processEachDot: ????????????");
                dot.x = dot.x - 0;
                dot.y = dot.y - 200;
            } else {
                LogUtils.i(TAG, "processEachDot: return");
                return;
            }
        }

        //LogUtils.i(TAG, "processEachDot 2: " + dot);
        saveData(dot, gColor, gWidth);
        if ((PageID != gCurPageID || BookID != gCurBookID) && dot.type == Dot.DotType.PEN_DOWN) {


            mPenView.setNoteParameter(BookID, PageID);

            setBackgroundImage(dot.SectionID, dot.OwnerID, BookID, PageID);

            //???????????????
            if (BookID != gCurBookID && mPenView.isChangLayout()) { //BookID ?????????  ?????????????????????????????????
                mPenView.setOnSizeChangeListener(new DrawView.OnSizeChangeListener() {
                    @Override
                    public void onSizeChanged(int w, int h, int oldw, int oldh) {

                        drawExistingStroke(BookID, PageID);
                        gCurPageID = PageID;
                        gCurBookID = BookID;
                        savePointData(dot, BookID, PageID);

                    }
                });
            } else {

                drawExistingStroke(BookID, PageID);//?????????????????????
                gCurPageID = PageID;
                gCurBookID = BookID;
                savePointData(dot, BookID, PageID);

            }
        } else {
            savePointData(dot, BookID, PageID);
        }

    }

    private void savePointData(Dot dot, int bookID, int pageID) {

        //???????????????????????????dot???
        float x = DotUtils.joiningTogether(dot.x, dot.fx);
        float y = DotUtils.joiningTogether(dot.y, dot.fy);
//        LogUtils.e("dbj111", "x :" + x + ",,y" + y);

//        showHandCode(dot);//????????????

        if (PenUtils.penDotType == 18 || PenUtils.penDotType == 19 || PenUtils.penDotType == 51) {
            pointX = DotUtils.getPoint(dot.ab_x, mPenView.getBG_WIDTH(), mPenView.getPAPER_WIDTH(), DotUtils.getDistPerunit());
            pointY = DotUtils.getPoint(dot.ab_y, mPenView.getBG_HEIGHT(), mPenView.getPAPER_HEIGHT(), DotUtils.getDistPerunit());
        } else {
            pointX = DotUtils.getPoint(x, mPenView.getBG_WIDTH(), mPenView.getPAPER_WIDTH(), DotUtils.getDistPerunit());
            pointY = DotUtils.getPoint(y, mPenView.getBG_HEIGHT(), mPenView.getPAPER_HEIGHT(), DotUtils.getDistPerunit());
        }

//        LogUtils.e("dbj111", "???????????? :" + pointX + ",,pointY" + pointY + ",PenUtils.penDotType:" + PenUtils.penDotType);
        isRect((int) pointX, (int) pointY, dot);
        /**
         * start
         */
//        int pointZ = dot.force;
////        float radioWidth =  (float) mPenView.getBG_WIDTH() /  2150F;  //983
////        float radioHeight = (float) mPenView.getBG_HEIGHT()/ 3024F;//   1387
//
////        float pointX2 = pointX/radioWidth;
////        float pointY2 = pointY/radioHeight;
////        LogUtils.e("dbj", "????????????pointX2 :" + pointX2 + ",,pointY2" + pointY2);
//        if (pointZ > 0) {
//            if (dot.type == Dot.DotType.PEN_DOWN) {
//                if (pageID < 0 || bookID < 0) {
//                    // ?????????????????????????????????Z
//                    return;
//                }
////                LogUtils.e("dbj", "downdowndowndowndown");
//                movePoints = new ArrayList<>();
//                pointsBean = new PointsBean();
//                movePoints.add(new Point((int) pointX, (int) pointY));
//                mPenView.processDot(pointX, pointY, pointZ, 0);
//            }
//            if (dot.type == Dot.DotType.PEN_MOVE) {
////                LogUtils.e("dbj", "movemovemovemovemovemove");
//                movePoints.add(new Point((int) pointX, (int) pointY));
//                mPenView.processDot(pointX, pointY, pointZ, 1);
//            }
//        } else if (dot.type == Dot.DotType.PEN_UP) {
//            if (movePoints == null) {
//                return;
//            }
////            LogUtils.e("dbj", "upupupupupupupup");
//            movePoints.add(new Point((int) pointX, (int) pointY));
//            if (movePoints.size() == 0) {
//                return;
//            }
//            pointsBean.setMovePoint(movePoints);
//            onSave();
////            pointsBeans.add(pointsBean);
//            mPenView.processDot(pointX, pointY, pointZ, 2);
//        }
/**
 * end
 */
    }

    public void onSave() {
        Gson gson = new Gson();
        String pos = gson.toJson(pointsBeans);
        LogUtils.e("dbj", pos);
        pointsBeans.clear();
        Map<String, String> params = new HashMap<>();
        params.put("pos_list", pos);
        OkHttpManager.getInstance().postRequest(getActivity(), "http://192.168.6.162:8031/tea/save_pos_new/", new LoadCallBack<String>(getActivity(), false) {
            @Override
            protected void onSuccess(Call call, Response response, String s) {
                Gson gson = new Gson();
                JudgeBean judgeBean = gson.fromJson(s, JudgeBean.class);
                if (judgeBean.getResponse().equals("ok")) {
                    showToast("???????????????????????????????????????");
//                    dot_word.get(index).clear();
                }
            }

            @Override
            protected void onError(Call call, int statusCode, Exception e) {
                showToast("?????????????????????????????????");
            }
        }, params);
//        String FILE_ROOT = Environment.getExternalStorageDirectory() + "/asd/";//?????????
//        FileUtils.writeTxtToFile(pos, FILE_ROOT, "dian.txt");
//        SavePos(pos);
    }

    private CalligraphyResult calligraphyResult;

    public void GetResult(int index) {

        Map<String, String> params = new HashMap<>();
        params.put("index_pos", index + "");
        LogUtils.e("dbjindex", index + "");
        OkHttpManager.getInstance().postRequest(getActivity(), "http://192.168.6.162:8031/tea/get_word_result/", new LoadCallBack<String>(getActivity(), false) {
            @Override
            protected void onSuccess(Call call, Response response, String s) {
                Gson gson = new Gson();
                calligraphyResult = gson.fromJson(s, CalligraphyResult.class);
                if (calligraphyResult.getResponse().equals("ok")) {
                    showReplayDialog(index, 0);

                } else {
                    showReplayDialog(index, 1);
                }
            }

            @Override
            protected void onError(Call call, int statusCode, Exception e) {
//                showShortToast("?????????????????????????????????");
                showToast("?????????????????????????????????");
            }
        }, params);

    }

    private void showHandCode(Dot dot) {
        if (dialogData == null) {
            dialogData = new Dialog(activity, R.style.customDialog2);
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.buttom_view_dot_data, null);
            llShowHandCode = view.findViewById(R.id.ll_show_hand_code);
            tvSidHand = view.findViewById(R.id.tv_Sid_hand);
            tvOidHand = view.findViewById(R.id.tv_Oid_hand);
            tvPidHand = view.findViewById(R.id.tv_Pid_hand);
            tvBidHand = view.findViewById(R.id.tv_Bid_hand);
            tvXhand = view.findViewById(R.id.tv_x_hand);
            tvYhand = view.findViewById(R.id.tv_y_hand);
            tvForceHand = view.findViewById(R.id.tv_force_hand);
            dialogData.setContentView(view);
            Window window = dialogData.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = 10;
            params.y = 100;
            params.width = 500;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
            window.setGravity(Gravity.TOP | Gravity.LEFT);
        }
        if (App.isShowHandCode) {
            tvSidHand.setText("SID =" + dot.SectionID);
            tvOidHand.setText("OID =" + dot.OwnerID);
            tvBidHand.setText("BID =" + dot.BookID);
            tvPidHand.setText("PID =" + dot.PageID);
            tvXhand.setText("X =" + dot.ab_x);
            tvYhand.setText("Y =" + dot.ab_y);
            tvForceHand.setText("Force =" + dot.force);
            dialogData.show();
        }
    }

    public void dismissDialog() {
        if (dialogData != null && dialogData.isShowing()) {
            dialogData.dismiss();
        }
    }

    public void dismissPenView(boolean b) {
        if (mPenView != null) {
            if (b) {
                mPenView.setVisibility(View.VISIBLE);
            } else {
                mPenView.setVisibility(View.GONE);
            }
        }
    }

    public PopupWindow reqPopWindow() {
        return popup;
    }

    /**
     * ???????????????
     *
     * @param sectionID
     * @param ownerID
     * @param bookID
     * @param pageID
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private void setBackgroundImage(int sectionID, int ownerID, int bookID, int pageID) {


        //Counter:93, SectionID:0, OwnerID:0, BookID:0, PageID:0, timelong:1643182080171,
        // x:96, y:40, fx:16, fy:86, ab_x:96.16, ab_y:40.86, force:184, type:PEN_MOVE, angle:207}
        if (sectionID == 2 && ownerID == 200) {
//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (bookID == 0 && pageID == 100) {
                mPenView.replaceBackgroundImage(R.drawable.empty_page1, 300);
            } else {
//                mPenView.replaceBackgroundImage2(R.drawable.pager_positive, 300);
                mPenView.replaceBackgroundImage(R.drawable.pager_positive, 300);
            }


        } else if (bookID == 0 && sectionID == 0 && ownerID == 0) {
//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            switch (pageID) {
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
                default:
                    mPenView.replaceBackgroundImage(R.drawable.empty_page1, 300);
                    break;
            }
        } else if (bookID == 168 && pageID == 212) {
//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mPenView.replaceBackgroundImage(R.drawable.pager7, 300);
        } else if (bookID == 168) {
//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mPenView.replaceBackgroundImage(R.drawable.board, 300);

        } else {
            if (bookID == 30) {
                mPenView.replaceBackgroundImage(R.drawable.pager_30, 300);
            } else {
                mPenView.replaceBackgroundImage(R.drawable.pager_positive, 300);
            }
        }
        mPenView.reset();
    }

    /**
     * ???????????????????????????
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
                for (int i = 0; i < dots.size(); i++) {
                    Dot dot = dots.get(i);
                    if (dot.BookID == bookID) {
                        setPenColor(dot.color);
//                        Log.i(TAG, dot.BookID + "  / ID /     " + bookID + "  setBgBitmap ????????????? dot =  " + dot.toString());
                        mPenView.setNoteParameter(dot.BookID, dot.PageID);
                        mPenView.processDot(dot);
                    }
                }
            }
        }
    }

    /**
     * ??????????????????
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
                    //??????????????????
                    if (bIsReplay) {
                        SetPenColor(dot.color);
                        mPenView.processDot(dot);
                        gReplayCurrentNumber++;
                        if (popup instanceof PopupReplay && popup.isShowing()) {
                            gSpeed = ((PopupReplay) popup).getSpeed();
                        }
                        SystemClock.sleep((6 - gSpeed) * 10);
                        mHandle.sendEmptyMessage(MSG_REPLAY);
                    }
                }
            }
        }

        bIsReplay = false;
        if (popup instanceof PopupReplay) {
            ((PopupReplay) popup).setStart(false);
        }
    }

    /**
     * ??????
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
     * ??????????????????
     *
     * @param dot
     * @param color
     * @param width
     */
    private void saveData(Dot dot, int color, int width) {
        dot_number.put(dot.PageID, dot);
        map.put(dot.BookID, dot_number);

//        LogUtils.i(TAG, "saveData: " + map.size());
//        saveOutDotLog(dot);
    }

    /**
     * ????????????
     *
     * @param dot
     */
    private void saveOutDotLog(Dot dot) {
        Date curDate = new Date(System.currentTimeMillis());
        String str = formatter.format(curDate);
        String logName = str.substring(0, 13);
        boolean bLogStart = false;
        //LogUtils.i(TAG, "saveOutDotLog: " + logName);
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
            //  BLEFileUtil.writeTxtToFile(dot.BookID,dot.PageID,dot.force,dot.angle, LOG_PATH, fileName,x,y);
        }
    }


    /**
     * ??????????????????
     *
     * @param ColorIndex
     */
    private void setPenColor(int ColorIndex) {
        switch (ColorIndex) {
            case 0:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.GRAY));
                break;
            case 1:
                mPenView.setPenColor(Color.RED);
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
     * ??????????????????????????????
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

    /**
     * ??????
     */
    private void showWidthPopup() {
        popup = new PopupWidth(activity);
        ((PopupWidth) popup).setListener(popupListener);
        ((PopupWidth) popup).setSelectIndex(gWidth);

        int popupWidth = ((PopupWidth) popup).getPopupWidth();    //????????????????????????
        int popupHeight = ((PopupWidth) popup).getPopupHeight();  //????????????????????????

        int[] location = new int[2];
        ll_bottom.getLocationOnScreen(location);

        LogUtils.i(TAG, "showPopup:location=" + location[0] + "," + location[1] + ",popupWidth=" + popupWidth + ",popupHeight=" + popupHeight);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // ?????????????????????7.0
            popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setFocusable(true);
            popup.showAtLocation(ll_bottom, Gravity.BOTTOM, 0, 105);
        } else {
            popup.showAtLocation(ll_bottom, Gravity.TOP, location[0], location[1] - popupHeight);
        }
    }

    /**
     * ??????
     */
    private void showColorPopup() {
        popup = new PopupColor(activity);
        ((PopupColor) popup).setListener(popupListener);
        ((PopupColor) popup).setSelectIndex(gColor);

        int popupWidth = ((PopupColor) popup).getPopupWidth();    //????????????????????????
        int popupHeight = ((PopupColor) popup).getPopupHeight();  //????????????????????????

        int[] location = new int[2];
        ll_bottom.getLocationOnScreen(location);

        // LogUtils.i(TAG, "showPopup:location=" + location[0] + "," + location[1] + ",popupWidth=" + popupWidth + ",popupHeight=" + popupHeight);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // ?????????????????????7.0
            popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setFocusable(true);
            popup.showAtLocation(ll_bottom, Gravity.BOTTOM, 0, 105);
        } else {
            popup.showAtLocation(ll_bottom, Gravity.TOP, (location[0]),
                    location[1] - popupHeight);
        }

    }

    /**
     * ??????
     */
    private void showClearDialog() {
        if (dialog == null) {
            dialog = new Dialog(activity, R.style.customDialog);
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_clear, null);

        TextView cancel = view.findViewById(R.id.tv_cancel);
        TextView delete = view.findViewById(R.id.tv_ok);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPenView != null) {
                    mPenView.reset();
                }
                if (!bIsReplay) {
                    dot_number.clear();
                }
                if (pointsBeans != null) {
                    pointsBeans.clear();
                }
                dialog.dismiss();
                dot_word.clear();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }


    private void ReplayNet(int index, DrawView1 mPenView) {
        int x = posListBeans.get(index).getX();
        int y = posListBeans.get(index).getY();
        List<CalligraphyResult.DataDTO.PosListDTO> dots = calligraphyResult.getData().getPos_list();
        if (dots == null || dots.isEmpty()) {
            bIsReplay = false;
            return;
        }
        for (int i = 0; i < dots.size(); i++) {
            bIsReplay = true;
            List<CalligraphyResult.DataDTO.PosListDTO.MovePointDTO> points = dots.get(i).getMovePoint();
            if (bIsReplay) {
                int type = 0;
                for (int i1 = 0; i1 < points.size(); i1++) {
//                    mPenView.setPenColor(1);
                    mPenView.setPenWidth(3);
                    if (i1 == 0) {
                        type = 0;
                    } else if (i1 == points.size() - 1) {
                        type = 2;
                    } else {
                        type = 1;
                    }
                    mPenView.processDotNewNet(points.get(i1), x, y, type);
                    SystemClock.sleep(30);
//                mHandle.sendEmptyMessage(MSG_REPLAY);
                }
            }
        }
        bIsReplay = false;
    }


    private void ReplayNet1(int index, DrawView1 mPenView, int a) {
        int x = posListBeans.get(index).getX();
        int y = posListBeans.get(index).getY();
        List<CalligraphyResult.DataDTO.PosListDTO> dots = calligraphyResult.getData().getPos_list();
        if (dots == null || dots.isEmpty()) {
            bIsReplay = false;
            return;
        }
        bIsReplay = true;
        if (a > dots.size()) {
            bIsReplay = false;
            return;
        }
        a = a - 1;
        List<CalligraphyResult.DataDTO.PosListDTO.MovePointDTO> points = dots.get(a).getMovePoint();
        if (bIsReplay) {
            int type = 0;
            for (int i1 = 0; i1 < points.size(); i1++) {
                mPenView.setPenColor(Color.RED);
                mPenView.setPenWidth(3);
                if (i1 == 0) {
                    type = 0;
                } else if (i1 == points.size() - 1) {
                    type = 2;
                } else {
                    type = 1;
                }
                mPenView.processDotNewNet(points.get(i1), x, y, type);
                SystemClock.sleep(50);
//                mHandle.sendEmptyMessage(MSG_REPLAY);
            }
        }

        bIsReplay = false;
    }


    private void Replay(int index, DrawView1 mPenView) {
        int x = posListBeans.get(index).getX();
        int y = posListBeans.get(index).getY();
        List<Dot> dots = dot_word.get(index);
        if (dots == null || dots.isEmpty()) {
            bIsReplay = false;
            return;
        }
        bIsReplay = true;

        for (final Dot dot : dots) {
            //??????????????????
            if (bIsReplay) {
                SetPenColor(dot.color);
                mPenView.setPenWidth(3);
                mPenView.processDotNew(dot, x, y);
                if (popup instanceof PopupReplay && popup.isShowing()) {
                    gSpeed = ((PopupReplay) popup).getSpeed();
                }
                SystemClock.sleep((6 - gSpeed) * 10);

//                mHandle.sendEmptyMessage(MSG_REPLAY);
            }
        }


        bIsReplay = false;
        if (popup instanceof PopupReplay) {
            ((PopupReplay) popup).setStart(false);
        }
    }


    private void showReplayDialog(int index, int t) {

        View pop = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_replay, null);
        final DrawView1 penview_dialog = pop.findViewById(R.id.penview_dialog);
        LinearLayout list = pop.findViewById(R.id.list);
        TextView evaluate = pop.findViewById(R.id.evaluate);
        TextView content_tv = pop.findViewById(R.id.content);
        ScrollView scrollView = pop.findViewById(R.id.scro);
        StrokeOrderView strokeOrderView = pop.findViewById(R.id.stroke_order_view);
        String name;
        if (index == 0) {
            name = "data/???.json";
        } else if (index == 1) {
            name = "data/???.json";
        } else {
            name = "data/???.json";
        }
        String svgSix = getFromAssets(name);
        strokeOrderView.setStrokesBySvg(svgSix);
        if (t == 0) {
            if (calligraphyResult.getData().getContent().getContent().isEmpty()) {
                content_tv.setVisibility(View.GONE);
            } else {
                content_tv.setText(calligraphyResult.getData().getContent().getContent());
            }
            List<CalligraphyResult.DataDTO.Content.ListDTO> listDTOS = calligraphyResult.getData().getContent().getList();
            initListData(listDTOS, list, penview_dialog, index);
            evaluate.setText(new StringBuilder().append("?????????????????????").append(calligraphyResult.getData().getScore()).append("???").toString());
            if (!bIsReplay) {
                new Handler().postDelayed(() -> ThreadManager.getThreadPool().exeute(new Thread(() -> ReplayNet(index, penview_dialog))), 1000);
            }
        } else if (t == 1) {
            content_tv.setVisibility(View.GONE);
            scrollView.setVisibility(View.GONE);
            evaluate.setText(new StringBuilder().append("????????????..."));
            if (!bIsReplay) {
                new Handler().postDelayed(() -> ThreadManager.getThreadPool().exeute(new Thread(() -> Replay(index, penview_dialog))), 1000);
            }
        }
        penview_dialog.post(new Runnable() {
            @Override
            public void run() {
                LogUtils.e("dbj", penview_dialog.getWidth() + penview_dialog.getHeight() + "----");
            }
        });

        PopupWindow popupWindow = new PopupWindow(pop, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        popupWindow.setWidth(720);
//                popupWindow.setAnimationStyle(R.style.first_popwindow_anim_style);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(false);

        View parent = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_hand, null);
        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
        //popupWindow?????????????????????????????????
        final WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.alpha = 0.5f;
        activity.getWindow().setAttributes(params);
        pop.findViewById(R.id.close).setOnClickListener(v -> {
            bIsReplay = false;
            penview_dialog.destroyDrawingCache();
            params.alpha = 1.0f;
            popupWindow.dismiss();
        });
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                params.alpha = 1.0f;
                activity.getWindow().setAttributes(params);
            }
        });
    }


    private void initListData(List<CalligraphyResult.DataDTO.Content.ListDTO> listDTOS, LinearLayout list, DrawView1 penview_dialog, int index) {
        final int size = listDTOS.size();

        for (int i = 0; i < size; i++) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            final View itemLayout = inflater.inflate(R.layout.name_item, null);
            TextView tvNmae = itemLayout.findViewById(R.id.tv_name);
            LinearLayout layoutContent = itemLayout.findViewById(R.id.layout_content);
            if (listDTOS.get(i).getContent().size() == 0) {
                tvNmae.setVisibility(View.GONE);
            } else {
                tvNmae.setVisibility(View.VISIBLE);
                tvNmae.setText(listDTOS.get(i).getName());
            }
            for (int j = 0; j < listDTOS.get(i).getContent().size(); j++) {
                CalligraphyResult.DataDTO.Content.ListDTO.ContentDTO contentDTO = listDTOS.get(i).getContent().get(j);
                final View classItemLayout = inflater.inflate(R.layout.content_item, null);
                final TextView tv_content_name = classItemLayout.findViewById(R.id.tv_content_name);
                tv_content_name.setText(contentDTO.getContent());

                tv_content_name.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LogUtils.e("dbjbIsReplay", bIsReplay + "--");
                        if (!bIsReplay) {
                            ThreadManager.getThreadPool().exeute(new Thread(() -> ReplayNet1(index, penview_dialog, contentDTO.getSequence())));
                        }
                        showToast(contentDTO.getContent());
                    }
                });
                layoutContent.addView(classItemLayout);
            }
            list.addView(itemLayout);
        }
    }


    public void clearPenView() {
        PenData.firstDot = false;
        if (mPenView != null) {
            mPenView.reset();
        }

        if (!bIsReplay) {
            dot_number.clear();
        }
    }

    /**
     * ??????
     */
    private void showReplayPopup() {
        if (popup == null || !(popup instanceof PopupReplay)) {
            popup = new PopupReplay(activity);
        }
        ((PopupReplay) popup).setListener(popupListener);

        ((PopupReplay) popup).setTotalNumber(gReplayTotalNumber);
        ((PopupReplay) popup).setCurrentNumber(gReplayCurrentNumber);
        ((PopupReplay) popup).setSpeed(gSpeed);
        ((PopupReplay) popup).setStart(bIsReplay);

        int popupWidth = ((PopupReplay) popup).getPopupWidth();    //????????????????????????
        int popupHeight = ((PopupReplay) popup).getPopupHeight();  //????????????????????????
        int[] location = new int[2];
        ll_bottom.getLocationOnScreen(location);
        // ((PopupReplay) popup).setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        LogUtils.i(TAG, "showPopup:location=" + location[0] + "," + location[1] + ",popupWidth=" + popupWidth + ",popupHeight=" + popupHeight);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // ?????????????????????7.0
            popup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setFocusable(true);
            popup.showAtLocation(ll_bottom, Gravity.BOTTOM, 0, 105);
        } else {
            popup.showAtLocation(ll_bottom, Gravity.TOP, location[0], location[1] - popupHeight);
        }
    }

    /**
     * ??????   ????????????movePopup  ????????????????????????  ???????????????
     */
    private void showMorePopup() {
        if (movePopup == null || !(movePopup instanceof PopupMore)) {
            movePopup = new PopupMore(activity);
        }
        ((PopupMore) movePopup).setListener(popupListener);

        ((PopupMore) movePopup).setCheckedListener(popupCheckListener);
        int popupWidth = ((PopupMore) movePopup).getPopupWidth();    //????????????????????????
        int popupHeight = ((PopupMore) movePopup).getPopupHeight();  //????????????????????????

        int[] location = new int[2];
        ll_bottom.getLocationOnScreen(location);

//        LogUtils.i(TAG, "showPopup:location=" + location[0] + "," + location[1] + ",popupWidth=" + popupWidth + ",popupHeight=" + popupHeight);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // ?????????????????????7.0
            movePopup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            movePopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            movePopup.setFocusable(true);
            movePopup.showAtLocation(ll_bottom, Gravity.BOTTOM | Gravity.RIGHT, 0, 105);
        } else {
            movePopup.showAtLocation(ll_bottom, Gravity.TOP | Gravity.RIGHT, location[0], location[1] - popupHeight);
        }
    }

    /**
     * ??????
     */
    private void showOfflinePopup() {

        if (popup == null || !(popup instanceof PopupOffline)) {
            popup = new PopupOffline(activity);
        }
        ((PopupOffline) popup).setListener(popupListener);

        int popupWidth = ((PopupOffline) popup).getPopupWidth();    //????????????????????????
        int popupHeight = ((PopupOffline) popup).getPopupHeight();  //????????????????????????

        int[] location = new int[2];
        ll_bottom.getLocationOnScreen(location);

        LogUtils.i(TAG, "showPopup:location=" + location[0] + "," + location[1] + ",popupWidth=" + popupWidth + ",popupHeight=" + popupHeight);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // ?????????????????????7.0
            popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setFocusable(true);
            popup.showAtLocation(ll_bottom, Gravity.BOTTOM | Gravity.RIGHT, 0, 105);
        } else {
            popup.showAtLocation(ll_bottom, Gravity.TOP | Gravity.RIGHT, (location[0]), location[1] - popupHeight);
        }

    }


    /**
     * ????????????????????????
     */
    private void showOfflineProgressPopup(int progress) {

        if (popup == null || !(popup instanceof PopupOfflineProgress)) {
            popup = new PopupOfflineProgress(activity);
            ((PopupOfflineProgress) popup).setListener(popupListener);

            popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    Log.i(TAG, "OnDismissListener");
                    showProgress = false;
                    PenUtils.isMcuUpgrade = false;
                    PenUtils.isReqOfflineData = false;
//                    popup = new PopupOffline(activity);
                }
            });
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // ?????????????????????7.0
                popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setFocusable(true);
                //  popup.showAtLocation(ll_bottom, Gravity.BOTTOM, 0, 105);
            }
        }


        popup.showAtLocation(mPenView, Gravity.CENTER, 0, 0);

        if (progress == 100) {
            ((PopupOfflineProgress) popup).setProgress(99);
        } else {
            ((PopupOfflineProgress) popup).setProgress(progress);
        }


//        if (progress == 100) {
//            //???????????????????????????   ?????????????????????100
        if (endOffline) {
            ((PopupOfflineProgress) popup).setProgress(100);
            //?????????100  ????????????????????????????????????
            mHandle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((PopupOfflineProgress) popup).dismiss();
                    showProgress = false;
                }
            }, 200);
        }
//        }

    }

    /**
     * ?????????dialog
     */
    private void showPenStatusDialog() {
        if (dialog == null) {
            dialog = new Dialog(activity, R.style.customDialog);
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_pen_status, null);
        RelativeLayout rl_close = view.findViewById(R.id.rl_close);
        TextView tv_pen_name = view.findViewById(R.id.tv_pen_name);
        TextView tv_pen_mac = view.findViewById(R.id.tv_pen_mac);
        TextView tv_pen_battery = view.findViewById(R.id.tv_pen_battery);
        TextView tv_change_pen = view.findViewById(R.id.tv_change_pen);

        tv_pen_name.setText(getString(R.string.pen_name, App.getInstance().getDeviceName()));
        tv_pen_mac.setText(getString(R.string.pen_address, App.getInstance().getDeviceAddress()));
        tv_pen_battery.setText(getString(R.string.pen_battery, App.getInstance().getDeviceBattery()));

        rl_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_change_pen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                App.getInstance().deviceDisConnect();
                Intent intent = new Intent(activity, SearchActivity.class);
                startActivity(intent);
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    /**
     * ????????????dialog
     */
    private void showGoConnectDialog() {
        if (dialog == null) {
            dialog = new Dialog(activity, R.style.customDialog);
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_go_connect, null);
        TextView cancel = view.findViewById(R.id.cancel);
        TextView connect = view.findViewById(R.id.connect);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                Intent intent = new Intent(activity, SearchActivity.class);
                startActivity(intent);
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     * ??????????????????dialog
     *
     * @return
     */
    private void showOfflineNumberDialog(int number) {
        if (!isActiveActivity) {
            return;
        }
        if (dialog == null) {
            dialog = new Dialog(activity, R.style.customDialog);
        }
        //alertDialog??????????????????????????????
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_offline_number, null);
        TextView offNumTextView = view.findViewById(R.id.textView2);
        TextView confirmBtn = view.findViewById(R.id.button);

        offNumTextView.setText(number + "bytes");

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);

        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }

    }


    /**
     * Switch??????????????????
     */
    PopupCheckListener popupCheckListener = new PopupCheckListener() {
        @Override
        public void checkedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.sw_save_log:
                    Log.e(TAG, "??????????????????-------" + isChecked);
                    App.isSaveSDKLog = isChecked;
                    swSaveLog = buttonView.findViewById(R.id.sw_save_log);
                    boolean mPermissionTip = (Boolean) SPUtils.get(activity, com.sonix.oidbluetooth.Constants.FIRST_SAVE, false);
                    Log.i(TAG, "onResume mPermissionTip::" + mPermissionTip);
                    if (mPermissionTip) {
                        requestPermission1(1);
                    } else {
                        initPermissionsTip(1);
                    }
                    break;
                case R.id.sw_draw_stroke:
                    Log.e(TAG, "??????????????????-------" + isChecked);
                    App.isDrawStoke = isChecked;
                    swDrawStroke = buttonView.findViewById(R.id.sw_draw_stroke);
                    if (isChecked) {
                        //????????????
                        mPenView.setPenMode(DrawView.TYPE_STROKE_PEN);
                    } else {
                        //????????????
                        mPenView.setPenMode(DrawView.TYPE_NORMAL_PEN);
                    }
                    break;
                case R.id.sw_invalid_code_value:
                    //??????????????????16*16  ?????????12*12
                    penCommAgent.setCodeValue(isChecked);
                    break;
                case R.id.sw_practise_calligraphy:
                    //????????????  ????????????
                    penCommAgent.setWriteSpeed(isChecked ? 1 : 0);
                    break;
                case R.id.sw_filter_algorithm:

                    swFilterAlgorithm = buttonView.findViewById(R.id.sw_filter_algorithm);
                    App.isShowAlgorithm = isChecked;
                    penCommAgent.setIsSplitFiltration(isChecked);
                    break;
                case R.id.sw_hand_code:
                    Log.e(TAG, "????????????-------" + isChecked);
                    App.isShowHandCode = isChecked;
                    if (App.isShowHandCode) {
                        //  llShowHandCode.setVisibility(View.VISIBLE);
                        // dialogData.show();
                    } else {
                        if (dialogData != null) {
                            dialogData.dismiss();
                        }
                        //   llShowHandCode.setVisibility(View.GONE);
                    }
                    break;
                case R.id.sw_invalid_code:
                    Log.e(TAG, "?????????-------" + isChecked);
                    App.isInvalidCode = isChecked;
                    if (isChecked) {
                        penCommAgent.reqInvalidCode(App.is415Pen);
                    }
                    break;
            }
            updateStatus();
        }
    };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveInvalidCode(Events.ReceiveInvalidCode invalidCode) {
        Log.d(TAG, "InvalidCodeReporting ?????????: " + BLEByteUtil.bytesToHexString(invalidCode.data_invalid));
        showInvalidCode(invalidCode.data_invalid);
    }

    private void showInvalidCode(byte[] code) {
        if (App.isInvalidCode) {
            if (dialogInvalid == null) {
                dialogInvalid = new Dialog(activity, R.style.customDialog2);
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.buttom_view_invalid_code,
                        null);
                // showInvalidCode = view.findViewById(R.id.ll_show_invalid_code);
                tvInvalidCode = view.findViewById(R.id.tv_invalid_code);
                dialogInvalid.setContentView(view);
                Window window = dialogInvalid.getWindow();
                WindowManager.LayoutParams params = window.getAttributes();
                // params.x = 10;
                params.y = 120;
                params.width = 500;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
                window.setGravity(Gravity.TOP | Gravity.CENTER);
                tvInvalidCode.setText(getResources().getString(R.string.currentInvalid) + "\n" + BLEByteUtil.bytesToHexString(code));
            }
            dialogInvalid.show();
        }
    }


    /**
     * ???????????????
     */
    PopupListener popupListener = new PopupListener() {
        @Override
        public void click(int id) {
            switch (id) {
                case R.id.ll_recognition:
                    if (isDeviceConnected()) return;
                    if (gCurBookID == -1 && gCurPageID == -1) {
                        showToast(getResources().getString(R.string.no_data_recognition));
                        return;
                    }
                    Intent intentRecon = new Intent(activity, RecognitionActivity.class);
                    RecognitionActivity.dotsList = getRecognitionData(gCurBookID, gCurPageID);
//                    DataHolder.getInstance().setData("value", list);
                    if (RecognitionActivity.dotsList != null && RecognitionActivity.dotsList.size() > 0) {
                        startActivity(intentRecon);
                    } else {
                        showToast(getResources().getString(R.string.no_data_recognition));
                    }
                    break;
                case R.id.ll_offline:
                    if (isDeviceConnected()) return;
                    showOfflinePopup();
                    break;
                case R.id.ll_setting:
                    if (isDeviceConnected()) return;
                    Intent paramIntent = new Intent(activity, ParameterActivity.class);
                    startActivity(paramIntent);
                    break;
                case R.id.ll_bt_log:
//                    if (isDeviceConnected()) return;
                    penCommAgent.exportBtLog(new ExportLogListener() {
                        @Override
                        public void exportLogStart() {
                            mHandle.post(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("??????????????????");
                                }
                            });

                        }

                        @Override
                        public void exportLogEnd() {
                            mHandle.post(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("????????????!!");
                                }
                            });

                        }

                        @Override
                        public void exportLogError(String error) {
                            mHandle.post(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("" + error);
                                }
                            });
                        }
                    });
                    break;
                case R.id.ll_bt:
                    if (isDeviceConnected()) return;
                    Intent intent = new Intent(activity, OTAActivity.class);
//                    Intent intent = new Intent(activity, Test.class);
                    intent.putExtra("addr", App.getInstance().getDeviceAddress());
                    intent.putExtra("type", App.getInstance().getDeviceType());
                    startActivity(intent);
                    break;
                case R.id.ll_mcu:
                    if (isDeviceConnected()) return;
                    Intent mcu_intent = new Intent(activity, McuActivity.class);
                    mcu_intent.putExtra("addr", App.getInstance().getDeviceAddress());
                    mcu_intent.putExtra("type", App.getInstance().getDeviceType());
                    startActivity(mcu_intent);
                    break;
                case R.id.ll_mcu_ota:
                    if (isDeviceConnected()) return;
                    Intent mcu_ota_intent = new Intent(activity, BtMcuActivity.class);
                    mcu_ota_intent.putExtra("addr", App.getInstance().getDeviceAddress());
                    mcu_ota_intent.putExtra("type", App.getInstance().getDeviceType());
                    startActivity(mcu_ota_intent);
                    break;
                case R.id.ll_txt:
//                    if (isDeviceConnected()) return;
                    boolean mPermissionTip = (Boolean) SPUtils.get(activity, com.sonix.oidbluetooth.Constants.FIRST_SAVE, false);
                    Log.i(TAG, "onResume mPermissionTip::" + mPermissionTip);
                    if (mPermissionTip) {
                        requestPermission1(2);
                    } else {
                        initPermissionsTip(2);
                    }
                    break;
                case R.id.ll_bit_error:
                    if (isDeviceConnected()) return;

                    App.isNoGoToMain = true;//?????????main
                    Intent bitErrorIntent = new Intent(activity, BitErrorActivity.class);
                    startActivity(bitErrorIntent);

                    break;
                case R.id.ll_about:
                    Intent aboutIntent = new Intent(activity, AboutActivity.class);
                    startActivity(aboutIntent);
                    break;
                case R.id.ll_offline_back:
                    popup.dismiss();
                    showMorePopup();
                    break;
                case R.id.tv_beiSaiEr:
                    if (App.isDrawStoke) {
                        showToast("???????????????");
                    } else {
                        isBessel = true;
                        showToast("?????????????????????");
                    }
                    break;
                case R.id.tv_lineTo:
                    if (App.isDrawStoke) {
                        showToast("???????????????");
                    } else {
                        showToast("??????????????????");
                        isBessel = false;
                    }
                    break;
                case R.id.tv_opne_8clockAlgorithm:
                    showToast("??????8???????????????");
                    PenUtils.is8ClockAlgorithm = true;
                    break;
                case R.id.tv_close_8clockAlgorithm:
                    showToast("??????8???????????????");
                    PenUtils.is8ClockAlgorithm = false;
                    break;
                case R.id.tv_Migration_close:
                    showToast("??????????????????????????????");
                    penCommAgent.setIsSplitMigration(false);
                    break;
                case R.id.tv_Migration_open:
                    penCommAgent.setIsSplitMigration(true);
                    showToast("??????????????????????????????");
                    break;
                case R.id.ll_start_offline:
                    Log.d(TAG, "showProgress:" + showProgress + ",getDeviceType--->" + App.getInstance().getDeviceType() + "==" + App.mBattery + ",offlineNumber=" + offlineNumber);
                    if (showProgress) {
                        showProgress = false;
                        return;
                    }
                    if (offlineNumber == 0) {
                        Toast.makeText(activity, "????????????????????????", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PenData.firstDot = false;
                    if (popup != null && popup instanceof PopupOffline) {
                        popup.dismiss();
                    }
                    if ((App.getInstance().getDeviceType() == 16 || App.getInstance().getDeviceType() == 8)) {
                        if (App.mBattery < 10) {
                            showDialogTip(getResources().getString(R.string.Electric_quantity_judgment), getResources().getString(R.string.zdl), 2);
                        } else {
                            showDialogTip(getResources().getString(R.string.Donot_charge_offline), getResources().getString(R.string.zdl_continue), 2);
                        }
                    } else {
                        startOffline = true;
                        endOffline = false;
                        showProgress = true;
                        //   PenUtils.isMcuUpgrade = true;
                        penCommAgent.ReqOfflineDataTransfer(true);
                    }
                    break;
                case R.id.ll_stop_offline:
                case R.id.tv_pause_stop:
                    penCommAgent.ReqOfflineDataTransfer(false);
                    startOffline = false;
                    showProgress = false;
                    if (popup != null && popup.isShowing()) {
                        popup.dismiss();
                    }
                    break;
                case R.id.tv_pause_continue:
                    pauseOrContinue();
                    break;
                case R.id.ll_delete_offline:
                    deleteDataDialog();
                    break;
                case R.id.ll_offline_number:
                    penCommAgent.getPenOfflineDataList();
                    break;
                case R.id.ll_breakpoint_offline:
                    penCommAgent.getPenBreakPointOfflineDataList();
                    break;
                case R.id.iv_width_1:
                    gWidth = 1;
                    mPenView.setPenWidth(gWidth);
                    break;
                case R.id.iv_width_2:
                    gWidth = 2;
                    mPenView.setPenWidth(gWidth);
                    break;
                case R.id.iv_width_3:
                    gWidth = 3;
                    mPenView.setPenWidth(gWidth);
                    break;
                case R.id.iv_width_4:
                    gWidth = 4;
                    mPenView.setPenWidth(gWidth);
                    break;
                case R.id.iv_width_5:
                    gWidth = 5;
                    mPenView.setPenWidth(gWidth);
                    break;

                case R.id.iv_color_black:
                    gColor = 1;
                    SetPenColor(gColor);
                    break;
                case R.id.iv_color_red:
                    gColor = 2;
                    SetPenColor(gColor);
                    break;
                case R.id.iv_color_blue:
                    gColor = 3;
                    SetPenColor(gColor);
                    break;
                case R.id.iv_color_green:
                    gColor = 4;
                    SetPenColor(gColor);
                    break;
                case R.id.iv_color_yellow:
                    gColor = 5;
                    SetPenColor(gColor);
                    break;
                case R.id.iv_color_orange:
                    gColor = 6;
                    SetPenColor(gColor);
                    break;
                case R.id.iv_replay_start:
                    if (((PopupReplay) popup).isStart()) {
                        ((PopupReplay) popup).setStart(false);
                        bIsReplay = false;
                    } else {
                        ((PopupReplay) popup).setStart(true);
                        bIsReplay = true;
                        RunReplay();
                    }
                    break;
                case R.id.tv_replay_speed:
                    gSpeed = ((PopupReplay) popup).getSpeed();
                    break;
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveStatus(Events.ReceiveStatus receiveStatus) {
        BLELogUtil.i(TAG, "receiveStatus: " + receiveStatus.penStatus);
        if (receiveStatus.penStatus == null) {
            updateStatus();
        }
    }

    /**
     * ????????????
     */
    private void updateStatus() {
        if (swDrawStroke != null) {
            swDrawStroke.setChecked(App.isDrawStoke);
        }
        if (swSaveLog != null) {
            swSaveLog.setChecked(penCommAgent.getLogStatus());
        }
        if (swFilterAlgorithm != null) {
            swFilterAlgorithm.setChecked(penCommAgent.getIsSplitFiltration());
        }
    }


    private void showToast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    private void showDialogTip(String message, String zdl, int index) {
        AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(zdl,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                if (index == 2) {
                                    startOffline = true;
                                    endOffline = false;
                                    showProgress = true;
                                    PenUtils.isReqOfflineData = true;
                                    penCommAgent.ReqOfflineDataTransfer(true);
                                }
                            }
                        })
                .show();
    }

    /**
     * ??????????????????dialog
     */
    private void deleteDataDialog() {
        if (dialog == null) {
            dialog = new Dialog(activity, R.style.customDialog);
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_go_connect, null);
        TextView cancel = view.findViewById(R.id.cancel);
        TextView connect = view.findViewById(R.id.connect);
        TextView connectTip = view.findViewById(R.id.tv_connect_tip);
        connect.setText(getResources().getString(R.string.ok));
        connectTip.setText(getResources().getString(R.string.isDeleteData));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                penCommAgent.RemoveOfflineData();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }


    /**
     * ?????????????????????
     */
    private void pauseOrContinue() {
        if (popup instanceof PopupOfflineProgress) {
            if (((PopupOfflineProgress) popup).isPause()) {
                if (App.getInstance().getDeviceType() == 0) {
                    Toast.makeText(activity, "?????????", Toast.LENGTH_SHORT).show();
                } else {
                    penCommAgent.PauseOrContinueOfflineData(true);
                    LogUtils.i(TAG, "pauseOrContinue: continue");
                }
            } else {
                if (App.getInstance().getDeviceType() == 0) {
                    Toast.makeText(activity, "?????????", Toast.LENGTH_SHORT).show();
                } else {
                    penCommAgent.PauseOrContinueOfflineData(false);
                    LogUtils.i(TAG, "pauseOrContinue: pause");
                }
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param ColorIndex
     */
    public void SetPenColor(int ColorIndex) {
        switch (ColorIndex) {
            case 1:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.BLACK));
                break;
            case 2:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.RED));
                break;
            case 3:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.BLUE));
                break;
            case 4:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.GREEN));
                break;
            case 5:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.YELLOW));
                break;
            case 6:
                mPenView.setPenColor(ColorUtil.int2Hex(Color.rgb(255, 143, 33)));
                break;
        }
    }

    /**
     * ???????????????Dot?????????
     */
    public void testReadLogData() {
        mHandle.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String data = BLEFileUtil.readFile("/mnt/sdcard/tql/test.txt");
                    Gson gson = new Gson();
                    if (TextUtils.isEmpty(data)) {
                        return;
                    }
                    List<Dot> dotList = gson.fromJson(data, new TypeToken<List<Dot>>() {
                    }.getType());
                    int size = dotList.size();
                    for (int i = 0; i < size; i++) {
                        final Dot dot = dotList.get(i);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processDots(dot);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 5000);
    }

    private void initPermissionsTip(int index) {
        Dialog mServiceDialog = new Dialog(activity, R.style.customDialog);
        LayoutInflater mInflater = LayoutInflater.from(activity);
        View view = mInflater.inflate(R.layout.dialog_service_agreement, null);
        mServiceDialog.setContentView(view);
        mServiceDialog.setCancelable(false);
        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        TextView tv_msg = (TextView) view.findViewById(R.id.tv_msg);
        View viewLine = (View) view.findViewById(R.id.views);
        TextView tvConfirm = (TextView) view.findViewById(R.id.tv_confirm);
        TextView tv_confirm_agree = (TextView) view.findViewById(R.id.tv_confirm_agree);
        tv_title.setText(getResources().getString(R.string.permissions_tips1));
        tv_msg.setText(getResources().getString(R.string.permissions_tips_save));
        viewLine.setVisibility(View.GONE);
        tvConfirm.setVisibility(View.GONE);
        tvConfirm.setText("??????");
        tv_confirm_agree.setText("??????????????????");
        ImageView tv_cancel = (ImageView) view.findViewById(R.id.tv_cancel);
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceDialog.dismiss();
            }
        });
        tv_confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceDialog.dismiss();
                SPUtils.put(activity, com.sonix.oidbluetooth.Constants.FIRST_SAVE, true);
                requestPermission1(index);
            }
        });
        mServiceDialog.show();
    }

    /**
     * ?????????????????????
     */
    private void requestPermission1(int index) {
        this.index = index;
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (isRequesting)
                return;
            isRequesting = true;
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            return;
        } else {
            if (index == 1) {
                penCommAgent.setLogStatus(App.isSaveSDKLog);
            } else {
                FileUtils.openFilePath(activity);
//                clearPenView();
            }
        }
        Log.i(TAG, "requestPermission1");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequesting = false;
                    if (index == 1) {
                        penCommAgent.setLogStatus(App.isSaveSDKLog);
                    } else {
                        FileUtils.openFilePath(activity);
//                        clearPenView();
                    }
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            activity, R.style.Theme_AppCompat_Light_Dialog)
                            .setTitle(R.string.permissions_tips1).setMessage(R.string.permissions_tips_save)
                            .setPositiveButton(R.string.toSet, (dialog1, which) -> {
                                isRequesting = false;
                                dialog1.cancel();
                            }).setNegativeButton(R.string.cancel, (dialog2, which) -> {
                                isRequesting = false;
                                dialog2.dismiss();
                            }).create();
                    dialog.show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    /*
     * ??????html??????
     */
    public String getFromAssets(String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(
                    getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null)
                Result += line;
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


//    @Override
//    public void ReceiveDot(Events.ReceiveDot receiveDot) {
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Dot dot = receiveDot.dot;
////                if ((dot.OwnerID == 201 && dot.BookID == 63) || dot.BookID == 168 || dot.BookID == 0) {
////                    if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
////                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
////                        mPenView.setRotateView(true);
////                    }
////                } else {
////                    if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
////                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
////                        mPenView.setRotateView(true);
////                    }
////                }
//                processDots(dot);
//            }
//        });
//
//    }


}
