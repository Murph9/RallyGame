package rallygame.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
}