package rallygame.world.path;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Log;
import rallygame.service.ILoadable;
import rallygame.service.ObjectPlacer;
import rallygame.service.ObjectPlacer.NodeId;

public class Terrain extends BaseAppState implements ILoadable {

    // TODO height adjustments need to be set across shared edges

    protected static final float HEIGHT_WEIGHT = 0.02f;
    protected static final float ROAD_WIDTH = 10f;

    protected final int sideLength;

    protected final Random rand;
    protected final FilteredBasisTerrain filteredBasis;
    protected final ExecutorService executor;
    private final Node rootNode;
    private final TerrainSettings features;

    // contains the list of TerrainQuads mapped to locations
    // each terrain quad contains the roads and objects that exist in it
    private final Map<Vector2f, TerrainObj> pieces = new HashMap<>();

    protected Terrain(TerrainSettings features) {
        this.features = features;
        this.sideLength = (1 << features.size) + 1; // 6 -> 64 + 1
        features.tileCount = Math.max(features.tileCount, 1);

        this.rootNode = new Node("Terrain root node");

        Log.p("Starting path world with seed: " + features.seed);
        this.rand = new Random(features.seed);

        this.filteredBasis = FilteredBasisTerrain.generate(new Vector2f(rand.nextInt(1000), rand.nextInt(1000)));

        this.executor = Executors.newFixedThreadPool(2);
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        // load terrainquads
        for (int i = 0; i < features.tileCount; i++)
            generateTQuadAt(new Vector2f(i, 0), app.getAssetManager());
    }

    private void generateTQuadAt(Vector2f center, AssetManager am) {
        var obj = new TerrainObj();
        obj.piece = new TerrainPiece(this, center, true);
        obj.piece.generate(am, getState(BulletAppState.class).getPhysicsSpace(), features.scale);

        this.pieces.put(center, obj);
        this.rootNode.attachChild(obj.piece.rootNode);
    }

    protected void finishedMinimalLoading(TerrainPiece piece) {
        var tObj = this.pieces.values().stream().filter(x -> x.piece == piece).findFirst();
        if (!tObj.isPresent())
            throw new IllegalStateException("How?");
        
        TerrainObj obj = tObj.get();
        obj.loaded = true;
        AssetManager am = getApplication().getAssetManager();

        List<Callable<? extends Object>> list = new LinkedList<>();
        Callable<GrassTerrain> grassCallable = null;
        Callable<List<Spatial>> cubeCallable = null;

        if (features.cubes) {
            cubeCallable = CubePlacer.generate(piece.terrain, am, 5000, (v2, size) -> piece.meshOnRoad(v2, size));
            list.add(cubeCallable);
        }
        if (features.grass) {
            grassCallable = GrassPlacer.generate(piece.terrain, am, 10000, (v2) -> piece.meshOnRoad(v2));
            list.add(grassCallable);
        }
        
        if (!list.isEmpty()) {
            try {
                executor.invokeAll(list);

                // grass
                if (grassCallable != null) {
                    obj.grass = grassCallable.call();
                    Geometry g = new Geometry("'grass'", obj.grass);
                    this.rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Pink));
                }

                // cubes
                if (cubeCallable != null) {
                    var cubes = cubeCallable.call();
                    var ob = getState(ObjectPlacer.class);
                    obj.cubes = ob.addBulk(cubes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Camera cam = getApplication().getCamera();
        for (var feat :this.pieces.values()) {
            if (feat.grass != null)
                feat.grass.update(cam.getRotation());
        }
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdownNow();
        rootNode.removeFromParent();

        var ob = getState(ObjectPlacer.class);
        for (var feat :this.pieces.values()) {
            if (feat.cubes != null)
                ob.removeBulk(feat.cubes);
        }

        for (var obj: this.pieces.values()) {
            obj.piece.cleanup(app, getState(BulletAppState.class).getPhysicsSpace());
        }

        this.rootNode.removeFromParent();
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

    @Override
    public LoadResult loadPercent() {
        if (this.pieces.values().isEmpty())
            return new LoadResult(0, null);
        var loaded = this.pieces.values().stream().filter(x -> x.loaded).count();
        return new LoadResult((float)loaded/this.pieces.size(), null);
    }

    protected Transform getStart() {
        if (this.pieces.isEmpty())
            return null;

        TerrainPiece first = this.pieces.values().iterator().next().piece;

        // TODO error checking at all
        Vector3f start = first.roadPointLists.get(0).getFirst();
        Vector3f start2 = first.roadPointLists.get(0).get(1);
        Vector3f offset = start2.subtract(start).normalize();
        return new Transform(start.add(0, 0.5f, 0).add(offset.mult(3)), 
            new Quaternion().lookAt(start2.subtract(start), Vector3f.UNIT_Y));
    }

    protected Collection<Vector3f> getRoadPoints() {
        if (this.pieces.isEmpty())
            return null;

        Collection<Vector3f> points = new LinkedList<>();
        for (var obj: this.pieces.values()) {
            for (RoadPointList list: obj.piece.roadPointLists)
                points.addAll(list);
        }
        return points;
    }

    class TerrainObj {
        public TerrainPiece piece;
        public boolean loaded;
        public GrassTerrain grass;
        public NodeId cubes;
    }
}
