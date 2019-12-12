package test;

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

import helper.Colours;
import helper.H;

public class HTest {

    @Test
    public void roundDecimal() {
        assertEquals("1.2", H.roundDecimal(1.23f, 1));
        assertEquals("1.235", H.roundDecimal(1.23456f, 3));
        assertNotEquals("10.674", H.roundDecimal(10.6745, 3));
    }

    @Test
    public void allTrue() {
        assertTrue(H.allTrue((x) -> { return x != -1; }, 0, 1, 2));
        assertFalse(H.allTrue((x) -> { return x != 1; }, 0, 1, 2));
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
    public void lerpTorqueArray() {
        assertEquals(1.5f, H.lerpTorqueArray(1500, new float[] { 0, 1, 2 }));
        assertEquals(2f, H.lerpTorqueArray(2000, new float[] { 0, 1, 2 }));
        assertEquals(1.22f, H.lerpTorqueArray(1220, new float[] { 0, 1, 2 }));
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
}