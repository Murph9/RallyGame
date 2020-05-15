package rallygame.drive;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public interface ICheckpointDrive extends IDrive {

    Transform resetPosition(RayCarControl car);
    Vector3f getLastCheckpoint(RayCarControl car);
    Vector3f getNextCheckpoint(RayCarControl car);
    Vector3f[] getNextCheckpoints(RayCarControl car, int count);
}
