package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Spline;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.Transform;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Geo;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.TerrainUtil;
import rallygame.service.search.AStar;
import rallygame.world.ICheckpointWorld;
import rallygame.world.World;
import rallygame.world.WorldType;

public class PathWorld extends World implements ICheckpointWorld {

    class RoadPointList extends LinkedList<Vector3f> {
        private static final long serialVersionUID = 1L;
        public boolean set;
        public boolean used;
    }

    private static final float HEIGHT_WEIGHT = 0.02f;

    private final Node roadNode;
    private final int sideLength;
    private final Vector2f start;
    private final Vector2f end;
    private final float terrainScaleXZ;
    private final float terrainScaleY;
    private final Random rand;

    private FilteredBasis filteredBasis;
    private TerrainQuad terrain;
    private Control collision;
    private ScheduledThreadPoolExecutor executor;
    private List<RoadPointList> roadPointLists;

    // TODO:
    // maybe we need to have 'any' point on the other side of the quad as the search goal
    // this is how we keep it moving along the world, the other sideways quads are only for show

    // https://github.com/jMonkeyEngine/sdk/blob/master/jme3-terrain-editor/src/com/jme3/gde/terraineditor/tools/LevelTerrainToolAction.java

    public PathWorld() {
        this(FastMath.nextRandomInt());
    }
    public PathWorld(int seed) {
        this(seed, 6, 25, 400);
    }
    public PathWorld(int seed, int size, float scaleXZ, float scaleY) {
        super("PathWorld");

        roadNode = new Node("lineNode");
        sideLength = (1 << size) + 1; //6 -> 64 + 1
        terrainScaleXZ = scaleXZ;
        terrainScaleY = scaleY;
        
        rand = new Random(seed);

        int length = sideLength / 2 - 2;
        start = new Vector2f(-length, -length);
        end = new Vector2f(length, length);

        roadPointLists = new LinkedList<>();
    }

    @Override
    public void reset() {
    }

    @Override
    public Vector3f getStartPos() {
        if (terrain == null)
            return H.v2tov3fXZ(this.start);
        return gridToWorldSpace(terrain, this.start);
    }

    @Override
    public WorldType getType() {
        return WorldType.PATH;
    }

    private FilteredBasis getFilteredBasis() {
        // larger height variance filter
        FractalSum base = new FractalSum();
        base.addModulator(new NoiseModulator() {
            @Override
            public float value(float... in) {
                return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
            }
        });
        FilteredBasis ground = new FilteredBasis(base);
        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(0.119f);

        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(10);
        therm.setTalus(0.011f);

        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(1);
        smooth.setEffect(0.7f);

        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(1); // higher numbers make it really smooth

        ground.addPreFilter(iterate);

        return ground;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        this.rootNode.attachChild(roadNode);

        AssetManager am = app.getAssetManager();

        filteredBasis = getFilteredBasis();

        int sx = rand.nextInt(1000);
        int sy = rand.nextInt(1000);
        float[] heightMap = filteredBasis.getBuffer(sx*(sideLength - 1), sy*(sideLength - 1), 0, sideLength).array();
        terrain = new TerrainQuad("path terrain", sideLength, sideLength, heightMap);
        terrain.setLocalScale(new Vector3f(terrainScaleXZ, terrainScaleY, terrainScaleXZ));

        Material baseMat = new Material(am, "mat/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setFloat("Scale", terrainScaleY * 0.8f); //margin of 0.1f
        baseMat.setFloat("Offset", terrainScaleY * 0.1f);

        terrain.setMaterial(baseMat);
        terrain.setQueueBucket(Bucket.Opaque);
        this.rootNode.attachChild(terrain);
        updateTerrainCollision();

        executor = new ScheduledThreadPoolExecutor(2);

        searchFrom(start, new Vector2f());
        searchFrom(new Vector2f(), end);
    }

    private void updateTerrainCollision() {
        if (collision != null)
            getState(BulletAppState.class).getPhysicsSpace().remove(collision);

        collision = new RigidBodyControl(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()), 0);
        terrain.addControl(collision);
        getState(BulletAppState.class).getPhysicsSpace().add(collision);
    }

