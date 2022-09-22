package com.sonix.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;


import androidx.multidex.MultiDex;

import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshFooterCreator;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator;
import com.sonix.base.BaseActivity;
import com.sonix.oidbluetooth.BoardActivity;
import com.sonix.oidbluetooth.MainActivity;
import com.sonix.surfaceview.DrawView;
import com.sonix.util.CrashHandler;
import com.sonix.util.Events;
import com.sonix.util.MultiLanguageUtils;

import com.tqltech.tqlpencomm.BLEException;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.bean.ElementCode;
import com.tqltech.tqlpencomm.bean.PenStatus;
import com.tqltech.tqlpencomm.listener.TQLPenSignal;
import com.tqltech.tqlpencomm.pen.PenUtils;
import com.tqltech.tqlpencomm.util.BLEByteUtil;
import com.tqltech.tqlpencomm.util.BLELogUtil;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;


/**
 * Created by wangyong on 7/4/17.
 */
public class App extends Application implements TQLPenSignal {

    private static final String TAG = "App";
    public static String SP_LANGUAGE = "language";
    public static String SP_COUNTRY = "country";
    public static String mPenName = "NAME";
    public static String mFirmWare = "BT";
    public static int mFlash = 0;
    public static String mMCUFirmWare = "MCU";
    public static String mCustomerID = "0000";
    public static String mBTMac = "00:00:00:00:00:00";
    public static int mBattery = 100;
    public static boolean mCharging = false;
    public static int mUsedMem = 0;
    public static boolean mBeep = true;
    public static boolean mPowerOnMode = false;
    public static boolean mEnableLED = false;
    public static int mPowerOffTime = 20;
    public static long mTimer = 0; // 2010-01-01 00:00:00
    public static int mPenSens = 0;
    public static int mTwentyPressure = 0;
    public static int mThreeHundredPressure = 0;

    public static String tmp_mPenName;
    public static boolean tmp_mBeep = false;
    public static boolean tmp_mPowerOnMode = false;
    public static boolean tmp_mEnableLED = false;
    public static int tmp_mPowerOffTime;
    public static int tmp_mPenSens;
    public static long tmp_mTimer;

    public static int mUsbEnable;

    public static int mPenType = -1;

    public static boolean isDrawStoke = false;
    public static boolean isUsbOpen = false;
    public static boolean isSaveSDKLog = false;
    public static boolean isShowHandCode = false;
    public static boolean isShowAlgorithm = true;//默认是true
    public static boolean isInvalidCode = false;
    public static boolean is415Pen = false;
    public static String is415BootPen;
    private static App instance;
    private PenCommAgent mPenAgent;
    public static String mDeviceName;
    public static String mDeviceAddress;
    private String mDeviceBattery;
    private int mDeviceType;
    private boolean mDeviceConnected;
    //点读码计时
    private long tmpTime;
    private ArrayList<BaseActivity> activities = new ArrayList<>();

    public static boolean isNoGoToMain;


    public static App getInstance() {
        return instance;
    }

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

