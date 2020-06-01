package rallygame.world.path;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import rallygame.helper.Log;
import rallygame.service.ILoadable;

public class Terrain extends BaseAppState implements ILoadable {

    protected static final float HEIGHT_WEIGHT = 0.02f;
    protected static final float ROAD_WIDTH = 10f;

    protected final int sideLength;
    protected final Vector3f terrainScale;
    protected final int tileCount;

    protected final Random rand;
    protected final FilteredBasisTerrain filteredBasis;
    protected final ExecutorService executor;
    private final Node rootNode;
    private final boolean showTerrainFeatures;

    // contains the list of TerrainQuads mapped to locations
    // each terrain quad contains the roads and objects that exist in it
    private final Map<Vector2f, TerrainPiece> pieces = new HashMap<>();

    public Terrain(long seed, int size, Vector3f scale, int tileCount, boolean showTerrainFeatures) {
        this.sideLength = (1 << size) + 1; // 6 -> 64 + 1
        this.terrainScale = scale;
        this.tileCount = Math.max(tileCount, 1);
        this.showTerrainFeatures = showTerrainFeatures;

        this.rootNode = new Node("Terrain root node");

        Log.p("Starting path world with seed: " + seed);
        this.rand = new Random(seed);
        
        this.filteredBasis = FilteredBasisTerrain.generate(new Vector2f(rand.nextInt(1000), rand.nextInt(1000)));

        this.executor = Executors.newFixedThreadPool(2);
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication)app).getRootNode().attachChild(rootNode);

        //load terrainquads
        for (int i = 0; i < tileCount; i++)
            generateTQuadAt(new Vector2f(i, 0), app.getAssetManager());
    }

    private void generateTQuadAt(Vector2f center, AssetManager am) {
        int cubeCount = 0;
        boolean drawGrass = false;
        if (showTerrainFeatures) {
            cubeCount = 1000;
            drawGrass = true;
        }
        TerrainPiece piece = new TerrainPiece(this, center, cubeCount, drawGrass);
        piece.generate(am, getState(BulletAppState.class).getPhysicsSpace());
        
        this.pieces.put(center, piece);
        this.rootNode.attachChild(piece.rootNode);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Camera cam = getApplication().getCamera();
        for (TerrainPiece p :this.pieces.values()) {
            if (p.gt != null)
                p.gt.update(cam);
        }
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdownNow();
        rootNode.removeFromParent();

        for (TerrainPiece p: this.pieces.values()) {
            p.cleanup(app, getState(BulletAppState.class).getPhysicsSpace());
        }
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

    @Override
    public LoadResult loadPercent() {
        if (this.pieces.values().isEmpty())
            return new LoadResult(0, null);
        if (this.pieces.values().iterator().next().loaded)
            return new LoadResult(1, null);
        return new LoadResult(0, null);
    }

    public Transform getStart() {
        if (this.pieces.isEmpty())
            return null;

        TerrainPiece first = this.pieces.values().iterator().next();

        Vector3f start = first.roadPointLists.get(0).getFirst();
        Vector3f start2 = first.roadPointLists.get(0).get(1);
        Vector3f offset = start2.subtract(start).normalize();
        return new Transform(start.add(0, 0.5f, 0).add(offset.mult(3)), 
            new Quaternion().lookAt(start2.subtract(start), Vector3f.UNIT_Y));
    }

    public Collection<Vector3f> getRoadPoints() {
        if (this.pieces.isEmpty())
            return null;

        Collection<Vector3f> points = new LinkedList<>();
        for (TerrainPiece road: this.pieces.values()) {
            for (RoadPointList list: road.roadPointLists)
                points.addAll(list);
        }
        return points;
    }
}
