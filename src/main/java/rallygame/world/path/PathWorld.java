package rallygame.world.path;

import com.jme3.app.Application;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.service.GridPositions;
import rallygame.world.ICheckpointWorld;
import rallygame.world.World;
import rallygame.world.WorldType;

public class PathWorld extends World implements ICheckpointWorld {

    private final Terrain terrain;

    public PathWorld() {
        this(FastMath.nextRandomInt());
    }
    public PathWorld(int seed) {
        this(seed, true);
    }
    public PathWorld(int seed, boolean terrainFeatures) {
        this(seed, 5, 25, 200, terrainFeatures);
    }
    public PathWorld(int seed, int size, float scaleXZ, float scaleY, boolean terrainFeatures) {
        super("PathWorld");
        var ft = TerrainSettings.withFeatures(terrainFeatures);
        terrain = new Terrain(ft);
    }

    @Override
    public void reset() {
    }

    @Override
    public Transform getStart() {
        if (this.loadPercent().percent >= 1) {
            return new Transform(terrain.getStart().getTranslation(), terrain.getStart().getRotation());
        }
        return new Transform();
    }

    @Override
    public WorldType getType() {
        return WorldType.PATH;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        getStateManager().attach(terrain);
    }


    @Override
    public void update(float tpf) {
        
    }

    @Override
    protected void cleanup(Application app) {
        super.cleanup(app);

        getStateManager().detach(terrain);
    }

    @Override
    public LoadResult loadPercent() {
        return terrain.loadPercent();
    }

    @Override
    public Transform start(int i) {
        Transform start = this.getStart();

        if (this.loadPercent().percent >= 1) {
            GridPositions grid = new GridPositions(2, 7);
            Vector3f pos = grid.generate(start.getTranslation(), start.getRotation().mult(Vector3f.UNIT_Z))
                .skip(i)
                .findFirst().get();
            return new Transform(pos, start.getRotation());
        }

        return start;
    }

    @Override
    public Vector3f[] checkpoints() {
        if (this.loadPercent().percent < 1) {
            return null;
        }

        //TODO proabably allow the dynamic checkpoint stuff to work from the dynamic race class
        //but for now its just a simple waypoint list

        return terrain.getRoadPoints().toArray(new Vector3f[0]);
    }
}
