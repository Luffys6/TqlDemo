package com.sonix.oidbluetooth.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sonix.oidbluetooth.R;
import com.sonix.oidbluetooth.adapter.PointReadCodeAdapter;
import com.sonix.util.Events;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class PointReadCodeFragment extends Fragment {
    private static final String TAG = "PointReadCodeFragment";
    private PointReadCodeAdapter pointReadAdapter;
    private TextView tv_clear_code;
    private List<String> codeList;
    private Activity activity;
    private ListView mPointListView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_point_read, container, false);
        mPointListView = view.findViewById(R.id.lv_point_read);
        tv_clear_code = view.findViewById(R.id.tv_clear_code);

        pointReadAdapter = new PointReadCodeAdapter(activity);
        mPointListView.setAdapter(pointReadAdapter);

        tv_clear_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pointReadAdapter != null) {
                    pointReadAdapter.clearDevice();
                    codeList.clear();
                }
            }
        });
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        codeList = new ArrayList<>();
        initData();
    }

    /**
     * 在线点读码
     *
     * @param elementCode
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveElementCode(Events.ReceiveElementCode elementCode) {
        if (elementCode != null) {
            String info = "SA = " + elementCode.code.SA + ",SB = " + elementCode.code.SB + ",SC = " + elementCode.code.SC + ",SD = " + elementCode.code.SD + ",index = " + elementCode.code.index;
            pointReadAdapter.addDevice(info, elementCode.index,1);
            codeList.add(info);
            mPointListView.smoothScrollToPosition(codeList.size() - 1);
            Log.i(TAG, "接收在线点读码 index :"+elementCode.index +",info:"+ info);
        }
    }

    /**
     * 接收离线点读码
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void ReqElementCode(Events.ReadElementCodeDot readElementCodeDot) {
        if (!TextUtils.isEmpty(readElementCodeDot.elementCode)) {
            pointReadAdapter.addDevice(readElementCodeDot.elementCode, readElementCodeDot.index,2);
            codeList.add(readElementCodeDot.elementCode);
            mPointListView.smoothScrollToPosition(codeList.size() - 1);
            Log.i(TAG, "接收离线点读码 index:" +readElementCodeDot.index+",elementCode:"+ readElementCodeDot.elementCode);
        }
    }

    private void initData() {
        EventBus.getDefault().register(this);
        Bundle arguments = getArguments();
        if (arguments != null) {
            String pointCode = arguments.getString("POINTCODE");
            long index = arguments.getLong("INDEX");
            if (!TextUtils.isEmpty(pointCode)) {
                codeList.add(pointCode);
                pointReadAdapter.addDevice(pointCode,index,1);
                mPointListView.smoothScrollToPosition(codeList.size() - 1);
                Log.i(TAG, "onResume 接收离线点读码 :" + pointCode);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (pointReadAdapter != null) {
            pointReadAdapter.clearDevice();
            codeList.clear();
        }
    }
}
