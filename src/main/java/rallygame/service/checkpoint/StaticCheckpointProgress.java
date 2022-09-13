package rallygame.service.checkpoint;

import java.util.Collection;

import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class StaticCheckpointProgress extends CheckpointProgressBase {

    public StaticCheckpointProgress(Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        super(checkpoints, cars, player);

        if (checkpoints == null || checkpoints.length < 2)
            throw new IllegalStateException("This needs checkpoints given");
    }
}