    /**
     * 获取本地软件版本号
     */
    public static int getLocalVersion(Context ctx) {
        int localVersion = 0;
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionCode;
            Log.d("TAG", "本软件的版本号。。" + localVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 获取本地软件版本号名称
     */
    public static String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
            Log.d("TAG", "本软件的版本号。。" + localVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 判断版本是否为Debug
     *
     * @param ctx
     * @return
     */
    public static boolean isDebuggable(Context ctx) {
        boolean debuggable = false;
        PackageManager pm = ctx.getApplicationContext().getPackageManager();
        try {
            ApplicationInfo appinfo = pm.getApplicationInfo(ctx.getPackageName(), 0);
            debuggable = (0 != (appinfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (PackageManager.NameNotFoundException e) {
            /*debuggable variable will remain false*/
        }
        return debuggable;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        instance = this;
        //多语言设置
        registerActivityLifecycleCallbacks(MultiLanguageUtils.callbacks);
        // SmartPen
        mPenAgent = PenCommAgent.GetInstance(this);
        mPenAgent.setLogStatus(true);
        mPenAgent.setTQLPenSignalListener(this);

        //建议在测试阶段建议设置成true，发布时设置为false
//        CrashReport.initCrashReport(getApplicationContext(), "0362ede735", false);

        closeAndroidPDialog();
        // 以下用来捕获程序崩溃异常
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }

    @Override
    protected void attachBaseContext(Context base) {
        //系统语言等设置发生改变时会调用此方法，需要要重置app语言
        super.attachBaseContext(MultiLanguageUtils.attachBaseContext(base));
    }

    /**
     * 关闭在Android P上提醒弹窗
     */
    private void closeAndroidPDialog() {
        try {
            Class aClass = Class.forName("android.content.pm.PackageParser$Package");
            Constructor declaredConstructor = aClass.getDeclaredConstructor(String.class);
            declaredConstructor.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
            declaredMethod.setAccessible(true);
            Object activityThread = declaredMethod.invoke(null);
            Field mHiddenApiWarningShown = cls.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

//        SharedPreferences sp = getSharedPreferences("app", MODE_PRIVATE);
//        SharedPreferences.Editor editor = sp.edit();
//        editor.putString("mDeviceName", App.mDeviceName);
//        editor.putString("mDeviceAddress", App.mDeviceAddress);
//        editor.commit();

        mPenAgent.connect(mDeviceAddress);
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
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(1000);
//                    mPenAgent.getPenAllStatus();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

    }

    @Override
    public void onDisconnected() {
        mDeviceConnected = false;
        EventBus.getDefault().post(new Events.DeviceDisconnected());
        Log.i(TAG, "onDisconnected: ");
    }

    @Override
    public void onConnectFailed() {
        mDeviceConnected = false;
        EventBus.getDefault().post(new Events.DeviceDisconnected());
        Log.i(TAG, "onConnectFailed: ");
    }

    @Override
    public void onReceiveDot(Dot dot) {

//        Log.i(TAG, (Looper.getMainLooper() == Looper.myLooper()) + "   onReceiveDot:在线 " + dot.type);

        if ("com.sonix.oidbluetooth.BitErrorActivity".equals(getTopActivity()) && isNoGoToMain) {
//            isNoGoToMain = true;
            EventBus.getDefault().post(new Events.ReceiveDotErrorPage(dot, true));
        } else {
//            isNoGoToMain = false;
            String topActivity = getTopActivity();
            if (!"com.sonix.oidbluetooth.MainActivity".equals(topActivity) && !isNoGoToMain) {

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

//            if (mReceiveDotListener != null) {
//                mReceiveDotListener.ReceiveDot(new Events.ReceiveDot(dot, true));
//            } else {
                EventBus.getDefault().post(new Events.ReceiveDot(dot, true));
//            }

        }

    }

//    private ReceiveDotListener mReceiveDotListener;
//
//    public void setmReceiveDotListener(ReceiveDotListener mReceiveDotListener) {
//        this.mReceiveDotListener = mReceiveDotListener;
//    }

    public interface ReceiveDotListener {
        void ReceiveDot(Events.ReceiveDot receiveDot);
    }


    //判断当前界面显示的是哪个Activity
    public String getTopActivity() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        //Log.d("测试", "cls:"+cn.getClassName());//包名加类名
        return cn.getClassName();
    }

    @Override
    public void onReceiveOfflineStrokes(Dot dot) {
        EventBus.getDefault().post(new Events.ReceiveDot(dot, false));
    }


    @Override
    public void onOfflineDataList(int offlineNotes) {
        Log.i(TAG, "onOfflineDataList:离线 " + offlineNotes);
        EventBus.getDefault().post(new Events.ReceiveOffline(offlineNotes));
    }

    @Override
    public void onStartOfflineDownload(boolean isSuccess) {

    }

    @Override
    public void onStopOfflineDownload(boolean isSuccess) {

    }

    @Override
    public void onPenPauseOfflineDataTransferResponse(boolean isSuccess) {
        Log.i(TAG, "onPenPauseOfflineDataTransferResponse: " + isSuccess);
        EventBus.getDefault().post(new Events.ReceiveOfflineDataTransferResponse(isSuccess));
    }

    @Override
    public void onPenContinueOfflineDataTransferResponse(boolean isSuccess) {

    }

    @Override
    public void onFinishedOfflineDownload(boolean isSuccess) {
        Log.i(TAG, "onFinishedOfflineDownload: ");
        //笔的离线数据  和 这个笔结束离线指令不同步导致这边特殊适配骚操作
        EventBus.getDefault().post(new Events.ReceiveOfflineProgress(true, 100));
    }

    @Override
    public void onReceiveOfflineProgress(int i) {
        Log.i(TAG, "onReceiveOfflineProgress: " + i);
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
        mPenName = s;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenMac(String penMac) {
        mBTMac = penMac;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenBtFirmware(String penBtFirmware) {
        mFirmWare = penBtFirmware;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenBattery(int i, boolean b) {
        mDeviceBattery = i + "%";
        mBattery = i;
        Log.i(TAG, "onReceivePenBattery: " + i);
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenTimetickSetupResponse(boolean bIsSuccess) {

    }

    @Override
    public void onReceivePenTime(long penTime) {
        App.mTimer = penTime;
        Log.i(TAG, "onReceivePenTime: " + penTime);
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenAutoShutdownSetUpResponse(boolean bIsSuccess) {

    }

    @Override
    public void onReceivePenAutoOffTime(int autoOffTime) {
        mPowerOffTime = autoOffTime;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenFactoryResetSetUpResponse(boolean bIsSuccess) {

    }

    @Override
    public void onReceivePenMemory(int penMemory) {
        mUsedMem = penMemory;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenAutoPowerOnSetUpResponse(boolean bIsSuccess) {
        if (bIsSuccess) {
            App.mPowerOnMode = App.tmp_mPowerOnMode;
        }

    }

    @Override
    public void onReceivePenAutoPowerOnModel(boolean b) {
        mPowerOnMode = b;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenBeepSetUpResponse(boolean bIsSuccess) {
        if (bIsSuccess) {
            App.mBeep = App.tmp_mBeep;
        }
    }

    @Override
    public void onReceivePenBeepModel(boolean b) {
        mBeep = b;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenSensitivitySetUpResponse(boolean bIsSuccess) {

    }

    @Override
    public void onReceivePenSensitivity(int i) {
        mPenSens = i;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onPenLedConfigResponse(boolean bIsSuccess) {

    }

    @Override
    public void onReceivePenLedConfig(int penLedConfig) {

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

    @Override
    public void onReceivePresssureValue(int minPressure, int maxPressure) {
        mTwentyPressure = minPressure;
        mThreeHundredPressure = maxPressure;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }


    @Override
    public void onReceivePenCustomer(String penCustomerID) {
        mCustomerID = penCustomerID;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenDotType(int penDotType) {
        BLELogUtil.d(TAG, "NOTIFY_PEN_DOTTYPE :" + penDotType);
    }

    @Override
    public void onReceivePenAllStatus(PenStatus penStatus) {
        Log.i(TAG, "onReceivePenAllStatus: " + penStatus.toString());
        EventBus.getDefault().post(new Events.ReceiveStatus(penStatus));
    }

    @Override
    public void onReceivePenTypeInt(int penType) {
        Log.i(TAG, "onReceivePenTypeInt: " + penType);
        mDeviceType = penType;
        mPenType = penType;
        EventBus.getDefault().post(new Events.ReceiveStatus(null));
    }

    @Override
    public void onReceivePenType(String penType) {

    }

    @Override
    public void onReceivePenDataType(byte penDataType) {

    }

    @Override
    public void onReceivePenEnableLed(boolean b) {
        mEnableLED = b;
    }

    @Override
    public void onReceivePenHandwritingColor(int color) {

    }

    @Override
    public void onReceiveElementCode(ElementCode elementCode, long index) {
        Log.i(TAG, "onReceiveElementCode: " + elementCode.toString());
        //int code = elementCode.index;
/*      long now = System.currentTimeMillis();
        if (now - tmpTime > 1000) {*/

        String topActivity = getTopActivity();
        if (!"com.sonix.oidbluetooth.MainActivity".equals(topActivity) && !isNoGoToMain) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        EventBus.getDefault().post(new Events.ReceiveElementCode(elementCode, index));
        //     tmpTime = now;
        //    }
    }

    @Override
    public void onWriteCmdResult(int i) {
        EventBus.getDefault().post(new Events.ReceiveError(i + ""));
        Log.i(TAG, "onWriteCmdResult: " + i);
    }

    @Override
    public void onException(BLEException e) {
        EventBus.getDefault().post(new Events.ReceiveError(e.getMessage()));
    }


    @Override
    public void onReceivePenBuzzerBuzzes(boolean buzzerBuzzes) {
        EventBus.getDefault().post(new Events.ReceiveBuzzes(buzzerBuzzes));
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
     * @param data_invalid
     */
    @Override
    public void onReceiveInvalidCodeReportingRange(byte[] data_invalid) {
        Log.d(TAG, "InvalidCodeReporting  : " + BLEByteUtil.bytesToHexString(data_invalid));
        EventBus.getDefault().post(new Events.ReceiveInvalidCode(data_invalid));
    }

    @Override
    public void onReceiveInvalidSetCode(boolean b) {
        Log.i(TAG, "onReceiveInvalidSetCode :" + b);
    }

    @Override
    public void onReceiveInvalidReqCode(boolean b) {

    }

    /**
     * 读取130笔mcu版本，
     *
     * @param mcuTest
     * @param var2    是否是415笔
     * @param var3    415专属boot版本
     */
    @Override
    public void onReceivePenMcuTestCode(String mcuTest, boolean var2, String var3) {
        Log.i(TAG, " onReceivePenMcuTestCode : " + mcuTest + ",var2:" + var2 + ",var3:" + var3);
        is415Pen = var2;
        is415BootPen = var3;
        if (PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T41A
                || PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T31A
                || PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T41
                || PenUtils.mPenType == com.tqltech.tqlpencomm.Constants.PEN_130_T31) {
            mMCUFirmWare = mcuTest;
            EventBus.getDefault().post(new Events.ReceiveStatus(null));
        }
    }

    /**
     * @param penMcuVersion 笔的MCU版本
     */
    @Override
    public void onReceivePenMcuVersion(String penMcuVersion) {
        Log.i(TAG, " onReceivePenMcuVersion : " + penMcuVersion);
        if (PenUtils.mPenType != com.tqltech.tqlpencomm.Constants.PEN_130_T41A
                && PenUtils.mPenType != com.tqltech.tqlpencomm.Constants.PEN_130_T31A
                && PenUtils.mPenType != com.tqltech.tqlpencomm.Constants.PEN_130_T41
                && PenUtils.mPenType != com.tqltech.tqlpencomm.Constants.PEN_130_T31) {
            mMCUFirmWare = penMcuVersion;
            EventBus.getDefault().post(new Events.ReceiveStatus(null));
        }
    }

    @Override
    public void onReceivePenFlashType(int type) {
        Log.i(TAG, "onReceivePenFlashType: " + type);
        mFlash = type;
    }
}

