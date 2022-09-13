package com.sonix.ota;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * 111笔升级
 */
public class OtaUpgrader {
    public static final UUID UUID_UPGRADE_SERVICE = UUID.fromString("9e5d1e47-5c13-43a0-8635-82ad38a1386f");
    public static final UUID UUID_UPGRADE_CONTROL_POINT = UUID.fromString("e3dd50bf-f7a7-4e99-838e-570a086c666b");
    public static final UUID UUID_UPGRADE_DATA = UUID.fromString("92e86c7a-d961-4091-b74f-2409e72efe36");
    public static final UUID UUID_UPGRADE_APP_INFO = UUID.fromString("347f7608-2e2d-47eb-913b-75d4edc4de3b");
    public static final UUID UUID_CLIENT_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int STATUS_OK = 0;
    public static final int STATUS_UNSUPPORTED_COMMAND = 1;
    public static final int STATUS_ILLEGAL_STATE = 2;
    public static final int STATUS_VERIFICATION_FAILED = 3;
    public static final int STATUS_INVALID_IMAGE = 4;
    public static final int STATUS_INVALID_IMAGE_SIZE = 5;
    public static final int STATUS_MORE_DATA = 6;
    public static final int STATUS_INVALID_APPID = 7;
    public static final int STATUS_INVALID_VERSION = 8;

    public static final int STATUS_DISCONNECT = 9;
    public static final int STATUS_ABORT = 10;
    public static final int STATUS_TIMEOUT = 11;
    public static final int STATUS_INVALID_DEVICE_ADDRESS = 12;
    public static final int STATUS_INVALID_FILE_PATH = 13;
    public static final int STATUS_IO_ERROR = 14;
    public static final int STATUS_NOT_BOND = 15;
    public static final int STATUS_UNKNOWN = 0xFF;

    public static final int COMMAND_PREPARE_DOWNLOAD = 1;
    public static final int COMMAND_DOWNLOAD = 2;
    public static final int COMMAND_VERIFY = 3;
    public static final int COMMAND_FINISH = 4;
    public static final int COMMAND_GET_STATUS = 5;
    public static final int COMMAND_CLEAR_STATUS = 6;
    public static final int COMMAND_ABORT = 7;

    private static final String TAG = "OtaUpgrader";
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_DATA = (DEBUG & false);
    private static final boolean DEBUG_CHAR = (DEBUG & false);

    private static final int DATE_BLOCK_SIZE = 20;
    private static final byte[] ENABLE_NOTIFICATION_VALUE = {0x03, 0x00};

    private Context mContext;
    private Callback mCallback;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mControlPointCharacteristic;
    private BluetoothGattCharacteristic mDataCharacteristic;

    private StateMachine mStateMachine;

    private String mDeviceAddress;
    private String mPatchFilePath;
    private int mPatchSize;
    private int mOffset;
    private CRC32 mCrc32;

    private BufferedInputStream mInputStream;

    private static final int EVENT_CONNECTED = 1;
    private static final int EVENT_DISCONNECTED = 2;
    private static final int EVENT_NOTIFY_SENT = 3;
    private static final int EVENT_DATA_SENT = 4;
    private static final int EVENT_COMMAND_SENT = 5;
    private static final int EVENT_ABORT = 6;
    private static final int EVENT_TIMEOUT = 7;

    public interface Callback {
        public void onProgress(int realSize, int precent);

        public void onFinish(int status);
    }

    public OtaUpgrader(Context context) {
        this(context, null, null, null);
    }

    public OtaUpgrader(Context context, String deviceAddress, String patchFilePath, Callback callback) {
        mContext = context;
        mDeviceAddress = deviceAddress;
        mPatchFilePath = patchFilePath;
        mCallback = callback;
    }

    public void setDeviceAddress(String deviceAddress) {
        mDeviceAddress = deviceAddress;
    }

