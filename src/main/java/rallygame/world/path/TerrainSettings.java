package rallygame.world.path;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class TerrainSettings {
    public boolean grass;
    public boolean cubes;
    public long seed;
    public int size;
    public Vector3f scale;
    public int tileCount;

    public TerrainSettings() {
        seed = FastMath.nextRandomInt();
        size = 5;
        tileCount = 2;
        cubes = true;
        grass = true;
        scale = new Vector3f(30, 400, 30);
    }

    public static TerrainSettings withFeatures(boolean value) {
        var ts = new TerrainSettings();
        ts.cubes = value;
        ts.grass = value;
        return ts;
    }
}