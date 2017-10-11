package com.tudan.smartlamp.model;

/**
 * Created by mario on 10.10.2017..
 */

public class HSLColor {
    private float hue;
    private float saturation;
    private float lightness;

    public HSLColor(float hue, float saturation, float lightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.lightness = lightness;
    }

    /**
     * Constructor when using Integer values, saturation and lightness in range of 0..100
     *
     * @param hue        Hue 0..360
     * @param saturation Saturation 0..100 converted to 0..1
     * @param lightness  Lightness 0..100 converted to 0..1
     */
    public HSLColor(int hue, int saturation, int lightness) {
        this.hue = hue;
        this.saturation = saturation / 100f;
        this.lightness = lightness / 100f;
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    /**
     * Saturation setter using Integer value
     *
     * @param saturation Saturation range 0..100 converted to 0..1
     */
    public void setSaturation(int saturation) {
        this.saturation = saturation / 100f;
    }

    public float getLightness() {
        return lightness;
    }

    public void setLightness(float lightness) {
        this.lightness = lightness;
    }

    /**
     * Lightness setter using Integer value
     *
     * @param lightness Lightness range 0..100 converted to 0..1
     */
    public void setLightness(int lightness) {
        this.lightness = lightness / 100f;
    }

    public HSLColor update(float hue, float saturation, float lightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.lightness = lightness;
        return this;
    }

    /**
     * Update using Integer values, saturation and lightness in range of 0..100
     *
     * @param hue        Hue 0..360
     * @param saturation Saturation 0..100 converted to 0..1
     * @param lightness  Lightness 0..100 converted to 0..1
     */
    public HSLColor update(int hue, int saturation, int lightness) {
        this.hue = hue;
        this.saturation = saturation / 100f;
        this.lightness = lightness / 100f;
        return this;
    }
}
