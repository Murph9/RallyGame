package rallygame.service.ray;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.math.Vector3f;

public interface IPhysicsRaycaster {
    PhysicsSpace getPhysicsSpace();
    RaycasterResult castRay(Vector3f from, Vector3f dir, PhysicsCollisionObject ignored);
}
