package rallygame.helper;

import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class Rand {
    
    public static Vector2f randBetween(Vector2f min, Vector2f max) {
        var diff = max.subtract(min);
        var out = randV2f(1, false);
        return out.multLocal(diff).addLocal(min);
    }

    public static Vector3f randBetween(Vector3f min, Vector3f max) {
        var diff = max.subtract(min);
        var out = randV3f(1, false);
        return out.multLocal(diff).addLocal(min);
    }

    /**
     * Generate a random Vector2f([0,1), [0,1)) scaleNegative for [-1, 1) parts
     */
    public static Vector2f randV2f(float max, boolean scaleNegative) {
        float offset = scaleNegative ? max : 0;
        float scale = scaleNegative ? 2 : 1;
        return new Vector2f(FastMath.nextRandomFloat() * scale * max - offset,
                FastMath.nextRandomFloat() * scale * max - offset);
    }

    /**
     * Generate a random Vector3f([0,1), [0,1), [0,1)) scaleNegative for [-1, 1)
     * parts
     */
    public static Vector3f randV3f(float max, boolean scaleNegative) {
        float offset = scaleNegative ? max : 0;
        float scale = scaleNegative ? 2 : 1;
        return new Vector3f(FastMath.nextRandomFloat() * scale * max - offset,
                FastMath.nextRandomFloat() * scale * max - offset, FastMath.nextRandomFloat() * scale * max - offset);
    }

    public static <T> T randFromArray(T[] array) {
        return array[FastMath.nextRandomInt(0, array.length - 1)];
    }

    public static <T> T randFromList(List<T> list) {
        return list.get(FastMath.nextRandomInt(0, list.size() - 1));
    }
}