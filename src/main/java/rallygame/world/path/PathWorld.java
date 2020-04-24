package rallygame.world.path;

import com.jme3.app.Application;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.service.GridPositions;
import rallygame.world.ICheckpointWorld;
import rallygame.world.World;
import rallygame.world.WorldType;

public class PathWorld extends World implements ICheckpointWorld {

    private final Terrain terrain;

    // https://github.com/jMonkeyEngine/sdk/blob/master/jme3-terrain-editor/src/com/jme3/gde/terraineditor/tools/LevelTerrainToolAction.java

    public PathWorld() {
        this(FastMath.nextRandomInt());
    }
    public PathWorld(int seed) {
        this(seed, 6, 25, 400);
    }
    public PathWorld(int seed, int size, float scaleXZ, float scaleY) {
        super("PathWorld");

        terrain = new Terrain(seed, size, new Vector3f(scaleXZ, scaleY, scaleXZ));
    }

    @Override
    public void reset() {
    }

    @Override
    public Vector3f getStartPos() {
        if (this.loadPercent().percent >= 1) {
            return terrain.getStart().getTranslation();
        }

        return new Vector3f();
    }

    @Override
    public Quaternion getStartRot() {
        if (this.loadPercent().percent >= 1) {
            return terrain.getStart().getRotation();
        }
        return Quaternion.IDENTITY.clone();
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
        Quaternion rot = this.getStartRot();
        Vector3f startPos = this.getStartPos();

        if (this.loadPercent().percent >= 1) {
            GridPositions grid = new GridPositions(2, 7);
            Vector3f pos = grid.generate(startPos, rot.mult(Vector3f.UNIT_Z))
                .skip(i)
                .findFirst().get();
            return new Transform(pos, rot);
        }

        return new Transform(startPos, rot);
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
