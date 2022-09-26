package com.sonix.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.View;


import com.sonix.oidbluetooth.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {
    public static final String img_root = Environment.getExternalStorageDirectory() + "/dxd";

    /**
     * Save Bitmap
     *
     * @param bitName file name
     * @param bitmap  picture to save
     */
    public static File saveToLocal(Bitmap bitmap, String bitName) throws IOException {
        File file = new File(img_root);
        if (!file.exists()) {
            file.mkdirs();
        }
        File file1 = new File(img_root, bitName);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file1);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
                LogUtils.e("dbj", "保存成功");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file1;
    }


    /**
     * 获得View的截屏 Bitmap
     *
     * @param view
     * @return
     */
    public static Bitmap getMagicDrawingCache(Context context, View view, boolean quick_cache) {
        Bitmap bitmap = (Bitmap) view.getTag(R.id.cacheBitmapKey);
        Boolean dirty = (Boolean) view.getTag(R.id.cacheBitmapDirtyKey);
        if (view.getWidth() + view.getHeight() == 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        if (bitmap == null || bitmap.getWidth() != viewWidth || bitmap.getHeight() != viewHeight) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            view.setTag(R.id.cacheBitmapKey, bitmap);
            dirty = true;
        }
        if (dirty == true || !quick_cache) {
            bitmap.eraseColor(context.getResources().getColor(android.R.color.transparent));
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            view.setTag(R.id.cacheBitmapDirtyKey, false);
        }
        try {
            saveToLocal(bitmap, System.currentTimeMillis() + ".png");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static void bit(View view) {

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
//使用Canvas，调用自定义view控件的onDraw方法，绘制图片
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        try {
            saveToLocal(bitmap, System.currentTimeMillis() + ".png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
