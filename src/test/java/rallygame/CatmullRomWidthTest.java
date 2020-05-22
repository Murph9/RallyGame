package rallygame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;

import org.junit.jupiter.api.Test;

import rallygame.world.path.CatmullRomWidth;

public class CatmullRomWidthTest {

    @Test
    public void getQuads() {
        Vector3f[] list = new Vector3f[] { new Vector3f(0, 0, 0), new Vector3f(0, 0, 1) };
        Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth

        CatmullRomWidth width = new CatmullRomWidth(s3, 1, 1);

        List<Vector3f[]> quads = width.getMeshAsQuads();

        assertEquals(1, quads.size());

        Vector3f[] firstQuad = quads.get(0);
        assertEquals(0.5f, firstQuad[0].x, 0.0001f);
        assertEquals(0f, firstQuad[0].y, 0.0001f);
        assertEquals(0f, firstQuad[0].z, 0.0001f);

        assertEquals(-0.5f, firstQuad[1].x, 0.0001f);
        assertEquals(0f, firstQuad[1].y, 0.0001f);
        assertEquals(0f, firstQuad[1].z, 0.0001f);

        assertEquals(0.5f, firstQuad[2].x, 0.0001f);
        assertEquals(0f, firstQuad[2].y, 0.0001f);
        assertEquals(1f, firstQuad[2].z, 0.0001f);

        assertEquals(-0.5f, firstQuad[3].x, 0.0001f);
        assertEquals(0f, firstQuad[3].y, 0.0001f);
        assertEquals(1f, firstQuad[3].z, 0.0001f);
    }
}