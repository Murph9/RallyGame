package rallygame.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

public class TerrainUtil {

    public static Map<Vector2f, Float> lowerTerrainSoItsUnderQuads(TerrainQuad terrain, List<Vector3f[]> quads) {

        Map<Vector2f, Float> results = new HashMap<>();

        Vector3f scale = terrain.getWorldScale();

        for (Vector3f[] quad : quads) {
            float height = H.minIn(quad[0].y, quad[1].y, quad[2].y, quad[3].y) / scale.y;

            List<Vector2f> points = getGridPosBoundingQuad(scale, quad);
            for (Vector2f point : points) {
                if (!results.containsKey(point) || results.get(point) > height) // pick the lower one
                    results.put(point, height);
            }
        }

        terrain.setHeight(new ArrayList<>(results.keySet()), new ArrayList<>(results.values()));
        return results;
    }

    public static List<Vector2f> getGridPosBoundingQuad(Vector3f terrainScale, Vector3f[] quad) {
        List<Vector2f> results = new LinkedList<>();
        
        float[] box = getTerrainGridBoundingBox(terrainScale, quad);
        for (int i = (int) box[0]; i <= box[2]; i += terrainScale.x) {
            for (int j = (int) box[1]; j <= box[3]; j += terrainScale.z) {
                results.add(new Vector2f(i, j));
            }
        }

        return results;
    }

    public static float[] getTerrainGridBoundingBox(Vector3f terrainScale, Vector3f[] quad) {
        float[] box = Trig.boundingBoxXZ(quad);
        box[0] = FastMath.floor(box[0] / terrainScale.x) * terrainScale.x; // find the closest lower grid point
        box[1] = FastMath.floor(box[1] / terrainScale.z) * terrainScale.z;
        box[2] = FastMath.ceil(box[2] / terrainScale.x) * terrainScale.x;
        box[3] = FastMath.ceil(box[3] / terrainScale.z) * terrainScale.z;

        return box;
    }
}
