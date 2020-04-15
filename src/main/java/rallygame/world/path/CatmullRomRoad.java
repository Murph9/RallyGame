package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class CatmullRomRoad extends Mesh {

    // https://github.com/JulienGreen/RoadTessalation/blob/master/RoadMesh.java
    // https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/scene/shape/Curve.java

    private final Spline spline;
    private final float width;
    private final Vector3f[] vertexArray;

    /** Road based on a CatmullRom spline, with width. Remember it can't use the first and last ones. */
    public CatmullRomRoad(Spline spline, int nbSubSegments, float width) {
        this.spline = spline;
        this.width = width;
        if (spline.getType() != SplineType.CatmullRom)
            throw new IllegalArgumentException("Only " + SplineType.CatmullRom + " please");
        if (spline.getControlPoints().size() < 2)
            throw new IllegalArgumentException("Spline must have at least 2 control points making 1 section [start->end]");
        
        final int cpCount = spline.getControlPoints().size() - 1;
        this.vertexArray = new Vector3f[cpCount * nbSubSegments * 3 * 2];

        final Vector3f temp1 = new Vector3f();
        final Vector3f temp2 = new Vector3f();
        Vector3f temp3 = null;
        Vector3f temp4 = null;
        int i = 0;
        for (int cptCP = 0; cptCP < cpCount; cptCP++) {
            for (int j = 0; j < nbSubSegments; j++) {
                setNormalPointsInto(temp1, temp2, (float) j / nbSubSegments, cptCP);
                if (temp3 != null && temp4 != null) {
                    vertexArray[i++] = temp3.clone();
                    vertexArray[i++] = temp4.clone();
                    vertexArray[i++] = temp1.clone();

                    vertexArray[i++] = temp4.clone();
                    vertexArray[i++] = temp1.clone();
                    vertexArray[i++] = temp2.clone();
                }
                
                temp3 = temp1.clone();
                temp4 = temp2.clone();
            }
        }

        //then set the last point, just back from the end of the last segment
        //this completes the last subsegment, the last segment
        setNormalPointsInto(temp1, temp2, 1, cpCount-1);
        if (temp3 != null && temp4 != null) {
            vertexArray[i++] = temp3.clone();
            vertexArray[i++] = temp4.clone();
            vertexArray[i++] = temp1.clone();

            vertexArray[i++] = temp4.clone();
            vertexArray[i++] = temp1.clone();
            vertexArray[i++] = temp2.clone();
        }
        
        this.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertexArray));
        this.setMode(Mesh.Mode.Triangles);
        this.updateBound();
        this.updateCounts();
    }

    private static final Quaternion rot90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    private void setNormalPointsInto(Vector3f temp1, Vector3f temp2, float t, int cptCP) {
        Vector3f pos = spline.interpolate(t, cptCP, temp1).clone();
        spline.interpolate(t + 0.01f, cptCP, temp2);
        Vector3f diff = temp2.subtract(temp1);
        diff.y = 0; //prevent weird angle-ing of the road
        Vector3f normal = rot90.mult(diff.normalize());
        
        pos.add(normal.mult(width / 2), temp1);
        pos.add(normal.mult(-width / 2), temp2);
    }

    public List<Vector3f[]> getMeshAsQuads() {
        List<Vector3f[]> results = new LinkedList<>();
        for (int i = 0; i < vertexArray.length-1; i+=6) {
            Vector3f[] list = new Vector3f[] {
                vertexArray[i], vertexArray[i+1], vertexArray[i+2], vertexArray[i+5]
            };

            results.add(list);
        }
        
        return results;
    }
}
