package rallygame.world.osm;

import java.util.Arrays;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Line;
import com.jme3.util.BufferUtils;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Log;
import rallygame.world.World;
import rallygame.world.WorldType;

public class OsmWorld extends World {
    private static String OSM_FILE_NAME = "/osm/data.osm";
    private static Quaternion ROT_90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);

    private final float scale = 48000;
    private final float roadScale = 8;
    private OsmReader reader;

    // TODO get terrain data from USGS satellite data
    // https://earthexplorer.usgs.gov/

    public OsmWorld() {
        super("osmWorldRoot");

        reader = new OsmReader();
        try {
            reader.load(getClass().getResourceAsStream(OSM_FILE_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        // next load the roads
        for (rallygame.world.osm.OsmReader.Road l : reader.getWithScale(scale)) {
            generateQuadFromLine(l.a, l.b, mapColorToType(l.subRoadType));

            Line objL = new Line(l.a, l.b);
            this.rootNode.attachChild(LoadModelWrapper.create(app.getAssetManager(), new Geometry("line", objL), ColorRGBA.Blue));
        }
    }

    @Override
    public Vector3f getStartPos() {
        return reader.getWithScale(scale).get(0).a.add(0, 1, 0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public WorldType getType() {
        return WorldType.OSM;
    }

    private void generateQuadFromLine(Vector3f start, Vector3f end, ColorRGBA colour) {
        Vector3f dir = end.subtract(start);
        dir.y = 0;
        dir.normalizeLocal();
        Vector3f left = ROT_90.mult(dir).mult(roadScale / 2);

        // lt-e-rt (end)
        // |..|..|
        // lb-s-rb (start)

        Vector3f lb = start.add(left);
        Vector3f lt = end.add(left);

        Vector3f rb = start.add(left.negate());
        Vector3f rt = end.add(left.negate());

        drawMeAQuad(new Vector3f[] { lb, lt, rb, rt }, colour);
    }


    private static int[] QUAD_INDEXES = { 2, 0, 1, 1, 3, 2 };
    private static float[] QUAD_NORMALS = new float[] { 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 };
    private static Vector2f[] QUAD_TEX_COORD = new Vector2f[] {
            new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1) 
        };
    private void drawMeAQuad(Vector3f[] v, ColorRGBA colour) {
        if (v == null || v.length != 4) {
            Log.e("Roads-drawMeAQuad: Not the correct length drawMeAQuad()");
            return;
        }
        if (Arrays.asList(v).stream().anyMatch(x -> !Vector3f.isValidVector(x))) {
            Log.e("Roads-drawMeAQuad: Invalid vector given");
            return;
        }

        PhysicsSpace phys = getState(BulletAppState.class).getPhysicsSpace();

        Mesh mesh = new Mesh(); // making a quad positions
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(v));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(QUAD_NORMALS));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(QUAD_TEX_COORD));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(QUAD_INDEXES));

        mesh.updateBound();

        Geometry geo = new Geometry("Quad", mesh);
        Node n = LoadModelWrapper.create(getApplication().getAssetManager(), geo, colour);

        CollisionShape col = CollisionShapeFactory.createMeshShape(geo);
        RigidBodyControl c = new RigidBodyControl(col, 0);

        rootNode.attachChild(n);
        phys.add(c);
    }

    private ColorRGBA mapColorToType(String subType) {
        if (subType == null)
            return ColorRGBA.White;

        switch (subType) {
            case "motorway":
                return ColorRGBA.Cyan;
            case "tertiary":
                return ColorRGBA.Magenta;
            case "cycleway":
                return ColorRGBA.Blue;
            case "track":
                return ColorRGBA.Green;
            case "service":
                return ColorRGBA.Gray;
            case "residential":
                return ColorRGBA.Pink;
            case "footway":
                return ColorRGBA.Magenta;
            default:
                return ColorRGBA.Blue;
        }
    }
}