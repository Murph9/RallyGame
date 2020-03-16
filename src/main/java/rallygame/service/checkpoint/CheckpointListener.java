package rallygame.service.checkpoint;

import java.util.function.Consumer;
import java.util.function.Function;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;

import rallygame.service.GhostObjectCollisionListener;

class CheckpointListener implements GhostObjectCollisionListener.IListener {
    
    private final Function<GhostControl, Checkpoint> ifCheckpoint;
    private final Function<RigidBodyControl, RacerState> ifCar;
    private final Consumer<RacerState> nextCheckpoint;

    private final GhostObjectCollisionListener checkpointCollisionListener;

    public CheckpointListener(Function<GhostControl, Checkpoint> ifCheckpoint,
        Function<RigidBodyControl, RacerState> ifCar,
        Consumer<RacerState> nextCheckpoint) {
        this.ifCheckpoint = ifCheckpoint;
        this.ifCar = ifCar;
        this.nextCheckpoint = nextCheckpoint;

        this.checkpointCollisionListener = new GhostObjectCollisionListener(this);
    }

    public void startListening(PhysicsSpace space) {
        space.addCollisionListener(checkpointCollisionListener);
    }
    public void stopListening(PhysicsSpace space) {
        space.removeCollisionListener(checkpointCollisionListener);
    }

    @Override
    public void ghostCollision(GhostControl ghost, RigidBodyControl obj) {
        Checkpoint checkpoint = ifCheckpoint.apply(ghost);
        RacerState racer = ifCar.apply(obj);
        if (checkpoint == null || racer == null)
            return;

        if (racer.nextCheckpoint == checkpoint) {
            nextCheckpoint.accept(racer);
        }
    }
}
