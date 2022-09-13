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

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
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
import com.sonix.base.BindEventBus;
import com.sonix.oidbluetooth.R;
import com.sonix.oidbluetooth.SearchActivity;
import com.tqltech.tqlpencomm.firmware.UpdateFirmwareUtil;
import com.tqltech.tqlpencomm.listener.OnUpdateFirmwareListener;
import com.tqltech.tqlpencomm.util.CRC16;
import com.sonix.util.ConnectThread;
import com.sonix.util.Events;
import com.sonix.util.FileUtils;

import com.sonix.util.SPUtils;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.pen.PenUtils;
import com.tqltech.tqlpencomm.util.BLEByteUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


@BindEventBus
public class McuActivity extends BaseActivity implements View.OnClickListener {

    private final static String TAG = "MCUActivity";

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
    private Button mcuUpdata;

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
    ArrayList<byte[]> mcuMultiData = new ArrayList<>();


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
                case MESSAGE_UPDATE_PROGRESS:
                    if (mUpgradeProgressDialog != null && App.getInstance().isDeviceConnected()) {
                        if (!mUpgradeProgressDialog.isShowing())
                            if ((Integer)msg.obj == 0) {
                                hideLoadingDialog();
                            }
                        mUpgradeProgressDialog.show();
                        mUpgradeProgressDialog.setProgress((Integer) msg.obj);
                    }
                    break;
                case MESSAGE_UPDATE_TIP:
                    //tip.append(msg.obj + "\n");
                    if(mUpgradeProgressDialog!=null && mUpgradeProgressDialog.isShowing()){
                        mUpgradeProgressDialog.dismiss();
                    }
                    Toast.makeText(McuActivity.this, msg.obj + "\n", Toast.LENGTH_SHORT).show();
                    if ("MCU升级成功".equals(msg.obj)) {
                        handler.removeCallbacksAndMessages(null);
                        showSuccessDialog();
                    } else if ("MCU升级失败".equals(msg.obj)){
                        showToast("MCU升级失败");
                        showFailDialog("ota failure");
                    }else {
                        showFailDialog(msg.obj+"");
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
    private PenCommAgent penCommAgent;
    private ConnectThread thread;
    private int indexMcu2;
    private byte[] bytes;
    private boolean isRequesting;
    private Dialog dialog;
    private UpdateFirmwareUtil updateFirmwareUtil;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_mcu;
    }

    @Override
    public void initView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//常亮
        if (penCommAgent == null) {
            penCommAgent = PenCommAgent.GetInstance(getApplication());
        }

        tv_title = findViewById(R.id.tv_title);
        rl_left = findViewById(R.id.rl_left);
        rl_right = findViewById(R.id.rl_right);
        iv_left = findViewById(R.id.iv_left);
        iv_right = findViewById(R.id.iv_right);

        rl_left.setOnClickListener(this);
        iv_left.setOnClickListener(this);
        tv_title.setText(getResources().getString(R.string.mcu_update));
        iv_right.setVisibility(View.GONE);

        filePath = findViewById(R.id.otaText);
        choseFile = findViewById(R.id.choseFile);
        mcuUpdata = findViewById(R.id.mcuUpdata);
        choseFile.setOnClickListener(this);
        mcuUpdata.setOnClickListener(this);
        mUpgradeProgressDialog = createUpgradeProgressDialog();

