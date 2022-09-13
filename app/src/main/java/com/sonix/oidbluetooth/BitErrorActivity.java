package com.sonix.oidbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sonix.app.App;
import com.sonix.base.BaseActivity;
import com.sonix.base.BindEventBus;
import com.sonix.oidbluetooth.adapter.BitErrorAdapter;
import com.sonix.oidbluetooth.bean.BitErrorBean;
import com.sonix.util.DotUtils;
import com.sonix.util.Events;
import com.sonix.util.LogUtils;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.pen.PenUtils;
import com.tqltech.tqlpencomm.util.BLEByteUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;

@BindEventBus
public class BitErrorActivity extends BaseActivity implements View.OnClickListener {

    private TextView tv_title, tv_pen_status, tv_probability_tv;

    private RelativeLayout rl_left;
    private Button setBtn, clearBtn;

    private RecyclerView mRecyclerView;
    private BitErrorAdapter mAdapter;

    private PenCommAgent penCommAgent;

    /**
     * 布局文件
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_bit_error;
    }

    /**
     * 初始界面
     */
    @Override
    protected void initView() {
        if (penCommAgent == null) {
            penCommAgent = PenCommAgent.GetInstance(getApplication());
        }
        penCommAgent.reqInvalidCode(App.is415Pen);//要先发送指令先

        tv_title = findViewById(R.id.tv_title);

        tv_pen_status = findViewById(R.id.tv_pen_status);
        tv_pen_status.setVisibility(View.VISIBLE);

        rl_left = findViewById(R.id.rl_left);

        tv_probability_tv = findViewById(R.id.activity_bit_error_probability_tv);

        setBtn = findViewById(R.id.activity_bit_error_set_btn);

        clearBtn = findViewById(R.id.activity_bit_error_clear_btn);

        mRecyclerView = findViewById(R.id.activity_bit_error_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new BitErrorAdapter(null);
        mRecyclerView.setAdapter(mAdapter);

    }


    DecimalFormat df = new DecimalFormat("#0.00");

    public void setProbabilityTv(double probability) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (probability == 0 && mAdapter.getData().size() < mAdapter.getSetProbability()) {
                    tv_probability_tv.setText("点数不足，\n请继续在需要检测的码点纸上书写或点击");
                } else {
                    tv_probability_tv.setText("当前误码率=" + df.format(probability) + "%");
                }

                if (mAdapter.getData().size() > 0) {
//                    mRecyclerView.smoothScrollToPosition(mAdapter.getData().size()-1);
                    mRecyclerView.scrollToPosition(mAdapter.getData().size() - 1);
                }


            }
        });


    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void receiveDot(Events.ReceiveDotErrorPage receiveDot) {
        // Log.i(TAG, "receiveDot " + receiveDot);
        Dot dot = receiveDot.dot;
        processEachDot(dot);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveInvalidCode(Events.ReceiveInvalidCode invalidCode) {
        mAdapter.addInvalidCode();
        addAdapterItem(BLEByteUtil.bytesToHexString(invalidCode.data_invalid), "无效码");
    }


    /**
     * 处理手写码绘制数据
     *
     * @param dot
     */
    private void processEachDot(Dot dot) {
        Log.e(TAG, "主线程?? = " + (Looper.getMainLooper() == Looper.myLooper()));
        String formatData = "SID =" + dot.SectionID + "  OID =" + dot.OwnerID + "  BID =" + dot.BookID
                + "  PID =" + dot.PageID + "  X =" + dot.ab_x + "  Y =" + dot.ab_y + "  Force =" + dot.force;
        addAdapterItem("", formatData);
    }

    /**
     * 处理点读码绘制数据
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveElementCode(Events.ReceiveElementCode elementCode) {//  public void onReceiveElementCode(ElementCode elementCode) {
        String formatData = "SA = " + elementCode.code.SA + ",SB = " + elementCode.code.SB + ",SC = " + elementCode.code.SC +
                ",SD = " + elementCode.code.SD + ",index = " + elementCode.code.index;
        addAdapterItem("", formatData);
    }

    private void addAdapterItem(String originalData, String formatData) {
        mAdapter.addData(new BitErrorBean(originalData, formatData));
        setProbabilityTv(mAdapter.getProbability());
    }

    /**
     * 初始数据
     */
    @Override
    protected void initData() {
        tv_title.setText("误码率检测");
        tv_pen_status.setText(getString(R.string.connected, App.getInstance().getDeviceName()));

        clearBtn.setOnClickListener(this);
        setBtn.setOnClickListener(this);
        rl_left.setOnClickListener(this);

    }


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_left:
                finish();
                break;
            case R.id.activity_bit_error_set_btn:
                showProbabilityDialog();
                break;
            case R.id.activity_bit_error_clear_btn:
                mAdapter.getData().clear();
                mAdapter.setNumber(0);
                mAdapter.setInvalidCodeNumber(0);
                mAdapter.notifyDataSetChanged();
                setProbabilityTv(mAdapter.getProbability());
                break;
        }
    }


    Dialog dialogProbability;

    private void showProbabilityDialog() {

        if (dialogProbability == null) {
            dialogProbability = new Dialog(this, R.style.customDialog2);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_bit_error_edit, null);
            EditText editCode = view.findViewById(R.id.dialog_bit_error_edit);
            dialogProbability.setContentView(view);
            view.findViewById(R.id.tv_cancel).setOnClickListener(v -> {
                dialogProbability.dismiss();
            });
            view.findViewById(R.id.tv_ok).setOnClickListener(v -> {
                String code = editCode.getText().toString().trim();
                if (!TextUtils.isEmpty(code)) {
                    Integer integer = Integer.valueOf(code);
                    if (integer >= 0 && integer <= 10000) {
                        mAdapter.setSetProbability(integer);
                        setProbabilityTv(mAdapter.getProbability());

                        dialogProbability.dismiss();
                    } else {
                        showToast("输入范围在0-10000之间");
                    }
                } else {
                    showToast("输入内容不能为null");
                }
            });
        }
        if (dialogProbability != null && !dialogProbability.isShowing()) {
            dialogProbability.show();
        }


    }


    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.i(TAG, "onResume:");
        penCommAgent.setIsSplitFiltration(false);

    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.i(TAG, "onStop:");
        App.isNoGoToMain = false;//要去main
        penCommAgent.setIsSplitFiltration(App.isShowAlgorithm);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogProbability != null && dialogProbability.isShowing()) {
            dialogProbability.dismiss();
            dialogProbability = null;
        }
    }
}
