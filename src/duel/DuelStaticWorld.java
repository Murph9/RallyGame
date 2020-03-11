package duel;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import effects.LoadModelWrapper;
import service.GridPositions;
import world.ICheckpointWorld;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class DuelStaticWorld extends StaticWorldBuilder implements ICheckpointWorld {

    private Vector3f[] path;
    private Vector3f[] worldStarts;
    private Quaternion worldRot;

    public DuelStaticWorld() {
        super(StaticWorld.dragstrip);
    }

    public void loadCheckpoints(AssetManager am) {
        Spatial spat = LoadModelWrapper.create(am, world.name, null);

        // attempt to read checkpoints from model
        if (spat instanceof Node) {
            List<Vector3f> _checkpoints = new LinkedList<Vector3f>();
            Spatial s = ((Node) spat).getChild(0);
            for (Spatial points : ((Node) s).getChildren()) {
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

        List<Vector3f> startPositions = new GridPositions(2, 4).generate(2, path[0],
                path[1].subtract(path[0]));

        this.worldStarts = startPositions.toArray(new Vector3f[0]);
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