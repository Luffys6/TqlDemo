package com.sonix.oidbluetooth;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sonix.base.BaseActivity;
import com.sonix.util.DataHolder;
import com.sonix.util.DotUtils;
import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.util.BLEFileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 笔记识别
 */
public class RecognitionActivity extends BaseActivity implements View.OnClickListener {

    public TextView tv_title;
    public RelativeLayout rl_left;
    public RelativeLayout rl_right;
    public ImageView iv_left;
    public ImageView iv_right;

    private TextView mTextView;

    //获取手写识别的key的URL
    private static final String keyUrl = "https://hwr.pencloud.cn/myscript/key";
    //获取手写识别的key的用户
    private static final String keyUser = "tql";
    //笔记识别的URL
    private final String url = "https://hwr.pencloud.cn/script";
    //笔记识别的key
    private String key = "a55a66c9-ae8f-482e-bb90-c2771390d388";
    //笔记识别的数据
    public static List<Dot> dotsList ;

    //笔记数据格式
    String textJson = "[{\"x\":\"919,918,916,915,915,914,914,914,915,917,919,922,924,925,925,925,924,923,921\",\"y\":\",1568,1569,1573,1579,1581,1583,1583,1581,1578,1576,1575,1575,1576,1578,1580,1581,1583,1584,1584\"},{\"x\":\"920,921,923,924,926,928,930,932,934\",\"y\":\"1561,1563,1567,1570,1574,1577,1580,1584,1586\"},{\"x\":\"910,911,912,916,921,925,928,930,931\",\"y\":\"1570,1569,1569,1568,1568,1567,1566,1565,1565\"},{\"x\":\"936,935,934,933,932,930,928,926\",\"y\":\"1570,1570,1571,1571,1572,1573,1576,1578\"},{\"x\":\"927,928,929,930\",\"y\":\"1562,1562,1564,1566\"},{\"x\":\"930,930,929,928,926\",\"y\":\"1590,1590,1591,1593,1596\"},{\"x\":\"912,912,938,940,942,943,943\",\"y\":\"1597,1599,1599,1599,1599,1599,1599\"},{\"x\":\"915,916,918,921,924,927,931,933,935,936,936,936,936,936,936,936,935,934,934,950,951,952,952,952,952,952,952,952,952,953,954,957,959\",\"y\":\"1593,1592,1592,1591,1590,1588,1586,1586,1586,1586,1586,1587,1587,1587,1590,1592,1594,1597,1599,1576,1576,1579,1584,1589,1593,1596,1596,1595,1595,1594,1592,1590,1587\"},{\"x\":\"923,923,923,923,923\",\"y\":\"1589,1591,1594,1596,1598\"},{\"x\":\"973,973,972,972,970,968,965,964,964,965,966,967,966,965,964,964\",\"y\":\"1558,1558,1560,1562,1566,1568,1571,1572,1572,1573,1576,1579,1581,1585,1588,1591\"},{\"x\":\"950,950,949,948,947\",\"y\":\"1565,1567,1571,1576,1580\"},{\"x\":\"968,974,976,978,979,979,979,979,979,978,977,976,974,972,970\",\"y\":\"1575,1574,1573,1571,1571,1571,1572,1574,1575,1578,1582,1584,1588,1591,1593\"},{\"x\":\"949,951,953,956,959,961,961,961,961,961,960,959,958\",\"y\":\"1573,1573,1572,1570,1568,1568,1568,1569,1570,1572,1574,1577,1580\"},{\"x\":\"969,969,970,971,973,975,977,980,982,985,988,989,991,993\",\"y\":\"1583,1583,1584,1584,1586,1587,1588,1590,1591,1593,1594,1594,1594,1593\"}]";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_recognition;
    }

    @Override
    protected void initView() {
        tv_title = findViewById(R.id.tv_title);
        rl_left = findViewById(R.id.rl_left);
        rl_right = findViewById(R.id.rl_right);
        iv_left = findViewById(R.id.iv_left);
        iv_right = findViewById(R.id.iv_right);

        rl_left.setOnClickListener(this);
        iv_left.setOnClickListener(this);
        tv_title.setText(getResources().getString(R.string.recognition));
        iv_right.setVisibility(View.GONE);

        mTextView = findViewById(R.id.tv_recognition);
    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        if (intent != null) {
//            dotsList = (List<Dot>) DataHolder.getInstance().getData("value");
//            Log.i(TAG, "onCreate: dotsList =" + dotsList.size());
            if(dotsList == null){
                showToast(getResources().getString(R.string.no_data_recognition));
                return;
            }
            showLoadingDialog(RecognitionActivity.this,getString(R.string.sbz));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //先去从服务器获取key
                        //JSONObject jsonObject = new JSONObject();
                        //jsonObject.put("cust", keyUser);
                        //String keyData = requestService(keyUrl, jsonObject.toString());
                        //key = getRecognitionResult(keyData);
                        Log.i(TAG, "run: " + key);
                        //再去调用手写识别接口
                        final String info = getJsonString(dotsList);
                        final String value = requestService(url, info);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextView.setText(getRecognitionResult(value));


                            }
                        });
                        hideLoadingDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * 对识别数据进行封装
     *
     * @param list
     * @return
     */
    private String getJsonString(List<Dot> list) {
        JSONObject json = new JSONObject();
        try {
            json.put("viewSizeHeight", 1900);
            json.put("viewSizeWidth", 2300);
            json.put("applicationKey", key);
            json.put("scriptType", "Text");
            //json.put("scriptType","Math");
            json.put("languages", "zh_CN");
            json.put("xDPI", 20);
            json.put("yDPI", 20);


//            JSONArray jsonArray = new JSONArray(textJson);
//            json.put("penData", jsonArray);

            JSONArray jsonArray = new JSONArray();
            if (list != null && list.size() > 0) {
                JSONObject jsonPoint = null;
                String xValue = "";
                String yValue = "";
                for (Dot dot : list) {
                    Log.i(TAG, "getJsonString: " + dot.toString());
                    if (dot.type == Dot.DotType.PEN_DOWN) {
                        jsonPoint = new JSONObject();
                        xValue += DotUtils.joiningTogether(dot.x, dot.fx);
                        yValue += DotUtils.joiningTogether(dot.y, dot.fy);
                    } else if (dot.type == Dot.DotType.PEN_MOVE) {
                        xValue += "," + DotUtils.joiningTogether(dot.x, dot.fx);
                        yValue += "," + DotUtils.joiningTogether(dot.y, dot.fy);
                    } else if (dot.type == Dot.DotType.PEN_UP) {
                        xValue += "," + DotUtils.joiningTogether(dot.x, dot.fx);
                        yValue += "," + DotUtils.joiningTogether(dot.y, dot.fy);
                        jsonPoint.put("x", xValue);
                        jsonPoint.put("y", yValue);
                        jsonArray.put(jsonPoint);
                        xValue = "";
                        yValue = "";
                    }
                }
            }
            json.put("penData", jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i(TAG, "getJsonString: " + json.toString());

        BLEFileUtil.writeTxtToFile(json.toString(), "/mnt/sdcard/tql/", "test.txt");
        return json.toString();
    }

    public String requestService(String urlValue, String keyValue) {
        OutputStream out = null;
        BufferedReader br = null;
        String msg = "";

        if (keyValue != null && !keyValue.equals("")) {
            try {
                URL url = new URL(urlValue);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                // 设置通用的请求属性
                httpURLConnection.setRequestProperty("accept", "*/*");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("charset", "utf-8");
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                out = httpURLConnection.getOutputStream();
                byte[] buf = keyValue.toString().getBytes();
                out.write(buf);

                int code = httpURLConnection.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    InputStream in = httpURLConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(in);
                    br = new BufferedReader(isr);
                    String tmp = "";
                    while ((tmp = br.readLine()) != null) {
                        msg += tmp;
                    }
                    Log.i(TAG, "onSuccess: " + msg);
                } else {
                    Log.i(TAG, "onFailure: " + code);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return msg;
    }

    /**
     * 解析服务器返回的数据
     *
     * @param value
     * @return
     */
    public String getRecognitionResult(String value) {
        String result = "";
        if (value != null && !value.trim().equals("")) {
            try {
                JSONObject jsonObject = new JSONObject(value);
                Log.i(TAG, "getRecognitionResult: " + jsonObject.toString());
                int code = jsonObject.getInt("code");
                result = jsonObject.getString("data");
            } catch (JSONException e) {
                e.printStackTrace();

            }
        }

        return result;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
            case R.id.iv_left:
                dotsList = null;//页面被回收的时候   减少内存
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dotsList = null;//页面被回收的时候   减少内存
    }
}
