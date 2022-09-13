package com.sonix.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据缓存类
 */
public class DataHolder {

    private Map dataList = new HashMap<>();
    private static volatile DataHolder instance;

    public static DataHolder getInstance() {
        if (instance == null) {
            synchronized (DataHolder.class) {
                if (instance == null) {
                    instance = new DataHolder();
                }
            }
        }
        return instance;
    }

    public void setData(String key, Object o) {
        WeakReference value = new WeakReference<>(o);
        dataList.put(key, value);
    }

    public Object getData(String key) {
        WeakReference reference = (WeakReference) dataList.get(key);
        if (reference != null) {
            Object o = reference.get();
            return o;
        }
        return null;
    }
}
