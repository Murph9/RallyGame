package rallygame.world.path;

import java.util.function.BiFunction;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import com.jme3.asset.AssetManager;
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

public class CubePlacer {
    public static Callable<List<Spatial>> generate(TerrainQuad terrain, AssetManager am, int count,
        BiFunction<Vector2f, Float, Boolean> posValid) {
        return () -> {
            var maxXZ = terrain.getLocalScale().x * terrain.getTerrainSize();

            float size = 1f;
            Box b = new Box(size, size, size);
            Spatial s = new Geometry("box", b);
            s = LoadModelWrapper.create(am, s, ColorRGBA.Blue);

            List<Spatial> list = new LinkedList<>();
            for (int i = 0; i < count; i++) {
                Spatial s1 = s.clone();
                float scale = FastMath.nextRandomFloat() * 25 + 1;
                s1.setLocalScale(scale);
                s1.setLocalRotation(new Quaternion(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(),
                        FastMath.nextRandomFloat(), FastMath.nextRandomFloat()));
                list.add(s1);
                s1.setLocalTranslation(generateValidPoint(terrain, maxXZ, scale, posValid));
            }

            return list;
        };
    }

    private static Vector3f generateValidPoint(TerrainQuad terrain, float maxXZ, float radius, BiFunction<Vector2f, Float, Boolean> posValid) {
        Vector2f location = H.randV2f(maxXZ, true);
        while (posValid.apply(location, radius)) {
            location = H.randV2f(maxXZ, true);
        }
        return new Vector3f(location.x, terrain.getHeight(location), location.y);
    }
}
