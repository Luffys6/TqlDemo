package com.sonix.bean;

import android.graphics.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class PointsBean implements Serializable {
    private int index;
    private ArrayList<Point> movePoint;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ArrayList<Point> getMovePoint() {
        if (movePoint == null) {
            return new ArrayList<>();
        }
        return movePoint;
    }

    public void setMovePoint(ArrayList<Point> movePoint) {
        this.movePoint = movePoint;
    }



}
