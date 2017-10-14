package com.tudan.smartlamp.utils;

import com.tudan.smartlamp.model.RGBColor;

/**
 * Created by mario on 14.10.2017..
 */

public class ColorHelper {
    public static RGBColor getRGBColor(String colorStr) {
        int red = Integer.valueOf(colorStr.substring(0, 3));
        int green = Integer.valueOf(colorStr.substring(3, 6));
        int blue = Integer.valueOf(colorStr.substring(6, 9));
        RGBColor rgbColor = new RGBColor(red, green, blue);
        RGBEyeFix.reverseFixedColor(rgbColor);
        return rgbColor;

    }
}
