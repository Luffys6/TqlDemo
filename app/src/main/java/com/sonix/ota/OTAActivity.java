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
    //??????????????????
    private String pAddress;
    //????????????
    private int pType;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //?????????????????????????????????
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
                        Toast.makeText(OTAActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
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
        //??????????????????
        bleManager.getPenType();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // ?????????????????????mac??????
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
                Toast.makeText(mContext, "???????????????MAC???????????????????????????", Toast.LENGTH_SHORT).show();
            }

            choseOtaMethod(pAddress, pType);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // DFU??????
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
        if (pType == 0 || pType == 1 || pType == 3) { // 111??????
            mAddress = pAddress;
        } else if (pType == 2 || pType == 16) {     // 112??????
            setOtaMacAddress(pAddress);
        } else if (pType == 8) {                    // 130??????
            mAddress = pAddress;
        }
    }

    /**
     * ??????OTA???????????????
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
     * ??????hexString
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
     * ??????DFU
     */
    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            //???DFU???????????????DFU??????????????????????????????
            Log.e("ota", "DFU???????????????DFU????????????," + deviceAddress);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            //?????????????????????????????????????????????????????????DFU???????????????DFU?????????
            Log.d("ota", "??????????????????,??????????????????DFU???????????????DFU??????." + deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            //???DFU????????????????????????????????? ???????????????DFU?????????????????????DFU START????????????Init??????????????????????????????
            Log.d("ota", "DFU????????????," + deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            //???DFU??????????????????????????????????????????????????????
            Log.d("ota", "DFU?????????????????????????????????," + deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            //???????????????DFU???????????????????????????????????????????????????DFU??????????????????????????? ??????????????????????????????DFU??????????????????????????? ?????????????????????onDeviceDisconnected???String????????????
            Log.d("ota", "???????????????DFU???????????????????????????????????????????????????DFU?????????????????????");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            //??????????????????????????????????????? ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????\
            mUpgradeProgressDialog.setProgress(percent);
            //bar.setVisibility(View.VISIBLE);
            //bar.setProgress(percent);
            Log.d("debug", "????????????????????????????????????---" + percent);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            //??????????????????????????????????????????????????????
            Log.d("debug", "????????????????????????????????????????????????");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            //???????????????????????????????????????????????????????????????
            Log.d("debug", "????????????????????????????????????????????????????????????");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            //??????????????????????????????????????????????????? ??????????????????
            Log.d("debug", "??????????????????????????????????????????????????? ??????????????????");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            //Method called when the DFU process succeeded.
            //bar.setVisibility(View.GONE);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // ??????Toast??????
                    Toast toast;
                    Display display = getWindowManager().getDefaultDisplay();
                    int height = display.getHeight();
                    toast = Toast.makeText(OTAActivity.this, "OTA?????????", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, height / 6);
                    toast.show();
                    finish();
                }
            });
            Log.d("debug", "DFU?????????");
            mUpgradeProgressDialog.dismiss();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            //???DFU????????????????????????????????????
            Log.d("debug", "???DFU????????????????????????????????????");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            //?????????????????????????????????
            Log.d("debug", "??????????????????????????????onError");
            mUpgradeProgressDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(OTAActivity.this, "OTA??????", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    };

    /**
     * ??????DFU????????????
     *
     * @param bluetoothDevice ????????????
     * @param keepBond        ???????????????????????????
     * @param force           ???DFU?????????true??????????????????DFU Bootloader????????????????????????
     * @param PacketsReceipt  ???????????????????????????????????????PRN????????????
     *                        ???????????????????????????Android Marshmallow?????????????????????????????????PEN??????????????????????????????
     * @param numberOfPackets ??????????????????????????????????????????????????????????????????PEN?????????????????????????????? PEN????????????????????????????????????
     * @param filePath        ???????????????ZIP??????????????????
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
                        showToast("????????????????????????");
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
        // ????????????????????????
        String otaText = filePath.getText().toString().trim();
        File file = new File(otaText);
        if (!file.exists()) {
            Toast.makeText(OTAActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pType == 0 || pType == 1 || pType == 3) { // 111?????? bin
            if (!file.getName().endsWith(".bin")) {
                Toast.makeText(OTAActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod1(otaText, mAddress);
        } else if (pType == 2 || pType == 16) {     // 112?????? zip
            if (!file.getName().endsWith(".zip")) {
                Toast.makeText(OTAActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod2();
        } else if (pType == 18) {  //???????????????
            if (!file.getName().endsWith(".bin")) {
                Toast.makeText(OTAActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod1(otaText, pAddress);
        } else if (pType == 8 || pType == 9 || pType == 10 || pType == 11) { // 130?????? bin
            if (!file.getName().endsWith(".bin")) {
                Toast.makeText(OTAActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                return;
            }
            otaUpdateMethod3(otaText);
        }

    }

    /**
     * 111??????
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
                                        Toast.makeText(mContext, "????????????", Toast.LENGTH_SHORT).show();
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
     * 112??????
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
     * 130??????
     */
    private void otaUpdateMethod3(String otaText) {

        mFirmware = readFirmware(otaText);

        Log.i(TAG, "bytes = " + BLEByteUtil.bytesToHexString(mFirmware));

        if(mFirmware[0] == (byte)0x54 && mFirmware[1] == (byte)0x51 && mFirmware[2] == (byte)0x4C){
            mFirmware = SplitBinData(mFirmware);// ?????????  ?????????????????????
        }else{
            mFirmware = null;
            Toast.makeText(this, "OTA ?????????????????????, ???????????????bin?????? !", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mFirmware == null || mFirmware.length == 0) {
            Toast.makeText(this, "????????????!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "OTA ?????????null !" );
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

        //???????????? MD5???
        byte[] byteBtVersion = new byte[10];
        System.arraycopy(bytes, 5, byteBtVersion, 0, 10);
        byte[] byteBtLength = new byte[4];
        System.arraycopy(bytes, 15, byteBtLength, 0, 4);

        int byteBtLengthInt = BLEByteUtil.bytesToInt(byteBtLength);//????????????

        //??????MCU MD5???
        byte[] byteMcuVersion = new byte[10];
        System.arraycopy(bytes, 19, byteMcuVersion, 0, 10);
        byte[] byteMcuLength = new byte[4];
        System.arraycopy(bytes, 29, byteMcuLength, 0, 4);
        int byteMcuLengthInt = BLEByteUtil.bytesToInt(byteMcuLength);//MCU ??????

        //??????byte
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
            // ??????????????????
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.startScan(scanCallback);
                }
            }, 1 * 1000);


            // ????????????????????????
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
     * ????????????
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
     * 130??????????????????
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

    // ????????????
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
                SPUtils.put(OTAActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, true);
                requestPermission1();
            }
        });
        mServiceDialog.show();
    }

    /**
     * ?????????????????????
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