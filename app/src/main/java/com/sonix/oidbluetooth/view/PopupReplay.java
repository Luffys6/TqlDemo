package com.sonix.oidbluetooth.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sonix.oidbluetooth.R;

/**
 * 回放弹出框
 */
public class PopupReplay extends PopupWindow implements View.OnClickListener {

    private Context mContext;
    private View view;
    private int popupWidth;//测量后的宽度
    private int popupHeight;//测量后的高度
    private int speed = 1;
    private int totalNumber = 0;
    private int currentNumber = 0;
    private int progress = 0;
    private boolean isStart = false;

    private ImageView iv_replay_start;
    private TextView tv_replay_current_time;
    private ProgressBar pb_replay;
    private TextView tv_replay_total_time;
    private TextView tv_replay_speed;

    private final static int MSG_START = 1;
    private final static int MSG_NUMBER = 2;
    private final static int MSG_PROGRESS = 3;
    private final static int MSG_SPEED = 4;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START:
                    if (isStart) {
                        iv_replay_start.setImageResource(R.mipmap.icon_pause);
                    } else {
                        iv_replay_start.setImageResource(R.mipmap.icon_play);
                    }
                    break;
                case MSG_NUMBER:
                    tv_replay_total_time.setText(totalNumber + "");
                    tv_replay_current_time.setText(currentNumber + "");
                    if (totalNumber <= 0) {
                        pb_replay.setProgress(0);
                    } else {
                        pb_replay.setProgress(currentNumber * 100 / totalNumber);
                    }
                    break;
                case MSG_PROGRESS:
                    pb_replay.setProgress(progress);
                    break;
                case MSG_SPEED:
                    tv_replay_speed.setText(speed + "X");
                    break;
                default:
                    break;
            }
        }
    };

    public PopupReplay(Context context) {
        super(context);
        mContext = context;
        calWidthAndHeight(mContext);
        view = LayoutInflater.from(mContext).inflate(R.layout.popup_replay, null);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        setContentView(view);

        initView();

        // 设置SelectPicPopupWindow弹出窗体可点击
        setFocusable(true);
        setOutsideTouchable(true);

        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0000000000);
        // 点back键和其他地方使其消失,设置了这个才能触发OnDismisslistener ，设置其他控件变化等操作
        setBackgroundDrawable(dw);
        // 设置弹出窗体动画效果
        setAnimationStyle(android.R.style.Animation_Dialog);
    }

    private void initView() {
        popupWidth = view.getMeasuredWidth();    //获取测量后的宽度
        popupHeight = view.getMeasuredHeight();  //获取测量后的高度

        iv_replay_start = view.findViewById(R.id.iv_replay_start);
        tv_replay_current_time = view.findViewById(R.id.tv_replay_current_time);
        pb_replay = view.findViewById(R.id.pb_replay);
        tv_replay_total_time = view.findViewById(R.id.tv_replay_total_time);
        tv_replay_speed = view.findViewById(R.id.tv_replay_speed);

        iv_replay_start.setOnClickListener(this);
        tv_replay_speed.setOnClickListener(this);

    }

    public int getPopupWidth() {
        return popupWidth;
    }

    public int getPopupHeight() {
        return popupHeight;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
        handler.sendEmptyMessage(MSG_SPEED);
    }

    public int getSpeed() {
        return speed;
    }


    public void setTotalNumber(int totalNumber) {
        this.totalNumber = totalNumber;
        handler.sendEmptyMessage(MSG_NUMBER);
    }

    public void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
        handler.sendEmptyMessage(MSG_NUMBER);
    }

    public void setProgress(int progress) {
        this.progress = progress;
        handler.sendEmptyMessage(MSG_PROGRESS);
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
        handler.sendEmptyMessage(MSG_START);
    }

    /**
     * 设置PopupWindow的大小
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        int mWidth = metrics.widthPixels;
        //设置高度为全屏高度的70%
        //mHeight= (int) (metrics.heightPixels*0.7);

        setWidth(mWidth);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_replay_speed:
                if (speed <= 4) {
                    speed++;
                } else {
                    speed = 1;
                }
                tv_replay_speed.setText(speed + "X");
            case R.id.iv_replay_start:
                if (isStart) {
                    iv_replay_start.setImageResource(R.mipmap.icon_pause);
                } else {
                    iv_replay_start.setImageResource(R.mipmap.icon_play);
                }
                break;
        }
        listener.click(view.getId());
    }

    private PopupListener listener;

    public void setListener(PopupListener listener) {
        this.listener = listener;
    }

}
