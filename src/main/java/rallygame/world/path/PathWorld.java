package rallygame.world.path;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Geo;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.TerrainUtil;
import rallygame.service.search.AStar;
import rallygame.service.search.ISearch;
import rallygame.service.PerlinNoise;
import rallygame.service.search.ISearchWorld;
import rallygame.world.World;
import rallygame.world.WorldType;

public class PathWorld extends World {

    class RoadPointList extends LinkedList<Vector3f> {
        private static final long serialVersionUID = 1L;
        public boolean used;
    }

    private final Node roadNode;
    private final int sideLength;
    private final Vector2f start;
    private final Vector2f end;
    private final float terrainScaleXZ;
    private final float terrainScaleY;

    private PerlinNoise noise;
    private TerrainQuad terrain;
    private Control collision;
    private ScheduledThreadPoolExecutor executor;
    private List<RoadPointList> roadPointLists;

    // TODO maybe we need to have 'any' point on the other side of the quad as the
    // search goal
    // this is how we keep it moving along the world, the other sideways quads are
    // only for show

    // https://github.com/jMonkeyEngine/sdk/blob/master/jme3-terrain-editor/src/com/jme3/gde/terraineditor/tools/LevelTerrainToolAction.java

    public PathWorld() {
        super("PathWorld");

        roadNode = new Node("lineNode");
        sideLength = (1 << 6) + 1;// 64 -> 6
        terrainScaleXZ = 10;
        terrainScaleY = 20;

        int length = sideLength / 2 - 2;
        start = new Vector2f(-length, -length);
        end = new Vector2f(length, length);
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

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        AssetManager am = app.getAssetManager();

        noise = new PerlinNoise(sideLength, FastMath.nextRandomInt());
        noise.load();
        float[] heights = noise.findMinMaxHeights();
        terrain = new TerrainQuad("path terrain", sideLength, sideLength, noise.getHeightMap());
        terrain.setLocalScale(new Vector3f(terrainScaleXZ, terrainScaleY, terrainScaleXZ));
        Material baseMat = new Material(am, "mat/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setFloat("Scale", heights[1] - heights[0]);
        baseMat.setFloat("Offset", heights[0]);

        terrain.setMaterial(baseMat);
        terrain.setQueueBucket(Bucket.Opaque);
        this.rootNode.attachChild(terrain);
        updateTerrainCollision();

        executor = new ScheduledThreadPoolExecutor(2);
        roadPointLists = new LinkedList<>();
        searchFrom(start, new Vector2f());
        searchFrom(new Vector2f(), end);

        this.rootNode.attachChild(roadNode);
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
        for (RoadPointList roadPointList : new LinkedList<>(roadPointLists))
            if (roadPointList != null && !roadPointList.isEmpty() && !roadPointList.used) {
                AssetManager am = getApplication().getAssetManager();
                roadPointList.add(0, roadPointList.get(0)); // copy the first one
                roadPointList.add(roadPointList.get(roadPointList.size() - 1)); // copy the last one
                CatmullRomRoad road = drawRoad(am, roadPointList);

                executor.submit(() -> {
                    List<Vector3f[]> quads = road.getMeshAsQuads();
                    getApplication().enqueue(() -> {
                        Map<Vector2f, Float> heights = TerrainUtil.lowerTerrainSoItsUnderQuads(terrain, quads);
                        updateTerrainCollision();
                        List<Vector3f> heights3 = heights.entrySet().stream().map(
                                x -> new Vector3f(x.getKey().x, x.getValue() * terrain.getWorldScale().y, x.getKey().y))
                                .collect(Collectors.toList());
                        drawBoxes(am, this.roadNode, heights3);
                    });
                });

                roadPointList.used = true;
            }
    }

    private CatmullRomRoad drawRoad(AssetManager am, List<Vector3f> list) {
        // TODO use a rolling average to smooth points so there are less hard corners

        // draw a spline road to show where it is
        Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth
        CatmullRomRoad c3 = new CatmullRomRoad(s3, 1, 10);
        Geometry g = new Geometry("spline road", c3);
        g.setLocalTranslation(0, 0.2f, 0);
        this.roadNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Green));

        // add to physics space
        CollisionShape col = CollisionShapeFactory.createMeshShape(g);
        RigidBodyControl c = new RigidBodyControl(col, 0);
        getState(BulletAppState.class).getPhysicsSpace().add(c);

        return c3;
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
            Log.p("Started astar search ", outList);
            List<Vector2f> list = null;
            try {
                ISearch<Vector2f> search = new AStar<Vector2f>(new SearchWorld(terrain), null);
                list = search.findPath(H.v3tov2fXZ(gridToWorldSpace(terrain, start)),
                        H.v3tov2fXZ(gridToWorldSpace(terrain, end)));

                outList.addAll(list.stream().map(x -> new Vector3f(x.x, terrain.getHeight(x), x.y))
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                Log.p(e);
            }
            Log.p("Done astar search, path length = ", list == null ? 0 : list.size());
        });
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdownNow(); // TODO forceful because when the search doesn't finish this blocks
        super.cleanup(app);
    }

    class SearchWorld implements ISearchWorld<Vector2f> {
        private static final float HEIGHT_WEIGHT = 0.08f;
        private final TerrainQuad terrain;
        private final Vector3f scale;

        public SearchWorld(TerrainQuad terrain) {
            this.terrain = terrain;
            this.scale = terrain.getWorldScale().clone();
        }

        @Override
        public float getWeight(Vector2f v1, Vector2f v2) {
            float diffHeight = Math.abs(terrain.getHeight(v1) - terrain.getHeight(v2));
            // http://blog.runevision.com/2016/03/note-on-creating-natural-paths-in.html
            return v2.distance(v1) * (1 + diffHeight * diffHeight * scale.y / scale.x * HEIGHT_WEIGHT);
        }

        @Override
        public float getHeuristic(Vector2f v1, Vector2f v2) {
            // to be admissable this must be strictly less than the getWeight function
            // however its height diff is squared for weighting reasons, so we can't just
            // and the height diff here
            return v2.distance(v1);
        }

        @Override
        public Set<Vector2f> getNeighbours(Vector2f pos) {
            List<Vector2f> results = new LinkedList<>();
            float scaleX = scale.x;
            float scaleZ = scale.z;
            for (float x = pos.x - scaleX; x < pos.x + 2 * scaleX; x += scaleX) {
                for (float z = pos.y - scaleZ; z < pos.y + 2 * scaleZ; z += scaleZ) {
                    if (x == pos.x && z == pos.y)
                        continue; // ignore self

                    Vector2f newPos = new Vector2f(x, z);

                    if (Float.isNaN(terrain.getHeight(newPos)))
                        continue;
                    // TODO how do we avoid the edge of the terrain?
                    results.add(newPos);
                }
            }
            return new HashSet<>(results);
        }
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
}
