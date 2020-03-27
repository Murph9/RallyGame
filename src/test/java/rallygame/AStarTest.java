package rallygame;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jme3.math.Vector2f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import rallygame.service.search.AStar;
import rallygame.service.search.ISearchWorld;
import rallygame.service.PerlinNoise;

public class AStarTest {

    @Test
    public void BasicAStar() {
        int sideLength = 9;
        PerlinNoise noise = new PerlinNoise(sideLength, 0);
        noise.load();

        float[] heightMap = noise.getHeightMap();
        TerrainQuad terrain = new TerrainQuad("path terrain", sideLength, sideLength, heightMap);
        AStar<Vector2f> star = new AStar<>(new AStarWorld(terrain));

        List<Vector2f> result = star.findPath(new Vector2f(-2, -2), new Vector2f(2, 2));

        Assertions.assertTrue(result.size() == 5);
    }

    class AStarWorld implements ISearchWorld<Vector2f> {

        private final TerrainQuad terrain;

        public AStarWorld(TerrainQuad terrain) {
            this.terrain = terrain;
        }

        @Override
        public float getWeight(Vector2f v1, Vector2f v2) {
            float diffHeight = Math.abs(terrain.getHeight(v1) - terrain.getHeight(v2));
            return v2.distance(v1) * (1 + diffHeight * diffHeight * 5);
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
                    results.add(newPos);
                }
            }
            return new HashSet<>(results);
        }

    }
}
