package rallygame;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import rallygame.helper.Colours;
import rallygame.helper.H;

public class HTest {

    @Test
    public void str() {
        assertEquals("1,2", H.str(Arrays.asList(1, 2), ","));
        assertEquals("1,2", H.str(new Integer[] { 1, 2 }, ","));
        assertEquals("1,2", H.join(1, 2));
    }

    @Test
    public void oneTrue() {
        assertTrue(!H.oneTrue((x) -> x, new Boolean[] { }));
        assertTrue(H.oneTrue((x) -> x, new Boolean[] { true, false, false }));
        assertTrue(!H.oneTrue((x) -> x, new Boolean[] { false, false, false }));
        assertTrue(H.oneTrue((x) -> x, new Boolean[] { true, true, true }));
    }
    
    @Test
    public void allTrue() {
        assertTrue(H.allTrue((x) -> x, new Boolean[] {}));
        assertTrue(!H.allTrue((x) -> x, new Boolean[] { true, false, false }));
        assertTrue(!H.allTrue((x) -> x, new Boolean[] { false, false, false }));
        assertTrue(H.allTrue((x) -> x, new Boolean[] { true, true, true }));
    }

    @Test
    public void dotXZ() {
        assertEquals(1, H.dotXZ(new Vector3f(0, 0, 1), new Vector3f(0, 1, 1)));
        assertEquals(0, H.dotXZ(new Vector3f(2, 1, 0), new Vector3f(0, 0, 2)));
        assertEquals(2, H.dotXZ(new Vector3f(2, 1, 0), new Vector3f(1, 0, 1)));
    }

    @Test
    public void roundDecimal() {
        assertEquals("1.2", H.roundDecimal(1.23f, 1));
        assertEquals("1.235", H.roundDecimal(1.23456f, 3));
        assertNotEquals("10.674", H.roundDecimal(10.6745, 3));
    }
    @Test 
    public void decimalFormat() {
        assertEquals("001.2", H.decimalFormat(1.23f, "#000.0"));
        assertEquals("0001.2", H.decimalFormat(1.23456f, "0000.0"));
        assertNotEquals("0010.6", H.decimalFormat(10.6745f, "0000.0"));
    }

