package com.sonix.oidbluetooth.adapter;

import android.graphics.Color;
import android.util.Log;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.sonix.oidbluetooth.R;
import com.sonix.oidbluetooth.bean.BitErrorBean;

import java.text.DecimalFormat;
import java.util.List;

public class BitErrorAdapter extends BaseQuickAdapter<BitErrorBean, BaseViewHolder> {

    private int invalidCodeNumber, mSetProbability = 10;//默认值是10

    /**
     * Same as QuickAdapter#QuickAdapter(Context,int) but with
     * some initialization data.
     *
     * @param data A new list is created out of this one to avoid mutable list
     */
    public BitErrorAdapter(@Nullable List<BitErrorBean> data) {
        super(R.layout.item_point_read, data);
    }

    /**
     * Implement this method and use the helper to adapt the view to the given item.
     *
     * @param helper A fully initialized helper.
     * @param item   The item that needs to be displayed.
     */
    @Override
    protected void convert(BaseViewHolder helper, BitErrorBean item) {

        helper.setGone(R.id.point_original_data_layout, item.getFormatData().equals("无效码"));
        helper.setText(R.id.point_read_code, item.getFormatData())
                .setText(R.id.point_type,"第"+ (helper.getLayoutPosition() + 1)+"码点")
                .setText(R.id.point_original_data, item.getOriginalData());

        int color;
        if(item.getFormatData().equals("无效码")){
            color = Color.parseColor("#FFCF1111");
        }else{
            color = Color.parseColor("#0026E3");
        }

        helper.setTextColor(R.id.point_type,color)
                .setTextColor(R.id.point_original_data_tv,color)
                .setTextColor(R.id.point_original_data,color)
                .setTextColor(R.id.point_read_code_tv,color)
                .setTextColor(R.id.point_read_code,color);


    }

    public void setInvalidCodeNumber(int invalidCodeNumber) {
        this.invalidCodeNumber = invalidCodeNumber;
    }

    public void setSetProbability(int setProbability) {
        this.mSetProbability = setProbability;
        getData().clear();
        number = 0;
        invalidCodeNumber = 0;
        notifyDataSetChanged();
    }

    public void addInvalidCode() {
        invalidCodeNumber++;
    }

    private double number;

    public void setNumber(double number) {
        this.number = number;
    }

    public int getSetProbability() {
        return mSetProbability;
    }

    public double getProbability() {
        if(getData().size() == 0){
            return 0;
        }

        if (mSetProbability == 0) {
            number = invalidCodeNumber * 100 / Double.valueOf(getData().size());
        } else if (getData().size() % mSetProbability == 0) {//能整除的情况
            number = invalidCodeNumber * 100 / Double.valueOf(mSetProbability);
            invalidCodeNumber = 0;//重置
        }
        return number;

    }


}
