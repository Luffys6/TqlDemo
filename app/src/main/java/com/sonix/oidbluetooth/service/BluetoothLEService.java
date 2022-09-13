package com.sonix.oidbluetooth.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.sonix.app.App;
import com.sonix.oidbluetooth.ElementActivity;
import com.tqltech.tqlpencomm.BLEException;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.bean.ElementCode;
import com.tqltech.tqlpencomm.bean.PenStatus;
import com.tqltech.tqlpencomm.listener.TQLPenSignal;
import com.tqltech.tqlpencomm.util.BLELogUtil;

/**
 * 服务接收数据（未使用）
 */
public class BluetoothLEService extends Service {
    private final static String TAG = "BluetoothLEService";
    private String mBluetoothDeviceAddress;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String ACTION_CMD_AVAILABLE ="ACTION_CMD_AVAILABLE";
    public final static String ACTION_OFFLINE_AVAILABLE ="ACTION_OFFLINE_AVAILABLE";
    public final static String ACTION_PEN_STATUS_CHANGE = "ACTION_PEN_STATUS_CHANGE";
    public final static String RECEVICE_DOT = "RECEVICE_DOT";
    public final static String RECEVICE_USB_STATUS = "RECEVICE_USB_STATUS";
    public final static String EXTRA_DATA ="COM.EXAMPLE.SONIXBLESAMPLE.EXTRA_DATA";

    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "DEVICE_DOES_NOT_SUPPORT_UART";
    private PenCommAgent bleManager;
    private boolean isPenConnected = false;
    private Handler handlerThree = new Handler(Looper.getMainLooper());
    private long time;

