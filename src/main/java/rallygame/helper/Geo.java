package rallygame.helper;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;

public class Geo {

    public static Geometry createPointMesh(AssetManager am, Vector3f[] points, ColorRGBA color, float pointSize) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(points));
        mesh.setMode(Mesh.Mode.Points);
        mesh.updateBound();
        mesh.setStatic();
        
        Geometry geo = new Geometry("point mesh", mesh);
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.setFloat("PointSize", pointSize);
        mat.getAdditionalRenderState().setWireframe(true);
        geo.setMaterial(mat);

        return geo;
    }

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
        float[] points = new float[count * 3];

        float radStep = FastMath.TWO_PI / count;
        for (int i = 0; i < count; i++) {
            points[i * 3] = FastMath.sin(FastMath.HALF_PI + i * radStep);
            points[i * 3 + 1] = FastMath.cos(FastMath.HALF_PI + i * radStep);
            points[i * 3 + 2] = 0;
        }

        return points;
    }

    public static Geometry makeShapeArrow(AssetManager am, ColorRGBA color, Vector3f dir, Vector3f pos) {
        if (!Vector3f.isValidVector(pos) || !Vector3f.isValidVector(dir)) {
            Log.e("not valid pos or dir", pos, dir);
            return null;
        }
        Arrow arrow = new Arrow(dir);
        Geometry arrowG = createShape(am, arrow, color, pos, "an arrow");
        return arrowG;
    }

    public static Geometry makeShapeBox(AssetManager am, ColorRGBA color, Vector3f pos, float size) {
        if (!Vector3f.isValidVector(pos)) {
            Log.e("not valid position");
            return null;
        }

        Box box = new Box(size, size, size);
        Geometry boxG = createShape(am, box, color, pos, "a box");
        return boxG;
    }

    public static Geometry makeShapeLine(AssetManager am, ColorRGBA color, Vector3f start, Vector3f end,
            float lineWidth) {
        if (!Vector3f.isValidVector(start) || !Vector3f.isValidVector(end)) {
            Log.e("not valid start or end", start, end);
            return null;
        }
        Line l = new Line(start, end);
        Geometry lineG = createShape(am, l, color, Vector3f.ZERO, "a line");
        lineG.getMaterial().getAdditionalRenderState().setLineWidth(lineWidth);
        return lineG;
    }

    public static Geometry makeShapeLine(AssetManager am, ColorRGBA color, Vector3f start, Vector3f end) {
        return makeShapeLine(am, color, start, end, 1);
    }

    public static Geometry makeShapeSphere(AssetManager am, ColorRGBA color, Vector3f pos, float size) {
        if (!Vector3f.isValidVector(pos)) {
            Log.e("not valid position");
            return null;
        }

        Sphere sphere = new Sphere(16, 16, size);
        Geometry sphereG = createShape(am, sphere, color, pos, "a sphere");
        return sphereG;
    }

    public static Geometry createShape(AssetManager am, Mesh shape, ColorRGBA color, Vector3f pos, String name) {
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setWireframe(true);

        Geometry g = new Geometry(name, shape);
        g.setMaterial(mat);
        g.setLocalTranslation(pos);
        return g;
    }

    public static Spatial getNamedSpatial(Node n, String name) {
        return n.getChild(name);
    }

    public static Spatial removeNamedSpatial(Node n, String name) {
        Spatial s = n.getChild(name);
        if (s == null)
            return null;
        s.removeFromParent();
        return s;
    }

    public static List<Geometry> getGeomsWithMaterialNameContaining(Spatial s, String str) {
        return Geo.getGeomList(s)
            .stream()
            .filter(x -> x.getMaterial() != null && x.getMaterial().getName() != null && 
                x.getMaterial().getName().contains(str))
            .collect(Collectors.toList());
    }

    public static List<Geometry> getGeomList(Spatial n) {
        return rGeomList(n);
    }

    private static List<Geometry> rGeomList(Spatial s) {
        List<Geometry> listg = new LinkedList<Geometry>();
        if (s instanceof Geometry) {
            listg.add((Geometry) s);
            return listg;
        }

        Node n = (Node) s;
        List<Spatial> list = n.getChildren();
        if (list.isEmpty())
            return listg;

        for (Spatial sp : list) {
            if (sp instanceof Node) {
                listg.addAll(rGeomList(sp));
            }
            if (sp instanceof Geometry) {
                listg.add((Geometry) sp);
            }
        }
        return listg;
    }

    public static boolean hasParentNode(Spatial s, Node node) {
        while (s != null) {
            if (s == node) {
                return true;
            }

            s = s.getParent();
        }
        return false;
    }


    private static final FloatBuffer quadTexCoordBuffer = BufferUtils.createFloatBuffer(new Vector2f[] {
        new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1)
    });
    private static final FloatBuffer quadNormalBuffer = BufferUtils.createFloatBuffer(new float[] {
        0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0
    });
    private static final IntBuffer quadIndexBuffer = BufferUtils.createIntBuffer(new int[] { 2, 0, 1, 1, 3, 2 });

    public static Mesh createQuad(Vector3f[] v) {
        if (v == null || v.length != 4) {
            Log.e("Geo.createQuad(): Not the correct length");
            Log.e(v, ",");
            return null;
        }
        if (Arrays.asList(v).stream().anyMatch(x -> !Vector3f.isValidVector(x))) {
            Log.e("Geo.createQuad(): Invalid vector given");
            Log.e(v, ",");
            return null;
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(v));
        mesh.setBuffer(Type.Normal, 3, quadNormalBuffer);
        mesh.setBuffer(Type.TexCoord, 2, quadTexCoordBuffer);
        mesh.setBuffer(Type.Index, 3, quadIndexBuffer);
        mesh.updateBound();
        return mesh;
    }
}
