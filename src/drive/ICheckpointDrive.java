package drive;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;

public interface ICheckpointDrive {

    Transform resetPosition(RayCarControl car);
    Vector3f getNextCheckpoint(RayCarControl car);
}