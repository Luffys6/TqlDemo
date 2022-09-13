package com.sonix.surfaceview;

public interface OnGestureListener {

    void onDrag(Float dx, Float dy);

    void onFling(Float startX, Float startY, Float velocityX,
                 Float velocityY);

    void onScale(Float scaleFactor, Float focusX, Float focusY);
}
