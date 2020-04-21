package rallygame.world.path;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.TerrainUtil;
import rallygame.helper.Trig;
import rallygame.service.ObjectPlacer;
import rallygame.service.ObjectPlacer.NodeId;
import rallygame.service.search.AStar;

class TerrainPiece {

    private final Terrain terrainApp;
    public final List<RoadPointList> roadPointLists;
    public final int sideLength;
    public final Vector3f terrainScale;

    public TerrainQuad terrain;
    public Vector2f offset;
    public boolean loaded;

    public Vector2f roadStart;
    public Vector2f roadEnd;

    public final Node rootNode;
    public NodeId placedObjects;
    public Control collision;

    public TerrainPiece(Terrain terrainApp, Vector2f center) {
        this.terrainApp = terrainApp;
        this.sideLength = terrainApp.sideLength;
        this.terrainScale = terrainApp.terrainScale;
        this.offset = center;
        this.rootNode = new Node("terrain at " + offset);
        this.rootNode.setLocalTranslation(H.v2tov3fXZ(offset));

        this.roadPointLists = new LinkedList<RoadPointList>();
    }

    public void generate(AssetManager am, PhysicsSpace space) {
        float[] heightMap = terrainApp.filteredBasis.getBuffer(sideLength, offset).array();
        terrain = new TerrainQuad("path terrain", sideLength, sideLength, heightMap);
        terrain.setLocalScale(terrainScale);

        Material baseMat = new Material(am, "mat/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setFloat("Scale", terrainScale.y * 0.8f); // margin of 0.1f
        baseMat.setFloat("Offset", terrainScale.y * 0.1f);

        terrain.setMaterial(baseMat);
        rootNode.attachChild(terrain);

        updateTerrainCollision(space);

        // generate start points from one edge to the other
        final int halfLength = sideLength / 2;
        roadStart = new Vector2f(-halfLength, terrainApp.rand.nextInt(halfLength) - halfLength / 2);
        roadEnd = new Vector2f(halfLength - 1, terrainApp.rand.nextInt(halfLength) - halfLength / 2);

        generateExtra(space);
    }

    private void updateTerrainCollision(PhysicsSpace space) {
        if (collision != null)
            space.remove(collision);

        collision = new RigidBodyControl(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()),
                0);
        terrain.addControl(collision);
        space.add(collision);
    }

    private void generateExtra(PhysicsSpace space) {
        Callable<Void> roadPart1 = searchFrom(roadStart, new Vector2f(), space);
        Callable<Void> roadPart2 = searchFrom(new Vector2f(), roadEnd, space);

        try {
            List<Future<Void>> waiting = terrainApp.executor.invokeAll(Arrays.asList(roadPart1, roadPart2));
            for (Future<Void> wait: waiting)
                wait.get();
            //yay
        } catch (InterruptedException | ExecutionException e) {
            Log.e(e);
            return;
        }
        
        Application app = terrainApp.getApplication();
        AssetManager am = app.getAssetManager();
        app.enqueue(() -> {
            for (RoadPointList outRoad : roadPointLists) {
                outRoad.road = drawRoad(app.getAssetManager(), outRoad, space);
                List<Vector3f[]> quads = outRoad.road.getMeshAsQuads();

                TerrainUtil.lowerTerrainSoItsUnderQuads(terrain, quads);
            }
            
            updateTerrainCollision(space);
        
            
            // Place random cubes
            ObjectPlacer op = terrainApp.getState(ObjectPlacer.class);
            float size = 1f;
            Box b = new Box(size, size, size);
            Spatial s = new Geometry("box", b);
            s = LoadModelWrapper.create(am, s, ColorRGBA.Blue);
            float maxXZ = terrainScale.x * (sideLength - 1) / 2;

            List<Spatial> list = new LinkedList<>();
            List<Vector3f> locations = new LinkedList<>();
            for (int i = 0; i < 1000; i++) {
                Spatial s1 = s.clone();
                float scale = FastMath.nextRandomFloat() * 25 + 1;
                s1.setLocalScale(scale);
                s1.setLocalRotation(new Quaternion(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(),
                        FastMath.nextRandomFloat(), FastMath.nextRandomFloat()));
                list.add(s1);
                locations.add(selectPointNotOnRoad(scale, maxXZ));
            }

            placedObjects = op.addBulk(list, locations);

            drawGrass(am);

            this.loaded = true;
        });
    }

