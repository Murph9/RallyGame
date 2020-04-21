package rallygame.duel;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;
import rallygame.service.GridPositions;
import rallygame.world.ICheckpointWorld;
import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;

public class DuelStaticWorld extends StaticWorldBuilder implements ICheckpointWorld {

    private Vector3f[] path;
    private Vector3f[] worldStarts;
    private Quaternion worldRot;

    public DuelStaticWorld() {
        super(StaticWorld.dragstrip);
    }

    public void loadCheckpoints(AssetManager am) {
        Spatial spat = LoadModelWrapper.create(am, world.name);

        // attempt to read checkpoints from model
        if (spat instanceof Node) {
            List<Vector3f> _checkpoints = new LinkedList<Vector3f>();
            for (Spatial points : ((Node) spat).getChildren()) {
                if (points.getName().equals("Points")) {
                    for (Spatial checkpoint : ((Node) points).getChildren()) {
                        _checkpoints.add(checkpoint.getLocalTranslation());
                    }
                }
            }
            this.path = new Vector3f[_checkpoints.size()];
            if (!_checkpoints.isEmpty()) {
                _checkpoints.toArray(this.path);
            }
        }

        // generate starting positions and rotations
        this.worldRot = new Quaternion();
        this.worldRot.lookAt(path[1].subtract(path[0]), Vector3f.UNIT_Y);

        this.worldStarts = new GridPositions(2, 5)
                .generate(path[0], path[1].subtract(path[0]))
                .limit(2).toArray(i -> new Vector3f[i]);
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);
    }

    @Override
    public Transform start(int i) {
        return new Transform(worldStarts[i], worldRot);
    }

    @Override
    public Vector3f[] checkpoints() {
        return this.path;
    }
}
