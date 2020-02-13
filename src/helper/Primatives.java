package helper;

import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;

public class Primatives {

    public static Geometry getXYCircleGeometry(int count) {
        Mesh m = new Mesh();
        m.setBuffer(Type.Position, 3, getCirclePoints(count));
        m.setMode(Mode.LineLoop);
        m.updateBound();
        m.setStatic();

        Geometry g = new Geometry("circle mesh", m);
        return g;
    }

    private static float[] getCirclePoints(int count) {
        float[] points = new float[count*3];

        float radStep = FastMath.TWO_PI/count;
        for (int i = 0; i < count; i++) {
            points[i * 3] = FastMath.sin(FastMath.HALF_PI + i * radStep);
            points[i * 3 + 1] = FastMath.cos(FastMath.HALF_PI + i * radStep);
            points[i * 3 + 2] = 0;
        }

        return points;
    }
}