package rallygame.world.osm;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Line;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Geo;
import rallygame.helper.Trig;
import rallygame.world.World;
import rallygame.world.WorldType;

public class OsmWorld extends World {
    private static String OSM_FILE_NAME = "/osm/data.osm";

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
            drawMeAQuad(Trig.rectFromLineXZ(l.a, l.b, roadScale), mapColorToType(l.subRoadType));

            Line objL = new Line(l.a, l.b);
            this.rootNode.attachChild(LoadModelWrapper.create(app.getAssetManager(), new Geometry("line", objL), ColorRGBA.Blue));
        }
    }

    @Override
    public Transform getStart() {
        return new Transform(reader.getWithScale(scale).get(0).a.add(0, 1, 0));
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

    private void drawMeAQuad(Vector3f[] v, ColorRGBA colour) {
        PhysicsSpace phys = getState(BulletAppState.class).getPhysicsSpace();
        Mesh mesh = Geo.createQuad(v);
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