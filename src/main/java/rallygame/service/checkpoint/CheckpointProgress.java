package rallygame.service.checkpoint;

import java.util.Collection;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;
import rallygame.world.wp.DefaultBuilder;

public class CheckpointProgress extends CheckpointProgressBase implements DefaultBuilder.IPieceChanged {

    public CheckpointProgress(DefaultBuilder world, Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        super(checkpoints, cars, player);

        world.registerListener(this);
    }

    /** Adds a checkpoint to the list */
    public void addCheckpoint(Vector3f pos) {
        if (!this.isInitialized()) {
            preInitCheckpoints.add(pos);
            return;
        }
        attachCheckpoint(pos);
    }

    public void setMinCheckpoint(Vector3f pos) {
        Checkpoint check = engine.getCheckpointFromPos(pos);
        if (check == null)
            return;

        // called by the race class so that this can remove old checkpoints
        // so that cars don't ever lose the checkpoint
        // and update anyone really behind with a better, valid one

        for (RacerState racer : this.getRaceState()) {
            if (racer.nextCheckpoint.num <= check.num) {
                engine.racerHitCheckpoint(racer, check);

                Vector3f dir = racer.nextCheckpoint.position.subtract(racer.lastCheckpoint.position).normalize();
                Quaternion q = new Quaternion();
                q.lookAt(dir, new Vector3f());
                racer.car.setPhysicsProperties(racer.lastCheckpoint.position.add(0, 1, 0), dir.mult(10), q,
                        new Vector3f());
            }
        }

        // remove all visual checkpoints
        for (Checkpoint curC: engine.getAllPreviousCheckpoints(check.num)) {
            curC.visualModel.removeFromParent();
        }
    }

    @Override
    public void pieceAdded(Vector3f pos) {
        if (!isInitialized())   {
            this.preInitCheckpoints.add(pos);
            return;
        }
        
        addCheckpoint(pos);
    }

    @Override
    public void pieceRemoved(Vector3f pos) {
        if (!isInitialized())
            throw new IllegalStateException("Think about it, how?");

        setMinCheckpoint(pos);
    }
}
