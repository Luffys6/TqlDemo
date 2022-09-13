package com.sonix.oidbluetooth.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sonix.oidbluetooth.R;

import java.util.ArrayList;
import java.util.List;

public class PointReadCodeAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;

    private List<String> pointList;
    private List<Long> pointIndexList;
    private int i;


    public PointReadCodeAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        pointList = new ArrayList<>();
        pointIndexList = new ArrayList<>();
    }

    public void clearDevice() {
        pointList.clear();
        pointIndexList.clear();
       notifyDataSetChanged();
    }

    public void addDevice(String info, long index, int i) {
        pointList.add(info);
        pointIndexList.add(index);
        this.i = i;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return pointList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
         ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_point_read, parent, false);
            //生成一个ViewHolder的对象
            holder = new ViewHolder();

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.pointType = convertView.findViewById(R.id.point_type);
        holder.pointOriginalData = convertView.findViewById(R.id.point_original_data);
        holder.pointReadCode = convertView.findViewById(R.id.point_read_code);
        if (pointList != null && pointList.size() != 0) {
            holder.pointReadCode.setText(pointList.get(position));
        }
        holder.pointOriginalData.setText(String.valueOf(pointIndexList.get(position)));
        if (i == 1) {
            holder.pointType.setText("在线数据,第"+(position+1)+"个点读码:");
        } else {
            holder.pointType.setText("离线数据,第"+(position+1)+"个点读码:");
        }
        return convertView;
    }

     class ViewHolder {
        public TextView pointType;
        public TextView pointReadCode;
        public TextView pointOriginalData;
    }
}
