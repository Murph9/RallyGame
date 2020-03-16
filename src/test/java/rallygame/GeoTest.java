package rallygame;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import rallygame.helper.Geo;

public class GeoTest {

    private static final float V3_ERROR = 0.01f;
    private static final float[] CirclePoints4 = new float[] {
        1, 0, 0,
        0, -1, 0,
        -1, 0, 0,
        0, 1, 0
    };

    @Test
    public void getXYCircleGeometry() {
        Geometry g = Geo.getXYCircleGeometry(4);
        VertexBuffer points = g.getMesh().getBuffer(Type.Position);

        for (int i = 0; i < points.getNumElements(); i++) {
            Vector3f pos = new Vector3f((float)points.getElementComponent(i, 0), (float)points.getElementComponent(i, 1), (float)points.getElementComponent(i, 2));
            assertVector3fEquals(new Vector3f(CirclePoints4[i * 3], CirclePoints4[i * 3 + 1], CirclePoints4[i * 3 + 2]), pos, V3_ERROR);
        }
    }

    private static void assertVector3fEquals(Vector3f one, Vector3f two, float delta) {
        Assertions.assertEquals(one.x, two.x, delta);
        Assertions.assertEquals(one.y, two.y, delta);
        Assertions.assertEquals(one.z, two.z, delta);
    }
}
