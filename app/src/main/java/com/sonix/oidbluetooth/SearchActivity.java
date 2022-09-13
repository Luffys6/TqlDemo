package com.sonix.oidbluetooth;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.base.BindEventBus;
import com.sonix.oidbluetooth.adapter.SearchAdapter;
import com.sonix.util.Events;
import com.sonix.util.SPUtils;
import com.tqltech.tqlpencomm.BLEException;
import com.tqltech.tqlpencomm.BLEScanner;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.PenStatus;
import com.tqltech.tqlpencomm.pen.PenUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 搜索蓝牙笔
 */
@BindEventBus
public class SearchActivity extends BaseActivity implements View.OnClickListener {
    private static final int PERMISSION_REQUEST1 = 100;
    private static final int PERMISSION_REQUEST2 = 200;
    private static final int PERMISSION_REQUEST3 = 400;
    private static final int REQUEST_BLUETOOTH_ENABLE = 300;

    public TextView tv_title;
    public RelativeLayout rl_left;
    public RelativeLayout rl_right;
    public ImageView iv_left;
    public ImageView iv_right;

    public SmartRefreshLayout smart_refresh_layout;
    public RecyclerView recyclerView;

    public LinearLayout ll_not_found;
    public Button btn_retry;

    private PenCommAgent penCommAgent;
    private boolean isScanning;
    private boolean isRequesting;

    private long lastClickTime = 0L;
    private SearchAdapter searchAdapter;

