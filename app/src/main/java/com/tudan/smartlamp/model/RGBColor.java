package com.tudan.smartlamp.model;

import android.graphics.Color;

/**
 * Created by mario on 12.10.2017..
 */

public class RGBColor {
    private int red;
    private int green;
    private int blue;

    public RGBColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public RGBColor(int intColor) {
        this.red = Color.red(intColor);
        this.green = Color.green(intColor);
        this.blue = Color.blue(intColor);
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }
}

