package duel;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import world.IWorldTrack;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class DuelWorld extends StaticWorldBuilder implements IWorldTrack {

    private Vector3f[] path;

    public DuelWorld() {
        super(StaticWorld.lakelooproad);
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        // attempt to read checkpoints from model
        if (model instanceof Node) {
            List<Vector3f> _checkpoints = new LinkedList<Vector3f>();
            Spatial s = ((Node) model).getChild(0);
            for (Spatial points : ((Node) s).getChildren()) {
                if (points.getName().equals("Points")) {
                    for (Spatial checkpoint : ((Node) points).getChildren()) {
                        _checkpoints.add(checkpoint.getLocalTranslation());
                    }
                }
            }
            if (!_checkpoints.isEmpty()) {
                this.path = new Vector3f[_checkpoints.size()];
                _checkpoints.toArray(this.path);
            }
        }
    }

    @Override
    public Transform start(int i) {
        return new Transform(super.getStartPos(), new Quaternion().fromRotationMatrix(super.getStartRot()));
    }

    @Override
    public Vector3f[] checkpoints() {
        return this.path;
    }
}
