package com.sonix.oidbluetooth;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.base.BindEventBus;
import com.sonix.oidbluetooth.view.SelectorView;
import com.sonix.util.Events;
import com.sonix.util.SPUtils;
import com.tqltech.tqlpencomm.Constants;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.PenStatus;
import com.tqltech.tqlpencomm.pen.PenUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 笔参数
 */
@BindEventBus
public class ParameterActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final int MSG_ONE = 1;  //更新时间
    private RelativeLayout rl_left;
    private TextView tv_title;

    private LinearLayout ll_name;
    private LinearLayout ll_rtc_time;
    private LinearLayout ll_auto_close_time;
    private LinearLayout ll_sensitivity;

    private TextView tv_name;
    private TextView tv_address;
    private TextView tv_bt_version;
    private TextView tv_mcu_version;
    private TextView tv_pen_boot_version;
    private TextView tv_custom_id;
    private TextView tv_battery;
    private TextView tv_used_memory;
    private TextView tv_rtc_time;

    private TextView tv_auto_close_time;

    private TextView tv_sensitivity;
    private TextView tv_force_range;
    private TextView tv_element_code;

    private Switch sw_beep;
    private Switch sw_pressure_switch_on;
    private Switch sw_led;


    private PenCommAgent penCommAgent;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private long tmpTimer;
    private String mName;
    private LinearLayout ll_flash;
    private TextView tv_flash;
    private String storageSize;
    private long lastTime = 0L;
    private boolean isRequesting;
    private boolean isChecked;
    private Dialog mServiceDialog;
    private LinearLayout ll_sw_beep_set;
    private PenStatus penStatus;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_parameter;
    }

    @Override
    protected void initView() {
        rl_left = findViewById(R.id.rl_left);
        tv_title = findViewById(R.id.tv_title);
        rl_left.setOnClickListener(this);

        ll_name = findViewById(R.id.ll_name);
        ll_rtc_time = findViewById(R.id.ll_rtc_time);
        ll_sw_beep_set = findViewById(R.id.ll_sw_beep_set);
        ll_auto_close_time = findViewById(R.id.ll_auto_close_time);
        ll_sensitivity = findViewById(R.id.ll_sensitivity);
        ll_flash = findViewById(R.id.ll_flash);
        tv_pen_boot_version = findViewById(R.id.tv_pen_boot_version);
        ll_name.setOnClickListener(this);
        ll_rtc_time.setOnClickListener(this);
        ll_auto_close_time.setOnClickListener(this);
        ll_sensitivity.setOnClickListener(this);
        ll_sw_beep_set.setOnClickListener(this);
        Log.i(TAG, "415笔:" + App.is415Pen);
        if (!App.is415Pen) {
            ll_sw_beep_set.setVisibility(View.GONE);
        }


        tv_name = findViewById(R.id.tv_name);
        tv_address = findViewById(R.id.tv_address);
        tv_bt_version = findViewById(R.id.tv_bt_version);
        tv_mcu_version = findViewById(R.id.tv_mcu_version);

        tv_custom_id = findViewById(R.id.tv_custom_id);
        tv_battery = findViewById(R.id.tv_battery);
        tv_used_memory = findViewById(R.id.tv_used_memory);
        tv_flash = findViewById(R.id.tv_flash);
        tv_rtc_time = findViewById(R.id.tv_rtc_time);

        tv_auto_close_time = findViewById(R.id.tv_auto_close_time);

        tv_sensitivity = findViewById(R.id.tv_sensitivity);
        tv_force_range = findViewById(R.id.tv_force_range);
        tv_element_code = findViewById(R.id.tv_element_code);

        sw_beep = findViewById(R.id.sw_beep);
        sw_pressure_switch_on = findViewById(R.id.sw_pressure_switch_on);
        sw_led = findViewById(R.id.sw_led);


        sw_beep.setOnCheckedChangeListener(this);
        sw_pressure_switch_on.setOnCheckedChangeListener(this);
        sw_led.setOnCheckedChangeListener(this);


        sw_beep.setChecked(false);
        sw_pressure_switch_on.setChecked(false);
        sw_led.setChecked(false);//初始化
    }

    @Override
    protected void initData() {
        penCommAgent = PenCommAgent.GetInstance(getApplication());
        penStatus = penCommAgent.getPenStatus();

//        if (penStatus.mPenSensitivity == -1 ) {
//            showLoadingDialog(this, getResources().getString(R.string.loading));
//            getDialog().setCancelable(true);//允许关闭
//            getDialog().setCanceledOnTouchOutside(true);
//        }

        tv_title.setText(getString(R.string.bcs));

        getPenFlash(App.mFlash);

//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
////                hideLoadingDialog();
//
//            }
//        }, 5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//               penCommAgent.getPenAllStatus();
//            }
//        }).start();
        //  penCommAgent.getPenMcuFirmVersion();
        penCommAgent.getPenAllStatus();
        //只有130笔才有内容容量
        if (PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T41A
                || PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T31A
                || PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T41
                || PenUtils.mPenType == Constants.PEN_130_T31) {
            ll_flash.setVisibility(View.VISIBLE);
        } else {
            ll_flash.setVisibility(View.GONE);
        }
        handler.sendEmptyMessage(MSG_ONE);
/*        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //handler.removeMessages(MSG_ONE);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeMessages(MSG_ONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveBuzzes(Events.ReceiveBuzzes receiveBuzzes) {
        Log.i(TAG, "receiveBuzzes: " + receiveBuzzes);
        if (receiveBuzzes.buzzerBuzzes) {
            showToast("设置成功");
        } else {
            showToast("设置失败");
        }
    }

    private int receiveStatusNumber;//进入这个方法的次数

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveStatus(Events.ReceiveStatus receiveStatus) {
        Log.i(TAG, "receiveStatus: " + receiveStatus.penStatus);
        if (receiveStatus.penStatus != null) {

        } else {

            receiveStatusNumber++;
            handler.sendEmptyMessageDelayed(0, 200);//200毫秒 刷新一次界面, 200毫秒内进来多次  只刷一次就把数据全赋值
/*          long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime > 2000) {
                lastTime = currentTime;*/

//     }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveElementCode(Events.ReceiveElementCode elementCode) {
        String info = "SA:" + elementCode.code.SA + "/SB:" + elementCode.code.SB + "/SC:" + elementCode.code.SC + "/SD:" + elementCode.code.SD + "/index:" + elementCode.code.index;
        tv_element_code.setText(info);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveSetNameResponse(Events.ReceiveSetNameResponse status) {
        Log.i(TAG, "receiveSetNameResponse: " + mName);
        if (status.isSuccess) {
            App.mPenName = mName;
            App.mDeviceName = mName;
        } else {
            showToast("笔名设置失败");
        }
        updateStatus();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
                finish();
                break;
            case R.id.ll_name:
                showSetPenNameDialog();

                break;
            case R.id.ll_rtc_time:
                showSetPenRtcDialog();
                break;
            case R.id.ll_sw_beep_set:
                showSetPenBuzzerBuzzesDialog();
                break;
            case R.id.ll_auto_close_time:
                showSetPenAutoCloseTimeDialog();
                break;
            case R.id.ll_sensitivity:
                showSetPenSensitivityDialog();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Log.e(TAG, "compoundButton.getId()-------" + compoundButton.getId());
        switch (compoundButton.getId()) {

            case R.id.sw_pressure_switch_on:
                Log.e(TAG, "压感开机-------" + isChecked);
                App.tmp_mPowerOnMode = isChecked;
                penCommAgent.setPenAutoPowerOnMode(isChecked);
                break;
            case R.id.sw_led:
                Log.e(TAG, "按键改变LED颜色-------" + isChecked);
                App.tmp_mEnableLED = isChecked;
                penCommAgent.setPenEnableLED(isChecked);
                break;
            case R.id.sw_save_log:
                Log.e(TAG, "保存日志功能-------" + isChecked);
                this.isChecked = isChecked;
                boolean mPermissionTip = (Boolean) SPUtils.get(ParameterActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, false);
                Log.i(TAG, "onResume mPermissionTip::" + mPermissionTip);
                if (mPermissionTip) {
                    requestPermission1(isChecked);
                } else {
                    initPermissionsTip(isChecked);
                }
                break;
            case R.id.sw_draw_stroke:
                Log.e(TAG, "笔锋绘制功能-------" + isChecked);
                App.isDrawStoke = isChecked;
                break;
            case R.id.sw_filter_algorithm:
                Log.e(TAG, "过滤算法-------" + isChecked);
                penCommAgent.setIsSplitFiltration(isChecked);
                break;
            case R.id.sw_beep:
                Log.e(TAG, "蜂鸣器-------" + isChecked);
                App.tmp_mBeep = isChecked;
                penCommAgent.setPenBeepMode(isChecked);
                break;
        }
    }

    private void initPermissionsTip(boolean isChecked) {
        mServiceDialog = new Dialog(this, R.style.customDialog);
        LayoutInflater mInflater = LayoutInflater.from(this);
        View view = mInflater.inflate(R.layout.dialog_service_agreement, null);
        mServiceDialog.setContentView(view);
        mServiceDialog.setCancelable(false);
        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        TextView tv_msg = (TextView) view.findViewById(R.id.tv_msg);
        View viewLine = (View) view.findViewById(R.id.views);
        TextView tvConfirm = (TextView) view.findViewById(R.id.tv_confirm);
        TextView tv_confirm_agree = (TextView) view.findViewById(R.id.tv_confirm_agree);
        tv_msg.setText(getResources().getString(R.string.permissions_tips1));
        tv_title.setText(getResources().getString(R.string.permissions_tips1));
        tv_msg.setText(getResources().getString(R.string.permissions_tips3));
        viewLine.setVisibility(View.GONE);
        tvConfirm.setVisibility(View.GONE);
        tvConfirm.setText("取消");
        tv_confirm_agree.setText("好的，去开启");
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
                SPUtils.put(ParameterActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, true);
                requestPermission1(isChecked);
            }
        });
        mServiceDialog.show();
    }

    /**
     * 请求写文件权限
     *
     * @param isChecked
     */
    private void requestPermission1(boolean isChecked) {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (isRequesting)
                return;
            isRequesting = true;
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST1);
            return;
        }
        Log.i(TAG, "requestPermission1");
        penCommAgent.setLogStatus(isChecked);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequesting = false;
                    penCommAgent.setLogStatus(isChecked);
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this, R.style.Theme_AppCompat_Light_Dialog)
                            .setTitle(R.string.permissions_tips1).setMessage(R.string.permissions_tips_save)
                            .setPositiveButton(R.string.toSet, (dialog1, which) -> {
                                isRequesting = false;
                                dialogCancel(dialog1);
                                Uri packageURI = Uri.parse("package:" + "com.sonix.oidbluetooth");
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                                startActivity(intent);
                            }).setNegativeButton(R.string.cancel, (dialog2, which) -> {
                                isRequesting = false;
                                dialogCancel(dialog2);
                            }).create();
                    dialogShow(dialog);
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }


    /**
     * 状态显示
     */
    private void updateStatus() {

//        if( !TextUtils.isEmpty(penStatus.mPenName) ){
//            tv_name.setText(penStatus.mPenName);
//        }

        tv_name.setText(App.mPenName);

        if (!TextUtils.isEmpty(penStatus.mPenMac)) {
            tv_address.setText(penStatus.mPenMac);
        } else {
            tv_address.setText("");
        }

        if (!TextUtils.isEmpty(penStatus.mBtFirmware)) {
            tv_bt_version.setText("BT-" + penStatus.mBtFirmware);
        } else {
            tv_bt_version.setText("");
        }

        if (!TextUtils.isEmpty(penStatus.mPenMcuVersion)) {

            if(!TextUtils.isEmpty(penStatus.mTail)){
                tv_mcu_version.setText("MCU-" + penStatus.mPenMcuVersion + "."+penStatus.mTail);
            }else{
                tv_mcu_version.setText("MCU-" + penStatus.mPenMcuVersion);
            }


        } else {
            tv_mcu_version.setText("");
        }

        if (!TextUtils.isEmpty(penStatus.mPenBoot)) {
            findViewById(R.id.layout_pen_boot_version).setVisibility(View.VISIBLE);
            tv_pen_boot_version.setText(penStatus.mPenBoot);
        } else {
            tv_pen_boot_version.setText("");
            findViewById(R.id.layout_pen_boot_version).setVisibility(View.GONE);
        }

//        tv_name.setText(App.mPenName);
//        tv_address.setText(App.mBTMac);
//        tv_bt_version.setText(App.mFirmWare);
//        tv_mcu_version.setText(App.mMCUFirmWare);

        if (PenUtils.penDotType != -1 ) {
            tv_custom_id.setText(PenUtils.penDotType + "");
        } else {
            tv_custom_id.setText("");
        }
//        tv_custom_id.setText(App.mCustomerID);

        if (penStatus.mPenBattery != -1) {
            tv_battery.setText(penStatus.mPenBattery + "%");
        } else {
            tv_battery.setText("");
        }

        if (penStatus.mPenMemory != -1) {
            tv_used_memory.setText(penStatus.mPenMemory + "%");
        } else {
            tv_used_memory.setText("");
        }

//        tv_battery.setText(App.mBattery + "%");
//        tv_used_memory.setText(App.mUsedMem + "%");

        if (tmpTimer != App.mTimer) {
            tv_rtc_time.setText(rtcTimeToDate(App.mTimer));
            tmpTimer = App.mTimer;
        }


        if (penStatus.mPenAutoOffTime != -1) {
            tv_auto_close_time.setText(penStatus.mPenAutoOffTime + ""); //自动关机时间
        } else {
            tv_auto_close_time.setText("");
        }


        if (penStatus.mPenSensitivity != -1) {
//            hideLoadingDialog();//这里就可以做个屏蔽了,避免没有获得压力值一直转圈
            tv_sensitivity.setText(penStatus.mPenSensitivity + ""); //灵敏度
        } else {
            tv_sensitivity.setText("");
        }

        if (App.mTwentyPressure != 0 && App.mThreeHundredPressure != 0) {
//            hideLoadingDialog();
            tv_force_range.setText(App.mTwentyPressure + "-" + App.mThreeHundredPressure);
        } else {
            tv_force_range.setText("");
        }

//        tv_auto_close_time.setText(Integer.toString(App.mPowerOffTime));
//        tv_sensitivity.setText(Integer.toString(App.mPenSens));
//        tv_force_range.setText(App.mTwentyPressure + "-" + App.mThreeHundredPressure);

        sw_beep.setChecked(App.mBeep);
        sw_pressure_switch_on.setChecked(App.mPowerOnMode);
        sw_led.setChecked(App.tmp_mEnableLED);

    }

    private void getPenFlash(int i) {
        Log.i(TAG, "内存容量 " + i);
        storageSize = String.valueOf(i);
        if (i == 1) {
            tv_flash.setText("128M");
        } else if (i == 2) {//8M
            tv_flash.setText("8M");
        }
    }

    /**
     * RTC时间转换为日期
     *
     * @param rtcTime rtc时间，起始时间2010年，单位秒
     * @return
     */
    private String rtcTimeToDate(long rtcTime) {
        long time = (1262275200L + rtcTime) * 1000; //毫秒
        Date date = new Date(time);
        String value = format.format(date);
        return value;
    }

    /**
     * 设置笔名dialog
     */
    private void showSetPenNameDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_name, null);
        EditText et_pen_name = view.findViewById(R.id.et_pen_name);
        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        et_pen_name.setText(tv_name.getText().toString());
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mName = et_pen_name.getText().toString();
                if (App.is415Pen && mName.length() > 12) {
                    showToast("笔名长度不能超过12");
                    return;
                }
                penCommAgent.setPenName(mName);
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        //window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }

    /**
     * 设置笔RTC时间dialog
     */
    private void showSetPenRtcDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
