package com.sonix.ota;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.base.BindEventBus;
import com.sonix.oidbluetooth.R;
import com.sonix.oidbluetooth.SearchActivity;
import com.sonix.util.Events;
import com.tqltech.tqlpencomm.firmware.UpdateFirmwareUtil;
import com.tqltech.tqlpencomm.listener.OnUpdateFirmwareListener;
import com.sonix.util.FileUtils;
import com.sonix.util.SPUtils;
import com.tqltech.tqlpencomm.PenCommAgent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;

@BindEventBus
public class BtMcuActivity extends BaseActivity implements View.OnClickListener, OnUpdateFirmwareListener {

    private final static String TAG = "BtMcuActivity";

    private PenCommAgent bleManager;

    public TextView tv_title;
    public RelativeLayout rl_left;
    public RelativeLayout rl_right;
    public ImageView iv_left;
    public ImageView iv_right;

    private TextView filePath, tv_choseCheckFile, tv_choseFile;
    private LinearLayout choseFile, choseCheckFile;
    private Button mcuUpdate;


    private ProgressDialog mUpgradeProgressDialog;
    private int mMaxProgress = 100;

    private static final int MESSAGE_UPDATE_TIP = 10000;
    private static final int MESSAGE_UPDATE_PROGRESS = 20000;
    private static final int MESSAGE_FINISH_PROGRESS = 30000;
    private static final int MESSAGE_DIALOG_TIP = 40000;
    private static final int MESSAGE_UPDATE_MAS = 50000;
    private static final int MESSAGE_DIALOG_TIP2 = 40002;

    private SharedPreferences sp;

    private boolean isCheckFile;


    private boolean isRequesting;
    private TextView mTvTtVersionNum;
    private TextView mTvBtSize;
    private TextView mTvBtMd5;
    private TextView mTvMcuVersionNum;
    private TextView mTvMcuSize;
    private TextView mTvMcuMd5;

    private Dialog dialog;
    private UpdateFirmwareUtil updateFirmwareUtil;

    File file;
    //??????????????????
    private String pAddress;
    //????????????
    private int pType;
    ArrayList<byte[]> mcuMultiData = new ArrayList<>();

    private Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
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
                case MESSAGE_UPDATE_PROGRESS:
                    if (mUpgradeProgressDialog != null  && App.getInstance().isDeviceConnected()) {
                        if (!mUpgradeProgressDialog.isShowing()) {
                            mUpgradeProgressDialog.show();
                        }
                        mUpgradeProgressDialog.setProgress((Integer) msg.obj);
                    }
                    hideLoadingDialog();
                    break;
                case MESSAGE_UPDATE_MAS:
                    if (mUpgradeProgressDialog != null) {
                        mUpgradeProgressDialog.setTitle((String) msg.obj);
                    }
                    break;
                case MESSAGE_UPDATE_TIP:
                    //tip.append(msg.obj + "\n");
//                    showToast(msg.obj +"");
                    if (mUpgradeProgressDialog != null && mUpgradeProgressDialog.isShowing()) {
                        mUpgradeProgressDialog.dismiss();
                    }
                    if ("ota success".equals(msg.obj)) {
                        handler.removeCallbacksAndMessages(null);
                        showSuccessDialog();
//                    } else if ("ota failure".equals(msg.obj)){
//                        showFailDialog("ota failure");
//                    }else if("????????????".equals(msg.obj)){
//                        showFailDialog("????????????");
                    } else{
                        showFailDialog(""+msg.obj);
                    }
                    break;
                case MESSAGE_DIALOG_TIP:
                    String obj = (String) msg.obj;
                    showLoadingDialog(BtMcuActivity.this, obj);
                    //   Toast.makeText(MCUOTAActivity.this, "ss", Toast.LENGTH_SHORT).show();

