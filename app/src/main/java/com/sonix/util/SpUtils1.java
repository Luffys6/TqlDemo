package com.sonix.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @Author: DBJ
 * @Date: 2018/6/23 15:23
 * @Description:
 *
 */

public class SpUtils1 {

    private static SharedPreferences sp;
    private static SharedPreferences.Editor mEditor;
    private static SpUtils1 SpUtils;

    /**
     * 获取实例
     * @param context
     */
    public SpUtils1(Context context) {
        sp =   context.getSharedPreferences("tbkt",Context.MODE_PRIVATE);
        mEditor = sp.edit();
    }

    /**
     * 单例模式
     * @param context
     * @return
     */
    public static SpUtils1 getInstance(Context context) {
        if (SpUtils ==null){
            SpUtils =new SpUtils1(context);
        }
        return  SpUtils;
    }

    /**
     * 将数据保存在缓冲中
     * @param key
     * @param obj
     */
    public static void save(String key, Object obj){

        if(obj instanceof String){
            mEditor.putString(key, (String) obj);
        }else if(obj instanceof Boolean){
            mEditor.putBoolean(key, (Boolean) obj);
        }else if(obj instanceof Integer){
            mEditor.putInt(key, (Integer) obj);
        }else if(obj instanceof Long){
            mEditor.putLong(key, (long)obj);
        } else if(obj instanceof Float){
            mEditor.putFloat(key, (float) obj);
        }
        mEditor.commit();//保存数据到缓冲
    }
    /**
     * 获取SharedPreferences的实例
     * @param context
     * @return
     */
    public static SharedPreferences get(Context context){
        if (sp==null){
            return context.getSharedPreferences("tbkt", Context.MODE_PRIVATE);
        }
        return sp;
    }
    /**
     * 读取数据
     */
    public String getString(String key, String defValue) {
        return sp.getString(key, defValue);
    }

    public int getInt(String key, Integer defValue) {
        return sp.getInt(key, defValue);
    }

    public boolean getBoolean(String key, Boolean defValue) {
        return sp.getBoolean(key, defValue);
    }

    public double getFloat(String key, Float defValue) {
        return sp.getFloat(key, defValue);
    }



    public long getLong(String key, Long defValue) {
        return sp.getLong(key, defValue);
    }
    /**
     * 移除指定数据
     */
    public void remove(String key) {
        sp.edit().remove(key).commit();
    }
    /**
     * 清除所有数据
     * @param context
     */
    public static void clear(Context context) {
        SharedPreferences sp = context.getSharedPreferences("tbkt",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear().commit();
    }
}