        if (sp == null) {
            sp = getSharedPreferences("ota", MODE_PRIVATE);
        }
        String path = sp.getString("pathMcu", "");
        Log.i(TAG, "path=" + path);
        if (path != null && !path.equals("")) {
            filePath.setText(path);
        }
        updateFirmwareUtil = penCommAgent.updateMCUAndBTWith(new OnUpdateFirmwareListener() {
            @Override
            public void splitFileResult(int BtLength, String BtMD5, int McuLength, String McuMD5) {

            }

            @Override
            public void splitKeyFileResult(String BtMD5, String McuMD5) {

            }

            @Override
            public void updateMcuStart() {
                showLoadingDialog(McuActivity.this,getResources().getString(R.string.Upgrading));
            }

            @Override
            public void updateMcuProgress(int progress) {
                handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, progress).sendToTarget();
            }

            @Override
            public void updateMcuSuccess() {
                isOver =true;
                handler.obtainMessage(MESSAGE_UPDATE_TIP,"MCU升级成功").sendToTarget();
            }

            @Override
            public void updateMcuFail(int errorType) {
                isOver =true;
                handler.obtainMessage(MESSAGE_UPDATE_TIP,"MCU升级失败 errorType = " +errorType).sendToTarget();
            }

            @Override
            public void updateBTStart() {

            }

            @Override
            public void updateBTProgress(int progress) {

            }

            @Override
            public void updateBTSuccess() {

            }

            @Override
            public void updateBTFail() {

            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        indexMcu2 = 0;
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
            PenUtils.isMcuUpgrade = false;
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void initData() {
        boolean mPermissionTip = (Boolean) SPUtils.get(McuActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, false);
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

    }


    @Override
    protected void onPause() {
        super.onPause();

        String path = filePath.getText().toString();
        Log.i(TAG, "onPause: path= " + path);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("pathMcu", filePath.getText().toString());
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
            case R.id.mcuUpdata:
                if (isDeviceConnected()) return;

                if(!filePath.getText().toString().trim().endsWith(".bin") ){
                    showToast("请选择正确的文件");
                    return;
                }

                long l = System.currentTimeMillis();
                if (l - lastConnectTime > 3000) {

                    lastConnectTime = l;
                    int i = updateFirmwareUtil.startNoCheckOTAUpdate(filePath.getText().toString().trim());

                    if (i != 0) {
                        showToast("失败原因  :  " + updateFirmwareUtil.getErrorString(i));
                        //   1; //错误选择文件 或者 文件解析错误
                        //   2; //校验key失败
                        //   4; //文件缺失
                    }


                }

//                isOTARunning = true;
//
//                showLoadingDialog(McuActivity.this,getResources().getString(R.string.Upgrading));
//
//                penCommAgent.reqPenMcuUpgrade();
//
//                penCommAgent.setStartMcuUpgrade(new PenCommAgent.StartMcuUpgrade() {
//                    @Override
//                    public void onStartMcuUpgrade(int position) {
//                        if (mcuMultiData.size() == 0) { //预防多次进入
//                            handler.obtainMessage(MESSAGE_UPDATE_PROGRESS,0).sendToTarget();
//                            synchronized (this) {
//                                mcuUpdate();
//                            }
//                        }
//
//                    }
//                });
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        hideLoadingDialog();
//                    }
//                },5*1000);

                break;
            default:
                break;
        }

    }
    private long lastConnectTime;
    File  file;



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
                    Log.i(TAG, "onActivityResult: path=" + path);
                    if(path.endsWith(".bin")){ // || !new File(path).getName().startsWith("MCU")
                        file = new File(path);
                        filePath.setText(path);
                        updateFirmwareUtil.SplitMACBinData(path);

                    }else{
                        showToast("请选择正确的文件");
                    }
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void mcuUpdate() {
        // 判断文件是否存在
        String otaText = filePath.getText().toString().trim();
        File file = new File(otaText);
        if (!file.exists()) {
            Toast.makeText(McuActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        analysisMcuData(file);
    }

    private int mcuIndex = 0;
    private int mcu = 0;


    public byte[] readFile(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        int success = fileInputStream.read(bytes);
        return bytes;
    }

    /**
     * 读取文件大小保存到集合
     * @param file
     */
    private void analysisMcuData(File file) {
        mcuMultiData.clear();
        try {
            bytes = readFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Integer cc = bytes.length / 16;
        Log.i(TAG,"mcu文件总长度 "+"" + bytes.length+",,mcu文件总包数 =="+cc + "");
        if (cc * 16 < bytes.length) {
            cc++;
        }
        for (int i = 0; i < cc; i++) {
            if (i == cc - 1) {
                byte[] data = new byte[bytes.length - (i * 16)];
                System.arraycopy(bytes, i * 16, data, 0, bytes.length - (i * 16));
                mcuMultiData.add(data);
            } else {
                byte[] data = new byte[16];
                System.arraycopy(bytes, i * 16, data, 0, 16);
                mcuMultiData.add(data);
            }
        }
        byte[] lastPack = mcuMultiData.get(mcuMultiData.size() - 1);
        if (lastPack.length == 12) {
            byte[] remove = mcuMultiData.remove(mcuMultiData.size() - 1);
            byte[] data1 = new byte[8];
            byte[] data2 = new byte[4];
            System.arraycopy(lastPack, 0, data1, 0, 8);
            mcuMultiData.add(data1);
            System.arraycopy(lastPack, 8, data2, 0, 4);
            mcuMultiData.add(data2);
        }
        sendMcuData(file);
    }

    private void sendMcuData(File file) {
        CRC16 crc16 = new CRC16();//CRC校验

        if (mcuIndex == 0) {
            ByteBuffer buff = ByteBuffer.allocate(12);
            ByteBuffer buff1 = ByteBuffer.allocate(8);
            buff.put((byte) 0xA1);
            buff.put((byte)0x00);
            int chkSum = BLEByteUtil.calCheckSum2(bytes);
            byte[] bytes6 = BLEByteUtil.intToBytes2(chkSum);
            Log.e(TAG, "MCU长度  McuLengthByteByte:"+ chkSum+",newByte3:"+BLEByteUtil.bytesToHexString(bytes6));
            for (byte aByte : bytes6) {
                buff.put(aByte);
            }
            Log.i(TAG, "file.length():" + file.length());
            buff.put((byte) (file.length() >> 24 & 0xff));
            buff.put((byte) (file.length() >> 16 & 0xff));
            buff.put((byte) (file.length() >> 8 & 0xff));
            buff.put((byte) (file.length() & 0xff));
            byte[] array = buff.array();
            for (int i = 0; i < array.length; i++) {
                if (i >= 2 &&i <10) {
                    buff1.put(array[i]);
                }
            }
            Log.i(TAG, "CRC校验 数据:" + BLEByteUtil.bytesToHexString(buff1.array()));
            byte[] bytes = crc16.calcCRC(buff1.array());
            for (byte aByte : bytes) {
                buff.put(aByte);
            }
            penCommAgent.setMCUUpgrade(new PenCommAgent.MCUPenCmdInterface() {
                @Override
                public void startMCUUpgrade(byte cmd) {
                    Log.i(TAG, "CRC校验 第一包回调:" + BLEByteUtil.bytesToHexString(array)+",,cmd:"+cmd);
                    if (cmd == 6) {
                        handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, (100/mcuMultiData.size())).sendToTarget();
                        mcuIndex++;
                        mcu++;
                        sendMcuData(file);
                    }
                }

                @Override
                public void progressMCUUpgrade(int cmd) {

                }

                @Override
                public void endMCUUpgrade(int type) {

                }

                @Override
                public void openMCUUpgrade() {

                }

                @Override
                public void startBtUpgrade() {

                }
            });
            penCommAgent.ReqOfflineDataMcu(array);
            Log.i(TAG, "CRC校验 第一包数据:" + BLEByteUtil.bytesToHexString(array));
        } else {
            byte[] w = mcuMultiData.get(mcuIndex - 1);
            ByteBuffer mData = ByteBuffer.allocate(w.length+4);
            ByteBuffer buff1 = ByteBuffer.allocate(w.length);
            if (mcu > 0xff) {
                mcu = 0;
            }
            int f;
            if (w.length > 8) {
                f = 0xA2;
            } else if (w.length == 4) {
                f = 0xA0;
            } else {
                f = 0xA1;
            }
            mData.put((byte)f);
            int s = Integer.parseInt(addZero(mcu + ""));
            mData.put((byte)s);
            mData.put(w);
            byte[] array = mData.array();
            for (int i = 0; i < array.length; i++) {
                if (i >= 2 &&i <(array.length-2)) {
                    buff1.put(array[i]);
                }
            }
            //  Log.i(TAG, "CRC校验 数据:" + BLEByteUtil.bytesToHexString(buff1.array())+",,array.length::"+array.length);
            byte[] bytes = crc16.calcCRC(buff1.array());
            for (byte aByte : bytes) {
                mData.put(aByte);
            }
            penCommAgent.setMCUUpgrade(new PenCommAgent.MCUPenCmdInterface() {
                @Override
                public void startMCUUpgrade(byte cmd) {
                    Log.i(TAG, "第二包回调的状态:" + cmd + ",,index:: " + mcuIndex + ",,mcuMultiData.size()===" + mcuMultiData.size()+",比值:"+(mcuIndex*100/mcuMultiData.size()));
                    if (cmd == 6) {
                        handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, (mcuIndex*100/mcuMultiData.size())).sendToTarget();
                        mcu++;
                        if (mcuIndex <= mcuMultiData.size()) {
                            mcuIndex++;
                        }
                        if (mcuIndex - 1 == mcuMultiData.size()) {
                            ByteBuffer mData = ByteBuffer.allocate(1);
                            mData.put((byte) 0x04);
                            penCommAgent.setReqEndOrderCmd(new PenCommAgent.ReqEndOrderCmdInterface() {
                                @Override
                                public void ReqEndOrderCmd(int i) {
                                    Log.i(TAG, "最后结果：：：" + cmd);

                                    if (i == 0) {
                                        mcu = 0;
                                        mcuIndex = 0;
                                        handler.obtainMessage(MESSAGE_UPDATE_TIP,"MCU升级成功").sendToTarget();
                                        penCommAgent.reqPenYModem();////升级完成，开始启动
                                    }  else if (i == 2){
                                        //2 是错误  10暂时未知  10会给两次应该是固件问题
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                showToast("MCU升级失败");
                                            }
                                        });
                                    }
                                }
                            });
                            penCommAgent.ReqOfflineDataMcu(mData.array());
                            mcuIndex += 100;//这样保证不会再进来一次!而是等待回应结束
                        } else if (mcuIndex > mcuMultiData.size()){
                            //进入这里  只接收等待结果
                        } else {
                            sendMcuData(file);
                        }
                    }else {
                        sendMcuData(file);
                    }
                }

                @Override
                public void progressMCUUpgrade(int cmd) {

                }

                @Override
                public void endMCUUpgrade(int type) {

                }

                @Override
                public void openMCUUpgrade() {

                }

                @Override
                public void startBtUpgrade() {

                }
            });
            penCommAgent.ReqOfflineDataMcu(mData.array());
            Log.i(TAG, "CRC校验后 拼接好的数据:" + BLEByteUtil.bytesToHexString(mData.array())+",,,index:"+mcuIndex);
        }
    }


    private ProgressDialog createUpgradeProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);

        dialog.setTitle(R.string.ota_upgrade_progress_dialog_title);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(mMaxProgress);
        dialog.setProgress(0);
        dialog.setCancelable(false);
  /*      dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ByteBuffer mData = ByteBuffer.allocate(1);
                        mData.put((byte) 0x41);
                        penCommAgent.setReqEndOrderCmd(new PenCommAgent.ReqEndOrderCmdInterface() {
                            @Override
                            public void ReqEndOrderCmd(int i) {
                                Log.i(TAG, "dialog最后结果：：：" + i);
                                if (i == 8) {
                                    mcu = 0;
                                    mcuIndex = 0;
                                    if (mUpgradeProgressDialog != null && mUpgradeProgressDialog.isShowing()) {
                                        mUpgradeProgressDialog.dismiss();
                                    }
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.i(TAG, "再次连接蓝牙::");
                                            deviceConnect(App.mDeviceName, App.getInstance().getDeviceAddress());//重连
                                        }
                                    },2*1000);

                                }
                            }
                        });
                        Log.i(TAG, "TTT点击取消::" + BLEByteUtil.bytesToHexString(mData.array()));
                        penCommAgent.ReqOfflineDataMcu(mData.array());

                    }
                });*/

        return dialog;
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

    private boolean isOver;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceDisconnected events) {
        Log.e(TAG, "receivePenStatus : 断开连接 OTA:" + isOTARunning);
        if (mUpgradeProgressDialog != null && mUpgradeProgressDialog.isShowing()) {
            mUpgradeProgressDialog.dismiss();
            if(!isOver){
                handler.obtainMessage(MESSAGE_UPDATE_TIP,"断开连接").sendToTarget();
            }
        }
        if (isOTARunning) {
            //  isOTARunning = false;
//            startConnect(mDevice);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    deviceConnect(App.mDeviceName, App.getInstance().getDeviceAddress());//重连
                }
            },1000);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceConnected events) {
        isOTARunning = false;//再设置为false

    }

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
                SPUtils.put(McuActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, true);
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
                                Intent intent =  new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,packageURI);
                                startActivity(intent);
                            }).setNegativeButton(R.string.cancel, (dialog2, which) ->{
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

    private boolean isDeviceConnected() {
        if (!App.getInstance().isDeviceConnected()) {
            showGoConnectDialog();
            return true;
        }
        return false;
    }

    /**
     * 去连接笔dialog
     */
    private void showGoConnectDialog() {
        if (dialog == null) {
            dialog = new Dialog(McuActivity.this, R.style.customDialog);
        }
        View view = LayoutInflater.from(McuActivity.this).inflate(R.layout.dialog_go_connect, null);
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
                Intent intent = new Intent(McuActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showSuccessDialog() {
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

        tv_title.setText("升级成功");
        tv_msg.setText("完成pen的固件升级");
        tv_msg.setGravity(Gravity.CENTER);

        viewLine.setVisibility(View.GONE);
        tvConfirm.setVisibility(View.GONE);

        tv_confirm_agree.setText("好的");
        ImageView tv_cancel = (ImageView) view.findViewById(R.id.tv_cancel);
        tv_cancel.setVisibility(View.GONE);

        tv_confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceDialog.dismiss();
                Intent intent = new Intent(McuActivity.this, SearchActivity.class);
                startActivity(intent);
                showToast("升级成功");
                finish();
            }
        });
        mServiceDialog.show();
    }

    private void showFailDialog(String str) {
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

        tv_title.setText("升级失败");
        tv_msg.setText("出现异常, 升级pen的固件失败 " +str);
        tv_msg.setGravity(Gravity.CENTER);

        viewLine.setVisibility(View.GONE);
        tvConfirm.setVisibility(View.GONE);

        tv_confirm_agree.setText("好的");
        ImageView tv_cancel = (ImageView) view.findViewById(R.id.tv_cancel);
        tv_cancel.setVisibility(View.GONE);

        tv_confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceDialog.dismiss();
                Intent intent = new Intent(McuActivity.this, SearchActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mServiceDialog.show();
    }

}