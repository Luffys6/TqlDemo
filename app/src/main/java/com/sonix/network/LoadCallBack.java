package com.sonix.network;


import android.content.Context;


import com.sonix.oidbluetooth.R;
import com.sonix.util.Loading_view;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by smile_raccoon on 2018/1/11.
 */

public abstract class LoadCallBack<T> extends BaseCallBack<T> {
    private Context context;
    private boolean showDialog = true;
    private Loading_view dialog;

    public LoadCallBack(Context context) {
        this.context = context;
        dialog = new Loading_view(context, R.style.CustomDialog);
        showDialog();
    }

    public LoadCallBack(Context context, boolean showDialog) {
        this.context = context;
        this.showDialog = showDialog;
        dialog = new Loading_view(context, R.style.CustomDialog);
        if (showDialog) {
            showDialog();
        }
    }

    //显示进度条
    public void showDialog() {
        dialog.show();
    }

    public void hideDialog() {
        if (dialog != null) {
//            隐藏进度条
            dialog.dismiss();
        }
    }


    @Override
    protected void OnRequestBefore(Request request) {
        if (showDialog) {
            showDialog();
        }
    }

    @Override
    protected void onFailure(Call call, IOException e) {
        hideDialog();
    }

    @Override
    protected void onResponse(Response response) {
        hideDialog();
    }

    @Override
    protected void inProgress(int progress, long total, int id) {

    }

    @Override
    protected void onError(Call call, int statusCode, Exception e) {
        hideDialog();
    }
}