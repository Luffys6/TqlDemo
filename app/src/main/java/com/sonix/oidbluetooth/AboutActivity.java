package com.sonix.oidbluetooth;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sonix.base.BaseActivity;
import com.sonix.oidbluetooth.view.ProtocolPopu;
import com.sonix.util.AppUtils;
import com.sonix.util.SPUtils;
import com.tqltech.tqlpencomm.PenCommAgent;

/**
 * 关于
 */
public class AboutActivity extends BaseActivity implements View.OnClickListener {

    public TextView tv_title;
    public RelativeLayout rl_left;
    public RelativeLayout rl_right;
    public ImageView iv_left;
    public ImageView iv_right;

    public ImageView iv_app_icon;
    public TextView tv_app_name;
    public TextView tv_app_version;
    public TextView tv_user_agreement;
    public TextView tv_privacy_policy;

    private ProtocolPopu popu;
    private PenCommAgent penCommAgent;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_about;
    }

    @Override
    protected void initView() {
        tv_title = findViewById(R.id.tv_title);
        rl_left = findViewById(R.id.rl_left);
        rl_right = findViewById(R.id.rl_right);
        iv_left = findViewById(R.id.iv_left);
        iv_right = findViewById(R.id.iv_right);

        iv_app_icon = findViewById(R.id.iv_app_icon);
        tv_app_name = findViewById(R.id.tv_app_name);
        tv_app_version = findViewById(R.id.tv_app_version);
        tv_user_agreement = findViewById(R.id.tv_user_agreement);
        tv_privacy_policy = findViewById(R.id.tv_privacy_policy);
    }

    @Override
    protected void initData() {

        rl_left.setOnClickListener(this);
        iv_left.setOnClickListener(this);
        tv_title.setText(getResources().getString(R.string.about));
        iv_right.setVisibility(View.GONE);

        tv_user_agreement.setOnClickListener(this);
        tv_privacy_policy.setOnClickListener(this);

        iv_app_icon.setImageBitmap(AppUtils.getBitmap(mContext));
        tv_app_name.setText(AppUtils.getAppName(mContext));
        tv_app_version.setText(AppUtils.getVersionName(mContext));
        penCommAgent = PenCommAgent.GetInstance(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean connect = penCommAgent.isConnect();
        Log.i(TAG, "onResume isConnect:" + connect);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
            case R.id.iv_left:
                finish();
                break;
            case R.id.tv_user_agreement:
                showUserProtocol(1);
                break;
            case R.id.tv_privacy_policy:
                showUserProtocol(2);
                break;
        }
    }

    /**
     * 协议弹出框
     *
     * @param type
     */
    private void showUserProtocol(int type) {
        if (popu == null)
            popu = new ProtocolPopu(this, type);
        popu.setType(type);
        popu.setPopListener(popListener);
        popu.showAtLocation(tv_title, Gravity.NO_GRAVITY, 0, 0);
    }

    /**
     * 点击事件
     */
    ProtocolPopu.PopListener popListener = new ProtocolPopu.PopListener() {
        @Override
        public void onResult(boolean flag) {
            SPUtils.put(mContext, "UserProtocol", flag);
        }
    };
}
