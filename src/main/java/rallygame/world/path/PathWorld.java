package rallygame.world.path;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
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
import rallygame.service.AStar;
import rallygame.service.PerlinNoise;
import rallygame.world.World;
import rallygame.world.WorldType;

public class PathWorld extends World {

    public static Vector3f gridToWorldSpace(TerrainQuad terrain, Vector2f pos) {
        Vector3f scale = terrain.getLocalScale();
        Vector2f worldPos = pos.clone();
        worldPos.x *= scale.x;
        worldPos.y *= scale.z;
        return new Vector3f(worldPos.x, terrain.getHeight(worldPos), worldPos.y);
    }
    
    public static Vector2f worldToGridSpace(TerrainQuad terrain, Vector3f pos) {
        Vector3f scale = terrain.getLocalScale();
        Vector2f out = H.v3tov2fXZ(pos);
        out.x /= scale.x;
        out.y /= scale.z;
        return out;
    }

    private final Node lineNode;
    private final int sideLength;
    private final Vector2f start;
    private final Vector2f end;

    private PerlinNoise noise;
    private TerrainQuad terrain;
    private Control collision;
    private ScheduledThreadPoolExecutor executor;
    private AStar<Vector2f> astar;
    private List<Vector3f> list;
    private boolean done;

    //TODO maybe we need to have 'any' point on the other side of the quad to win with
    //this is how we keep it moving along the world, the other sideways quads are only for show

    public PathWorld() {
        super("PathWorld");

        lineNode = new Node("lineNode");
        sideLength = 65;

        int length = sideLength / 2 - 4;
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

        
        noise = new PerlinNoise(sideLength);
        noise.load();
        float[] heights = noise.findMinMaxHeights();
        terrain = new TerrainQuad("path terrain", sideLength, sideLength, noise.getHeightMap());
        terrain.setLocalScale(new Vector3f(30, 80, 30));
        Material baseMat = new Material(app.getAssetManager(), "mat/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setFloat("Scale", heights[1] - heights[0]);
        baseMat.setFloat("Offset", heights[0]);

        terrain.setMaterial(baseMat);
        terrain.setQueueBucket(Bucket.Opaque);
        this.rootNode.attachChild(terrain);

        collision = new RigidBodyControl(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()), 0);
        terrain.addControl(collision);
        getState(BulletAppState.class).getPhysicsSpace().add(collision);

        astar = new AStar<Vector2f>(new AStarWorld(terrain));

        executor = new ScheduledThreadPoolExecutor(1);
        executor.submit(() -> {
            List<Vector2f> list = astar.findPath(H.v3tov2fXZ(gridToWorldSpace(terrain, start)),
                H.v3tov2fXZ(gridToWorldSpace(terrain, end)));
            
            this.list = list.stream().map(x -> new Vector3f(x.x, terrain.getHeight(x), x.y))
                    .collect(Collectors.toList());
            this.done = true;
            Log.p("Done astar search, path length = ", list == null ? 0 : list.size());
        });

        this.rootNode.attachChild(lineNode);
    }

    @Override
    public void update(float tpf) {
        AssetManager am = getApplication().getAssetManager();

        if (!done) {
            lineNode.detachAllChildren();
            // get explored nodes to show them visually
            drawHelpNodes(am); // stop drawing them once done
        } else {
            if (list != null) {
                // draw a spline road to show where it is
                Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth
                CatmullRomRoad c3 = new CatmullRomRoad(s3, 16, 10);
                Geometry g = new Geometry("spline road", c3);
                this.lineNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Green));

                list = null;
            }
        }
    }

    private void drawHelpNodes(AssetManager am) {
        float size = 0.02f;
        Box box = new Box(size, size, size);

        for (Vector2f pos : new LinkedList<>(astar.closed)) {
            Vector3f pos3 = H.v2tov3fXZ(pos);
            pos3.y = terrain.getHeight(pos);
            Geometry g = Geo.createShape(am, box, ColorRGBA.Gray, pos3.add(0, size / 2, 0), "box2");
            g.getMaterial().getAdditionalRenderState().setWireframe(false);
            this.lineNode.attachChild(g);
        }

        Vector3f start3 = gridToWorldSpace(terrain, start);
        this.lineNode.attachChild(Geo.makeShapeBox(am, ColorRGBA.Pink, start3, 0.2f));
        Vector3f end3 = H.v2tov3fXZ(end);
        end3.y = terrain.getHeight(end);
        this.lineNode.attachChild(Geo.makeShapeBox(am, ColorRGBA.Pink, end3, 0.2f));
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdown();
        super.cleanup(app);
    }

    class AStarWorld implements AStar.IAStarWorld<Vector2f> {

        private final TerrainQuad terrain;
        private final Vector3f scale;

        public AStarWorld(TerrainQuad terrain) {
            this.terrain = terrain;
            this.scale = terrain.getLocalScale().clone();
        }

        @Override
        public float getWeight(Vector2f v1, Vector2f v2) {
            float diffHeight = Math.abs(terrain.getHeight(v1) - terrain.getHeight(v2));
            // http://blog.runevision.com/2016/03/note-on-creating-natural-paths-in.html
            return v2.distance(v1) * (1 + diffHeight * diffHeight * 0.4f);
        }

        @Override
        public float getHeuristic(Vector2f v1, Vector2f v2) {
            //to be admissable this must be strictly less than the getWeight function
            //however its height diff is squared for weighting reasons, so we can't just and the height diff here
            return v2.distance(v1);
        }

        @Override
        public Set<Vector2f> getNeighbours(Vector2f pos) {
            List<Vector2f> results = new LinkedList<>();

            for (float x = pos.x - scale.x; x < pos.x + 2 * scale.x; x += scale.x) {
                for (float z = pos.y - scale.z; z < pos.y + 2 * scale.z; z += scale.z) {
                    if (x == pos.x && z == pos.y)
                        continue; // ignore self

                    Vector2f newPos = new Vector2f(x, z);

                    if (Float.isNaN(terrain.getHeight(newPos)))
                        continue;
                    //TODO how do we avoid the edge of the terrain?
                    results.add(newPos);
                }
            }
            return new HashSet<>(results);
        }

    }
}
