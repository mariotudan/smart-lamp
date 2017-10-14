package com.tudan.smartlamp.model;

/**
 * Created by mario on 11.10.2017..
 */

public class TouchCoordinate {
    private float x;
    private float y;
    private boolean dualTouch;

    public TouchCoordinate(float x, float y, boolean dualTouch) {
        this.x = x;
        this.y = y;
        this.dualTouch = dualTouch;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public boolean isDualTouch() {
        return dualTouch;
    }

    public void setDualTouch(boolean dualTouch) {
        this.dualTouch = dualTouch;
    }
}
