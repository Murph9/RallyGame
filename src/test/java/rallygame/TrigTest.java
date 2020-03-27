package rallygame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import rallygame.helper.Trig;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class TrigTest {

    @Test
    public void boundingBoxXZ() {
        float[] result = Trig.boundingBoxXZ(new Vector3f());
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
        assertEquals(0, result[3]);

        result = Trig.boundingBoxXZ(new Vector3f(-1, 0, 1), new Vector3f(1, 0, -1));
        assertEquals(-1, result[0]);
        assertEquals(-1, result[1]);
        assertEquals(1, result[2]);
        assertEquals(1, result[3]);

        result = Trig.boundingBoxXZ(new Vector3f(0, 1, 0));
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
        assertEquals(0, result[3]);

        //some 'random' numbers test
        result = Trig.boundingBoxXZ(
            new Vector3f(-63.535f, 0.94f, -63.535f),
            new Vector3f(-56.4644f, 0.94f, -56.4644f),
            new Vector3f(-56.499f, 0.94f, -56.499f),
            new Vector3f(-63.500f, 0.94f, -63.500f));
        assertEquals(-63.535f, result[0]);
        assertEquals(-63.535f, result[1]);
        assertEquals(-56.4644f, result[2]);
        assertEquals(-56.4644f, result[3]);
    }

    @Test
    public void dotXZ() {
        assertEquals(1, Trig.dotXZ(new Vector3f(0, 0, 1), new Vector3f(0, 1, 1)));
        assertEquals(0, Trig.dotXZ(new Vector3f(2, 1, 0), new Vector3f(0, 0, 2)));
        assertEquals(2, Trig.dotXZ(new Vector3f(2, 1, 0), new Vector3f(1, 0, 1)));
    }

    @Test
    public void rectFromLineXZ() {
        Vector3f[] result = Trig.rectFromLineXZ(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), 1);
        assertEquals(result[0], new Vector3f(-0.5f, 0, 0));
        assertEquals(result[1], new Vector3f(-0.5f, 0, 1));
        assertEquals(result[2], new Vector3f(0.5f, 0, 1));
        assertEquals(result[3], new Vector3f(0.5f, 0, 0));
    }

    @Test
    public void distFromLineXZ() {
        assertEquals(1f, Trig.distFromLineXZ(new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1)));
        assertEquals(1 / Math.sqrt(2),
                Trig.distFromLineXZ(new Vector3f(), new Vector3f(1, 0, 1), new Vector3f(0, 0, 1)), 0.0001);
    }

    @Test
    public void heightInQuad() {
        Vector3f a = new Vector3f(0, 0, 0);
        Vector3f b = new Vector3f(1, 1, 1);
        Vector3f c = new Vector3f(1, 1, -1);
        Vector3f d = new Vector3f(2, 2, 0);
        float z = 0f;
        assertEquals(z, Trig.heightInQuad(new Vector2f(z, 0), a, b, c, d));
        z = 0.3f;
        assertEquals(z, Trig.heightInQuad(new Vector2f(z, 0), a, b, c, d));
        z = 1.3f;
        assertEquals(z, Trig.heightInQuad(new Vector2f(z, 0), a, b, c, d));
        z = 2f;
        assertEquals(z, Trig.heightInQuad(new Vector2f(z, 0), a, b, c, d));
        z = 4f;
        assertEquals(Float.NaN, Trig.heightInQuad(new Vector2f(z, 0), a, b, c, d));

        assertEquals(0.5f, Trig.heightInQuad(new Vector2f(0.5f, 0.5f), a, b, c, d), 0.0001f);
        assertEquals(1.5f, Trig.heightInQuad(new Vector2f(1.5f, 0.5f), a, b, c, d), 0.0001f);
        assertEquals(Float.NaN, Trig.heightInQuad(new Vector2f(1.5f, 0.51f), a, b, c, d), 0.0001f);
    }

    @Test
    public void heightInTri() {
        // isoceles base on y axis
        Vector3f a = new Vector3f(0, 1, 1);
        Vector3f b = new Vector3f(0, 1, 0);
        Vector3f c = new Vector3f(2, 3, 0.5f);
        assertEquals(2, Trig.heightInTri(a, b, c, new Vector3f(1, 999, 0.5f)));
        assertEquals(Float.NaN, Trig.heightInTri(a, b, c, new Vector3f(1, 999, 1)));
        assertEquals(Float.NaN, Trig.heightInTri(a, b, c, new Vector2f(1, 1)));
        assertEquals(1, Trig.heightInTri(a, b, c, new Vector3f(0, 1, 1)));
        assertEquals(1, Trig.heightInTri(a, b, c, new Vector2f(0, 1)));
        assertEquals(1, Trig.heightInTri(a, b, c, new Vector3f(0, 1, 0.2f)));
    }

    @Test
    public void intersectionOf2LinesGiven2PointsEach() {
        // cross
        Vector2f v1 = new Vector2f(1, -1);
        Vector2f v2 = new Vector2f(-1, 1);
        Vector2f v3 = new Vector2f(-1, -1);
        Vector2f v4 = new Vector2f(1, 1);
        assertEquals(0, Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4).x, 0.000001f);
        assertEquals(0, Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4).y, 0.000001f);

        // perpendicular
        v1 = new Vector2f(0, 3);
        v2 = new Vector2f(0, 1);
        v3 = new Vector2f(1, 2);
        v4 = new Vector2f(2, 2);
        assertEquals(0, Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4).x, 0.000001f);
        assertEquals(2, Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4).y, 0.000001f);

        // y=x and x = 10000
        v1 = new Vector2f(0, 0);
        v2 = new Vector2f(1, 1);
        v3 = new Vector2f(10000, 1);
        v4 = new Vector2f(10000, 0);
        assertEquals(10000, Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4).x, 0.000001f);
        assertEquals(10000, Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4).y, 0.000001f);

        // parrallel
        v1 = new Vector2f(0, 0);
        v2 = new Vector2f(0, 1);
        v3 = new Vector2f(1, 1);
        v4 = new Vector2f(1, 0);
        Vector2f result = Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4);
        assertTrue(Float.isNaN(result.x) || Float.isInfinite(result.x));
        assertTrue(Float.isNaN(result.y) || Float.isInfinite(result.y));

        // co-linear
        v1 = new Vector2f(0, 0);
        v2 = new Vector2f(0, 3);
        v3 = new Vector2f(0, 1);
        v4 = new Vector2f(0, 2);
        result = Trig.intersectionOf2LinesGiven2PointsEach(v1, v2, v3, v4);
        assertTrue(Float.isNaN(result.x) || Float.isInfinite(result.x));
        assertTrue(Float.isNaN(result.y) || Float.isInfinite(result.y));
    }

    @Test
    public void closestTo() {
        Vector3f a = new Vector3f(1, 0, 0);
        Vector3f b = new Vector3f(0, 1, 0);
        Vector3f c = new Vector3f(0, 0, 1);
        Vector3f[] list = new Vector3f[] { a, b, c };
        assertEquals(a, Trig.closestTo(new Vector3f(0.1f, 0, 0), list));
        assertEquals(c, Trig.closestTo(new Vector3f(0, 0, 0.1f), list));
        assertEquals(b, Trig.closestTo(new Vector3f(0, 0.1f, 0), list));
    }
}