    private Callable<Void> searchFrom(Vector2f start, Vector2f end, PhysicsSpace space) {
        RoadPointList outRoad = new RoadPointList();
        roadPointLists.add(outRoad);
        
        return () -> {
            List<Vector2f> list = null;
            try {
                AStar<Vector2f> search = new AStar<>(new SearchWorld(terrain, Terrain.HEIGHT_WEIGHT, 0.2f));
                search.setTimeoutMills((long) (30 * 1E6)); // x sec
                list = search.findPath(H.v3tov2fXZ(
                        gridToWorldSpace(this, start)),
                        H.v3tov2fXZ(gridToWorldSpace(this, end)));
                outRoad.addAll(list.stream().map(x -> new Vector3f(x.x, terrain.getHeight(x), x.y))
                        .collect(Collectors.toList()));
                Log.p("Done astar search, path length =", list.size());
            } catch (TimeoutException e) {
                outRoad.addAll(new LinkedList<>());
                Log.e(e);
                Log.e("Astar search failed due to timeout");
                outRoad.failed = true;
            } catch (Exception e) {
                Log.e(e);
            }
            
            return null;
        };
    }

    private CatmullRomRoad drawRoad(AssetManager am, List<Vector3f> list, PhysicsSpace space) {
        // TODO use a rolling average to smooth points so there are less hard corners

        // draw a spline road to show where it is
        Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth
        CatmullRomRoad c3 = new CatmullRomRoad(s3, 1, Terrain.ROAD_WIDTH);
        Geometry g = new Geometry("spline road", c3);
        rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Green));
        //TODO offset on this?

        // add to physics space
        CollisionShape col = CollisionShapeFactory.createMeshShape(g);
        RigidBodyControl c = new RigidBodyControl(col, 0);
        space.add(c);

        return c3;
    }

    private GrassTerrain drawGrass(AssetManager am) {
        GrassTerrain gt = new GrassTerrain(this, 100000, (v2) -> meshOnRoad(v2));
        Geometry g = new Geometry("'grass'", gt);
        rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Pink));

        return gt;
    }

    private Vector3f selectPointNotOnRoad(float objRadius, float max) {
        Vector2f location = H.randV2f(max, true);
        while (meshOnRoad(objRadius, location)) {
            location = H.randV2f(max, true);
        }
        return new Vector3f(location.x, terrain.getHeight(location), location.y);
    }

    private boolean meshOnRoad(Vector2f location) {
        return meshOnRoad(0, location);
    }
    private boolean meshOnRoad(float objRadius, Vector2f location) {
        // simple check that its more than x from a road vertex
        for (RoadPointList road : this.roadPointLists) {
            for (int i = 0; i < road.size() - 1; i++) {
                Vector3f cur = road.get(i);
                Vector3f point = road.get(i + 1);
                if (Trig.distFromSegment(H.v3tov2fXZ(cur), H.v3tov2fXZ(point), location) < FastMath.sqrt(2) * objRadius
                        + Terrain.ROAD_WIDTH / 2)
                    return true;
                cur = point;
            }
        }

        return false;
    }


/*
    private void drawBoxes(AssetManager am, Node node, Map<Vector2f, Float> vecHeightMap) {
        List<Vector3f> heights3 = vecHeightMap.entrySet().stream()
                .map(x -> new Vector3f(x.getKey().x, x.getValue() * terrain.getWorldScale().y, x.getKey().y))
                .collect(Collectors.toList());
        drawBoxes(am, this.rootNode, heights3);
    }
    private void drawBoxes(AssetManager am, Node node, List<Vector3f> points) {
        float size = 0.2f;
        Box box = new Box(size, size, size);
        for (Vector3f pos : points) {
            Geometry g = Geo.createShape(am, box, ColorRGBA.Yellow, pos.add(0, size / 2, 0), "boxes");
            this.rootNode.attachChild(g);
        }
    }
*/


    public void cleanup(Application app, PhysicsSpace space) {
        this.rootNode.removeFromParent();

        if (this.placedObjects != null) {
            ObjectPlacer op = app.getStateManager().getState(ObjectPlacer.class);
            op.removeBulk(this.placedObjects);
        }
    }

    private static Vector3f gridToWorldSpace(TerrainPiece piece, Vector2f pos) {
        Vector3f scale = piece.terrain.getLocalScale();
        Vector2f worldPos = pos.clone();
        worldPos.x *= scale.x;
        worldPos.y *= scale.z;
        return new Vector3f(worldPos.x, piece.terrain.getHeight(worldPos), worldPos.y).add(H.v2tov3fXZ(piece.offset));
    }

    //TODO test
    static Vector2f worldToGridSpace(TerrainPiece piece, Vector3f pos) {
        Vector3f scale = piece.terrain.getLocalScale();
        Vector2f out = H.v3tov2fXZ(pos).subtract(piece.offset);
        out.x /= scale.x;
        out.y /= scale.z;
        return out;
    }
}

class RoadPointList extends LinkedList<Vector3f> {
    private static final long serialVersionUID = 1L;
    public boolean failed;
    public CatmullRomRoad road;
}