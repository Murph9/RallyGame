package rallygame.world.highway;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Julien Green
 */
public class RoadMesh extends Mesh {

	List<Vector3f[]> quads;
	
    private float width;
    private List<Vector3f> cp;

    /**
     * Serialization only. Do not use.
     */
    public RoadMesh() {
    }

    /**
     * Create a quad with the given width and height. The quad is always created
     * in the XY plane.
     *
     * @param width The X extent or width
     * @param controlPoint
     */
    public RoadMesh(float width, float height, List<Vector3f> controlPoint) {
        updateGeometry(width, height, controlPoint);
    }

    /**
     * Create a quad with the given width and height. The quad is always created
     * in the XY plane.
     *
     * @param width The X extent or width
     * @param controlPoint
     * @param flipCoords If true, the texture coordinates will be flipped along
     * the Y axis.
     */
    public RoadMesh(float width, float height, List<Vector3f> controlPoint, boolean flipCoords) {
        updateGeometry(width, height, controlPoint, flipCoords, false);
    }

    /**
     * Create a quad with the given width and height. The quad is always created
     * in the XY plane.
     *
     * @param width The X extent or width
     * @param height
     * @param controlPoint
     * @param flipCoords If true, the texture coordinates will be flipped along
     * the Y axis.
     * @param tessellation Set if to use Tesselation indexation
     */
    public RoadMesh(float width, float height, List<Vector3f> controlPoint, boolean flipCoords, boolean tessellation) {
        updateGeometry(width, height, controlPoint, flipCoords, tessellation);
    }

    public float getWidth() {
        return width;
    }
    public List<Vector3f> getControlPoints() {
    	return cp;
    }

    public void updateGeometry(float width, float height, List<Vector3f> controlPoint) {
        updateGeometry(width, height, controlPoint, false, false);
    }

    @Override
    public void clearBuffer(VertexBuffer.Type type) {
        super.clearBuffer(type);
    }

