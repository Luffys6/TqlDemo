package com.sonix.base;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshFooterCreator;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator;
import com.sonix.app.App;
import com.sonix.oidbluetooth.BoardActivity;
import com.sonix.oidbluetooth.MainActivity;
import com.sonix.util.Events;
import com.sonix.util.MultiLanguageUtils;
import com.tqltech.tqlpencomm.BLEException;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.bean.ElementCode;
import com.tqltech.tqlpencomm.bean.PenStatus;
import com.tqltech.tqlpencomm.listener.TQLPenSignal;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * 与笔交互，实现笔回调
 */
public abstract class BaseApplication extends Application implements TQLPenSignal {
    private static final String TAG = "BaseApplication";
    public static String SP_LANGUAGE = "language";
    public static String SP_COUNTRY = "country";

    private Handler mHandler;
    private PenCommAgent mPenAgent;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceBattery;
    private int mDeviceType;
    private boolean mDeviceConnected;
    //点读码计时
    private long tmpTime;

    //static 代码段可以防止内存泄露
    static {
        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator(new DefaultRefreshHeaderCreator() {
            @Override
            public RefreshHeader createRefreshHeader(Context context, RefreshLayout layout) {
                //layout.setPrimaryColorsId(R.color.colorPrimary, android.R.color.white);//全局设置主题颜色
                return new ClassicsHeader(context);//.setTimeFormat(new DynamicTimeFormat("更新于 %s"));//指定为经典Header，默认是 贝塞尔雷达Header
            }
        });
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator(new DefaultRefreshFooterCreator() {
            @Override
            public RefreshFooter createRefreshFooter(Context context, RefreshLayout layout) {
                //指定为经典Footer，默认是 BallPulseFooter
                return new ClassicsFooter(context).setDrawableSize(20);
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //多语言设置
        registerActivityLifecycleCallbacks(MultiLanguageUtils.callbacks);

        mHandler = new Handler();
        // SmartPen
        mPenAgent = PenCommAgent.GetInstance(this);
        mPenAgent.setTQLPenSignalListener(this);
        mPenAgent.setReConnectStatus(false);
    }


    @Override
    protected void attachBaseContext(Context base) {
        //系统语言等设置发生改变时会调用此方法，需要要重置app语言
        super.attachBaseContext(MultiLanguageUtils.attachBaseContext(base));
    }

    private ArrayList<BaseActivity> activities = new ArrayList<>();

    public void activityCreate(BaseActivity activity) {
        activities.add(activity);
    }

    public void activityResume(BaseActivity activity) {
        activities.remove(activity);
        activities.add(activity);
    }

    public void activityDestroy(BaseActivity activity) {
        activities.remove(activity);
    }

    public void activityClear() {
        for (BaseActivity activity : activities) {
            activity.dialogClear();
            activity.finish();
        }
    }

    public boolean activityNotTop(BaseActivity activity) {
        int size = activities.size();
        if (size == 0)
            return true;
        return activity != activities.get(size - 1);
    }


    public final void deviceConnect(String name, String address) {
        mDeviceConnected = false;
        EventBus.getDefault().post(new Events.DeviceConnecting(name, address));
        if (address.equals(mDeviceAddress) && mPenAgent.isConnect(address)) {
            mDeviceConnected = true;
            EventBus.getDefault().post(new Events.DeviceConnected());
            return;
        }
        if (mPenAgent.isConnect(mDeviceAddress))
            mPenAgent.disconnect(mDeviceAddress);
        mDeviceName = TextUtils.isEmpty(name) ? "SmartPen" : name;
        mDeviceAddress = address;
        mPenAgent.connect(mDeviceAddress);
        // Log.i(TAG, "deviceConnect: " + mDeviceAddress);
        //E0:9F:2A:B2:68:8D
    }

    public final void deviceDisConnect() {
        mDeviceConnected = false;
        mPenAgent.disconnect(mDeviceAddress);
    }

    public final String getDeviceName() {
        return mDeviceName;
    }

    public final String getDeviceAddress() {
        return mDeviceAddress;
    }

    public final String getDeviceBattery() {
        return mDeviceBattery;
    }

    public final int getDeviceType() {
        return mDeviceType;
    }

    public final boolean isConnected() {
        return mPenAgent.isConnect(mDeviceAddress);
    }

    public boolean isDeviceConnected() {
        return mDeviceConnected;
    }


    @Override
    public void onConnected() {
        mDeviceConnected = true;
        EventBus.getDefault().post(new Events.DeviceConnected());
    }

    @Override
    public void onDisconnected() {
        mDeviceConnected = false;
        EventBus.getDefault().post(new Events.DeviceDisconnected());
        //Log.i(TAG, "onDisconnected: ");
    }

    @Override
    public void onConnectFailed() {
        mDeviceConnected = false;
        EventBus.getDefault().post(new Events.DeviceDisconnected());
        Log.i(TAG, "onConnectFailed: ");
    }

    @Override
    public void onReceiveDot(Dot dot) {
//        if (dot.OwnerID == 201) { //BookId == 63
//            EventBus.getDefault().postSticky(new Events.ReceiveDot(dot));
//
//            Intent intent = new Intent(this, BoardActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        } else {
//            EventBus.getDefault().post(new Events.ReceiveDot(dot));
//
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        }
//        if (dot.BookID == 168) { //BookId == 63
//            EventBus.getDefault().postSticky(new Events.ReceiveDot(dot,true));
//
//            Intent intent = new Intent(this, BoardActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        } else {
            EventBus.getDefault().post(new Events.ReceiveDot(dot,true));

            if(!App.isNoGoToMain){
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

//        }
    }

    @Override
    public void onReceiveOfflineStrokes(Dot dot) {
        Log.i(TAG, "onReceiveOfflineStrokes: ");
        EventBus.getDefault().post(new Events.ReceiveDot(dot,false));
    }

    @Override
    public void onOfflineDataList(int i) {
        EventBus.getDefault().post(new Events.ReceiveOffline(i));
    }

    @Override
    public void onStartOfflineDownload(boolean b) {

    }

    @Override
    public void onStopOfflineDownload(boolean b) {

    }

    @Override
    public void onPenPauseOfflineDataTransferResponse(boolean b) {

    }

    @Override
    public void onPenContinueOfflineDataTransferResponse(boolean b) {

    }

    @Override
    public void onFinishedOfflineDownload(boolean b) {
        Log.i(TAG, "onFinishedOfflineDownload: ");
        EventBus.getDefault().post(new Events.ReceiveOfflineProgress(true, 100));
    }

    @Override
    public void onReceiveOfflineProgress(int i) {
        Log.i(TAG, "onReceiveOfflineProgress: ");
        EventBus.getDefault().post(new Events.ReceiveOfflineProgress(false, i));
    }

    @Override
    public void onPenDeleteOfflineDataResponse(boolean b) {
        EventBus.getDefault().post(new Events.ReceiveOfflineDeleteStatus(b));
    }

    @Override
    public void onPenNameSetupResponse(boolean b) {
        Log.i(TAG, "onPenNameSetupResponse: " + b);
        //mPenAgent.getPenName();
        EventBus.getDefault().post(new Events.ReceiveSetNameResponse(b));
    }

    @Override
    public void onReceivePenName(String s) {
        Log.i(TAG, "onReceivePenName: " + s);
        mDeviceName = s;
        App.mPenName = s;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenMac(String s) {
        App.mBTMac = s;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenBtFirmware(String s) {
        App.mFirmWare = s;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenBattery(int i, boolean b) {
        mDeviceBattery = i + "%";
        App.mBattery = i;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenTimetickSetupResponse(boolean b) {

    }

    @Override
    public void onReceivePenTime(long l) {
        App.mTimer = l;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenAutoShutdownSetUpResponse(boolean b) {

    }

    @Override
    public void onReceivePenAutoOffTime(int i) {
        App.mPowerOffTime = i;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenFactoryResetSetUpResponse(boolean b) {

    }

    @Override
    public void onReceivePenMemory(int i) {
        App.mUsedMem = i;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenAutoPowerOnSetUpResponse(boolean b) {

    }

    @Override
    public void onReceivePenAutoPowerOnModel(boolean b) {
        App.mPowerOnMode = b;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenBeepSetUpResponse(boolean b) {

    }

    @Override
    public void onReceivePenBeepModel(boolean b) {
        App.mBeep = b;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenSensitivitySetUpResponse(boolean b) {

    }

    @Override
    public void onReceivePenSensitivity(int i) {
        App.mPenSens = i;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenLedConfigResponse(boolean b) {

    }

    @Override
    public void onReceivePenLedConfig(int i) {

    }

    @Override
    public void onPenDotTypeResponse(boolean b) {

    }

    @Override
    public void onPenChangeLedColorResponse(boolean b) {

    }

    @Override
    public void onReceivePresssureValue(int i, int i1) {
        App.mTwentyPressure = i;
        App.mThreeHundredPressure = i1;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenMcuVersion(String s) {
        App.mMCUFirmWare = s;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenDotType(int i) {

    }

    @Override
    public void onReceivePenAllStatus(PenStatus penStatus) {
        //Log.i(TAG, "onReceivePenAllStatus:******** " + penStatus.toString());
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(), "获取笔全部属性", Toast.LENGTH_SHORT).show();
//            }
//        });
        EventBus.getDefault().post(new Events.ReceiveStatus(penStatus));


    }

    @Override
    public void onReceivePenType(String s) {

    }

    @Override
    public void onReceivePenEnableLed(boolean b) {
        App.mEnableLED = b;
    }

    @Override
    public void onReceivePenHandwritingColor(int i) {

    }

    @Override
    public void onReceiveElementCode(ElementCode elementCode, long index) {
        Log.i(TAG, "onReceiveElementCode: " + elementCode.toString());
        //int code = elementCode.index;
        long now = System.currentTimeMillis();
        if (now - tmpTime > 1000) {
            EventBus.getDefault().post(new Events.ReceiveElementCode(elementCode, index));
            tmpTime = now;
        }
    }


    @Override
    public void onPenWriteCustomerIDResponse(boolean bIsSuccess) {

    }

    @Override
    public void onReceivePenCustomer(String penCustomerID) {
        App.mCustomerID = penCustomerID;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenTypeInt(int penType) {
        mDeviceType = penType;
        App.mPenType = penType;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenDataType(byte penDataType) {

    }

    @Override
    public void onWriteCmdResult(int i) {
        EventBus.getDefault().post(new Events.ReceiveError(i + ""));
        // mPenAgent.connect("00:12:6F:5C:BF:92");
        Log.i(TAG, "onWriteCmdResult: " + i);
    }

    @Override
    public void onException(BLEException e) {
        EventBus.getDefault().post(new Events.ReceiveError(e.getMessage()));
    }
}
