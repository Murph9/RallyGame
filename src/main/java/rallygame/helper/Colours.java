package rallygame.helper;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

public class Colours {

    public static ColorRGBA getOnRGBScale(float value) {
        // 0 is white, 0.333f is green, 0.666f is red, 1 is blue
        value = FastMath.clamp(Math.abs(value), 0, 1);

        if (value < 1f/3f)
            return lerpColor(value*3, ColorRGBA.White, ColorRGBA.Green);
        else if (value < 2f/3f)
            return lerpColor((value - 1f/3f) * 3, ColorRGBA.Green, ColorRGBA.Red);
        return lerpColor((value - 2f/3f)*3, ColorRGBA.Red, ColorRGBA.Blue);
    }

    public static ColorRGBA getOnGreenToRedScale(float value) {
        //0 is green, 1 is red
        if (value < 0) return ColorRGBA.Green;
        if (value > 1) return ColorRGBA.Red;
        return lerpColor(value, ColorRGBA.Green, ColorRGBA.Red);
    }

    public static ColorRGBA lerpColor(float value, ColorRGBA a, ColorRGBA b) {
        return new ColorRGBA().interpolateLocal(a, b, value);
    }

    public static ColorRGBA randomColourHSV() {
        float[] rgb = hsvToRGB(FastMath.rand.nextInt(360), (FastMath.rand.nextFloat() / 2) + 0.5, (FastMath.rand.nextDouble() / 2) + 0.5);
        return new ColorRGBA(rgb[0], rgb[1], rgb[2], 1);
    }

    //#region hsv to rgb
    // 0 <= h < 360, 0 <= s< 1, 0 <= v< 1, type
    private static float[] hsvToRGB(int h, double s, double v) {
        float[] out = new float[3];
        double[] temp = new double[3];
        double c = v * s;
        int tempH = h / 60;
        double x = c * (1 - Math.abs(tempH % 2 - 1));
        double m = v - c;
        if (tempH < 1) {
            temp[0] = c;
            temp[1] = x;
            temp[2] = 0;
        } else if (tempH < 2) {
            temp[0] = x;
            temp[1] = c;
            temp[2] = 0;
        } else if (tempH < 3) {
            temp[0] = 0;
            temp[1] = c;
            temp[2] = x;
        } else if (tempH < 4) {
            temp[0] = 0;
            temp[1] = x;
            temp[2] = c;
        } else if (tempH < 5) {
            temp[0] = x;
            temp[1] = 0;
            temp[2] = c;
        } else if (tempH < 6) {
            temp[0] = c;
            temp[1] = 0;
            temp[2] = x;
        } else { // just in case
            temp[0] = 0;
            temp[1] = 0;
            temp[2] = 0;
        }
        out[0] = (float) (temp[0] + m);
        out[1] = (float) (temp[1] + m);
        out[2] = (float) (temp[2] + m);
        return out;
    }
    //#endregion
}
