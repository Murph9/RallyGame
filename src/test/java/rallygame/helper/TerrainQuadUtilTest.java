package rallygame.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

public class TerrainQuadUtilTest {
   
    @Test
    public void getTerrainGridBoundingBox() {
        Vector3f scale = new Vector3f(10, 10, 10);

        Vector3f a = new Vector3f(-63.5f, 0.94f, -63.5f);
        Vector3f b = new Vector3f(-56.4f, 0.94f, -56.4f);
        Vector3f c = new Vector3f(-56.4f, 0.94f, -56.4f);
        Vector3f d = new Vector3f(-63.5f, 0.94f, -63.5f);
        

        Vector3f[] quad = new Vector3f[] { a, b, c, d };
        float[] result = TerrainQuadUtil.getTerrainGridBoundingBox(scale, quad);

        assertEquals(4, result.length);
        assertEquals(-70, result[0]);
        assertEquals(-70, result[1]);
        assertEquals(-50, result[2]);
        assertEquals(-50, result[3]);
    }

    @Test
    public void getGridPosBoundingQuad() {
        Vector3f scale = new Vector3f(2, 10, 7);

        Vector3f a = new Vector3f(-63.5f, 0.94f, -63.5f);
        Vector3f b = new Vector3f(-56.4f, 0.94f, -56.4f);
        Vector3f c = new Vector3f(-56.4f, 0.94f, -56.4f);
        Vector3f d = new Vector3f(-63.5f, 0.94f, -63.5f);

        Vector3f[] quad = new Vector3f[] { a, b, c, d };
        float[] boundingBox = TerrainQuadUtil.getTerrainGridBoundingBox(scale, quad);
        List<Vector2f> result = TerrainQuadUtil.getGridPosBoundingQuad(scale, quad);

        for (float i = boundingBox[0]; i <= boundingBox[2]; i += scale.x) {
            for (float j = boundingBox[1]; j <= boundingBox[3]; j += scale.z) {
                Vector2f pos = new Vector2f(i, j);
                assertTrue(result.contains(pos), "list did not contain " + pos);
            }
        }
    }

    @Test
    public void getHeightsForQuads() {
        Vector3f scale = new Vector3f(1,1,1);

        Vector3f[] quad = new Vector3f[] { 
                new Vector3f(0,0,0), new Vector3f(2,0,0),
                new Vector3f(0,2,2), new Vector3f(2,2,2)
            };
        Vector3f[] quad2 = new Vector3f[] {
                new Vector3f(10,10,10), new Vector3f(10,10,12),
                new Vector3f(12,12,10), new Vector3f(12,12,12)
        };

        var result = TerrainQuadUtil.getHeightsForQuads(scale, Arrays.asList(quad, quad2));

        assertTrue(!result.isEmpty());
        for (Entry<Vector2f, Float> pos: result.entrySet()) {
            Vector2f p = new Vector2f(pos.getKey().x % 10, pos.getKey().y % 10);
            float height = pos.getValue() % 10;
            //specific corner tests
            if (p.x == 0 && p.y == 0) {
                assertEquals(0, height, 1e-4, "height not correct");
            } else if (p.x == 2 && p.y == 2) {
                assertEquals(2, height, 1e-4, "height not correct");
            } else {
                assertTrue(height >= 0 - 1e-4 && height <= 2 + 1e-4, "not in range");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6})
    public void calcWorldExtents(int x) {
        Vector3f rand = Rand.randV3f(1000, true);

        int size = 1 << x;
        var heightMap = new float[(size)*(size)];
        Arrays.fill(heightMap, 0.3f);
        TerrainQuad quad = new TerrainQuad("test terrain", size + 1, size + 1, heightMap);
        quad.setLocalTranslation(rand);

        var boundingVolume = TerrainQuadUtil.calcWorldExtents(quad);

        assertEquals(rand, boundingVolume.getCenter(), "Center not set correctly");
    }

    @Test
    public void getClosestGridPoint() {
        int size = 1 << 2;
        var heightMap = new float[(size)*(size)];
        Arrays.fill(heightMap, 0.3f);
        TerrainQuad quad = new TerrainQuad("test terrain", size + 1, size + 1, heightMap);
        quad.setLocalScale(5, 3, 2);
        
        // same
        assertEquals(new Vector2f(), TerrainQuadUtil.getClosestGridPoint(quad, new Vector2f(0, 0)));
        assertEquals(new Vector2f(5, 2), TerrainQuadUtil.getClosestGridPoint(quad, new Vector2f(5, 2)));
        assertEquals(new Vector2f(-10, -2), TerrainQuadUtil.getClosestGridPoint(quad, new Vector2f(-10, -2)));

        // close
        assertEquals(new Vector2f(5, 2), TerrainQuadUtil.getClosestGridPoint(quad, new Vector2f(7.4f, 1.1f)));
        assertEquals(new Vector2f(-5, 2), TerrainQuadUtil.getClosestGridPoint(quad, new Vector2f(-3f, 2.9f)));
        assertEquals(new Vector2f(-500, 0), TerrainQuadUtil.getClosestGridPoint(quad, new Vector2f(-502.4f, 0.99f)));
    }
}
