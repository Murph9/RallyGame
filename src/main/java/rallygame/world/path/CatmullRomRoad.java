package rallygame.world.path;

import java.util.Iterator;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

public class CatmullRomRoad extends Mesh {

    // https://github.com/JulienGreen/RoadTessalation/blob/master/RoadMesh.java
    // https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/scene/shape/Curve.java

    private final Spline spline;
    private final Vector3f temp1 = new Vector3f();
    private final Vector3f temp2 = new Vector3f();
    private final float width;

    public CatmullRomRoad(Spline spline, int nbSubSegments, float width) {
        this.spline = spline;
        this.width = width;
        if (spline.getType() != SplineType.CatmullRom)
            throw new IllegalArgumentException("Only CatmullRom please");

        createCatmullRomMesh(nbSubSegments);
    }

    private void createCatmullRomMesh(int nbSubSegments) {
        float[] array = new float[((spline.getControlPoints().size() - 1) * nbSubSegments + 1) * 3 * 2];
        int i = 0;
        int cptCP = 0;
        for (Iterator<Vector3f> it = spline.getControlPoints().iterator(); it.hasNext();) {
            it.next();
            if (it.hasNext()) {
                for (int j = 0; j < nbSubSegments; j++) {
                    setTempNormalPoints((float) j / nbSubSegments, cptCP);
                    array[i] = temp1.getX();
                    i++;
                    array[i] = temp1.getY();
                    i++;
                    array[i] = temp1.getZ();
                    i++;
                    array[i] = temp2.getX();
                    i++;
                    array[i] = temp2.getY();
                    i++;
                    array[i] = temp2.getZ();
                    i++;
                }
            }
            cptCP++;
        }
        this.setBuffer(VertexBuffer.Type.Position, 3, array);
        this.setMode(Mesh.Mode.TriangleStrip);
        this.updateBound();
        this.updateCounts();
    }

    private static final Quaternion rot90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    private void setTempNormalPoints(float t, int cptCP) {
        Vector3f pos = spline.interpolate(t, cptCP, temp1).clone();
        spline.interpolate(t + 0.01f, cptCP, temp2);
        Vector3f diff = temp2.subtract(temp1);
        diff.y = 0; //prevent weird angle-ing of the road
        Vector3f normal = rot90.mult(diff.normalize());
        
        pos.add(normal.mult(width / 2), temp1);
        pos.add(normal.mult(-width / 2), temp2);
    }
}
