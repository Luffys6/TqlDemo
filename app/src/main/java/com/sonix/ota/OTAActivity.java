package com.sonix.ota;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.oidbluetooth.R;
import com.sonix.oidbluetooth.SearchActivity;
import com.tqltech.tqlpencomm.firmware.UpdateError;
import com.tqltech.tqlpencomm.firmware.UpdateFirmwareUtil;
import com.tqltech.tqlpencomm.firmware.controller.Device;
import com.sonix.util.FileUtils;
import com.sonix.util.SPUtils;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.util.BLEByteUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class OTAActivity extends BaseActivity implements View.OnClickListener {

    private final static String TAG = "OTAActivity";

    private BluetoothDevice mDevice = null;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBluetoothAdapter;

    private String mAddress = "";
    private PenCommAgent bleManager;

    public TextView tv_title;
    public RelativeLayout rl_left;
    public RelativeLayout rl_right;
    public ImageView iv_left;
    public ImageView iv_right;

    private TextView filePath;
    private LinearLayout choseFile;
    private Button otaUpdata;

    private OtaUpgrader mOtaUpgrader;
    private ProgressDialog mUpgradeProgressDialog;
    private int mMaxProgress;

    private static final int MESSAGE_UPDATE_TIP = 10000;
    private static final int MESSAGE_UPDATE_PROGRESS = 20000;
    private static final int MESSAGE_FINISH_PROGRESS = 30000;
    private byte[] mFirmware;
    private boolean isOTARunning = false;
    private long otaStartDelay = 0;

    private SharedPreferences sp;
    //笔的蓝牙地址
    private String pAddress;
    //笔的类型
    private int pType;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //重新获取笔的地址、类型
                    pAddress = App.getInstance().getDeviceAddress();
                    pType = App.getInstance().getDeviceType();
                    break;
                case 2:
                    break;
                case 0x123:
                    String otaText = filePath.getText().toString();
                    File file = new File(otaText);
                    Log.i(TAG, "====112 otaText path====" + otaText);
                    if (file.exists()) {
                        startDFU(mDevice, true, false, true, 0, otaText);
                    } else {
                        Toast.makeText(OTAActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MESSAGE_UPDATE_PROGRESS:
                    if (mUpgradeProgressDialog != null) {
                        if (!mUpgradeProgressDialog.isShowing())
                            mUpgradeProgressDialog.show();
                        mUpgradeProgressDialog.setProgress((Integer) msg.obj);
                    }
                    break;
                case MESSAGE_UPDATE_TIP:
                    //tip.append(msg.obj + "\n");
                    Toast.makeText(OTAActivity.this, msg.obj + "\n", Toast.LENGTH_SHORT).show();
                    if ("disconnected".equals(msg.obj)) {
                        if (mUpgradeProgressDialog != null && mUpgradeProgressDialog.isShowing()) {
                            mUpgradeProgressDialog.dismiss();
                        }
                        Intent intent = new Intent(OTAActivity.this, SearchActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    break;
                case MESSAGE_FINISH_PROGRESS:
                    if (mUpgradeProgressDialog != null && mUpgradeProgressDialog.isShowing()) {
                        mUpgradeProgressDialog.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private boolean isRequesting;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_ota;
    }

    @Override
    public void initView() {
        tv_title = findViewById(R.id.tv_title);
        rl_left = findViewById(R.id.rl_left);
        rl_right = findViewById(R.id.rl_right);
        iv_left = findViewById(R.id.iv_left);
        iv_right = findViewById(R.id.iv_right);

        rl_left.setOnClickListener(this);
        iv_left.setOnClickListener(this);
        tv_title.setText(getResources().getString(R.string.ota_update));
        iv_right.setVisibility(View.GONE);

        filePath = findViewById(R.id.otaText);
        choseFile = findViewById(R.id.choseFile);
        otaUpdata = findViewById(R.id.otaUpdata);
        choseFile.setOnClickListener(this);
        otaUpdata.setOnClickListener(this);
        mUpgradeProgressDialog = createUpgradeProgressDialog();

        if (sp == null) {
            sp = getSharedPreferences("ota", MODE_PRIVATE);
        }
        String path = sp.getString("path", "");
        Log.i(TAG, "path=" + path);
        if (path != null && !path.equals("")) {
            filePath.setText(path);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpgradeProgressDialog != null) {
            if (mUpgradeProgressDialog.isShowing()) {
                Context context = ((ContextWrapper) mUpgradeProgressDialog.getContext()).getBaseContext();
                if (context instanceof Activity) {
                    if (!((Activity) context).isFinishing() && !((Activity) context).isDestroyed())
                        mUpgradeProgressDialog.dismiss();
                } else {
                    mUpgradeProgressDialog.dismiss();
                }
            }
            mUpgradeProgressDialog = null;
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void initData() {
        boolean mPermissionTip = (Boolean) SPUtils.get(OTAActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, false);
        Log.i(TAG, "onResume mPermissionTip::" + mPermissionTip);
        if (mPermissionTip) {
            requestPermission1();
        } else {
            initPermissionsTip();
        }
        bleManager = PenCommAgent.GetInstance(getApplication());
        //请求笔的类型
        bleManager.getPenType();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 获取当前设备的mac地址
        Intent intent = getIntent();
        if (intent != null) {
            pAddress = intent.getStringExtra("addr");
            pType = intent.getIntExtra("type", -1);
            Log.i(TAG, "pAddress=" + pAddress + ",pType=" + pType);

            if (TextUtils.isEmpty(pAddress)) {
                bleManager.getPenMac();
            }

            if (pType < 0) {
                bleManager.getPenType();
            }

            if (TextUtils.isEmpty(pAddress) || pType < 0) {
                handler.sendEmptyMessageDelayed(1, 2000);
                Toast.makeText(mContext, "未获取到笔MAC、类型，请稍后再试", Toast.LENGTH_SHORT).show();
            }

            choseOtaMethod(pAddress, pType);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // DFU监听
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);

        String path = filePath.getText().toString();
        Log.i(TAG, "onPause: path= " + path);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("path", filePath.getText().toString());
        editor.commit();


    }

    private void choseOtaMethod(String pAddress, int pType) {
        if (pType == 0 || pType == 1 || pType == 3) { // 111系列
            mAddress = pAddress;
        } else if (pType == 2 || pType == 16) {     // 112系列
            setOtaMacAddress(pAddress);
        } else if (pType == 8) {                    // 130系列
            mAddress = pAddress;
        }
    }

    /**
     * 设置OTA的蓝牙地址
     *
     * @param pAddress
     */
    private void setOtaMacAddress(String pAddress) {
        if (!TextUtils.isEmpty(pAddress) && pAddress.length() > 12) {
            try {
                String b = pAddress.substring(pAddress.length() - 2, pAddress.length());
                int a = Integer.parseInt(b, 16);
                String str = "";
                if (a == 255) {
                    str = "00";
                } else {
                    str = Integer.toHexString(a + 1).toUpperCase();
                }
                str = addZero(str);
                mAddress = pAddress.substring(0, pAddress.length() - 2) + str;

                Log.i(TAG, "mAddress=" + mAddress);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 组装hexString
     *
     * @param str
     * @return
     */
    private String addZero(String str) {
        String outputStr = str;
        if (str.length() == 1) {
            outputStr = "0" + str;
        }

        return outputStr;
    }

    /**
     * 监听DFU
     */
    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            //当DFU服务开始与DFU目标连接时调用的方法
            Log.e("ota", "DFU服务开始与DFU目标连接," + deviceAddress);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            //方法在服务成功连接时调用，发现服务并在DFU目标上找到DFU服务。
            Log.d("ota", "服务成功连接,发现服务并在DFU目标上找到DFU服务." + deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            //当DFU进程启动时调用的方法。 这包括读取DFU版本特性，发送DFU START命令以及Init数据包（如果设置）。
            Log.d("ota", "DFU进程启动," + deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            //当DFU进程启动和要发送的字节时调用的方法。
            Log.d("ota", "DFU进程启动和要发送的字节," + deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            //当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方法。 将发送开关命令，并且DFU过程应该再次开始。 此调用后不会有onDeviceDisconnected（String）事件。
            Log.d("ota", "当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            //在上传固件期间调用的方法。 它不会使用相同的百分比值调用两次，但是在小型固件文件的情况下，可能会省略一些值。\
            mUpgradeProgressDialog.setProgress(percent);
            //bar.setVisibility(View.VISIBLE);
            //bar.setProgress(percent);
            Log.d("debug", "在上传固件期间调用的方法---" + percent);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            //在目标设备上验证新固件时调用的方法。
            Log.d("debug", "目标设备上验证新固件时调用的方法");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            //服务开始断开与目标设备的连接时调用的方法。
            Log.d("debug", "服务开始断开与目标设备的连接时调用的方法");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            //当服务从设备断开连接时调用的方法。 设备已重置。
            Log.d("debug", "当服务从设备断开连接时调用的方法。 设备已重置。");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            //Method called when the DFU process succeeded.
            //bar.setVisibility(View.GONE);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 设置Toast位置
                    Toast toast;
                    Display display = getWindowManager().getDefaultDisplay();
                    int height = display.getHeight();
                    toast = Toast.makeText(OTAActivity.this, "OTA已完成", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, height / 6);
                    toast.show();
                    finish();
                }
            });
            Log.d("debug", "DFU已完成");
            mUpgradeProgressDialog.dismiss();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            //当DFU进程已中止时调用的方法。
            Log.d("debug", "当DFU进程已中止时调用的方法。");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            //发生错误时调用的方法。
            Log.d("debug", "发生错误时调用的方法onError");
            mUpgradeProgressDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(OTAActivity.this, "OTA失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    };

    /**
     * 启动DFU升级服务
     *
     * @param bluetoothDevice 蓝牙设备
     * @param keepBond        升级后是否保持连接
     * @param force           将DFU设置为true将防止跳转到DFU Bootloader引导加载程序模式
     * @param PacketsReceipt  启用或禁用数据包接收通知（PRN）过程。
     *                        默认情况下，在使用Android Marshmallow或更高版本的设备上禁用PEN，并在旧设备上启用。
     * @param numberOfPackets 如果启用分组接收通知过程，则此方法设置在接收PEN之前要发送的分组数。 PEN用于同步发射器和接收器。
     * @param filePath        约定匹配的ZIP文件的路径。
     */
    private void startDFU(BluetoothDevice bluetoothDevice, boolean keepBond, boolean force,
                          boolean PacketsReceipt, int numberOfPackets, String filePath) {
        Log.i(TAG, "---startDFU---");
        final DfuServiceInitiator starter = new DfuServiceInitiator(mAddress)
                .setDisableNotification(true)
                .setKeepBond(false)
                .setZip(filePath);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            starter.setForeground(false);
            starter.setDisableNotification(true);
        }

        starter.start(OTAActivity.this, DfuService.class);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_left:
            case R.id.iv_left:
                finish();
                break;
            case R.id.choseFile:
                FileUtils.openFilePath(this);
                break;
            case R.id.otaUpdata:
                long l = System.currentTimeMillis();
                if (l - lastConnectTime > 3000) {
                    otaUpdate();
                }
                break;
            default:
                break;
        }

    }

    private long lastConnectTime;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FileUtils.GET_FILEPATH_SUCCESS_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    String path = "";
                    Uri uri = data.getData();
                    Log.i(TAG, "onActivityResult: path=" + uri + ",path2=" + uri.getPath().toString());
                    if (uri != null) {
                        path = FileUtils.getRealPathFromURI(mContext, uri);
                    }

                    if ((pType == 2 || pType == 16) && path.endsWith(".zip")) {
                        filePath.setText(path);
                    } else if (!path.endsWith(".bin")) {// || !new File(path).getName().startsWith("BT")
                        showToast("请选择正确的文件");
                    } else {
                        filePath.setText(path);
                    }


                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void otaUpdate() {
        // 判断文件是否存在
        String otaText = filePath.getText().toString().trim();
        File file = new File(otaText);
        if (!file.exists()) {
            Toast.makeText(OTAActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pType == 0 || pType == 1 || pType == 3) { // 111系列 bin
            if (!file.getName().endsWith(".bin")) {
                Toast.makeText(OTAActivity.this, "文件格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod1(otaText, mAddress);
        } else if (pType == 2 || pType == 16) {     // 112系列 zip
            if (!file.getName().endsWith(".zip")) {
                Toast.makeText(OTAActivity.this, "文件格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod2();
        } else if (pType == 18) {  //低成本逻辑
            if (!file.getName().endsWith(".bin")) {
                Toast.makeText(OTAActivity.this, "文件格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod1(otaText, pAddress);
        } else if (pType == 8 || pType == 9 || pType == 10 || pType == 11) { // 130系列 bin
            if (!file.getName().endsWith(".bin")) {
                Toast.makeText(OTAActivity.this, "文件格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod3(otaText);
        }

    }

    /**
     * 111升级
     */
    private void otaUpdateMethod1(String otaText, String mAddress) {
        if (mOtaUpgrader == null) {
            mOtaUpgrader = new OtaUpgrader(OTAActivity.this,
                    mAddress,
                    otaText,
                    new OtaUpgrader.Callback() {

                        public void onFinish(int status) {
                            Log.i(TAG, "onFinish: status=" + status);
                        }

                        public void onProgress(final int realSize, final int precent) {
                            Log.i(TAG, "onProgress: realSize=" + realSize + ",precent=" + precent);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    mUpgradeProgressDialog.setMax(mMaxProgress);
                                    mUpgradeProgressDialog.setProgress(realSize);
                                    if (precent == 100) {
                                        mUpgradeProgressDialog.dismiss();
                                        Toast.makeText(mContext, "升级完成", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });

            mMaxProgress = mOtaUpgrader.getPatchSize();
            mOtaUpgrader.start();
        }
        mUpgradeProgressDialog.show();
    }

    /**
     * 112升级
     */
    private void otaUpdateMethod2() {
        bleManager.setOTAModel();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanLeDevice(true);
            }
        }, 2000);

        mUpgradeProgressDialog.show();
    }

    /**
     * 130升级
     */
    private void otaUpdateMethod3(String otaText) {

        mFirmware = readFirmware(otaText);

        Log.i(TAG, "bytes = " + BLEByteUtil.bytesToHexString(mFirmware));

        if(mFirmware[0] == (byte)0x54 && mFirmware[1] == (byte)0x51 && mFirmware[2] == (byte)0x4C){
            mFirmware = SplitBinData(mFirmware);// 新版本  切除前面的数据
        }else{
            mFirmware = null;
            Toast.makeText(this, "OTA 文件中格式错误, 请用最新版bin文件 !", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mFirmware == null || mFirmware.length == 0) {
            Toast.makeText(this, "文件错误!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "OTA 数据为null !" );
            return;
        }

        bleManager.setOTAModel();

        if (!TextUtils.isEmpty(pAddress)) {
            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(pAddress);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startConnect(mDevice);
            }
        }, 1000);

        mUpgradeProgressDialog.show();

    }

    private byte[] SplitBinData(byte[] bytes) {

        //解析蓝牙 MD5值
        byte[] byteBtVersion = new byte[10];
        System.arraycopy(bytes, 5, byteBtVersion, 0, 10);
        byte[] byteBtLength = new byte[4];
        System.arraycopy(bytes, 15, byteBtLength, 0, 4);

        int byteBtLengthInt = BLEByteUtil.bytesToInt(byteBtLength);//蓝牙长度

        //解析MCU MD5值
        byte[] byteMcuVersion = new byte[10];
        System.arraycopy(bytes, 19, byteMcuVersion, 0, 10);
        byte[] byteMcuLength = new byte[4];
        System.arraycopy(bytes, 29, byteMcuLength, 0, 4);
        int byteMcuLengthInt = BLEByteUtil.bytesToInt(byteMcuLength);//MCU 长度

        //蓝牙byte
        byte[] byteBtLengthByte = new byte[byteBtLengthInt];
        System.arraycopy(bytes, (49 + byteMcuLengthInt), byteBtLengthByte, 0, byteBtLengthInt);

        return byteBtLengthByte;

    }

    private ProgressDialog createUpgradeProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);

        dialog.setTitle(R.string.ota_upgrade_progress_dialog_title);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(mMaxProgress);
        dialog.setProgress(0);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mOtaUpgrader != null) {
                            mOtaUpgrader.stop();
                        }

                    }
                });

        return dialog;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // 开始扫描设备
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.startScan(scanCallback);
                }
            }, 1 * 1000);


            // 设置扫描超时时间
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(scanCallback);
                }
            }, 11 * 1000);

        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
        }
    }


    /**
     * 扫描回调
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback scanCallback = new ScanCallback() {
        boolean flag = false;

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            Log.i(TAG, "scan result mAddress->" + mAddress + "========device-->" + device.getAddress() + "===" + device.getName());
            if (device.getAddress().equals(mAddress) && device.getName().contains("In DFU")) {
                if (!flag) {
                    Log.e(TAG, "find DFU device");
                    bleManager.disconnect(pAddress);
                    bleManager.connect(mAddress);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddress);
                    handler.obtainMessage(0x123, "").sendToTarget();
                    flag = true;
                    mBluetoothLeScanner.stopScan(scanCallback);
                }
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /**
     * 130升级读文件流
     *
     * @param fileName
     * @return
     */
    private byte[] readFirmware(String fileName) {
        try {
            InputStream stream = new FileInputStream(fileName);
            int length = stream.available();
            byte[] firmware = new byte[length];
            stream.read(firmware);
            stream.close();
            return firmware;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 开启连接
    private void startConnect(BluetoothDevice hidDevice) {
        Device device = new Device(hidDevice, null, 0);
        device.setCallback(mDeviceCallback);
        device.connect(getApplicationContext());
        isOTARunning = true;
        handler.obtainMessage(MESSAGE_UPDATE_TIP, "send connect request to " + hidDevice.getAddress()).sendToTarget();
    }

    private Device.Callback mDeviceCallback = new Device.Callback() {
        @Override
        public void onConnected(Device device) {
            handler.obtainMessage(MESSAGE_UPDATE_TIP, "connected").sendToTarget();
            isOTARunning = true;
        }

        @Override
        public void onDisconnected(Device device) {
            handler.obtainMessage(MESSAGE_UPDATE_TIP, "disconnected").sendToTarget();
            isOTARunning = false;
        }

        @Override
        public void onServicesDiscovered(final Device device) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "start ota:" + BLEByteUtil.bytesToHexString(mFirmware));
                    handler.obtainMessage(MESSAGE_UPDATE_TIP, "start ota").sendToTarget();
                    device.startOta(mFirmware);
                }
            }, otaStartDelay * 2000);
        }

        @Override
        public void onOtaStateChanged(Device device, int state) {
            switch (state) {
                case Device.STATE_PROGRESS:
//                    Log.i(TAG, "ota progress : " + device.getOtaProgress() + "device==" + device);
                    handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, device.getOtaProgress()).sendToTarget();
                    break;
                case Device.STATE_SUCCESS:
                    Log.i(TAG, "ota success : ");
                    handler.obtainMessage(MESSAGE_UPDATE_TIP, "ota success").sendToTarget();
                    /*if (mDevice != null) {
                        mDevice.disconnect();
                    }*/
//                    handler.sendEmptyMessageDelayed(MESSAGE_FINISH_PROGRESS, 300);
                    break;
                case Device.STATE_FAILURE:
                    Log.i(TAG, "ota failure : ");
                    handler.obtainMessage(MESSAGE_UPDATE_TIP, "ota failure").sendToTarget();
                    /*if (mDevice != null) {
                        mDevice.disconnect();
                    }*/
//                    handler.sendEmptyMessageDelayed(MESSAGE_FINISH_PROGRESS, 300);
                    break;
            }
        }
    };

    private void initPermissionsTip() {
        Dialog mServiceDialog = new Dialog(this, R.style.customDialog);
        LayoutInflater mInflater = LayoutInflater.from(this);
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
                SPUtils.put(OTAActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, true);
                requestPermission1();
            }
        });
        mServiceDialog.show();
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequesting = false;
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

}