package rallygame.world.path;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.service.search.ISearchWorld;

class SearchWorld implements ISearchWorld<Vector2f> {
    private final float heightWeight;
    private final TerrainQuad terrain;
    private final Vector3f scale;

    public SearchWorld(TerrainQuad terrain, float heightWeight) {
        this.terrain = terrain;
        this.heightWeight = heightWeight;
        this.scale = terrain.getWorldScale().clone();
    }

    @Override
    public float getWeight(Vector2f v1, Vector2f v2) {
        float diffHeight = Math.abs(terrain.getHeight(v1) - terrain.getHeight(v2));
        // http://blog.runevision.com/2016/03/note-on-creating-natural-paths-in.html
        return v2.distance(v1) * (1 + diffHeight * diffHeight * scale.y / scale.x * heightWeight);
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