    private IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    requestPermission1();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopScan();
                    break;
            }
        }
    };
    private Dialog mServiceDialog;
    private boolean mPermissionTip;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_search;
    }

    @Override
    protected void initView() {
        tv_title = findViewById(R.id.tv_title);
        rl_left = findViewById(R.id.rl_left);
        rl_right = findViewById(R.id.rl_right);
        iv_left = findViewById(R.id.iv_left);
        iv_right = findViewById(R.id.iv_right);
        smart_refresh_layout = findViewById(R.id.smart_refresh_layout);
        recyclerView = findViewById(R.id.rv_bluetooth);

        ll_not_found = findViewById(R.id.ll_not_found);
        btn_retry = findViewById(R.id.btn_retry);

        rl_left.setOnClickListener(this);
        iv_left.setOnClickListener(this);
        tv_title.setText(getResources().getString(R.string.search_title));
        iv_right.setVisibility(View.GONE);

        searchAdapter = new SearchAdapter(this);
        searchAdapter.setOnItemClickListener(onItemClickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setAdapter(searchAdapter);
    }


    @Override
    protected void initData() {
        penCommAgent = PenCommAgent.GetInstance(getApplication());

        PenStatus penStatus = penCommAgent.getPenStatus();

        penStatus.setInitializing();//初始化笔的参数类

        smart_refresh_layout.setEnableLoadMore(false);//是否启用上拉加载功能
        smart_refresh_layout.setHeaderTriggerRate(1);//触发刷新距离 与 HeaderHeight 的比率1.0.4
        smart_refresh_layout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                stopScan();
                smart_refresh_layout.finishRefresh(3000);////延迟3000毫秒后结束刷新
                if (mPermissionTip) {
                    requestPermission1();
                }
            }
        });
        btn_retry.setOnClickListener(this);
        PenUtils.penDotType = -1;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPermissionTip = (Boolean) SPUtils.get(SearchActivity.this, Constants.FIRST_SEARCH, false);
        Log.i(TAG, "onResume mPermissionTip::" + mPermissionTip);
        if (mPermissionTip) {
            registerReceiver(receiver, filter);
            requestPermission1();
        } else {
            initPermissionsTip();
            smart_refresh_layout.autoRefresh();//自动刷新
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchAdapter.clearDevice();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceConnected events) {
        Log.i(TAG, "receivePenStatus Connected: " + events);
        hideLoadingDialog();
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceDisconnected events) {
        Log.e(TAG, "receivePenStatus Connected: 断开连接");
        hideLoadingDialog();
        // showToast(getString(R.string.connect_failed));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_left:
            case R.id.iv_left:
                finish();
                break;
            case R.id.btn_retry:
                requestPermission1();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequesting = false;
                    requestPermission2();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this, R.style.Theme_AppCompat_Light_Dialog)
                            .setTitle(R.string.permissions_tips1).setMessage(R.string.permissions_tips3)
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
            case PERMISSION_REQUEST2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequesting = false;
                    //requestBluetooth();
                    requestPermission3();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this, R.style.Theme_AppCompat_Light_Dialog)
                            .setTitle(R.string.permissions_tips1).setMessage(R.string.permissions_tips2)
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
            case PERMISSION_REQUEST3:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequesting = false;
                    requestBluetooth();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this, R.style.Theme_AppCompat_Light_Dialog)
                            .setTitle(R.string.permission_title).setMessage(R.string.permission_fine_location_message)
                            .setPositiveButton(R.string.permission_button, (dialog1, which) -> {
                                isRequesting = false;
                                dialogCancel(dialog1);
                            }).setCancelable(false).create();
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
        switch (requestCode) {
            case REQUEST_BLUETOOTH_ENABLE:
                if (resultCode != RESULT_OK) {
                    AlertDialog dialog = new AlertDialog.Builder(
                            this, R.style.Theme_AppCompat_Light_Dialog)
                            .setTitle(R.string.permission_title).setMessage(R.string.permission_bluetooth_message)
                            .setPositiveButton(R.string.permission_button, (dialog1, which) -> {
                                isRequesting = false;
                                dialogCancel(dialog1);
                            }).setCancelable(false).create();
                    dialogShow(dialog);
                } else
                    isRequesting = false;
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * 请求写文件权限
     */
    private void requestPermission1() {
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
        requestPermission2();
    }

    /**
     * 请求位置权限
     */
    private void requestPermission2() {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (isRequesting)
                return;
            isRequesting = true;
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST2);
            return;
        }
        Log.i(TAG, "requestPermission2");
        requestPermission3();
    }

    /**
     * 请求精确位置权限
     */
    private void requestPermission3() {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (isRequesting)
                return;
            isRequesting = true;
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST3);
            return;
        }
        Log.i(TAG, "requestPermission3");
        requestBluetooth();
    }

    /**
     * 请求蓝牙权限
     */
    private void requestBluetooth() {
        smart_refresh_layout.setVisibility(View.VISIBLE);
        ll_not_found.setVisibility(View.GONE);
        Log.i(TAG, "requestBluetooth");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            if (isRequesting)
                return;
            isRequesting = true;
            Log.i(TAG, "requestBluetooth   null");
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_BLUETOOTH_ENABLE);
        } else {
            isRequesting = false;
            //if (isScanning)
            //    return;
            isScanning = true;
            Log.i(TAG, "requestBluetooth   FindAllDevices");
            penCommAgent.FindAllDevices(scanCallback);
        }
    }

    /**
     * 停止扫描
     */
    private void stopScan() {
        //if (!isScanning)
        //    return;
        isScanning = false;
        searchAdapter.clearDevice();

        penCommAgent.stopFindAllDevices();
    }


    /**
     * 蓝牙扫描回调
     */
    BLEScanner.OnBLEScanListener scanCallback = new BLEScanner.OnBLEScanListener() {

        @Override
        public void onScanResult(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.i(TAG, "onScanResult: " + device.getAddress());
            searchAdapter.addDevice(device, scanRecord);
//            if (device.getAddress().length() > 0) {
//                smart_refresh_layout.finishRefresh();//结束刷新
//            }
        }

        @Override
        public void onScanFailed(BLEException bleException) {
            Log.e(TAG, bleException.getMessage());
            isScanning = false;
            if (searchAdapter.getItemCount() == 0) {
                smart_refresh_layout.setVisibility(View.GONE);
                ll_not_found.setVisibility(View.VISIBLE);
            }
        }
    };

    /**
     * 点击事件
     */
    SearchAdapter.OnItemClickListener onItemClickListener = new SearchAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(BluetoothDevice device) {
            smart_refresh_layout.finishRefresh();
            if (System.currentTimeMillis() - lastClickTime < 500) {
                return;
            }
            Log.e(TAG, "device:" + device.getAddress());
            // lastClickTime = System.currentTimeMillis();
            deviceConnect(device.getName(), device.getAddress());
        }
    };


    private void initPermissionsTip() {
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
                SPUtils.put(SearchActivity.this, Constants.FIRST_SEARCH, true);
                SPUtils.put(SearchActivity.this, Constants.FIRST_SAVE, true);
                registerReceiver(receiver, filter);
                requestPermission1();
            }
        });
        mServiceDialog.show();
    }

}