    public void setPatchFilePath(String patchFilePath) {
        mPatchFilePath = patchFilePath;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void start() {
        if (DEBUG) {
            Log.i(TAG, "start()");
        }

        if (mStateMachine == null) {
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();

            Looper looper = handlerThread.getLooper();
            mStateMachine = new StateMachine(looper);
            mStateMachine.start();
        }
    }

    public void stop() {
        if (DEBUG) {
            Log.i(TAG, "stop()");
        }

        if (mStateMachine != null) {
            mStateMachine.stop();
        }
    }

    public int getPatchSize() {
        File patchFile = new File(mPatchFilePath);

        return (int) patchFile.length();
    }

    private int connect() {
        if (DEBUG) {
            Log.i(TAG, "connect()");
        }

        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "connect(), bluetoothManager = null");
            return STATUS_UNKNOWN;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "connect(), bluetoothAdapter = null");
            return STATUS_UNKNOWN;
        }

        if (DEBUG) {
            Log.i(TAG, "connect(), mDeviceAddress = " + mDeviceAddress);
        }

        if (mDeviceAddress == null) {
            return STATUS_INVALID_DEVICE_ADDRESS;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);

        if (device == null) {
            Log.e(TAG, "connect(), device = null");
            return STATUS_INVALID_DEVICE_ADDRESS;
        }

        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);

        if (mBluetoothGatt == null) {
            Log.e(TAG, "connect(), mBluetoothGatt = null");
            return STATUS_UNKNOWN;
        }

        return STATUS_OK;
    }

    private int handleConnected() {
        if (DEBUG) {
            Log.i(TAG, "handleConnected()");
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID_UPGRADE_SERVICE);

        if (gattService == null) {
            Log.e(TAG, "handleConnected(), gattService = null");
            return STATUS_UNKNOWN;
        }

        mControlPointCharacteristic = gattService.getCharacteristic(UUID_UPGRADE_CONTROL_POINT);
        mDataCharacteristic = gattService.getCharacteristic(UUID_UPGRADE_DATA);

        if ((mControlPointCharacteristic == null) ||
                (mDataCharacteristic == null)) {
            Log.e(TAG, "handleConnected(), mControlPointCharacteristic = " + mControlPointCharacteristic);
            Log.e(TAG, "handleConnected(), mDataCharacteristic = " + mDataCharacteristic);
            return STATUS_UNKNOWN;
        }

        return STATUS_OK;
    }

    private void disconnect() {
        if (DEBUG) {
            Log.i(TAG, "disconnect()");
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        mControlPointCharacteristic = null;
        mDataCharacteristic = null;
    }

    private int enableNotification() {
        if (DEBUG) {
            Log.i(TAG, "enableNotification()");
        }

        int status = STATUS_UNKNOWN;
        boolean result = mBluetoothGatt.setCharacteristicNotification(mControlPointCharacteristic, true);

        if (result) {
            BluetoothGattDescriptor clientConfig = mControlPointCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);

            if ((clientConfig != null) &&
                    clientConfig.setValue(ENABLE_NOTIFICATION_VALUE) &&
                    mBluetoothGatt.writeDescriptor(clientConfig)) {
                status = STATUS_OK;
            }
        }

        return status;
    }

    private int sendCommand(int command) {
        byte value[] = {(byte) (command & 0xFF)};

        return sendCommand(value);
    }

    private int sendCommand(int command, short param) {
        byte value[] = {(byte) (command & 0xFF), (byte) (param & 0xFF), (byte) ((param >> 8) & 0xFF)};

        return sendCommand(value);
    }

    private int sendCommand(int command, int param) {
        byte value[] = {(byte) (command & 0xFF), (byte) (param & 0xFF), (byte) ((param >> 8) & 0xFF), (byte) ((param >> 16) & 0xFF), (byte) ((param >> 24) & 0xFF)};

        return sendCommand(value);
    }

    private int sendCommand(byte[] value) {
        if (DEBUG) {
            Log.i(TAG, "sendCommand(), value = " + Arrays.toString(value));
        }
        int status = STATUS_UNKNOWN;

        if (mControlPointCharacteristic.setValue(value) &&
                mBluetoothGatt.writeCharacteristic(mControlPointCharacteristic)) {
            status = STATUS_OK;
        }

        return status;
    }

    private int sendUpgradeData(byte[] value) {
        if (DEBUG_DATA) {
            Log.i(TAG, "sendUpgradeData(), value = " + Arrays.toString(value));
        }

        int status = STATUS_UNKNOWN;

        if (mDataCharacteristic.setValue(value) &&
                mBluetoothGatt.writeCharacteristic(mDataCharacteristic)) {
            status = STATUS_OK;
        }

        return status;
    }

    private int initDataTransfer() {
        if (DEBUG) {
            Log.i(TAG, "initDataTransfer(), mPatchFilePath = " + mPatchFilePath);
        }

        int status = STATUS_OK;

        if (mPatchFilePath == null) {
            status = STATUS_INVALID_FILE_PATH;
        } else {
            try {
                File patchFile = new File(mPatchFilePath);
                FileInputStream fileInputStream = new FileInputStream(patchFile);

                mInputStream = new BufferedInputStream(fileInputStream);
            } catch (Exception e) {
                Log.e(TAG, "initDataTransfer(), e = " + e);
                status = STATUS_INVALID_FILE_PATH;
            }
        }

        mOffset = 0;
        mCrc32 = new CRC32();

        return status;
    }

    private void deinitDataTransfer() {
        if (DEBUG) {
            Log.i(TAG, "deinitDataTransfer()");
        }

        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
        } finally {
            mInputStream = null;
        }
    }

    private int transferDataBlock() {
        if (DEBUG) {
            Log.i(TAG, "transferDataBlock()");
        }

        int ret = STATUS_OK;

        try {
            int length = mPatchSize - mOffset;
            if (length > DATE_BLOCK_SIZE) {
                length = DATE_BLOCK_SIZE;
            }

            byte[] data = new byte[length];

            mInputStream.read(data, 0, length);
            mCrc32.update(data, length);
            mOffset += length;
            ret = sendUpgradeData(data);
        } catch (IOException e) {
            Log.i(TAG, "transferDataBlock(), e = " + e);
            ret = STATUS_IO_ERROR;
        }

        return ret;
    }

    public String ByteArrayToString(byte[] bt_ary) {

        StringBuilder sb = new StringBuilder();

        if (bt_ary != null)

            for (byte b : bt_ary) {

                sb.append(String.format("%02X ", b));

            }

        return sb.toString();

    }

    private int convertGattStatus(int gattStatus) {
        int status;

        switch (gattStatus) {
            case BluetoothGatt.GATT_SUCCESS:
                status = STATUS_OK;
                break;

            default:
                status = STATUS_UNKNOWN;
                break;
        }

        return status;
    }

    private void handleProgress() {
        if (DEBUG) {
            Log.i(TAG, "handleProgress(), mOffset = " + mOffset + ", mPatchSize = " + mPatchSize);
        }

        if (mCallback != null) {
            int precent = (mOffset * 100) / mPatchSize;

            mCallback.onProgress(mOffset, precent);
        }
    }

    private void handleFinish(int status) {
        if (DEBUG) {
            Log.i(TAG, "handleFinish(), status = " + status);
        }

        disconnect();

        if (mCallback != null) {
            mCallback.onFinish(status);
        }

        mStateMachine = null;
    }

    private final class CRC32 {
        private static final int POLYNOMIAL = 0x04C11DB7;
        private static final int WIDTH = 32;
        private static final int MSB_BIT = (1 << (WIDTH - 1));
        private static final int INITIAL_REMAINDER = 0xFFFFFFFF;
        private static final int FINAL_XOR_VALUE = 0xFFFFFFFF;

        private int mCrc32;

        public CRC32() {
            mCrc32 = INITIAL_REMAINDER;
        }

        public void update(byte[] buffer, int nBytes) {
            // Perform modulo-2 division, a byte at a time.
            for (int i = 0; i < nBytes; i++) {
                // Bring the next byte into the crc32.
                mCrc32 ^= (reflectData(buffer[i]) << (WIDTH - 8));
                // Perform modulo-2 division, a bit at a time.
                for (int j = 8; j > 0; j--) {
                    // Try to divide the current data bit.
                    if ((mCrc32 & MSB_BIT) == 0) {
                        mCrc32 = (mCrc32 << 1);
                    } else {
                        mCrc32 = (mCrc32 << 1) ^ POLYNOMIAL;
                    }
                }
            }
        }

        public int getValue() {
            int crc32 = (reflectReminder(mCrc32) ^ FINAL_XOR_VALUE);
            return crc32;
        }

        private int reflectData(int data) {
            return reflect(data, 8);
        }

        private int reflectReminder(int data) {
            return reflect(data, WIDTH);
        }

        private int reflect(int data, int nBits) {
            int reflection = 0x00000000;

            // Reflect the data about the center bit.
            for (int i = 0; i < nBits; i++) {
                // If the LSB bit is set, set the reflection of it.
                if ((data & 0x01) == 0x01) {
                    reflection |= (1 << ((nBits - 1) - i));
                }

                data = (data >> 1);
            }

            return reflection;
        }
    }

    private final class StateMachine extends Handler {
        private final State STATE_IDLE = new IdleState();
        private final State STATE_CONNECTING = new ConnectingState();
        private final State STATE_ENABLE_NOTIFICATION = new EnableNotificationState();
        private final State STATE_PREPRARE_DOWNLOAD = new PrepareDownloadState();
        private final State STATE_READY_FOR_DOWNLOAD = new ReadyForDownloadState();
        private final State STATE_DATA_TRANSFER = new DataTransferState();
        private final State STATE_VERIFICATION = new VerificationState();
        private final State STATE_FINISH = new FinishState();

        private static final int MSG_TRANSITION_TO = 1;
        private static final int MSG_PROCESS_EVENT = 2;
        private static final int MSG_TIMEOUT = 3;
        private static final int MSG_QUIT = 4;

        private static final int STATE_TIMEOUT = 10 * 1000;

        private boolean mIsRunning = false;
        private State mCurrState = STATE_IDLE;

        private class State {
            private int mStatus;

            protected State() {
            }

            public void setStatus(int status) {
                mStatus = status;
            }

            public int getStatus() {
                return mStatus;
            }

            public void enter() {
                Log.i(TAG, "processEvent: 2 startTimeout");
                startTimeout();
            }

            public void exit() {
                stopTimeout();
            }

            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_DISCONNECTED:
                    case EVENT_TIMEOUT:
                    case EVENT_ABORT:
                        transitionTo(STATE_FINISH, status);
                        break;

                    default:
                        break;
                }
            }
        }

        private final class IdleState extends State {
            public void enter() {
                quit();
            }

            @Override
            public void processEvent(int event, int status) {
                //Do nothing in Idle state
            }

            @Override
            public void exit() {
                //Do nothing in Idle state
            }

            @Override
            public String toString() {
                return "IdleState";
            }
        }

        private final class ConnectingState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = connect();

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_CONNECTED: {
                        if (status == STATUS_OK) {
                            status = handleConnected();
                        }

                        if (status == STATUS_OK) {
                            transitionTo(STATE_ENABLE_NOTIFICATION, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "ConnectingState";
            }
        }

        private final class EnableNotificationState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = enableNotification();

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_NOTIFY_SENT: {
                        if (status == STATUS_OK) {
                            transitionTo(STATE_PREPRARE_DOWNLOAD, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "EnableNotificationState";
            }
        }

        private final class PrepareDownloadState extends State {
            @Override
            public void enter() {
                super.enter();

                mPatchSize = getPatchSize();
                int status = sendCommand(COMMAND_PREPARE_DOWNLOAD, (int) mPatchSize);

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_COMMAND_SENT: {
                        if (status == STATUS_OK) {
                            transitionTo(STATE_READY_FOR_DOWNLOAD, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "PrepareDownloadState";
            }
        }

        private final class ReadyForDownloadState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = sendCommand(COMMAND_DOWNLOAD);

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_COMMAND_SENT: {
                        if (status == STATUS_OK) {
                            transitionTo(STATE_DATA_TRANSFER, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "ReadyForDownloadState";
            }
        }

        private final class DataTransferState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = initDataTransfer();

                if (status == STATUS_OK) {
                    status = transferDataBlock();
                }

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_DATA_SENT: {
                        stopTimeout();

                        if (status == STATUS_OK) {
                            if (mPatchSize == mOffset) {
                                transitionTo(STATE_VERIFICATION, status);
                            } else {
                                Log.i(TAG, "processEvent: 1 startTimeout");
                                startTimeout();
                                status = transferDataBlock();
                            }
                        }

                        if (status == STATUS_OK) {
                            handleProgress();
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public void exit() {
                deinitDataTransfer();
            }

            @Override
            public String toString() {
                return "DataTransferState";
            }
        }

        private final class VerificationState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = sendCommand(COMMAND_VERIFY, mCrc32.getValue());

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_COMMAND_SENT: {
                        transitionTo(STATE_FINISH, status);
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "VerificationState";
            }
        }

        private final class FinishState extends State {
            @Override
            public void enter() {
                transitionTo(STATE_IDLE, STATUS_OK);
            }

            @Override
            public void exit() {
                int status = getStatus();

                handleFinish(status);
            }

            @Override
            public String toString() {
                return "FinishState";
            }
        }

        public StateMachine(Looper looper) {
            super(looper);
        }

        public void start() {
            if (!mIsRunning) {
                mIsRunning = true;
                transitionTo(STATE_CONNECTING, STATUS_OK);
            }
        }

        public void stop() {
            postEvent(EVENT_ABORT, STATUS_ABORT);
        }

        public void quit() {
            Message msg = obtainMessage(MSG_QUIT);
            sendMessage(msg);
        }

        public void postEvent(int event, int status) {
            Message msg = obtainMessage(MSG_PROCESS_EVENT, event, status);
            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage: msg=" + msg.what);
            switch (msg.what) {
                case MSG_TRANSITION_TO: {
                    State state = (State) msg.obj;
                    int status = msg.arg1;

                    handleTransitionTo(state, status);
                    break;
                }

                case MSG_PROCESS_EVENT: {
                    handleProcessEvent(msg.arg1, msg.arg2);
                    break;
                }

                case MSG_TIMEOUT: {
                    handleProcessEvent(EVENT_TIMEOUT, STATUS_TIMEOUT);
                    break;
                }

                case MSG_QUIT: {
                    handleQuit();
                    break;
                }

                default:
                    break;
            }
        }

        private void transitionTo(State state, int status) {
            if (DEBUG) {
                Log.i(TAG, "transitionTo state = " + state);
            }

            Message msg = obtainMessage(MSG_TRANSITION_TO);
            msg.arg1 = status;
            msg.obj = state;
            sendMessageDelayed(msg, 200);
        }

        private void startTimeout() {
            Message msg = obtainMessage(MSG_TIMEOUT);
            sendMessageDelayed(msg, STATE_TIMEOUT);
        }

        private void stopTimeout() {
            removeMessages(MSG_TIMEOUT);
        }

        private void handleTransitionTo(State state, int status) {
            if (DEBUG) {
                Log.i(TAG, "handleTransitionTo mCurrState = " + mCurrState + ", state = " + state);
            }

            mCurrState.exit();
            mCurrState = state;
            mCurrState.setStatus(status);
            mCurrState.enter();
        }

        private void handleProcessEvent(int event, int status) {
            if (DEBUG) {
                Log.i(TAG, "handleProcessEvent mCurrState = " + mCurrState + ", event = " + event + ", status = " + status);
            }

            if (mCurrState != null) {
                mCurrState.processEvent(event, status);
            }
        }

        private void handleQuit() {
            getLooper().quit();
            mStateMachine = null;
            mIsRunning = false;
        }
    }

    ;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (DEBUG) {
                Log.i(TAG, "onConnectionStateChange(), status = " + status + ", newState = " + newState);
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mStateMachine.postEvent(EVENT_DISCONNECTED, STATUS_DISCONNECT);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (DEBUG) {
                Log.i(TAG, "onServicesDiscovered(), status = " + status);
            }

            int ret = convertGattStatus(status);
            mStateMachine.postEvent(EVENT_CONNECTED, ret);
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();

            if (DEBUG_CHAR) {
                Log.i(TAG, "onCharacteristicChanged(), uuid = " + uuid);
            }

            if (uuid.equals(UUID_UPGRADE_CONTROL_POINT)) {
                byte[] value = characteristic.getValue();
                int status = value[0];
                mStateMachine.postEvent(EVENT_COMMAND_SENT, status);
            }
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            UUID uuid = characteristic.getUuid();

            if (DEBUG_CHAR) {
                Log.i(TAG, "onCharacteristicWrite(), uuid = " + uuid + ", status = " + status);
            }

            if (uuid.equals(UUID_UPGRADE_DATA)) {
                int ret = convertGattStatus(status);
                mStateMachine.postEvent(EVENT_DATA_SENT, ret);
            }
        }

        @Override
        public void onDescriptorWrite(
                BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor,
                int status) {
            UUID uuid = descriptor.getUuid();

            if (DEBUG_CHAR) {
                Log.i(TAG, "onDescriptorWrite(), uuid = " + uuid + ", status = " + status);
            }

            if (uuid.equals(UUID_CLIENT_CONFIGURATION)) {
                int ret = convertGattStatus(status);
                mStateMachine.postEvent(EVENT_NOTIFY_SENT, ret);
            }
        }
    };
}
