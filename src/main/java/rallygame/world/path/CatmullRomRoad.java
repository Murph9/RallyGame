package rallygame.world.path;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
    private final float[] vertexArray;

    /** Road based on a CatmullRom spline, with width. Remember it can't use the first and last ones. */
    public CatmullRomRoad(Spline spline, int nbSubSegments, float width) {
        this.spline = spline;
        this.width = width;
        if (spline.getType() != SplineType.CatmullRom)
            throw new IllegalArgumentException("Only " + SplineType.CatmullRom + " please");
        if (spline.getControlPoints().size() < 4)
            throw new IllegalArgumentException("Spline must have at least 2 sections");
        
        this.vertexArray = new float[((spline.getControlPoints().size() - 1) * nbSubSegments) * 3 * 2];

        int i = 0;
        int cptCP = 0;
        for (Iterator<Vector3f> it = spline.getControlPoints().iterator(); it.hasNext(); cptCP++) {
            it.next();
            if (it.hasNext()) {
                for (int j = 0; j < nbSubSegments; j++) {
                    setNormalPointsIntoTemp((float) j / nbSubSegments, cptCP);
                    vertexArray[i] = temp1.getX();
                    i++;
                    vertexArray[i] = temp1.getY();
                    i++;
                    vertexArray[i] = temp1.getZ();
                    i++;
                    vertexArray[i] = temp2.getX();
                    i++;
                    vertexArray[i] = temp2.getY();
                    i++;
                    vertexArray[i] = temp2.getZ();
                    i++;
                }
            }
        }
        this.setBuffer(VertexBuffer.Type.Position, 3, vertexArray);
        this.setMode(Mesh.Mode.TriangleStrip);
        this.updateBound();
        this.updateCounts();
    }

    private static final Quaternion rot90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    private void setNormalPointsIntoTemp(float t, int cptCP) {
        Vector3f pos = spline.interpolate(t, cptCP, temp1).clone();
        spline.interpolate(t + 0.01f, cptCP, temp2);
        Vector3f diff = temp2.subtract(temp1);
        diff.y = 0; //prevent weird angle-ing of the road
        Vector3f normal = rot90.mult(diff.normalize());
        
        pos.add(normal.mult(width / 2), temp1);
        pos.add(normal.mult(-width / 2), temp2);
    }

    public List<Vector3f[]> getMeshAsQuads() {
        //[0, 1, 2, 3, 4, 5, 6, 7, 8, 9] -> [[0,1,2,3], [1,2,3,4], [2,3,4,5], ...]

        List<Vector3f[]> results = new LinkedList<>();
        for (int i = 0; i < vertexArray.length-4*3; i+=3*4) {
            results.add(new Vector3f[]{
                    new Vector3f(vertexArray[i], vertexArray[i + 1], vertexArray[i + 2]),
                    new Vector3f(vertexArray[i + 3], vertexArray[i + 4], vertexArray[i + 5]),
                    new Vector3f(vertexArray[i + 6], vertexArray[i + 7], vertexArray[i + 8]),
                    new Vector3f(vertexArray[i + 9], vertexArray[i + 10], vertexArray[i + 11])
            });
        }
        
        return results;
    }
}
