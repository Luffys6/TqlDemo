package com.sonix.oidbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.base.BindEventBus;
import com.sonix.oidbluetooth.fragment.HandCodeFragment;
import com.sonix.oidbluetooth.fragment.PointReadCodeFragment;
import com.sonix.oidbluetooth.view.PopupOfflineProgress;
import com.sonix.util.Events;
import com.sonix.util.FileUtils;
import com.sonix.util.LogUtils;

import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.util.BLELogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * 主界面
 */
@BindEventBus
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TagTwo = 1;
    private static final int TagOne = 2;
    private PenCommAgent penCommAgent;

    private SharedPreferences sp;

    private Dialog dialog;
    private TextView itv_hand_code;
    private TextView itv_point_read_code;
    private TextView tv_app_name;
    private TextView tv_pen_status;
    private HandCodeFragment handCodeFragment;
    private PointReadCodeFragment readCodeFragment;
    private Fragment currentFragment;
    private String pointCode = "";
    private long index = 0;

    private Handler mHandler = new Handler();


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        if (sp == null) {
            sp = getSharedPreferences("app", MODE_PRIVATE);
        }

        tv_app_name = findViewById(R.id.tv_app_name);
        tv_pen_status = findViewById(R.id.tv_pen_status);

        itv_hand_code = findViewById(R.id.tv_hand_code);
        itv_point_read_code = findViewById(R.id.tv_point_read_tv);

        tv_pen_status.setOnClickListener(this);
        itv_hand_code.setOnClickListener(this);
        itv_point_read_code.setOnClickListener(this);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveElementCode(Events.ReceiveElementCode elementCode) {//  public void onReceiveElementCode(ElementCode elementCode) {
        String info = "SA = " + elementCode.code.SA + ",SB = " + elementCode.code.SB + ",SC = " + elementCode.code.SC + ",SD = " + elementCode.code.SD + ",index = " + elementCode.code.index;
        Log.i(TAG, "receiveElementCode :" + info);
        pointCode = info;
        index = elementCode.index;
        if (!App.isNoGoToMain) {
            switchFragment(readCodeFragment, TagTwo);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void receiveDot(Events.ReceiveDot receiveDot) {
        Dot dot = receiveDot.dot;
        if (receiveDot.b && !App.isNoGoToMain) {
            switchFragment(handCodeFragment, TagOne);
        }
//        }
    }

    @Override
    protected void initData() {
        LogUtils.i(TAG, "initData: ");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (readCodeFragment == null) {
            readCodeFragment = new PointReadCodeFragment();
            ft.add(R.id.id_content, readCodeFragment, "TAG" + readCodeFragment.getClass().getName());
        }
        if (handCodeFragment == null) {
            handCodeFragment = new HandCodeFragment();
            ft.add(R.id.id_content, handCodeFragment, "TAG" + handCodeFragment.getClass().getName());
        }
        ft.commit();
        if (penCommAgent == null) {
            penCommAgent = PenCommAgent.GetInstance(getApplication());
        }



//        App.mDeviceAddress = sp.getString("mDeviceAddress", "");
//        if(!TextUtils.isEmpty(App.mDeviceAddress)){
//
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    //延迟一秒去连接笔
//                    App.mDeviceName = sp.getString("mDeviceName", "");
//                    penCommAgent.connectDevices(App.mDeviceAddress);
//                }
//            },1000);
//        }


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//不需要申请权限

        penCommAgent.setElementCode(new PenCommAgent.ElementCodeInterface() {
            @Override
            public void startElementCode(String code, long index) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchFragment(readCodeFragment, TagTwo);
                        Log.i(TAG, "发送离线点读码 :" + code);
                        EventBus.getDefault().post(new Events.ReadElementCodeDot(code, index));
                    }
                });
            }
        });
        penCommAgent.setHandCode(new PenCommAgent.HandCodeInterface() {
            @Override
            public void startHandCode() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //发送离线手写码
                        switchFragment(handCodeFragment, TagOne);
                    }
                });
            }
        });
        currentFragment = readCodeFragment;
        switchFragment(handCodeFragment, TagOne);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, " 主界面   onResume");
        if (App.getInstance().isDeviceConnected()) {
            tv_pen_status.setText(getString(R.string.connected, App.getInstance().getDeviceName()));
        } else {
            tv_pen_status.setText(getString(R.string.not_connect_pen));
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (handCodeFragment.isResumed()) {
            handCodeFragment.dismissDialog();
        }
        return super.onTouchEvent(event);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_pen_status:
                if (!App.getInstance().isDeviceConnected()) {
                    Intent intent = new Intent(mContext, SearchActivity.class);
                    startActivity(intent);
                } else {
                    showPenStatusDialog();
                }
                break;
            case R.id.tv_hand_code:
                switchFragment(handCodeFragment, TagOne);
                break;
            case R.id.tv_point_read_tv:
                switchFragment(readCodeFragment, TagTwo);
                break;
            default:
                break;
        }
    }

    /**
     * 切换fragment
     *
     * @param f
     * @param tagPage
     */

    private void switchFragment(Fragment f, int tagPage) {
        if (f == readCodeFragment) {
            if (!TextUtils.isEmpty(pointCode)) {
                Bundle bundle = new Bundle();
                bundle.putString("POINTCODE", pointCode);
                bundle.putLong("INDEX", index);
                readCodeFragment.setArguments(bundle);
            }

//            itv_point_read_code.setTextColor(getResources().getColor(R.color.statusbar_color));
            itv_point_read_code.setBackgroundResource(R.drawable.main_tv_bg);

//            itv_hand_code.setTextColor(getResources().getColor(R.color.result_view));
            itv_hand_code.setBackground(null);

            handCodeFragment.dismissPenView(false);
        } else {
//            itv_hand_code.setTextColor(getResources().getColor(R.color.statusbar_color));
            itv_hand_code.setBackgroundResource(R.drawable.main_tv_bg);

//            itv_point_read_code.setTextColor(getResources().getColor(R.color.result_view));
            itv_point_read_code.setBackground(null);

            handCodeFragment.dismissPenView(true);
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        if (!f.isAdded() && null == getSupportFragmentManager().findFragmentByTag("TAG" + tagPage)) {
//            if (currentFragment != null) {
//                ft.hide(currentFragment).add(R.id.id_content, f, "TAG" + tagPage);
//            } else {
//                ft.add(R.id.id_content, f, "TAG" + tagPage);
//            }
//        } else { //已经加载进容器里去了....
        if (currentFragment != f) {
            ft.hide(currentFragment).show(f);
        } else {
            ft.show(f);
        }
//        }
        currentFragment = f;

        ft.commitAllowingStateLoss();
//        if (!isFinishing()) {
//            ft.commitAllowingStateLoss();
//            getSupportFragmentManager().executePendingTransactions();
//        }
    }

    /**
     * 笔状态dialog
     */
    private void showPenStatusDialog() {
        if (dialog == null) {
            dialog = new Dialog(this, R.style.customDialog);
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pen_status, null);
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
                Intent intent = new Intent(mContext, SearchActivity.class);
                startActivity(intent);
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceDisconnected events) {
        if (!App.getInstance().isDeviceConnected()) {
            tv_pen_status.setText(getString(R.string.not_connect_pen));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceConnected events) {
        Log.e(TAG, "receivePenStatus : 连接成功");

        if (penCommAgent == null) {
            penCommAgent = PenCommAgent.GetInstance(getApplication());
        }
        Log.i(TAG, " 主界面   onResume  调用笔参数");

//        tv_pen_status.setText(getString(R.string.connected, App.getInstance().getDeviceName()));

        penCommAgent.getPenDotType();
//        penCommAgent.ReadPenTestMcuVersion();
        penCommAgent.getPenBeepMode();
        penCommAgent.getPenAutoPowerOnMode();
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
                        penCommAgent = PenCommAgent.GetInstance(getApplication());
                    }
                    LogUtils.i(TAG, "onActivityResult: path=" + uri + ",path2=" + uri.getPath().toString());
                    if (!uri.getPath().endsWith(".txt")) {
                        showToast("请选择正确的日志");
                        return;
                    }
                    if (uri != null) {
                        final String path = FileUtils.getRealPathFromURI(this, uri);
//                        if(handCodeFragment!=null ){
//                            handCodeFragment.clearPenView();
//                        }
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //拦截弹窗外部点击事件
        if (handCodeFragment.reqPopWindow() != null && handCodeFragment.reqPopWindow().isShowing() && handCodeFragment.reqPopWindow() instanceof PopupOfflineProgress) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.i(TAG, "onPause: ");

    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.i(TAG, "onStop: ");
        if (penCommAgent != null) {
            penCommAgent = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.i(TAG, "onDestroy: ");
        if (App.getInstance().isDeviceConnected()) {
            App.getInstance().deviceDisConnect();
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }


    private long mExitTime;

    // 按两次退出程序
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                showToast("再按一次退出程序");
                mExitTime = System.currentTimeMillis();
            } else {
                moveTaskToBack(false);
//                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



}



