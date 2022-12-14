package com.sonix.util;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

///import android.support.annotation.NonNull;


@TargetApi(Build.VERSION_CODES.M)        ////高版本API的代码在低版本SDK不报错
public class PermissionUtils {
    private static String TAG = "PermissionUtils";

    private static HashMap<String, Object> map = new HashMap<String, Object>();

    private static boolean checkSDK() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static void requestPermissions(Context context, String[] permissions,
                                          int requestCode, String explainMsg, OnPermissionResult onPermissionResult) {
        onPermissionResult = initOnPermissionResult(onPermissionResult, permissions, requestCode, explainMsg);
        if (permissions.length == 0) {
            invokeOnRequestPermissionsResult(context, onPermissionResult);
        } else if (context instanceof Activity || (Object) context instanceof Fragment) {
            if (checkSDK()) {
                onPermissionResult.deniedPermissions = getDeniedPermissions(context, permissions);
                if (onPermissionResult.deniedPermissions.length > 0) {
                    //存在被拒绝的权限
                    onPermissionResult.rationalePermissions = getRationalePermissions(context, onPermissionResult.deniedPermissions);
                    if (onPermissionResult.rationalePermissions.length > 0) {//向用户解释请求权限的理由
                        Log.i(TAG, "1111you quan xian bei jinzhi");
                        shouldShowRequestPermissionRationale(context, onPermissionResult);
                    } else {
                        Log.i(TAG, "you quan xian bei jinzhi");
                        invokeRequestPermissions(context, onPermissionResult);
                    }
                } else {
                    //所有权限允许
                    onPermissionResult.grantResults = new int[permissions.length];
                    for (int i = 0; i < onPermissionResult.grantResults.length; i++) {
                        onPermissionResult.grantResults[i] = PackageManager.PERMISSION_GRANTED;
                    }
                    Log.i(TAG, "you quan xian bei yunxu");
                    invokeOnRequestPermissionsResult(context, onPermissionResult);
                }
            } else {
                onPermissionResult.grantResults = getPermissionsResults(context, permissions);
                invokeOnRequestPermissionsResult(context, onPermissionResult);
            }
        }
    }

    /**
     * 获取被拒绝的权限
     *
     * @param context
     * @param permissions
     * @return
     */
    private static String[] getDeniedPermissions(Context context, String[] permissions) {
        List<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (checkPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                list.add(permission);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * 获取权限请求结果
     *
     * @param context
     * @param permissions
     * @return
     */
    private static int[] getPermissionsResults(Context context, String[] permissions) {
        int[] results = new int[permissions.length];
        for (int i = 0; i < results.length; i++)
            results[i] = checkPermission(context, permissions[i]);
        return results;
    }

    private static String[] getRationalePermissions(Context context, String[] deniedPermissions) {
        List<String> list = new ArrayList<>();
        for (String permission : deniedPermissions) {
            if (context instanceof Activity) {
                if (((Activity) context).shouldShowRequestPermissionRationale(permission)) {
                    list.add(permission);
                }
            } else if ((Object) context instanceof Fragment) {
                if (((Fragment) (Object) context).shouldShowRequestPermissionRationale(permission)) {
                    list.add(permission);
                }
            } else {
                throw new IllegalArgumentException("context 只能是Activity或Fragment");
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * 调用权限请求方法
     *
     * @param context
     * @param onPermissionResult
     */
    private static void invokeRequestPermissions(Context context, OnPermissionResult onPermissionResult) {
        if (context instanceof Activity)
            ((Activity) context).requestPermissions(onPermissionResult.deniedPermissions, onPermissionResult.requestCode);
        else if ((Object) context instanceof Fragment)
            ((Fragment) (Object) context).requestPermissions(onPermissionResult.deniedPermissions, onPermissionResult.requestCode);
    }

    /**
     * 调用权限请求结果回调
     *
     * @param context
     * @param onPermissionResult
     */
    private static void invokeOnRequestPermissionsResult(Context context, OnPermissionResult onPermissionResult) {
        if (context instanceof Activity) {
            if (checkSDK()) {
                ((Activity) context).onRequestPermissionsResult(onPermissionResult.requestCode,
                        onPermissionResult.permissions, onPermissionResult.grantResults);
            } else if (context instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
                ((ActivityCompat.OnRequestPermissionsResultCallback) context)
                        .onRequestPermissionsResult(onPermissionResult.requestCode, onPermissionResult.permissions, onPermissionResult.grantResults);
            } else {
                onRequestPermissionsResult(onPermissionResult.requestCode, onPermissionResult.permissions, onPermissionResult.grantResults);
            }
        } else if ((Object) context instanceof Fragment) {
            ((Fragment) (Object) context).onRequestPermissionsResult(onPermissionResult.requestCode,
                    onPermissionResult.permissions, onPermissionResult.grantResults);
        }
    }

    /**
     * 显示权限解释
     *
     * @param context
     * @param onPermissionResult
     */
    private static void shouldShowRequestPermissionRationale(final Context context, final OnPermissionResult onPermissionResult) {
        invokeRequestPermissions(context, onPermissionResult);
    }

    /**
     * 检查权限
     *
     * @param context
     * @param permission
     * @return
     */
    private static int checkPermission(Context context, String permission) {
        return context.checkPermission(permission, Process.myPid(), Process.myUid());
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        synchronized (map) {
            OnPermissionResult onPermissionResult = (OnPermissionResult) map.get(String.valueOf(requestCode));
            if (onPermissionResult != null) {
                List<String> deniedPermissions = new ArrayList<>();
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permissions[i]);
                    }
                }
                if (deniedPermissions.size() > 0) {
                    onPermissionResult.denied(requestCode);
                } else {
                    onPermissionResult.granted(requestCode);
                }
                map.remove(String.valueOf(requestCode));
            }
        }
    }

    /**
     * 初始化权限请求回调
     *
     * @param onPermissionResult
     * @param permissions
     * @param requestCode
     * @param explainMsg         @return
     */
    private static OnPermissionResult initOnPermissionResult(OnPermissionResult onPermissionResult, String[] permissions,
                                                             int requestCode, String explainMsg) {
        synchronized (map) {
            if (onPermissionResult == null) {
                onPermissionResult = new OnPermissionResult() {
                    @Override
                    public void granted(int requestCode) {

                    }

                    @Override
                    public void denied(int requestCode) {

                    }
                };
            }
            onPermissionResult.permissions = permissions;
            onPermissionResult.requestCode = requestCode;
            onPermissionResult.explainMsg = explainMsg;
            onPermissionResult.grantResults = new int[0];
            map.put(String.valueOf(requestCode), onPermissionResult);
            return onPermissionResult;
        }
    }

    public abstract static class OnPermissionResult {
        int requestCode;
        String explainMsg;
        String[] permissions;
        String[] deniedPermissions;
        String[] rationalePermissions;
        int[] grantResults;

        //权限允许
        public abstract void granted(int requestCode);

        //权限拒绝
        public abstract void denied(int requestCode);
    }
}
