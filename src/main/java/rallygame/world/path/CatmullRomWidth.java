package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class CatmullRomWidth extends Mesh {

    // https://github.com/JulienGreen/RoadTessalation/blob/master/RoadMesh.java
    // https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/scene/shape/Curve.java

    private final Spline spline;
    private final BiFunction<Vector3f, Vector3f, Vector3f[]> cross;
    private final Vector3f[] vertexArray;
    private final Vector2f[] vertexTexCoord;
    private final Vector3f[] vertexNormalCoord;
    
    /** Road based on a CatmullRom spline, with width. Remember it can't use the first and last ones. */
    public CatmullRomWidth(Spline spline, int nbSubSegments, float width) {
        this(spline, nbSubSegments, Flat(width));
    }

    public CatmullRomWidth(Spline spline, int nbSubSegments, BiFunction<Vector3f, Vector3f, Vector3f[]> cross) {
        this.spline = spline;
        this.cross = cross;
        if (spline.getType() != SplineType.CatmullRom)
            throw new IllegalArgumentException("Only " + SplineType.CatmullRom + " please");
        if (spline.getControlPoints().size() < 2)
            throw new IllegalArgumentException("Spline must have at least 2 control points making 1 section [start->end]");
        if (cross.apply(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0)).length != 2)
            throw new IllegalArgumentException("Func should only returning 2 vectors");
        
        final int cpCount = spline.getControlPoints().size() - 1;
        this.vertexArray = new Vector3f[cpCount * nbSubSegments * 3 * 2];

        Vector3f[] tempsA = null;
        Vector3f[] tempsB = null;
        final Vector3f temp1 = new Vector3f();
        final Vector3f temp2 = new Vector3f();
        int i = 0;
        for (int cptCP = 0; cptCP < cpCount; cptCP++) {
            for (int j = 0; j < nbSubSegments; j++) {

                tempsB = getPoints(temp1, temp2, (float) j / nbSubSegments, cptCP);
                if (tempsA != null) {
                    for (int k = 0; k < tempsB.length - 1; k++) {
                        vertexArray[i++] = tempsB[k];
                        vertexArray[i++] = tempsB[k + 1];
                        vertexArray[i++] = tempsA[k];

                        vertexArray[i++] = tempsA[k];
                        vertexArray[i++] = tempsB[k + 1];
                        vertexArray[i++] = tempsA[k + 1];
                    }
                }
                tempsA = tempsB;
            }
        }

        //then set the last point, just back from the end of the last segment
        //this completes the last subsegment, the last segment
        tempsB = getPoints(temp1, temp2, 1, cpCount-1);
        if (tempsA != null) {
            for (int k = 0; k < tempsB.length - 1; k++) {
                vertexArray[i++] = tempsB[k];
                vertexArray[i++] = tempsB[k + 1];
                vertexArray[i++] = tempsA[k];

                vertexArray[i++] = tempsA[k];
                vertexArray[i++] = tempsB[k + 1];
                vertexArray[i++] = tempsA[k + 1];
            }
        }

        vertexTexCoord = new Vector2f[cpCount * nbSubSegments * 3 * 2];
        for (int k = 0; k < vertexTexCoord.length; k += 6) {
            vertexTexCoord[k] = new Vector2f(0, 1);
            vertexTexCoord[k + 1] = new Vector2f(1, 0);
            vertexTexCoord[k + 2] = new Vector2f(0, 1);
            vertexTexCoord[k + 3] = new Vector2f(0, 1);
            vertexTexCoord[k + 4] = new Vector2f(1, 0);
            vertexTexCoord[k + 5] = new Vector2f(1, 1);
        }

        vertexNormalCoord = new Vector3f[cpCount * nbSubSegments * 3 * 2];
        for (int k = 0; k < vertexNormalCoord.length; k++) {
            vertexNormalCoord[k] = new Vector3f(0, 0, 1);
        }

        this.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertexArray));
        this.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(vertexTexCoord));
        this.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(vertexNormalCoord));

        this.setMode(Mesh.Mode.Triangles);
        this.updateCounts();
        this.updateBound();

        this.setStatic();
    }

    private static final Quaternion rot90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    private Vector3f[] getPoints(Vector3f temp1, Vector3f temp2, float t, int cptCP) {
        Vector3f pos = spline.interpolate(t, cptCP, temp1).clone();
        spline.interpolate(t + 0.01f, cptCP, temp2);
        Vector3f diff = temp2.subtract(temp1);
        diff.y = 0; // prevent weird angle-ing of the road
        Vector3f normal = rot90.mult(diff.normalize());
        return cross.apply(pos, normal);
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

    public Spline getSpline() {
        return this.spline;
    }

    public static BiFunction<Vector3f, Vector3f, Vector3f[]> Flat(float width) {
        final float w = width / 2f;
        return (Vector3f mid, Vector3f normal) -> {
            return new Vector3f[] { mid.add(normal.mult(w)), mid.add(normal.mult(-w)) };
        };
    }
}
