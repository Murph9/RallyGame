package rallygame.helper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import org.junit.jupiter.api.RepeatedTest;

public class RandTest {

    @RepeatedTest(100)
    public void randBetweenV2() {
        var min = new Vector2f(-2, 0);
        var max = new Vector2f(9, 4);
        var b = Rand.randBetween(min, max);

        assertTrue(b.x >= min.x, "lower x bound failed");
        assertTrue(b.x <= max.x, "higher x bound failed");
        assertTrue(b.y >= min.y, "lower y bound failed");
        assertTrue(b.y <= max.y, "higher y bound failed");
    }

    @RepeatedTest(100)
    public void randBetweenV3() {
        var min = new Vector3f(-2, 0, 58);
        var max = new Vector3f(9, 4, 59);
        var b = Rand.randBetween(min, max);
        
        assertTrue(b.x >= min.x, "lower x bound failed");
        assertTrue(b.x <= max.x, "higher x bound failed");
        assertTrue(b.y >= min.y, "lower y bound failed");
        assertTrue(b.y <= max.y, "higher y bound failed");
        assertTrue(b.z >= min.z, "lower z bound failed");
        assertTrue(b.z <= max.z, "higher z bound failed");
    }

    @RepeatedTest(100)
    public void randV2f() {
        Vector2f result = Rand.randV2f(1, false);
        assertTrue(result.x < 1);
        assertTrue(result.y < 1);

        assertTrue(result.x >= 0);
        assertTrue(result.y >= 0);
    }

    @RepeatedTest(100)
    public void randV3f() {
        Vector3f result = Rand.randV3f(1, false);
        assertTrue(result.x < 1);
        assertTrue(result.y < 1);
        assertTrue(result.z < 1);

        assertTrue(result.x >= 0);
        assertTrue(result.y >= 0);
        assertTrue(result.z >= 0);
    }

    @RepeatedTest(100)
    public void randFromArray() {
        Float[] array = new Float[] { 0f, 1f, 7f, 3f, 4f, 5f };
        float result = Rand.randFromArray(array);
        assertTrue(Arrays.asList(array).contains(result));
    }
}
