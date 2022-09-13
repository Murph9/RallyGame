package rallygame.service.checkpoint;

import java.util.List;

import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public interface ICheckpointProgress {
    RacerState getPlayerRacerState();
    List<RacerState> getRaceState();
    Vector3f getNextCheckpoint(RayCarControl car);
    Vector3f[] getNextCheckpoints(RayCarControl car, int count);
    Vector3f getLastCheckpoint(RayCarControl car);
}
