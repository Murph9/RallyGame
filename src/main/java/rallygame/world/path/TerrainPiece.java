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
import com.jme3.math.Spline;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
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
    protected final List<RoadPointList> roadPointLists;

    protected final Node rootNode;
    private final Vector2f offset;

    private final boolean needsRoad;

    protected TerrainQuad terrain;
    private boolean inPhysics;

    private Vector2f roadStart;
    private Vector2f roadEnd;

    private NodeId placedObjects;

    protected TerrainPiece(Terrain terrainApp, Vector2f center) {
        this(terrainApp, center, true);
    }
    protected TerrainPiece(Terrain terrainApp, Vector2f center, boolean needsRoad) {
        this.terrainApp = terrainApp;
        this.offset = center;
        this.rootNode = new Node("terrain at " + offset);

        this.needsRoad = needsRoad;

        this.roadPointLists = new LinkedList<RoadPointList>();
    }

    protected void generate(AssetManager am, PhysicsSpace space, Vector3f scale) {
        float[] heightMap = terrainApp.filteredBasis.getBuffer(terrainApp.sideLength, offset).array();
        terrain = new TerrainQuad("path terrain", terrainApp.sideLength, terrainApp.sideLength, heightMap);
        terrain.setLocalScale(scale);

        terrain.setLocalTranslation(pieceGridToWorldPos(this.offset.mult((terrainApp.sideLength - 1)), scale));

        Material baseMat = new Material(am, "MatDefs/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setColor("LowColor", new ColorRGBA(1.0f, 0.55f, 0.0f, 1.0f));
        baseMat.setColor("HighColor", new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f));
        baseMat.setFloat("Scale", scale.y * 0.6f); // margin of 0.2f
        baseMat.setFloat("Offset", scale.y * 0.2f);

        terrain.setMaterial(baseMat);
        rootNode.attachChild(terrain);

        updateTerrainCollision(space);

        // generate start points from one edge to the other
        final int halfLength = terrainApp.sideLength / 2;
        roadStart = new Vector2f(-halfLength, terrainApp.rand.nextInt(halfLength) - halfLength / 2);
        roadEnd = new Vector2f(halfLength - 1, terrainApp.rand.nextInt(halfLength) - halfLength / 2);

        if (needsRoad)
            generateRoad(space);
        else
            terrainApp.finishedMinimalLoading(this);
    }

    private void generateRoad(PhysicsSpace space) {
        //generate both roads
        Callable<RoadPointList> roadPart1 = searchFrom(roadStart, new Vector2f());
        Callable<RoadPointList> roadPart2 = searchFrom(new Vector2f(), roadEnd);

        try {
            List<Future<RoadPointList>> waiting = terrainApp.executor.invokeAll(Arrays.asList(roadPart1, roadPart2));
            for (Future<RoadPointList> wait: waiting)
                roadPointLists.add(wait.get());
            //yay
        } catch (InterruptedException | ExecutionException e) {
            Log.e(e);
            return;
        }

        //add them to the world and physics spaces
        Application app = terrainApp.getApplication();
        AssetManager am = app.getAssetManager();
        app.enqueue(() -> {
            List<Vector3f[]> quads = new LinkedList<>();
            for (RoadPointList outRoad : roadPointLists) {
                if (outRoad.failed)
                    continue;
                outRoad.road = drawRoad(am, outRoad, space);
                quads.addAll(outRoad.road.getMeshAsQuads());
            }
            
            var heights = TerrainUtil.getHeightsForQuads(terrain.getLocalScale(), quads);
            TerrainUtil.setTerrainHeights(Arrays.asList(terrain), heights);
            terrainApp.finishedMinimalLoading(this);
        });
    }

    private void updateTerrainCollision(PhysicsSpace space) {
        if (inPhysics) {
            space.remove(terrain);
            terrain.removeControl(RigidBodyControl.class);
        }

        inPhysics = true;
        terrain.addControl(new RigidBodyControl(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()), 0));
        space.add(terrain);
    }

    private Callable<RoadPointList> searchFrom(Vector2f start, Vector2f end) {
        RoadPointList outRoad = new RoadPointList();
        return () -> {
            List<Vector2f> list = null;
            try {
                AStar<Vector2f> search = new AStar<>(new SearchWorld(terrain, Terrain.HEIGHT_WEIGHT, 0.2f));
                search.setTimeoutMills((long) (30 * 1E6)); // x sec
                list = search.findPath(H.v3tov2fXZ(localGridToTerrainSpace(this, start)),
                        H.v3tov2fXZ(localGridToTerrainSpace(this, end)));
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
            
            return outRoad;
        };
    }

    private CatmullRomWidth drawRoad(AssetManager am, List<Vector3f> list, PhysicsSpace space) {
        // TODO use a rolling average to smooth points so there are less hard corners

        // draw a spline road to show where it is
        Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth
        CatmullRomRoad road = new CatmullRomRoad(s3, 1, Terrain.ROAD_WIDTH);

        CatmullRomWidth c3 = road.middle;
        Geometry g = new Geometry("spline road", c3);
        rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Green));
        
        // add to physics space
        CollisionShape col = CollisionShapeFactory.createMeshShape(g);
        RigidBodyControl c = new RigidBodyControl(col, 0);
        space.add(c);

        //add the extra things
        road.addWidth(CatmullRomRoad.SideA(Terrain.ROAD_WIDTH));
        road.addWidth(CatmullRomRoad.SideB(Terrain.ROAD_WIDTH));
        for (CatmullRomWidth r : road.others) {
            g = new Geometry("spline road", r);
            g.setLocalTranslation(g.getLocalTranslation().add(0, -0.0001f, 0));
            rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Brown));

            col = CollisionShapeFactory.createMeshShape(g);
            c = new RigidBodyControl(col, 0);
            space.add(c);
        }

        return c3;
    }

    public boolean meshOnRoad(Vector2f location) {
        return meshOnRoad(location, 0);
    }
    public boolean meshOnRoad(Vector2f location, float objRadius) {
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

    protected void cleanup(Application app, PhysicsSpace space) {
        this.rootNode.removeFromParent();

        if (this.placedObjects != null) {
            ObjectPlacer op = app.getStateManager().getState(ObjectPlacer.class);
            op.removeBulk(this.placedObjects);
        }
    }

    private static Vector3f localGridToTerrainSpace(TerrainPiece piece, Vector2f pos) {
        Vector3f out = new Vector3f();
        piece.terrain.getLocalTransform().transformVector(H.v2tov3fXZ(pos), out);
        return out;
    }

    private static Vector3f pieceGridToWorldPos(Vector2f offset, Vector3f scale) {
        return H.v2tov3fXZ(H.v3tov2fXZ(scale).multLocal(offset));
    }
}

class RoadPointList extends LinkedList<Vector3f> {
    private static final long serialVersionUID = 1L;
    public boolean failed;
    public CatmullRomWidth road;
}
