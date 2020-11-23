package rallygame.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;

public class TerrainQuadUtil {

    public static Vector2f getClosestGridPoint(TerrainQuad terrain, Vector3f pos) {
        return getClosestGridPoint(terrain, H.v3tov2fXZ(pos));
    }
    public static Vector2f getClosestGridPoint(TerrainQuad terrain, Vector2f pos) {
        Vector3f terrainScale = terrain.getLocalScale();
        return new Vector2f(Math.round(pos.x / terrainScale.x) * terrainScale.x, Math.round(pos.y / terrainScale.z) * terrainScale.z);
    }

    public static void setTerrainHeights(List<TerrainQuad> terrains, Map<Vector2f, Float> heights) {
        for (TerrainQuad tq: terrains) {
            // convert to local coords (javadoc says nothing about this)
            Vector2f offset = new Vector2f(tq.getLocalTranslation().x, tq.getLocalTranslation().y);
            tq.setHeight(new ArrayList<>(heights.keySet().stream().map(x -> x.subtract(offset)).collect(Collectors.toList())),
                    new ArrayList<>(heights.values()));
        }
    }

    public static Map<Vector2f, Float> getHeightsForQuads(Vector3f scale, List<Vector3f[]> quads) {
        Map<Vector2f, Float> results = new HashMap<>();

        Vector3f intersectionPoint = new Vector3f();
        Ray r = new Ray(new Vector3f(), Vector3f.UNIT_Y); //vertical ray

        for (Vector3f[] quad : quads) {
            // get plane of the quad
            Plane plane = new Plane();
            plane.setPlanePoints(quad[0], quad[1], quad[2]);

            List<Vector2f> points = getGridPosBoundingQuad(scale, quad);
            for (Vector2f point : points) {
                // get height at point on the plane, by intersecting it with a vertical ray
                Vector3f p3 = H.v2tov3fXZ(point);
                p3.y = quad[0].y;
                r.setOrigin(p3.subtract(Vector3f.UNIT_Y.mult(100)));
                if (!r.intersectsWherePlane(plane, intersectionPoint))
                    continue;

                float height = intersectionPoint.y / scale.y;
                if (Float.isNaN(height))
                    continue;
                if (!results.containsKey(point) || results.get(point) > height) // pick the lower one
                    results.put(point, height);
            }
        }

        return results;
    }

    public static Map<Vector2f, Float> getHeightsForQuadsMin(Vector3f terrainScale, List<Vector3f[]> quads) {
        Map<Vector2f, Float> results = new HashMap<>();
        for (Vector3f[] quad : quads) {
            float height = H.minIn(quad[0].y, quad[1].y, quad[2].y, quad[3].y) / terrainScale.y;

            List<Vector2f> points = getGridPosBoundingQuad(terrainScale, quad);
            for (Vector2f point : points) {
                if (!results.containsKey(point) || results.get(point) > height) // pick the lower one
                    results.put(point, height);
            }
        }

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

    public static BoundingBox calcWorldExtents(List<TerrainQuad> quads) {
        BoundingBox bounding = new BoundingBox();
        for (TerrainQuad quad: quads) {
            BoundingVolume b = quad.getWorldBound();
            b.setCenter(quad.getLocalTranslation());
            bounding.mergeLocal(b);
        }
        return bounding;
    }

    public static BoundingBox calcWorldExtents(TerrainQuad quad) {
        BoundingBox bounding = (BoundingBox)quad.getWorldBound();
        bounding.setCenter(quad.getLocalTranslation());
        return bounding;
    }

    public static float getHeight(List<TerrainQuad> terrains, Vector2f pos) {
        for (TerrainQuad terrain : terrains) {
            float value = terrain.getHeight(pos);
            if (!Float.isNaN(value))
                return value;
        }
        return Float.NaN;
    }

    //debug method to draw boxes at terrain heights, can be used with the output of the main method at the top
    //i acknowledge that isn't where this should live 
    public static void debugDrawHeights(TerrainQuad terrain, AssetManager am, Node node, Map<Vector2f, Float> vecHeightMap) {
        List<Vector3f> heights3 = vecHeightMap.entrySet().stream()
                .map(x -> new Vector3f(x.getKey().x, x.getValue() * terrain.getWorldScale().y, x.getKey().y))
                .collect(Collectors.toList());
        
        float size = 0.2f;
        for (Vector3f pos : heights3) {
            node.attachChild(Geo.makeShapeBox(am, ColorRGBA.Yellow, pos.add(0, size / 2, 0), size / 2));
        }
    }
}
