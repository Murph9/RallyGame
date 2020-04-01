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
        
        this.vertexArray = new float[((spline.getControlPoints().size() - 2) * nbSubSegments) * 3 * 2 * 3];

        Vector3f temp3 = null;
        Vector3f temp4 = null;
        int i = 0;
        int cptCP = 0;
        for (Iterator<Vector3f> it = spline.getControlPoints().iterator(); it.hasNext(); cptCP++) {
            it.next();
            if (it.hasNext()) {
                for (int j = 0; j < nbSubSegments; j++) {
                    setNormalPointsInto(temp1, temp2, (float) j / nbSubSegments, cptCP);
                    if (temp3 != null && temp4 != null) {
                        vertexArray[i++] = temp3.getX();
                        vertexArray[i++] = temp3.getY();
                        vertexArray[i++] = temp3.getZ();
                        
                        vertexArray[i++] = temp4.getX();
                        vertexArray[i++] = temp4.getY();
                        vertexArray[i++] = temp4.getZ();
    
                        vertexArray[i++] = temp1.getX();
                        vertexArray[i++] = temp1.getY();
                        vertexArray[i++] = temp1.getZ();
    

                        vertexArray[i++] = temp4.getX();
                        vertexArray[i++] = temp4.getY();
                        vertexArray[i++] = temp4.getZ();

                        vertexArray[i++] = temp1.getX();
                        vertexArray[i++] = temp1.getY();
                        vertexArray[i++] = temp1.getZ();

                        vertexArray[i++] = temp2.getX();
                        vertexArray[i++] = temp2.getY();
                        vertexArray[i++] = temp2.getZ();
                    }
                    
                    temp3 = temp1.clone();
                    temp4 = temp2.clone();
                }
            }
        }
        this.setBuffer(VertexBuffer.Type.Position, 3, vertexArray);
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
