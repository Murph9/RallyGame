package rallygame.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

public class TerrainUtil {

    public static Map<Vector2f, Float> setHeightsFor(TerrainQuad terrain, List<Vector3f[]> quads) {

        Map<Vector2f, Float> results = new HashMap<>();

        Vector3f scale = terrain.getWorldScale();

        for (Vector3f[] quad : quads) {
            float height = (quad[0].y + quad[1].y + quad[2].y + quad[3].y)/4;
            height /= scale.y;

            float[] box = H.boundingBoxXZ(quad);
            box[0] = (int) (box[0] / scale.x) * scale.x; // find the closest lower grid point
            box[1] = (int) (box[1] / scale.z) * scale.z;
            box[2] = (1 + (int) (box[2] / scale.x)) * scale.x;
            box[3] = (1 + (int) (box[3] / scale.z)) * scale.z;
            Log.p(quad, ",");
            Log.p(box[0], box[1], box[2], box[3]);

            for (int i = (int) box[0]; i < box[2]; i += scale.x) {
                for (int j = (int) box[1]; j < box[3]; j += scale.z) {
                    Vector2f pos = new Vector2f(i, j);
                    if (!results.containsKey(pos) || results.get(pos) > height) // pick the lower one
                        results.put(pos, height);
                }
            }
        }

        terrain.setHeight(new ArrayList<Vector2f>(results.keySet()), new ArrayList<Float>(results.values()));
        return results;
    }
}
