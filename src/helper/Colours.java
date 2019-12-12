package helper;

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

    public static ColorRGBA lerpColor(float value, ColorRGBA a, ColorRGBA b) {
        return new ColorRGBA().interpolateLocal(a, b, value);
    }
}