    @Test
    public void rectFromLine() {
        Vector3f[] result = H.rectFromLineXZ(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), 1);
        assertEquals(result[0], new Vector3f(-0.5f, 0, 0));
        assertEquals(result[0], new Vector3f(-0.5f, 0, 1));
        assertEquals(result[0], new Vector3f(0.5f, 0, 1));
        assertEquals(result[0], new Vector3f(0.5f, 0, 0));
    }

    @Test
    public void distFromLineXZ() {
        assertEquals(1f, H.distFromLineXZ(new Vector3f(), new Vector3f(1,0,0), new Vector3f(0,0,1)));
        assertEquals(1/Math.sqrt(2), H.distFromLineXZ(new Vector3f(), new Vector3f(1,0,1), new Vector3f(0,0,1)), 0.0001);
    }

    @Test
    public void lerpArray() {
        assertEquals(1.5f, H.lerpArray(1.5f, new float[] { 0, 1, 2 }));
        assertEquals(2f, H.lerpArray(2f, new float[] { 0, 1, 2 }));
        assertEquals(1.22f, H.lerpArray(1.22f, new float[] { 0, 1, 2 }));
    }
    
    @Test
    public void lerpColor() {
        assertEquals(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f), 
                Colours.lerpColor(0.5f, new ColorRGBA(0, 0, 0, 0), new ColorRGBA(1, 1, 1, 1)));
        assertEquals(new ColorRGBA(1, 1, 1, 1), 
                Colours.lerpColor(1, new ColorRGBA(0, 0, 0, 0), new ColorRGBA(1, 1, 1, 1)));
        assertEquals(new ColorRGBA(0, 0, 0, 0), 
                Colours.lerpColor(0, new ColorRGBA(0, 0, 0, 0), new ColorRGBA(1, 1, 1, 1)));
    }

    @Test
    public void clamp() {
        assertEquals(12, H.clamp(10, 12, 120));
        assertEquals(120, H.clamp(1320, 12, 120));
        assertEquals(100, H.clamp(100, 12, 120));
    }

    @Test
    public void cylinderInertia() {
        assertEquals(1, H.cylinderInertia(1, 2));
        assertEquals(10000/2, H.cylinderInertia(100, 1));
    }

    @Test
    public void substringBeforeFirst() {
        assertEquals("hello w", H.substringBeforeFirst("hello world", 'o'));
    }
    @Test
    public void substringAfterLast() {
        assertEquals("rld", H.substringAfterLast("hello world", 'o'));
    }

    @Test
    public void leftPad() {
        assertEquals("     aaa", H.leftPad("aaa", 8, ' '));
        assertEquals("aaa", H.leftPad("aaa", 3, 'x'));
    }

    @Test
    public void addTogetherNew() {
        float[] one = new float[] { 0, 1, 2 };
        float[] two = new float[] { 4, 3, 2 };
        assertArrayEquals(new float[] { 4, 4, 4 }, H.addTogetherNew(one, two));
        assertArrayEquals(new float[] { 4, 4, 4 }, H.addTogetherNew(two, one));
        assertArrayEquals(new float[] { 0, 0, 0 }, H.addTogetherNew(one, new float[]{ 0, -1, -2}));
    }

    @Test
    public void closestTo() {
        Vector3f a = new Vector3f(1,0,0);
        Vector3f b = new Vector3f(0,1,0);
        Vector3f c = new Vector3f(0,0,1);
        Vector3f[] list = new Vector3f[] { a, b, c };
        assertEquals(a, H.closestTo(new Vector3f(0.1f, 0, 0), list));
        assertEquals(c, H.closestTo(new Vector3f(0, 0, 0.1f), list));
        assertEquals(b, H.closestTo(new Vector3f(0, 0.1f, 0), list));
    }

    @Test
    public void v3tov2fXZ() {
        Vector3f a = new Vector3f(1.2f,4.1f,9f);
        assertEquals(a.x, H.v3tov2fXZ(a).x);
        assertEquals(a.z, H.v3tov2fXZ(a).y);
    }

    @Test
    public void v2tov3fXZ() {
        Vector2f a = new Vector2f(1.2f, 4.1f);
        assertEquals(1.2f, H.v2tov3fXZ(a).x);
        assertEquals(0, H.v2tov3fXZ(a).y);
        assertEquals(4.1f, H.v2tov3fXZ(a).z);
    }

    @Test
    public void v3tov2fXZ_v2tov3fXZ() {
        Vector3f a = new Vector3f(1, 2, 3);
        assertEquals(a.x, H.v2tov3fXZ(H.v3tov2fXZ(a)).x);
        assertEquals(0, H.v2tov3fXZ(H.v3tov2fXZ(a)).y);
        assertEquals(a.z, H.v2tov3fXZ(H.v3tov2fXZ(a)).z);
    }

    @RepeatedTest(100)
    public void randV3f() {
        Vector3f result = H.randV3f(1, false);
        assertTrue(result.x <= 1);
        assertTrue(result.y <= 1);
        assertTrue(result.z <= 1);
    }

    @RepeatedTest(100)
    public void randFromArray() {
        Float[] array = new Float[] { 0f, 1f, 7f, 3f, 4f, 5f };
        float result = H.randFromArray(array);
        assertTrue(Arrays.asList(array).contains(result));
    }

    @Test
    public void maxInArray() {
        assertEquals(7, H.maxInArray(new float[] { 0, 1, 7, 3, 4, 5 }));
        assertEquals(1, H.maxInArray(new float[] { 1 }));
        assertEquals(-12, H.maxInArray(new float[] { -12, -65 }));
        assertEquals(2, H.maxInArray(new float[] { 2, -3 }));
    }

    @Test
    public void boundingBoxXZ() {
        float[] result = H.boundingBoxXZ(new Vector3f());
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
        assertEquals(0, result[3]);

        result = H.boundingBoxXZ(new Vector3f(-1, 0, 1), new Vector3f(1, 0, -1));
        assertEquals(-1, result[0]);
        assertEquals(-1, result[1]);
        assertEquals(1, result[2]);
        assertEquals(1, result[3]);

        result = H.boundingBoxXZ(new Vector3f(0, 1, 0));
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
        assertEquals(0, result[3]);
    }


    @Test
    public void skew() {
        assertEquals(5, H.skew(5, 0, 10, 0, 10));
        assertEquals(0, H.skew(0, 0, 10, 0, 10));
        assertEquals(1, H.skew(0, 0, 1, 1, 2));
        
        assertEquals(1, H.skew(5, 4, 5, 0, 1));

        assertEquals(0.5f, H.skew(4.5f, 4, 5, 0, 1));
        assertEquals(1.5f, H.skew(5.5f, 4, 5, 0, 1));

        assertEquals(-0.5f, H.skew(1, 0, 2, -1, 0));
        assertEquals(0.5f, H.skew(0, -2, 2, 0, 1));
        assertEquals(0.75f, H.skew(1, -2, 2, 0, 1));

        assertEquals(-1, H.skew(0.25f, 0.75f, 1.25f, 0, 1));
    }
}