//        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_rtc, null);
//        LinearLayout ll_rtc_date = view.findViewById(R.id.ll_rtc_date);
//        LinearLayout ll_rtc_time = view.findViewById(R.id.ll_rtc_time);
//        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
//        TextView tv_ok = view.findViewById(R.id.tv_ok);
//
//        ll_rtc_date.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showSetPenRtcDateDialog();
//            }
//        });
//        ll_rtc_time.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showSetPenRtcTimeDialog();
//            }
//        });
//        tv_cancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//        tv_ok.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_adjust_rtc, null);
        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                penCommAgent.ReqAdjustRTC();
                penCommAgent.getPenRtc();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        //window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }

    /**
     * 设置日期dialog
     */
    private void showSetPenRtcDateDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_rtc_date, null);

        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        //window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }

    /**
     * 设置日期dialog
     */
    private void showSetPenBuzzerBuzzesDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_buzzer_buzzes, null);

        TextView et_pen_time_gap = view.findViewById(R.id.et_pen_time_gap);
        TextView et_pen_buzzes = view.findViewById(R.id.et_pen_buzzes);
        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeGap = et_pen_time_gap.getText().toString().trim();
                String penBuzzes = et_pen_buzzes.getText().toString().trim();
                penCommAgent.setPenBuzzerBuzzes(timeGap, penBuzzes);
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }


    /**
     * 设置时间dialog
     */
    private void showSetPenRtcTimeDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_rtc_time, null);
        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });


        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        //window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }

    /**
     * 设置笔自动关机时间dialog
     */
    private void showSetPenAutoCloseTimeDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_close_time, null);

        EditText et_pen_close_time = view.findViewById(R.id.et_pen_close_time);
        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        et_pen_close_time.setText(tv_auto_close_time.getText().toString());
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String time = et_pen_close_time.getText().toString();
                int timeInt = Integer.parseInt(time);
                if (timeInt < 0 || timeInt > 120) {
                    showToast("请设置范围0-120的整数");
                    return;
                }
                tv_auto_close_time.setText(time);

                penCommAgent.setPenAutoShutDownTime((short) timeInt);
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        //window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }

    /**
     * 设置笔灵敏度dialog
     */
    private void showSetPenSensitivityDialog() {
        Dialog dialog = new Dialog(this, R.style.customDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_sensitivity, null);

        SelectorView iv_sensitivity_0 = view.findViewById(R.id.iv_sensitivity_0);
        SelectorView iv_sensitivity_1 = view.findViewById(R.id.iv_sensitivity_1);
        SelectorView iv_sensitivity_2 = view.findViewById(R.id.iv_sensitivity_2);
        SelectorView iv_sensitivity_3 = view.findViewById(R.id.iv_sensitivity_3);
        SelectorView iv_sensitivity_4 = view.findViewById(R.id.iv_sensitivity_4);
        iv_sensitivity_0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iv_sensitivity_0.toggle(true);
                iv_sensitivity_1.toggle(false);
                iv_sensitivity_2.toggle(false);
                iv_sensitivity_3.toggle(false);
                iv_sensitivity_4.toggle(false);
            }
        });

        iv_sensitivity_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iv_sensitivity_0.toggle(false);
                iv_sensitivity_1.toggle(true);
                iv_sensitivity_2.toggle(false);
                iv_sensitivity_3.toggle(false);
                iv_sensitivity_4.toggle(false);
            }
        });

        iv_sensitivity_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iv_sensitivity_0.toggle(false);
                iv_sensitivity_1.toggle(false);
                iv_sensitivity_2.toggle(true);
                iv_sensitivity_3.toggle(false);
                iv_sensitivity_4.toggle(false);
            }
        });

        iv_sensitivity_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iv_sensitivity_0.toggle(false);
                iv_sensitivity_1.toggle(false);
                iv_sensitivity_2.toggle(false);
                iv_sensitivity_3.toggle(true);
                iv_sensitivity_4.toggle(false);
            }
        });

        iv_sensitivity_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iv_sensitivity_0.toggle(false);
                iv_sensitivity_1.toggle(false);
                iv_sensitivity_2.toggle(false);
                iv_sensitivity_3.toggle(false);
                iv_sensitivity_4.toggle(true);
            }
        });

        if (App.mPenSens == 0) {
            iv_sensitivity_0.toggle(true);
            iv_sensitivity_1.toggle(false);
            iv_sensitivity_2.toggle(false);
            iv_sensitivity_3.toggle(false);
            iv_sensitivity_4.toggle(false);
        } else if (App.mPenSens == 1) {
            iv_sensitivity_0.toggle(false);
            iv_sensitivity_1.toggle(true);
            iv_sensitivity_2.toggle(false);
            iv_sensitivity_3.toggle(false);
            iv_sensitivity_4.toggle(false);
        } else if (App.mPenSens == 2) {
            iv_sensitivity_0.toggle(false);
            iv_sensitivity_1.toggle(false);
            iv_sensitivity_2.toggle(true);
            iv_sensitivity_3.toggle(false);
            iv_sensitivity_4.toggle(false);
        } else if (App.mPenSens == 3) {
            iv_sensitivity_0.toggle(false);
            iv_sensitivity_1.toggle(false);
            iv_sensitivity_2.toggle(false);
            iv_sensitivity_3.toggle(true);
            iv_sensitivity_4.toggle(false);
        } else if (App.mPenSens == 4) {
            iv_sensitivity_0.toggle(false);
            iv_sensitivity_1.toggle(false);
            iv_sensitivity_2.toggle(false);
            iv_sensitivity_3.toggle(false);
            iv_sensitivity_4.toggle(true);
        }

        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_ok = view.findViewById(R.id.tv_ok);

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (iv_sensitivity_0.isChecked()) {
                    tv_sensitivity.setText("0");
                    penCommAgent.setPenSensitivity((short) 0);
                    App.mPenSens = 0;
                } else if (iv_sensitivity_1.isChecked()) {
                    tv_sensitivity.setText("1");
                    penCommAgent.setPenSensitivity((short) 1);
                    App.mPenSens = 1;
                } else if (iv_sensitivity_2.isChecked()) {
                    tv_sensitivity.setText("2");
                    penCommAgent.setPenSensitivity((short) 2);
                    App.mPenSens = 2;
                } else if (iv_sensitivity_3.isChecked()) {
                    tv_sensitivity.setText("3");
                    penCommAgent.setPenSensitivity((short) 3);
                    App.mPenSens = 3;
                } else if (iv_sensitivity_4.isChecked()) {
                    tv_sensitivity.setText("4");
                    penCommAgent.setPenSensitivity((short) 4);
                    App.mPenSens = 4;
                }
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        //window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        dialog.show();
    }

    Date date;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handler.removeMessages(0);
                    if (receiveStatusNumber != 0) {
                        updateStatus();
                        receiveStatusNumber = 0;
                    }
                    break;
                case MSG_ONE:
                    String time = tv_rtc_time.getText().toString();
                    if (!TextUtils.isEmpty(time) && !time.equals("0")) {
                        try {
                            date = format.parse(time);
                            //  tv_rtc_time.setText(format.format(new Date(date.getTime() + 1000)));
//                            Log.i(TAG, "date.getTime():" + date.getTime());
                            tv_rtc_time.setText(format.format(new Date(date.getTime() + 1000)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    handler.sendEmptyMessageDelayed(MSG_ONE, 1000);
                    break;
                default:
                    break;
            }

        }
    };
}
