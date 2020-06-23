package rallygame.world.path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.service.search.ISearchWorld;

class SearchWorld implements ISearchWorld<Vector2f> {
    private final float heightDiffWeight;
    private final List<TerrainQuad> terrains;
    private final Vector3f scale;
    private final float maxHeightDiff;
    private final HashMap<Vector2f, Float> terrainHeightCache = new HashMap<>();

    public SearchWorld(List<TerrainQuad> terrains, float heightWeight, float maxSlope) {
        this.terrains = terrains;
        this.scale = terrains.get(0).getWorldScale().clone();
        var xzAvg = (scale.x+scale.z)/2;
        this.heightDiffWeight = scale.y / xzAvg * heightWeight;
        this.maxHeightDiff = maxSlope * xzAvg;
    }

    @Override
    public float getWeight(Vector2f v1, Vector2f v2) {
        float diffHeight = Math.abs(getHeightCache(v1) - getHeightCache(v2));
        // http://blog.runevision.com/2016/03/note-on-creating-natural-paths-in.html
        return v2.distance(v1) * (1 + diffHeight * diffHeight * heightDiffWeight);
    }

    @Override
    public float getHeuristic(Vector2f v1, Vector2f v2) {
        // for the perfect solution this must be admissable [getHeuristic() <= getWeight()]
        // however getWeight() is x^2 not x so when comparing values with + they might not work
        // if thats the case we will just get a slightly non-optimal path which doesn't really matter
        float diffHeight = Math.abs(getHeightCache(v1) - getHeightCache(v2));
        return v2.distance(v1) * (1 + diffHeight * diffHeight * heightDiffWeight);
    }

    @Override
    public Set<Vector2f> getNeighbours(Vector2f pos) {
        float prevPosHeight = getHeightCache(pos);

        List<Vector2f> results = new LinkedList<>();
        float scaleX = scale.x;
        float scaleZ = scale.z;
        for (float x = pos.x - scaleX; x < pos.x + 2 * scaleX; x += scaleX) {
            for (float z = pos.y - scaleZ; z < pos.y + 2 * scaleZ; z += scaleZ) {
                if (x == pos.x && z == pos.y)
                    continue; // ignore self

                Vector2f newPos = new Vector2f(x, z);
                float newHeight = getHeightCache(newPos);
                if (Float.isNaN(newHeight))
                    continue; //therefore not on the terrain
                if (Math.abs(prevPosHeight - newHeight) > maxHeightDiff)
                    continue; //ignore high differences in height

                results.add(newPos);
            }
        }
        
        return new HashSet<>(results);
    }

    // reduce use on the terrainquad object as its fetch by height is real slow
    private float getHeightCache(Vector2f pos) {
        if (terrainHeightCache.containsKey(pos))
            return terrainHeightCache.get(pos);
        
        for (var terrain : terrains) {
            var value = terrain.getHeight(pos);
            if (!Float.isNaN(value)) {
                terrainHeightCache.put(pos, value);
                return value;
            }
        }
        terrainHeightCache.put(pos, Float.NaN);
        return Float.NaN;
    }
}