package service.checkpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import car.ray.RayCarControl;

public class CheckpointProgress extends CheckpointProgressBase {

    private final Vector3f[] checkpointPositions;
    private Checkpoint firstCheckpoint;

    public CheckpointProgress(Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        super(cars, player);

        this.checkpointPositions = checkpoints;
        this.checkpoints = new Checkpoint[checkpoints.length];
    }

    @Override
    protected void initialize(Application app) {

        PhysicsSpace physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        for (int i = 0; i < checkpointPositions.length; i++) {
            GhostControl ghost = new GhostControl(colShape);

            Spatial box = baseSpat.clone();
            box.setLocalTranslation(checkpointPositions[i]);
            box.addControl(ghost);
            if (attachModels)
                rootNode.attachChild(box);
            physicsSpace.add(ghost);

            this.checkpoints[i] = new Checkpoint(i, checkpointPositions[i], ghost);
        }
        this.firstCheckpoint = this.checkpoints[0];

        //set progress values
        for (RacerState racer : this.racers.values()) {
            racer.lastCheckpoint = this.firstCheckpoint;
            racer.nextCheckpoint = this.firstCheckpoint;
        }
    }

    public RayCarControl isThereAWinner(int laps, int checkpoints) {
        List<RacerState> racers = getRaceState();
        Collections.sort(racers);

        RacerState racer = racers.get(0);
        if (racer.lap >= laps && racer.lastCheckpoint != null && racer.lastCheckpoint.num >= checkpoints)
            return racer.car;

        return null;
    }

    public RacerState getPlayerRacerState() {
        return this.racers.get(player);
    }

    protected List<RacerState> getRaceState() {
        return new ArrayList<>(this.racers.values());
    }

    @Override
    public void cleanup(Application app) {
        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        for (Checkpoint checkpoint : checkpoints) {
            physicsSpace.remove(checkpoint.ghost);
        }
        physicsSpace.removeCollisionListener(checkpointCollisionListener);
    }
}