    public void updateGeometry(float width, float height, List<Vector3f> controlPoint, boolean flipCoords, boolean tessellation) {
        this.width = width;
        if (controlPoint.get(0).z > controlPoint.get(3).z) {
            cp = new ArrayList<Vector3f>();
            cp.add(controlPoint.get(3));
            cp.add(controlPoint.get(2));
            cp.add(controlPoint.get(1));
            cp.add(controlPoint.get(0));
        } else {
            cp = controlPoint;
        }

        float lenght = FastMath.getBezierP1toP2Length(cp.get(0), cp.get(1), cp.get(2), cp.get(3));

        int modulo = (int) (lenght % height);
        int nbSection = (int) ((lenght - modulo) / height);
        //nbSection = (int) ((lenght) / width)+1;
        List<Vector3f> computePosition = new ArrayList<Vector3f>();
        for (float i = 0; i <= nbSection; i++) {
            if (i < nbSection) {
                Vector3f v1 = FastMath.interpolateBezier(i / nbSection, cp.get(0), cp.get(1), cp.get(2), cp.get(3));
                Vector3f v2 = FastMath.interpolateBezier((i + 1) / nbSection, cp.get(0), cp.get(1), cp.get(2), cp.get(3));

                Vector3f vv1 = v2.subtract(v1);
                float anglex = FastMath.acos(vv1.x / height);

                float angle1 = anglex - FastMath.HALF_PI;
                float angle2 = anglex + FastMath.HALF_PI;

                computePosition.add( v1.add(new Vector3f(FastMath.cos(angle1) * width , 0, FastMath.sin(angle1) * width )));
                computePosition.add( v1.add(new Vector3f(FastMath.cos(angle2) * width , 0, FastMath.sin(angle2) * width )));
            } 
            else {
                Vector3f v1 = FastMath.interpolateBezier((i) / nbSection, cp.get(0), cp.get(1), cp.get(2), cp.get(3));
                Vector3f v2 = FastMath.interpolateBezier((i+1) / nbSection, cp.get(0), cp.get(1), cp.get(2), cp.get(3));
                
                Vector3f vv1 = v2.subtract(v1);
                float anglex = FastMath.acos(vv1.x / height);

                float angle1 = anglex - FastMath.HALF_PI;
                float angle2 = anglex + FastMath.HALF_PI;

                computePosition.add( v1.add(new Vector3f(FastMath.cos(angle1) * width , 0, FastMath.sin(angle1) * width )));
                computePosition.add( v1.add(new Vector3f(FastMath.cos(angle2) * width , 0, FastMath.sin(angle2) * width )));
            }
        }

        float[] vertexPosition = new float[(nbSection + 1) * 2 * 3];
        float[] vertexTexCoord = new float[nbSection * 4 * 2];
        float[] vertexNormalCoord = new float[nbSection * 4 * 3];
        int[] vertexIndex = new int[(nbSection) * 2 * 3];

        int i = 0;
        while (i <= nbSection) {
            int inPos = i * 2;
            int pos = i * 6;
            if (i % 2 == 0) {
                vertexPosition[pos + 0] = computePosition.get(inPos).x;
                vertexPosition[pos + 1] = computePosition.get(inPos).y;//Height <---- Will use the 3D Interpolation of the Start and End Height
                vertexPosition[pos + 2] = computePosition.get(inPos).z;

                vertexPosition[pos + 3] = computePosition.get(inPos + 1).x;
                vertexPosition[pos + 4] = computePosition.get(inPos + 1).y;//Height <---- Will use the 3D Interpolation of the Start and End Height
                vertexPosition[pos + 5] = computePosition.get(inPos + 1).z;

            } else {
                vertexPosition[pos + 3] = computePosition.get(inPos).x;
                vertexPosition[pos + 4] = computePosition.get(inPos).y;//Height <---- Will use the 3D Interpolation of the Start and End Height
                vertexPosition[pos + 5] = computePosition.get(inPos).z;

                vertexPosition[pos + 0] = computePosition.get(inPos + 1).x;
                vertexPosition[pos + 1] = computePosition.get(inPos + 1).y;//Height <---- Will use the 3D Interpolation of the Start and End Height
                vertexPosition[pos + 2] = computePosition.get(inPos + 1).z;
            }
            i++;
        }
        i = 0;
        while (i <= nbSection) {

            int pos = i * 4;
            if (i % 2 == 0) {
                vertexTexCoord[pos + 0] = 0;
                vertexTexCoord[pos + 1] = 0;

                vertexTexCoord[pos + 2] = 1;
                vertexTexCoord[pos + 3] = 0;
            } else {
                vertexTexCoord[pos + 0] = 1;
                vertexTexCoord[pos + 1] = 1;

                vertexTexCoord[pos + 2] = 0;
                vertexTexCoord[pos + 3] = 1;
            }
            i++;
        }
        i = 0;
        while (i < nbSection) {
            int pos = i * 6;
            vertexNormalCoord[pos + 0] = 0;
            vertexNormalCoord[pos + 1] = 0;
            vertexNormalCoord[pos + 2] = 1;

            vertexNormalCoord[pos + 3] = 0;
            vertexNormalCoord[pos + 4] = 0;
            vertexNormalCoord[pos + 5] = 1;
            i++;
        }
        i = 0;
        if (tessellation) {
            while (i < nbSection) {
                int inPos = i * 2;
                int pos = i * 4;
                if (i % 2 == 0) {
                    vertexIndex[pos + 0] = inPos + 0;
                    vertexIndex[pos + 1] = inPos + 1;
                    vertexIndex[pos + 2] = inPos + 2;
                    vertexIndex[pos + 3] = inPos + 3;
                } else {
                    vertexIndex[pos + 0] = inPos + 1;
                    vertexIndex[pos + 1] = inPos + 0;
                    vertexIndex[pos + 2] = inPos + 3;
                    vertexIndex[pos + 3] = inPos + 2;
                }

                i++;
            }
            setBuffer(VertexBuffer.Type.Index, 4, vertexIndex);
            setMode(Mesh.Mode.Patch);
            setPatchVertexCount(4);
        } else {
            while (i < nbSection) {
                int inPos = i * 2;
                int pos = i * 6;
                if (i % 2 == 0) {
                    vertexIndex[pos + 0] = inPos + 0;
                    vertexIndex[pos + 1] = inPos + 1;
                    vertexIndex[pos + 2] = inPos + 2;

                    vertexIndex[pos + 3] = inPos + 0;
                    vertexIndex[pos + 4] = inPos + 2;
                    vertexIndex[pos + 5] = inPos + 3;
                } else {
                    vertexIndex[pos + 0] = inPos + 0;
                    vertexIndex[pos + 1] = inPos + 2;
                    vertexIndex[pos + 2] = inPos + 1;

                    vertexIndex[pos + 3] = inPos + 0;
                    vertexIndex[pos + 4] = inPos + 3;
                    vertexIndex[pos + 5] = inPos + 2;
                }

                i++;
            }
            setBuffer(VertexBuffer.Type.Index, 3, vertexIndex);
        }

        setBuffer(VertexBuffer.Type.Position, 3, vertexPosition);
        setBuffer(VertexBuffer.Type.TexCoord, 2, vertexTexCoord);
        setBuffer(VertexBuffer.Type.Normal, 3, vertexNormalCoord);

        updateBound();
        setStatic();
        
        //convert vertexPosition into a quad list to use externally, for positional reasons
        quads = new LinkedList<Vector3f[]>();
        for (i = 0; i < computePosition.size()-3; i+=2) {
//        	if (i % 2 == 0)
        		quads.add(new Vector3f[] {computePosition.get(i),computePosition.get(i+1),computePosition.get(i+2),computePosition.get(i+3)});
//        	else
//        		quads.add(new Vector3f[] {computePosition.get(i+2),computePosition.get(i+3),computePosition.get(i),computePosition.get(i+1)});
        }
    }
    
    public List<Vector3f[]> getQuads() {
    	return quads;
    }

}