    @Override
    public void update(float tpf) {
        // wait until all the road points lists are set before doing this
        if (!H.allTrue(x -> x != null && x.set && !x.used, roadPointLists))
            return;

        AssetManager am = getApplication().getAssetManager();
        for (RoadPointList roadPointList : new LinkedList<>(roadPointLists)) {
            if (roadPointList.isEmpty()) {
                roadPointList.used = true;
                continue;
            }
            
            roadPointList.add(0, roadPointList.get(0)); // copy the first one
            roadPointList.add(roadPointList.get(roadPointList.size() - 1)); // copy the last one
            CatmullRomRoad road = drawRoad(am, roadPointList);

            roadPointList.used = true;
            executor.submit(() -> {
                List<Vector3f[]> quads = road.getMeshAsQuads();
                getApplication().enqueue(() -> {
                    Map<Vector2f, Float> heights = TerrainUtil.lowerTerrainSoItsUnderQuads(terrain, quads);
                    updateTerrainCollision();
                    drawBoxes(am, this.roadNode, heights);
                });
            });
        }
    }

    private CatmullRomRoad drawRoad(AssetManager am, List<Vector3f> list) {
        // TODO use a rolling average to smooth points so there are less hard corners

        // draw a spline road to show where it is
        Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth
        CatmullRomRoad c3 = new CatmullRomRoad(s3, 1, 10);
        Geometry g = new Geometry("spline road", c3);
        this.roadNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Green));

        // add to physics space
        CollisionShape col = CollisionShapeFactory.createMeshShape(g);
        RigidBodyControl c = new RigidBodyControl(col, 0);
        getState(BulletAppState.class).getPhysicsSpace().add(c);

        return c3;
    }

    private void drawBoxes(AssetManager am, Node node, Map<Vector2f, Float> vecMap) {
        List<Vector3f> heights3 = vecMap.entrySet().stream()
                .map(x -> new Vector3f(x.getKey().x, x.getValue() * terrain.getWorldScale().y, x.getKey().y))
                .collect(Collectors.toList());
        drawBoxes(am, this.roadNode, heights3);
    }
    private void drawBoxes(AssetManager am, Node node, List<Vector3f> points) {
        float size = 0.2f;
        Box box = new Box(size, size, size);
        for (Vector3f pos : points) {
            Geometry g = Geo.createShape(am, box, ColorRGBA.Yellow, pos.add(0, size / 2, 0), "boxes");
            g.getMaterial().getAdditionalRenderState().setWireframe(false);
            this.roadNode.attachChild(g);
        }
    }

    private void searchFrom(Vector2f start, Vector2f end) {
        RoadPointList outList = new RoadPointList();
        roadPointLists.add(outList);
        executor.submit(() -> {
            List<Vector2f> list = null;
            try {
                AStar<Vector2f> search = new AStar<>(new SearchWorld(terrain, HEIGHT_WEIGHT));
                search.setTimeoutMills((long)(10*1E6)); // x sec
                list = search.findPath(H.v3tov2fXZ(gridToWorldSpace(terrain, start)),
                        H.v3tov2fXZ(gridToWorldSpace(terrain, end)));
                outList.addAll(list.stream().map(x -> new Vector3f(x.x, terrain.getHeight(x), x.y))
                        .collect(Collectors.toList()));
                Log.p("Done astar search, path length = ", list.size());
            } catch (Exception e) {
                outList.addAll(new LinkedList<>());
                Log.e(e);
                Log.e("Astar search failed");
            } finally {
                outList.set = true;
            }
        });
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdownNow(); // TODO forceful because when the search doesn't finish this blocks closing
        super.cleanup(app);
    }

    static Vector3f gridToWorldSpace(TerrainQuad terrain, Vector2f pos) {
        Vector3f scale = terrain.getLocalScale();
        Vector2f worldPos = pos.clone();
        worldPos.x *= scale.x;
        worldPos.y *= scale.z;
        return new Vector3f(worldPos.x, terrain.getHeight(worldPos), worldPos.y);
    }

    static Vector2f worldToGridSpace(TerrainQuad terrain, Vector3f pos) {
        Vector3f scale = terrain.getLocalScale();
        Vector2f out = H.v3tov2fXZ(pos);
        out.x /= scale.x;
        out.y /= scale.z;
        return out;
    }

    @Override
    public float loadPercent() {
        if (roadPointLists.isEmpty())
            return 0;

        float total = 0;
        for (RoadPointList pointList : roadPointLists) {
            total += pointList.used ? 1 : 0;
        }
        return total / roadPointLists.size();
    }

    @Override
    public Transform start(int i) {
        return new Transform(this.getStartPos(), this.getStartRot());
    }

    @Override
    public Vector3f[] checkpoints() {
        if (this.roadPointLists.isEmpty())
            throw new IllegalStateException("Unknown checkpoint position, was i called before i was loaded?");
        List<Vector3f> output = new LinkedList<>();
        for (RoadPointList p: this.roadPointLists) {
            output.addAll(p);
        }

        return output.toArray(new Vector3f[0]);
    }
}
