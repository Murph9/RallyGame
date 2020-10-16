package rallygame.world.path;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.TerrainQuadUtil;
import rallygame.helper.Trig;
import rallygame.service.ILoadable;
import rallygame.service.ObjectPlacer;
import rallygame.service.ObjectPlacer.NodeId;

public class Terrain extends BaseAppState implements ILoadable {

    private static final float HEIGHT_WEIGHT = 0.02f;
    private static final float ROAD_WIDTH = 10f;
    private static final long SEARCH_TIMEOUT = 30 * (long)1e6; //x sec

    protected final int sideLength;

    protected final Random rand;
    protected final FilteredBasisTerrain filteredBasis;
    protected final ExecutorService executor;
    private final Node rootNode;
    private final TerrainSettings features;

    // Contains the list of TerrainQuads mapped to locations
    // Each terrain object contains the objects that exist on it
    private final Map<Vector2f, TerrainObj> pieces = new HashMap<>();
    private final List<TerrainRoad> roads;

    private String loadingMessage;
    private final int loadSteps = 4;
    private int curStep;
    private boolean loaded = false;

    protected Terrain(TerrainSettings features) {
        this.features = features;
        this.sideLength = (1 << features.size) + 1; // 6 -> 64 + 1
        
        features.tileCount = Math.max(features.tileCount, 1);
        
        this.rootNode = new Node("Terrain root node");
        this.roads = new LinkedList<>();

        Log.p("Starting path world with seed: " + features.seed);
        this.rand = new Random(features.seed);

        this.filteredBasis = FilteredBasisTerrain.generate(new Vector2f(rand.nextInt(1000), rand.nextInt(1000)));

        this.executor = Executors.newFixedThreadPool(1); //-_-
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);
        
