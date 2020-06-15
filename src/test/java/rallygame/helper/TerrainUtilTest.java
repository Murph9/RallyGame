package rallygame.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
public class TerrainUtilTest {
   
    @Test
    public void getTerrainGridBoundingBox() {
        Vector3f scale = new Vector3f(10, 10, 10);

        Vector3f a = new Vector3f(-63.5f, 0.94f, -63.5f);
        Vector3f b = new Vector3f(-56.4f, 0.94f, -56.4f);
        Vector3f c = new Vector3f(-56.4f, 0.94f, -56.4f);
        Vector3f d = new Vector3f(-63.5f, 0.94f, -63.5f);
        

        Vector3f[] quad = new Vector3f[] { a, b, c, d };
        float[] result = TerrainUtil.getTerrainGridBoundingBox(scale, quad);

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
        float[] boundingBox = TerrainUtil.getTerrainGridBoundingBox(scale, quad);
        List<Vector2f> result = TerrainUtil.getGridPosBoundingQuad(scale, quad);

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

        var result = TerrainUtil.getHeightsForQuads(scale, Arrays.asList(quad, quad2));

        assertTrue(!result.isEmpty());
        for (Entry<Vector2f, Float> pos: result.entrySet()) {
            Vector2f p = new Vector2f(pos.getKey().x % 10, pos.getKey().y % 10);
            float height = pos.getValue() % 10;
            //specific corner tests
            if (p.x == 0 && p.y == 0) {
                assertEquals(0, height, "height not correct");
            } else if (p.x == 2 && p.y == 2) {
                // assertEquals(2, height, "height not correct"); //TODO this test doesn't work
            }

            // generic range tests
            if (pos.getKey().x < 5) {
                assertTrue(pos.getValue() >= 0 && pos.getValue() <= 2);
            } else {
                assertTrue(pos.getValue() >= 10 && pos.getValue() <= 12);
            }
        }
    }

    //Please add more to this method
}