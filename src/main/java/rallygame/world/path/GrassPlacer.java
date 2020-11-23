package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.helper.TerrainQuadUtil;

public class GrassPlacer {

    public static GrassTerrain generate(TerrainQuad terrain, int count, Function<Vector2f, Boolean> posValid) {
        return new GrassTerrain(generatePoints(count, terrain, posValid));
    }

    private static List<Vector3f> generatePoints(int count, TerrainQuad terrain, Function<Vector2f, Boolean> posValid) {
        BoundingBox boundingBox = TerrainQuadUtil.calcWorldExtents(terrain);
        final Vector2f min = H.v3tov2fXZ(boundingBox.getMin(null));
        final Vector2f max = H.v3tov2fXZ(boundingBox.getMax(null));
        List<Vector3f> list = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            Vector2f pos = Rand.randBetween(min, max);
            if (posValid.apply(pos))
                continue; //then it lost the lottery
            float height = terrain.getHeight(pos);
            if (Float.isNaN(height))
                continue;

            list.add(new Vector3f(pos.x, height, pos.y));
        }

        return list;
    }
}