    public boolean getPenStatus() {
        return isPenConnected;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.

        BLELogUtil.i(TAG, "BluetoothLEService onUnbind");
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        bleManager = PenCommAgent.GetInstance(getApplication());
        bleManager.setTQLPenSignalListener(mPenSignalCallback);

        if (!bleManager.isSupportBluetooth()) {
            Log.e(TAG, "Unable to Support Bluetooth");
            return false;
        }

        if (!bleManager.isSupportBLE()) {
            Log.e(TAG, "Unable to Support BLE.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && bleManager.isConnect(address)) {
            Log.d(TAG, "Trying to use an existing pen for connection.===");
            return true;
        }

        Log.d(TAG, "Trying to create a new connection.");
        boolean flag = bleManager.connect(address);
        if (!flag) {
            Log.i(TAG, "bleManager.connect(address)-----false");
            return false;
        }

        Log.i(TAG, "bleManager.connect(address)-----true");
        return true;
    }

    public void disconnect() {
        BLELogUtil.i(TAG, "BluetoothLEService disconnect");
        bleManager.disconnect(mBluetoothDeviceAddress);
    }

    public void close() {
        if (bleManager == null) {
            return;
        }

        Log.w(TAG, "mBluetoothGatt closed");
        BLELogUtil.i(TAG, "BluetoothLEService close");
        bleManager.disconnect(mBluetoothDeviceAddress);
        mBluetoothDeviceAddress = null;
        bleManager = null;
    }

    /// ===========================================================
    private OnDataReceiveListener onDataReceiveListener = null;

    public interface OnDataReceiveListener {

        void onDataReceive(Dot dot);

        void onOfflineDataReceive(Dot dot);

        void onFinishedOfflineDown(boolean success);

        void onOfflineDataNum(int num);

        void onReceiveOfflineProgress(int i);

        void onReceivePenLED(int color);

        void onWriteCmdResult(int code);

        void onReceivePenType(int type);
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    private TQLPenSignal mPenSignalCallback = new TQLPenSignal() {

        /**********************************************************/
        /****************** part1: 蓝牙连接相关 *******************/
        /**********************************************************/
        @Override
        public void onConnected() {
            Log.i(TAG, "TQLPenSignal had onConnected");
            String intentAction;

            intentAction = ACTION_GATT_CONNECTED;
            broadcastUpdate(intentAction);
            isPenConnected = true;
        }

        @Override
        public void onDisconnected() {
            String intentAction;
            Log.i(TAG, "TQLPenSignal had onDisconnected");
            intentAction = ACTION_GATT_DISCONNECTED;
            broadcastUpdate(intentAction);
            isPenConnected = false;
        }

        @Override
        public void onConnectFailed() {
            String intentAction;
            Log.i(TAG, "TQLPenSignal had onConnectFailed");
            intentAction = ACTION_GATT_DISCONNECTED;
            broadcastUpdate(intentAction);
            isPenConnected = false;
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**********************************************************/
        /****************** part2: 在线数据    *******************/
        /**********************************************************/
        @Override
        public void onReceiveDot(Dot dot) {
            Log.d(TAG, "online:" + dot.toString());
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onDataReceive(dot);
            }
        }

        /**********************************************************/
        /****************** part3: 离线数据    *******************/
        /**********************************************************/
        @Override
        public void onReceiveOfflineStrokes(Dot dot) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onOfflineDataReceive(dot);
            }
        }


        @Override
        public void onOfflineDataList(int offlineNotes) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onOfflineDataNum(offlineNotes);
            }
        }

        @Override
        public void onStartOfflineDownload(final boolean isSuccess) {
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "StartOffline-->" + isSuccess, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStopOfflineDownload(boolean isSuccess) {

        }

        @Override
        public void onPenPauseOfflineDataTransferResponse(boolean isSuccess) {
            Log.i(TAG, "onPenPauseOfflineDataTransferResponse: " + isSuccess);
        }

        @Override
        public void onPenContinueOfflineDataTransferResponse(final boolean isSuccess) {
            Log.i(TAG, "onPenContinueOfflineDataTransferResponse: " + isSuccess);
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "ContinueOffline-->" + isSuccess, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onFinishedOfflineDownload(boolean isSuccess) {
            Log.i(TAG, "-------offline download success-------");
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onFinishedOfflineDown(isSuccess);
            }
        }

        @Override
        public void onReceiveOfflineProgress(int i) {
            Log.i(TAG, "onReceiveOfflineProgress----" + i);
            synchronized (this) {
                if (onDataReceiveListener != null) {
                    onDataReceiveListener.onReceiveOfflineProgress(i);
                }
            }
        }

        @Override
        public void onPenDeleteOfflineDataResponse(boolean isSuccess) {

        }


        /**********************************************************/
        /****************** part4: 请求的回复   *******************/
        /**********************************************************/
        @Override
        public void onReceivePenAllStatus(PenStatus status) {
            App.mBattery = status.mPenBattery;
            App.mUsedMem = status.mPenMemory;
            App.mTimer = status.mPenTime;
            Log.e(TAG, "ApplicationResources.mTimer is " + App.mTimer + ", status is " + status.toString());
            App.mPowerOnMode = status.mPenPowerOnMode;
            App.mPowerOffTime = status.mPenAutoOffTime;
            App.mBeep = status.mPenBeep;
            App.mPenSens = status.mPenSensitivity;
            Log.e(TAG, "status.mPenEnableLed" + status.mPenEnableLed);
            App.tmp_mEnableLED = status.mPenEnableLed;

            App.mPenName = status.mPenName;
            App.mBTMac = status.mPenMac;
            App.mFirmWare = status.mBtFirmware;
            App.mMCUFirmWare = status.mPenMcuVersion;
            App.mCustomerID = status.mPenCustomer;

            App.mTwentyPressure = status.mPenTwentyPressure;
            App.mThreeHundredPressure = status.mPenThirdPressure;

            String intentAction = ACTION_PEN_STATUS_CHANGE;
            broadcastUpdate(intentAction);
        }

        @Override
        public void onReceivePenMac(String penMac) {
            Log.e(TAG, "receive pen Mac " + penMac);
            mBluetoothDeviceAddress = penMac;
            App.mBTMac = penMac;
        }

        @Override
        public void onReceivePenName(String penName) {

        }

        @Override
        public void onReceivePenBtFirmware(String penBtFirmware) {

        }

        @Override
        public void onReceivePenTime(long penTime) {
            App.mTimer = penTime;
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            broadcastUpdate(intentAction);
        }

        @Override
        public void onReceivePenBattery(int penBattery, boolean bIsCharging) {
            Log.e(TAG, "receive pen battery is " + penBattery);
        }

        @Override
        public void onReceivePenMemory(int penMemory) {

        }

        @Override
        public void onReceivePenAutoPowerOnModel(boolean bIsOn) {

        }

        @Override
        public void onReceivePenBeepModel(boolean bIsOn) {

        }

        @Override
        public void onReceivePenAutoOffTime(int autoOffTime) {

        }

        @Override
        public void onReceivePenMcuVersion(String penMcuVersion) {

        }

        @Override
        public void onReceivePenCustomer(String penCustomerID) {

        }

        @Override
        public void onReceivePenSensitivity(int penSensitivity) {

        }

        @Override
        public void onReceivePenTypeInt(int penType) {
            App.mPenType = penType;
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceivePenType(penType);
            }
        }

        @Override
        public void onReceivePenType(String penType) {

        }

        @Override
        public void onReceivePenDataType(byte penDataType) {

        }

        @Override
        public void onReceivePenDotType(int penDotType) {
            BLELogUtil.d(TAG, "NOTIFY_PEN_DOTTYPE :" + penDotType);
        }

        @Override
        public void onReceivePenLedConfig(int penLedConfig) {
            Log.e(TAG, "receive hand write color is " + penLedConfig);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceivePenLED(penLedConfig);
            }
        }

        @Override
        public void onReceivePenEnableLed(boolean bEnableFlag) {

        }

        @Override
        public void onReceivePresssureValue(int minPressure, int maxPressure) {

        }


        /**********************************************************/
        /****************** part5: 设置的回复   *******************/
        /**********************************************************/
        @Override
        public void onPenNameSetupResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                App.mPenName = App.tmp_mPenName;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "设置名字成功", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onPenTimetickSetupResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                App.mTimer = App.tmp_mTimer;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "设置RTC时间成功", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onPenAutoShutdownSetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                App.mPowerOffTime = App.tmp_mPowerOffTime;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "设置自动关机时间成功", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onPenFactoryResetSetUpResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenAutoPowerOnSetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                App.mPowerOnMode = App.tmp_mPowerOnMode;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenBeepSetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                App.mBeep = App.tmp_mBeep;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenSensitivitySetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                App.mPenSens = App.tmp_mPenSens;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "设置灵敏度成功", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onPenLedConfigResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenDotTypeResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenWriteCustomerIDResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenChangeLedColorResponse(boolean bIsSuccess) {

        }


        /**********************************************************/
        /****************** part6: 上报   *******************/
        /**********************************************************/
        @Override
        public void onReceiveElementCode(ElementCode elementCode, long index) {
            Log.e(TAG, "onReceiveOIDFormat---> " + elementCode);
            //if (onDataReceiveListener != null) {
            //    onDataReceiveListener.onReceiveOIDSize( penOIDSize);
            //}
            long now = System.currentTimeMillis();
            if (now - time > 1000) {
                String intentAction;
                intentAction = ACTION_PEN_STATUS_CHANGE;
                broadcastUpdate(intentAction);
                Intent intent = new Intent(getBaseContext(), ElementActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                //intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra("value", elementCode.index);
                startActivity(intent);
                time = now;
            }
        }

        @Override
        public void onReceivePenHandwritingColor(int color) {
            Log.e(TAG, "receive hand write color is " + color);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceivePenLED(color);
            }
        }


        /**********************************************************/
        /****************** part7: 其它   *******************/
        /**********************************************************/
        @Override
        public void onWriteCmdResult(final int code) {
            Log.i(TAG, "onWriteCmdResult: " + code);
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    if (code != 0) {
                        Toast.makeText(getApplicationContext(), "WriteCmdResult :" + code, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            if (onDataReceiveListener != null) {
                onDataReceiveListener.onWriteCmdResult(code);
            }
        }

        @Override
        public void onException(final BLEException exception) {
            handlerThree.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "onException :" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }

        /**
         * 读取mcu测试版本，
         *
         * @param mcuTest
         * @param var2    是否是415笔
         * @param var3    415专属boot版本
         */
        @Override
       public void onReceivePenMcuTestCode(String mcuTest, boolean var2, String var3) {

       }
       @Override
       public void onReceivePenFlashType(int type) {

       }

        @Override
        public void onReceivePenBuzzerBuzzes(boolean buzzerBuzzes) {

        }

        /**
         * 厂测  双指令回调
         *
         * @param var1
         */
        @Override
        public void onReceivePenBothCommandData(byte[] var1) {

        }

        /**
         * 413 无效码读取
         *
         * @param var1
         */
        @Override
        public void onReceiveInvalidCodeReportingRange(byte[] var1) {

        }

        @Override
        public void onReceiveInvalidSetCode(boolean b) {

        }

        @Override
        public void onReceiveInvalidReqCode(boolean b) {

        }

        //@Override
        public void onReceiveUsbStatus(int status) {

        }
    };
}

