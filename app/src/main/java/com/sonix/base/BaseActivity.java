package com.sonix.base;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.sonix.app.App;
import com.sonix.oidbluetooth.R;
import com.sonix.util.LogUtils;
import com.sonix.util.statusbar.ImmersiveUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BaseActivity extends AppCompatActivity {

    protected final String TAG = this.getClass().getName();

    //权限
    public static final int PERMISSION_REQUEST1 = 10;
    protected static final int PERMISSION_REQUEST2 = 20;

    //蓝牙
    protected static final int REQUEST_BLUETOOTH_ENABLE = 30;
    protected static final int REQUEST_SEARCH_DEVICE = 40;

    protected Context mContext;

    private Toast mToast;
    private Dialog mBaseDialog;

    private Handler mBaseHandler = new Handler();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtils.i(TAG, "onNewIntent");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        mContext = this;
        initView();
        initData();

        //setStatusBar();

        //判断是否需要注册EventBus
        if (this.getClass().isAnnotationPresent(BindEventBus.class)) {
            EventBus.getDefault().register(this);
        }

        setKeyBoardHerlper();

        App.getInstance().activityCreate(this);
        LogUtils.i(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.i(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LogUtils.i(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.getInstance().activityResume(this);
        LogUtils.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.i(TAG, "onPause");
    }


    /**
     * 解决软键盘挡住输入框
     */
    protected void setKeyBoardHerlper() {

    }

    /**
     * 沉浸式状态栏
     */
    protected void setStatusBar() {
        ImmersiveUtils.setStatusBar(this, false, true);
        ImmersiveUtils.setStatusTextColor(true, this);
    }

    /**
     * 布局文件
     */
    protected abstract int getLayoutId();

    /**
     * 初始界面
     */
    protected abstract void initView();

    /**
     * 初始数据
     */
    protected abstract void initData();

    //Dialog
    private CopyOnWriteArrayList<DialogInterface> dialogs = new CopyOnWriteArrayList<>();

    public void dialogShow(Dialog dialog) {
        dialog.show();
        dialogs.add(dialog);
    }

    public void dialogCancel(DialogInterface dialog) {
        dialog.cancel();
        dialogs.remove(dialog);
    }

    public void dialogClear() {
        for (DialogInterface dialog : dialogs) {
            dialog.cancel();
            dialogs.remove(dialog);
        }
    }

    //设备名和设备地址
    private String deviceName, deviceAddress;
    private IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.STATE_ON == intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                unregisterReceiver(receiver);
                App.getInstance().deviceConnect(deviceName, deviceAddress);
                deviceName = null;
                deviceAddress = null;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    deviceConnect2();
                else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this)
                            .setTitle(R.string.permission_title).setMessage(R.string.permission_sdcard_message)
                            .setPositiveButton(R.string.permission_button, (dialog1, which) -> dialogCancel(dialog1)).setCancelable(false).create();
                    dialogShow(dialog);
                }
                break;
            case PERMISSION_REQUEST2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    deviceConnect3();
                else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this)
                            .setTitle(R.string.permission_title).setMessage(R.string.permission_location_message)
                            .setPositiveButton(R.string.permission_button, (dialog1, which) -> dialogCancel(dialog1)).setCancelable(false).create();
                    dialogShow(dialog);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i(TAG, "onActivityResult: ");
        switch (requestCode) {
            case REQUEST_BLUETOOTH_ENABLE:
                if (resultCode == RESULT_OK)
                    return;
                unregisterReceiver(receiver);
                AlertDialog dialog = new AlertDialog.Builder(
                        this)
                        .setTitle(R.string.permission_title).setMessage(R.string.permission_bluetooth_message)
                        .setPositiveButton(R.string.permission_button, (dialog1, which) -> dialogCancel(dialog1)).setCancelable(false).create();
                dialogShow(dialog);
                break;
            case REQUEST_SEARCH_DEVICE:
                boolean connected = App.getInstance().isConnected();
                Log.i(TAG, "isConnect:" + connected);
                if (data == null || resultCode == RESULT_CANCELED) {
                    if (TextUtils.isEmpty(App.getInstance().getDeviceAddress()))
                        return;
                    deviceConnect(
                            App.getInstance().getDeviceName(),
                            App.getInstance().getDeviceAddress());
                } else
                    deviceConnect(
                            data.getStringExtra("deviceName"),
                            data.getStringExtra("deviceAddress"));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void deviceConnect(String name, String address) {
        deviceName = name;
        deviceAddress = address;
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST1);
            return;
        }
        deviceConnect2();
    }

    private void deviceConnect2() {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST2);
            return;
        }
        deviceConnect3();
    }

    private void deviceConnect3() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            registerReceiver(receiver, filter);
            startActivityForResult(new Intent(BluetoothAdapter
                    .ACTION_REQUEST_ENABLE), REQUEST_BLUETOOTH_ENABLE);
        } else {
            App.getInstance().deviceConnect(deviceName, deviceAddress);
            deviceName = null;
            deviceAddress = null;
        }
    }


    /**
     * 获取状态栏高度
     */
    public int getStatusBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }


    /**
     * 全局的toast
     *
     * @param msg
     */
    public void showToast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        }
        mToast.setText(msg);
        mToast.show();
    }

    public void goToConnect() {
        //Intent intent = new Intent(mContext, SearchActivity.class);
        //startActivityForResult(intent, REQUEST_SEARCH_DEVICE);
    }

    /**
     * 显示加载dialog
     */
    public void showLoadingDialog(Activity activity, String msg) {

        mBaseDialog = new Dialog(activity, R.style.customDialog);

        if (mBaseDialog.isShowing()) {
            mBaseDialog.dismiss();
        }

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null);
        TextView tv_loading_text = view.findViewById(R.id.tv_loading_text);
        tv_loading_text.setText(msg);

        mBaseDialog.setContentView(view);
        mBaseDialog.setCancelable(false);
        mBaseDialog.setCanceledOnTouchOutside(false);

        mBaseDialog.show();

        mBaseHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBaseDialog != null && mBaseDialog.isShowing()) {
                    mBaseDialog.dismiss();
                }
            }
        }, 5000);

    }

    public Dialog getDialog() {
        return mBaseDialog;
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.i(TAG, "onStop");
//        if (mDialog != null && mDialog.isShowing()) {
//            mDialog.dismiss();
//        }
    }

    /**
     * 关闭加载dialog
     */
    public void hideLoadingDialog() {
        if (mBaseDialog != null) {
            if (mBaseDialog.isShowing()) {
                mBaseDialog.dismiss();
            }
            mBaseDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.getClass().isAnnotationPresent(BindEventBus.class)) {
            EventBus.getDefault().unregister(this);
        }
        App.getInstance().activityDestroy(this);
        LogUtils.i(TAG, "onDestroy");
        mBaseHandler.removeCallbacksAndMessages(null);
        mBaseHandler = null;
    }

}
