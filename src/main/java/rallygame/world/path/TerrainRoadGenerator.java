package rallygame.world.path;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.service.search.AStar;

public class TerrainRoadGenerator {
    private final Terrain terrainApp;
    private final float heightWeight;
    public TerrainRoadGenerator(Terrain terrainApp, float heightWeight) {
        this.terrainApp = terrainApp;
        this.heightWeight = heightWeight;
    }

    /**Generates a list of roads, please call on another thread. */
    public List<RoadPointList> generateFrom(List<TerrainQuad> terrains) {
        var roads = new LinkedList<RoadPointList>();

        for (var terrain : terrains)
            roads.addAll(generateForTerrainPiece(terrain));

        // TODO
        // pick points along the terrain
        // start in first piece, go until last piece
        // you'll need to calc the edges otherwise the search wont work

        // TODO something fancy like not assuming that we are driving on every quad

        return roads;
    }

    private List<RoadPointList> generateForTerrainPiece(TerrainQuad terrain) {
        // start points are from one edge to the other
        final int halfLength = terrainApp.sideLength / 2;
        var roadStart = new Vector2f(-halfLength, terrainApp.rand.nextInt(halfLength) - halfLength / 2);
        var roadEnd = new Vector2f(halfLength - 1, terrainApp.rand.nextInt(halfLength) - halfLength / 2);

        // generate both roads
        var roadPart1 = searchFrom(terrain, roadStart, new Vector2f());
        var roadPart2 = searchFrom(terrain, new Vector2f(), roadEnd);

        return Arrays.asList(roadPart1, roadPart2);
    }
    
    private RoadPointList searchFrom(TerrainQuad terrain, Vector2f localStart, Vector2f localEnd) {
        var outRoad = new RoadPointList();
        List<Vector2f> list = null;
        try {
            //TODO should SearchWorld be using local or world coords
            // i.e. should it care about the scale of the terrain? (it has to care about the relative height vs width though)
            AStar<Vector2f> search = new AStar<>(new SearchWorld(terrain, this.heightWeight, 0.2f));
            search.setTimeoutMills((long) (30 * 1E6)); // x sec
            list = search.findPath(H.v3tov2fXZ(localGridToTerrainSpace(terrain, localStart)),
                    H.v3tov2fXZ(localGridToTerrainSpace(terrain, localEnd)));
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
    }

    private Vector3f localGridToTerrainSpace(TerrainQuad quad, Vector2f pos) {
        Vector3f out = new Vector3f();
        quad.getLocalTransform().transformVector(H.v2tov3fXZ(pos), out);
        return out;
    }

    //TODO use
    public Vector2f[] calcExtents() {
        // start points are from one edge to the other
        final int halfLength = terrainApp.sideLength / 2;
        return new Vector2f[] {
            new Vector2f(-halfLength, terrainApp.rand.nextInt(halfLength) - halfLength / 2),
            new Vector2f(halfLength - 1, terrainApp.rand.nextInt(halfLength) - halfLength / 2)
        };
    }
}
