package rallygame.world;

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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.helper.Geo;
import rallygame.helper.H;
import rallygame.service.AStar;
import rallygame.service.PerlinNoise;

public class PathWorld extends World {

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
        return H.v2tov3fXZ(this.start);
    }

    @Override
    public WorldType getType() {
        return WorldType.PATH;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        // TODO copy "Common/MatDefs/Terrain/Terrain.j3md" for proper height material colour stuff

        noise = new PerlinNoise(sideLength);
        noise.load();

        terrain = new TerrainQuad("path terrain", sideLength, sideLength, noise.getHeightMap());
        terrain.setLocalScale(1, 2, 1);
        Material baseMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        baseMat.setColor("Color", ColorRGBA.Magenta);
        terrain.setMaterial(baseMat);

        this.rootNode.attachChild(terrain);

        collision = new RigidBodyControl(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()),
                0);
        terrain.addControl(collision);
        getState(BulletAppState.class).getPhysicsSpace().add(collision);

        astar = new AStar<Vector2f>(new AStarWorld(terrain));

        executor = new ScheduledThreadPoolExecutor(1);
        executor.submit(() -> {

            List<Vector2f> list = astar.findPath(start, end);

            this.list = list.stream().map(x -> new Vector3f(x.x, terrain.getHeight(x), x.y))
                    .collect(Collectors.toList());
            this.done = true;
        });

        this.rootNode.attachChild(lineNode);
    }

    @Override
    public void update(float tpf) {
        // get explored nodes to show them visually
        lineNode.detachAllChildren();
        AssetManager am = getApplication().getAssetManager();
        drawHelpNodes(am);

        if (done) {
            for (int i = 0; i < list.size() - 1; i++) {
                Geometry g = Geo.makeShapeLine(am, ColorRGBA.Orange, list.get(i + 1), list.get(i));
                this.lineNode.attachChild(g);
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

        Vector3f start3 = H.v2tov3fXZ(start);
        start3.y = terrain.getHeight(start);
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

        public AStarWorld(TerrainQuad terrain) {
            this.terrain = terrain;
        }

        @Override
        public float getWeight(Vector2f v1, Vector2f v2) {
            float diffHeight = Math.abs(terrain.getHeight(v1) - terrain.getHeight(v2));
            // http://blog.runevision.com/2016/03/note-on-creating-natural-paths-in.html
            return v2.distance(v1) * (1 + diffHeight * diffHeight * 54);
        }

        @Override
        public float getHeuristic(Vector2f v1, Vector2f v2) {
            return v2.distance(v1);
        }

        @Override
        public Set<Vector2f> getNeighbours(Vector2f pos) {
            List<Vector2f> results = new LinkedList<>();

            for (int x = (int) pos.x - 1; x < (int) pos.x + 2; x++) {
                for (int z = (int) pos.y - 1; z < (int) pos.y + 2; z++) {
                    if (x == pos.x && z == pos.y)
                        continue; // ignore self

                    Vector2f newPos = new Vector2f(x, z);

                    if (Float.isNaN(terrain.getHeight(newPos)))
                        continue;
                    results.add(newPos);
                }
            }
            return new HashSet<>(results);
        }

    }
}
