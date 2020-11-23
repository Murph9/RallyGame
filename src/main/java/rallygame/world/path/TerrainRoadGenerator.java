package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.Rand;
import rallygame.helper.TerrainQuadUtil;
import rallygame.service.search.AStar;

public class TerrainRoadGenerator {
    private final float heightWeight;
    private final long timeout;
    private final Consumer<String> updater;

    public TerrainRoadGenerator(float heightWeight, long timeout, Consumer<String> updater) {
        this.heightWeight = heightWeight;
        this.timeout = timeout;
        this.updater = updater;
    }

    /**Generates a list of roads, please call on a background thread. */
    public List<RoadPointList> generateFrom(List<TerrainQuad> terrains) {
        List<RoadPointList> roads = new LinkedList<>();

        if (terrains.isEmpty())
            return roads;

        // attempt2
        final BoundingBox boundingBox = TerrainQuadUtil.calcWorldExtents(terrains);
        final Vector2f min = H.v3tov2fXZ(boundingBox.getMin(null));
        final Vector2f max = H.v3tov2fXZ(boundingBox.getMax(null));
        
        final float distance = max.distance(min);
        
        Vector2f start = Rand.randBetween(min, max);
        Vector2f end = Rand.randBetween(min, max);
        while (start.distance(end) < distance/2) {
            // make sure the road distance is using 'some' of the map by checking its close-ish to distance
            start = Rand.randBetween(min, max);
            end = Rand.randBetween(min, max);
        }

        //normalise the start and end to grid positions, otherwise search will fail
        start = TerrainQuadUtil.getClosestGridPoint(terrains.get(0), start);
        end = TerrainQuadUtil.getClosestGridPoint(terrains.get(0), end);
        Vector2f center = TerrainQuadUtil.getClosestGridPoint(terrains.get(0), H.v3tov2fXZ(boundingBox.getCenter()));

        // generate road from start to end through the center
        roads.add(getFrom(terrains, start, center));
        roads.add(getFrom(terrains, center, end));

        // TODO something fancy like not assuming that we are driving on every quad

        return roads;
    }

    private RoadPointList getFrom(List<TerrainQuad> terrains, Vector2f worldStart, Vector2f worldEnd) {
        RoadPointList outRoad = new RoadPointList();
        List<Vector2f> list = null;
        try {
            // i.e. should it care about the scale of the terrain? (it has to care about the relative height vs width though)
            AStar<Vector2f> search = new AStar<>(new SearchWorld(terrains, this.heightWeight, 0.2f));
            search.setTimeoutMills(timeout);
            search.setProgressCallback((s, curList) -> this.updater.accept(s));
            list = search.findPath(worldStart, worldEnd);
            outRoad.addAll(list.stream().map(x -> new Vector3f(x.x, TerrainQuadUtil.getHeight(terrains, x), x.y))
                    .collect(Collectors.toList()));
            Log.p("Done astar search, path length = ", list.size());
        } catch (TimeoutException e) {
            outRoad.addAll(new LinkedList<>());
            Log.e(e);
            outRoad.failed = true;
            outRoad.failedMessage = "Astar search failed due to timeout";
        } catch (Exception e) {
            Log.e(e);
        }

        return outRoad;
    }
}