                    break;
                case MESSAGE_DIALOG_TIP2:
                    String obj2 = (String) msg.obj;
                    showLoadingDialog(BtMcuActivity.this, obj2);
                    //   Toast.makeText(MCUOTAActivity.this, "ss", Toast.LENGTH_SHORT).show();
                    Dialog dialog = getDialog();
                    if (dialog != null) {
                        dialog.setCancelable(true);
                        getDialog().setCanceledOnTouchOutside(true);
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


    @Override
    protected int getLayoutId() {
        return R.layout.activity_mcu_ota;
    }

    @Override
    public void initView() {
        if (bleManager == null) {
            bleManager = PenCommAgent.GetInstance(getApplication());
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//??????

        tv_title = findViewById(R.id.tv_title);
        rl_left = findViewById(R.id.rl_left);
        rl_right = findViewById(R.id.rl_right);
        iv_left = findViewById(R.id.iv_left);
        iv_right = findViewById(R.id.iv_right);

        rl_left.setOnClickListener(this);
        iv_left.setOnClickListener(this);
        tv_title.setText(getResources().getString(R.string.mcu_bt_update));
        iv_right.setVisibility(View.GONE);

        filePath = findViewById(R.id.mctOtaText);
        choseFile = findViewById(R.id.choseFile);
        choseCheckFile = findViewById(R.id.choseCheckFile);

        tv_choseCheckFile = findViewById(R.id.choseCheckFile_tv);
        tv_choseFile = findViewById(R.id.choseFile_tv);

        mcuUpdate = findViewById(R.id.mcuOtaUpgrade);

        mTvTtVersionNum = findViewById(R.id.tv_bt_version_num);
        mTvBtSize = findViewById(R.id.tv_bt_size);
        mTvBtMd5 = findViewById(R.id.tv_bt_md5);
        mTvMcuVersionNum = findViewById(R.id.tv_mcu_version_num);
        mTvMcuSize = findViewById(R.id.tv_mcu_size);
        mTvMcuMd5 = findViewById(R.id.tv_mcu_md5);

        choseFile.setOnClickListener(this);
        choseCheckFile.setOnClickListener(this);
        mcuUpdate.setOnClickListener(this);


//        if (sp == null) {
//            sp = getSharedPreferences("ota", MODE_PRIVATE);
//        }
//        String path = sp.getString("pathMcuOta", "");
//        Log.e(TAG, "path=" + path);
//        if (path != null && !path.equals("")) {
//            filePath.setText(path);
//        }

    }

    @Override
    public void initData() {
        boolean mPermissionTip = (Boolean) SPUtils.get(BtMcuActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, false);
        Log.i(TAG, "onResume mPermissionTip::" + mPermissionTip);
        if (mPermissionTip) {
            requestPermission1();
        } else {
            initPermissionsTip();
        }

        bleManager = PenCommAgent.GetInstance(getApplication());

        //???????????????????????? ??????????????????
        updateFirmwareUtil = bleManager.updateMCUAndBTWith(this);


    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {
        super.onPause();
//        String path = filePath.getText().toString();
//        Log.e(TAG, "onPause: path= " + path);
//        SharedPreferences.Editor editor = sp.edit();
//        editor.putString("pathMcuOta", filePath.getText().toString());
//        editor.commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_left:
            case R.id.iv_left:
                finish();
                break;
            case R.id.choseFile:
                isCheckFile = false;
                FileUtils.openFilePath(this);
                break;
            case R.id.choseCheckFile:
                isCheckFile = true;
                FileUtils.openFilePath(this);
                break;
            case R.id.mcuOtaUpgrade:
                //??????????????????!
                if (isDeviceConnected()) return;

//                updateFirmwareUtil.setUpdateModel(UpdateFirmwareUtil.MODEL_MCU);//?????????0  1????????????mcu  2??????????????????

                long l = System.currentTimeMillis();
                if (l - lastConnectTime > 3000) {

                    mUpgradeProgressDialog = createUpgradeProgressDialog("MCU???????????????(???????????????....)");
                    lastConnectTime = l;
                    int i = updateFirmwareUtil.startOTAUpdate();

//                int i = bleManager.stringKeyUpdateMCUAndBTWith(file.getPath(),
//                        "0B30EFF621C60384DAC479D4E59E68D4FA017D9FACB870907B75CE9A817C0DA4",
//                        this);

                    if (i != 0) {
                        showToast("????????????  :  " + updateFirmwareUtil.getErrorString(i));
                        //   1; //?????????????????? ?????? ??????????????????
                        //   2; //??????key??????
                        //   4; //????????????
                    }

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
                    Log.e(TAG, "onActivityResult: path=" + uri + ",path2=" + uri.getPath().toString());
                    if (uri != null) {
                        path = FileUtils.getRealPathFromURI(mContext, uri);
                    }
                    Log.e(TAG, "onActivityResult: path=" + path);
                    file = new File(path);
                    if (isCheckFile) {
                        int i = updateFirmwareUtil.SplitBinKeyFile(path);
                        tv_choseCheckFile.setText("????????? " + file.getName());
                        //  0; //??????
                        //  1; //?????????????????? ?????? ??????????????????
                        //  2; //??????key??????
                        //  3; //????????????,????????????
                        if (i != 0) {
                            showToast("????????????  :  " + updateFirmwareUtil.getErrorString(i));
                            //   1; //?????????????????? ?????? ??????????????????
                            //   2; //??????key??????
                            //   4; //????????????
                        }

                    } else {
                        filePath.setText(path);
                        tv_choseFile.setText("????????? " + file.getName());
//                        int i = updateFirmwareUtil.SplitBinData(path);
                        int i = updateFirmwareUtil.splitBinFile(path);

                        //  0; //??????
                        //  01; //?????????????????? ?????? ??????????????????
                        //  02; //??????key??????
                        if (i != 0) {
                            showToast("????????????  :  " + updateFirmwareUtil.getErrorString(i));
                            //   1; //?????????????????? ?????? ??????????????????
                            //   2; //??????key??????
                            //   4; //????????????
                        }

                    }


                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    /**
     * ?????????Dialog
     *
     * @param strTip
     * @return
     */
    private ProgressDialog createUpgradeProgressDialog(String strTip) {
        ProgressDialog dialog = new ProgressDialog(this);

        dialog.setTitle(strTip);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(mMaxProgress);
        dialog.setProgress(0);
        dialog.setCancelable(false);

        return dialog;
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
                SPUtils.put(BtMcuActivity.this, com.sonix.oidbluetooth.Constants.FIRST_SAVE, true);
                requestPermission1();
            }
        });
        mServiceDialog.show();
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

        tv_title.setText("????????????");
        tv_msg.setText("??????pen???????????????");
        tv_msg.setGravity(Gravity.CENTER);

        viewLine.setVisibility(View.GONE);
        tvConfirm.setVisibility(View.GONE);

        tv_confirm_agree.setText("??????");
        ImageView tv_cancel = (ImageView) view.findViewById(R.id.tv_cancel);
        tv_cancel.setVisibility(View.GONE);

        tv_confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceDialog.dismiss();
                Intent intent = new Intent(BtMcuActivity.this, SearchActivity.class);
                startActivity(intent);
                showToast("????????????");
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

        tv_title.setText("????????????");
        tv_msg.setText("????????????, ??????pen??????????????? " +str);
        tv_msg.setGravity(Gravity.CENTER);

        viewLine.setVisibility(View.GONE);
        tvConfirm.setVisibility(View.GONE);

        tv_confirm_agree.setText("??????");
        ImageView tv_cancel = (ImageView) view.findViewById(R.id.tv_cancel);
        tv_cancel.setVisibility(View.GONE);

        tv_confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceDialog.dismiss();
                Intent intent = new Intent(BtMcuActivity.this, SearchActivity.class);
                startActivity(intent);
                finish();
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

    private boolean isDeviceConnected() {
        if (!App.getInstance().isDeviceConnected()) {
            showGoConnectDialog();
            return true;
        }
        return false;
    }

    /**
     * ????????????dialog
     */
    private void showGoConnectDialog() {
        if (dialog == null) {
            dialog = new Dialog(BtMcuActivity.this, R.style.customDialog);
        }
        View view = LayoutInflater.from(BtMcuActivity.this).inflate(R.layout.dialog_go_connect, null);
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
                Intent intent = new Intent(BtMcuActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void splitFileResult(int BtLength, String BtMD5, int McuLength, String McuMD5) {
        mTvBtSize.setText("" + BtLength);
        mTvMcuSize.setText("" + McuLength);
    }

    @Override
    public void splitKeyFileResult(String BtMD5, String McuMD5) {
        mTvBtMd5.setText(BtMD5);
        mTvMcuMd5.setText(McuMD5);
        Log.e(TAG, "key????????? BtMD5 = " + BtMD5);
        Log.e(TAG, "key????????? McuMD5 = " + McuMD5);
    }

    @Override
    public void updateMcuStart() {
        hideLoadingDialog();
//        Message message = new Message();
//        message.obj = "???????????????";
//        message.what = MESSAGE_DIALOG_TIP;
//        handler.sendMessage(message);
    }

    @Override
    public void updateMcuProgress(int progress) {
        if (updateFirmwareUtil.getUpdateType() == UpdateFirmwareUtil.MODEL_MCU) {
            //?????????mcu
            handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, progress).sendToTarget();//????????????,mcu
        } else {
            handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, progress / 2).sendToTarget();//??????2????????????mcu???bt?????????????????????
        }
    }

    @Override
    public void updateMcuSuccess() {
        if (updateFirmwareUtil.getUpdateType() == UpdateFirmwareUtil.MODEL_MCU) {
            //?????????mcu
            isOver = true;
            handler.obtainMessage(MESSAGE_UPDATE_TIP, "ota success").sendToTarget();
        } else {
            Message message = new Message();
            message.obj = "MCU???????????????(??????????????????, ???????????????...)";
            message.what = MESSAGE_UPDATE_MAS;
            handler.sendMessage(message);
        }
    }

    @Override
    public void updateMcuFail(int errorType) {
        //1 ???????????????
        //2 ????????????
        //3 ????????????
        //4 ?????????????????????
        //5 ??????????????????
        //6 CRC???????????????
        //7 ????????????????????????
        //8 ??????????????????
        //9 ????????????
        isOver = true;
        handler.obtainMessage(MESSAGE_UPDATE_TIP, "ota fail errorType = " + errorType).sendToTarget();
    }

    @Override
    public void updateBTStart() {
        Message message = new Message();
        message.obj = "MCU???????????????(??????????????????...)";
        message.what = MESSAGE_UPDATE_MAS;
        handler.sendMessage(message);
    }

    @Override
    public void updateBTProgress(int progress) {
        if (updateFirmwareUtil.getUpdateType() == UpdateFirmwareUtil.MODEL_BT) {
            handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, progress).sendToTarget();//???????????????
        } else {
            handler.obtainMessage(MESSAGE_UPDATE_PROGRESS, 51 + progress / 2).sendToTarget();//mcu??????   mcu??????+????????????
        }
    }

    @Override
    public void updateBTSuccess() {
        isOver = true;
        handler.obtainMessage(MESSAGE_UPDATE_TIP, "ota success").sendToTarget();
    }

    @Override
    public void updateBTFail() {
        isOver = true;
        handler.obtainMessage(MESSAGE_UPDATE_TIP, "ota failure").sendToTarget();
    }

    private boolean isOver;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivePenStatus(Events.DeviceDisconnected events) {
        Log.e(TAG, "receivePenStatus : ????????????");
        if(!isOver){ //?????????  ????????????????????????????????????
            handler.obtainMessage(MESSAGE_UPDATE_TIP,"????????????").sendToTarget();
        }
        showToast("????????????");
    }

}