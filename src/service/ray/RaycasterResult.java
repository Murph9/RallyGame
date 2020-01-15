package service.ray;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

public class RaycasterResult {

    public final Vector3f pos;
    public final float dist; // 0 is at ray start
    public final Vector3f hitNormalInWorld;
    public final PhysicsRigidBody obj;

    public RaycasterResult(Vector3f pos, float dist, Vector3f hitNormalInWorld, PhysicsRigidBody obj) {
        this.pos = pos;
        this.dist = dist;
        this.hitNormalInWorld = hitNormalInWorld;
        this.obj = obj;
    }
}