        executor.submit(() -> {
            try {
                offThreadInitialize(app);
            } catch (Exception e) {
                Log.e(e);
                this.loaded = true;
                this.loadingMessage = "Failed to load: " + e.getMessage();
            }
        });
    }

    protected void offThreadInitialize(Application app) {
        var space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        var factory = new TerrainQuadFactory(this.filteredBasis);

        this.loadingMessage = "Loading terrain";

        for (int i = 0; i < features.tileCount; i++) {
            for (int j = 0; j < features.tileCount; j++) {
                var tObj = new TerrainObj();
                tObj.offset = new Vector2f(i, j);
                tObj.quad = factory.create(app.getAssetManager(), tObj.offset, features.scale, sideLength);
                tObj.rootNode = new Node("terrain at " + tObj.offset);
                tObj.rootNode.attachChild(tObj.quad);
                this.pieces.put(tObj.offset, tObj);
            }
        }
        
        this.curStep++;
        this.loadingMessage = "Loading roads";

        // Generate roads
        var roadGenerator = new TerrainRoadGenerator(HEIGHT_WEIGHT, SEARCH_TIMEOUT, (s) -> this.loadingMessage = "Loading roads\n"+s);
        List<TerrainQuad> terrainQuads = pieces.values().stream().map(x -> x.quad).collect(Collectors.toList());
        var roads = roadGenerator.generateFrom(terrainQuads);
        // make sure they are valid roads
        for (RoadPointList road : roads) {
            if (road.failed) {
                this.loadingMessage = road.failedMessage;
                this.loaded = true;
                return;
                //TODO something better
            }
        }

        var roadCreator = new TerrainRoadPointsToObject(ROAD_WIDTH);
        this.roads.addAll(roadCreator.create(roads, app.getAssetManager()));

        app.enqueue(() -> {
            for (TerrainRoad road : this.roads) {
                this.rootNode.attachChild(road.sp);

                road.sp.addControl(new RigidBodyControl(road.col, 0));
                space.add(road.sp);
            }
        });

        this.curStep++;
        this.loadingMessage = "Reloading terrain";

        // Update terrain heights
        List<Vector3f[]> quads = new LinkedList<>();
        for (RoadPointList road : roads) {
            if (road.failed) {
                this.loadingMessage = road.failedMessage;
                continue;
            }
            quads.addAll(road.road.middle.getMeshAsQuads());
        }

        var heights = TerrainQuadUtil.getHeightsForQuads(features.scale, quads);
        TerrainQuadUtil.setTerrainHeights(terrainQuads, heights);

        this.curStep++;
        this.loadingMessage = "Adding other features";

        // Add visual and physics spaces
        app.enqueue(() -> {
            for (TerrainObj piece : pieces.values()) {
                this.rootNode.attachChild(piece.rootNode);

                if (piece.inPhysics()) {
                    space.remove(piece.quad);
                    piece.quad.removeControl(RigidBodyControl.class);
                }

                piece.quad.addControl(new RigidBodyControl(
                        new HeightfieldCollisionShape(piece.quad.getHeightMap(), piece.quad.getLocalScale()), 0));
                space.add(piece.quad);
            }
        });
        
        this.curStep++;
        this.loadingMessage = "Done";

        // Loaded!
        loaded = true;
        
        for (var tObj: this.pieces.values())
            finishedMinimalLoading(tObj);
    }

    
    protected void finishedMinimalLoading(TerrainObj tObj) {
        AssetManager am = getApplication().getAssetManager();
        ColorRGBA colour = ColorRGBA.randomColor();
        if (features.cubes) {
            List<Spatial> cubes = CubePlacer.generate(tObj.quad, am, 500, (v2, size) -> onRoad(v2, size), colour);
            getApplication().enqueue(() -> {
                var ob = getState(ObjectPlacer.class);
                tObj.cubes = ob.addBulk(cubes);
            });
        }

        if (features.grass) {
            tObj.grass = GrassPlacer.generate(tObj.quad, 10000, (v2) -> onRoad(v2));
            Geometry g = new Geometry("'grass'", tObj.grass);
            getApplication().enqueue(() -> {
                this.rootNode.attachChild(LoadModelWrapper.createWithColour(am, g, colour));
            });
        }
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Camera cam = getApplication().getCamera();
        for (var tObj :this.pieces.values()) {
            if (tObj.grass != null)
                tObj.grass.update(cam.getRotation());
        }
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdownNow();
        rootNode.removeFromParent();

        var ob = getState(ObjectPlacer.class);
        for (var obj :this.pieces.values()) {
            if (obj.cubes != null)
                ob.removeBulk(obj.cubes);
        }

        this.rootNode.removeFromParent();
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

    @Override
    public LoadResult loadPercent() {
        if (loaded)
            return new LoadResult(1, loadingMessage);
        return new LoadResult(curStep / (float)loadSteps, loadingMessage);
    }

    private boolean onRoad(Vector2f location) {
        return onRoad(location, 0);
    }

    private boolean onRoad(Vector2f location, float objRadius) {
        // simple check that its more than x from a road vertex
        for (var r : this.roads) {
            var road = r.points;
            for (int i = 0; i < road.size() - 1; i++) {
                Vector3f cur = road.get(i);
                Vector3f point = road.get(i + 1);
                if (Trig.distFromSegment(H.v3tov2fXZ(cur), H.v3tov2fXZ(point), location) < FastMath.sqrt(2) * objRadius + ROAD_WIDTH / 2)
                    return true;
                cur = point;
            }
        }

        return false;
    }

    protected Transform getStart() {
        if (this.roads.isEmpty()) {
            return new Transform();
        }

        if (this.roads.get(0).points.size() < 2) {
            return new Transform(this.roads.get(0).points.peekFirst());
        }

        Vector3f start = this.roads.get(0).points.get(0);
        Vector3f next = this.roads.get(0).points.get(1);

        Vector3f offset = next.subtract(start).normalize();
        return new Transform(start.add(0, 0.5f, 0).add(offset.mult(3)), 
            new Quaternion().lookAt(next.subtract(start), Vector3f.UNIT_Y));
            
    }

    protected Collection<Vector3f> getRoadPoints() {
        if (this.pieces.isEmpty())
            return null;

        Collection<Vector3f> points = new LinkedList<>();
        for (var roads: this.roads) {
            points.addAll(roads.points);
        }
        return points;
    }

    class TerrainObj {
        public TerrainQuad quad;
        public Node rootNode;
        public Vector2f offset;

        public GrassTerrain grass;
        public NodeId cubes;

        public boolean inPhysics() {
            return quad.getNumControls() > 0;
        }
    }
}
