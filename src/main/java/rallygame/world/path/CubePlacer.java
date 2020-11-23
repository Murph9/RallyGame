package rallygame.world.path;

import java.util.function.BiFunction;
import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.helper.TerrainQuadUtil;

public class CubePlacer {
    
    public static List<Spatial> generate(TerrainQuad terrain, AssetManager am, int count,
        BiFunction<Vector2f, Float, Boolean> posValid, ColorRGBA colour) {
        BoundingBox boundingBox = TerrainQuadUtil.calcWorldExtents(terrain);
        final Vector2f min = H.v3tov2fXZ(boundingBox.getMin(null));
        final Vector2f max = H.v3tov2fXZ(boundingBox.getMax(null));

        float size = 1f;
        Box b = new Box(size, size, size);
        Spatial s = new Geometry("box", b);
        s = LoadModelWrapper.createWithColour(am, s, colour);

        List<Spatial> list = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            Spatial s1 = s.clone();
            float scale = FastMath.nextRandomFloat() * 25 + 1;
            s1.setLocalScale(scale);
            s1.setLocalRotation(new Quaternion(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(),
                    FastMath.nextRandomFloat(), FastMath.nextRandomFloat()));
            list.add(s1);
            s1.setLocalTranslation(generateValidPoint(terrain, min, max, scale, posValid));
        }

        return list;
    }

    private static Vector3f generateValidPoint(TerrainQuad terrain, Vector2f min, Vector2f max, float radius, BiFunction<Vector2f, Float, Boolean> posValid) {
        Vector2f location = Rand.randBetween(min, max);
        while (posValid.apply(location, radius)) {
            location = Rand.randBetween(min, max);
        }
        return new Vector3f(location.x, terrain.getHeight(location), location.y);
    }
}
