package rallygame.service.ray;

import java.util.LinkedList;
import java.util.List;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

public class PhysicsRaycaster implements IPhysicsRaycaster {

    private final PhysicsSpace space;

    public PhysicsRaycaster(PhysicsSpace space) {
        this.space = space;
    }

    // inspiration from
    // https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/DefaultVehicleRaycaster.java
    @Override
    public RaycasterResult castRay(Vector3f from, Vector3f dir, PhysicsCollisionObject ignored) {
        List<PhysicsRayTestResult> results = new LinkedList<PhysicsRayTestResult>();
        space.rayTest(from, from.add(dir), results);

        for (PhysicsRayTestResult result : results) {
            if (!(result.getCollisionObject() instanceof PhysicsRigidBody))
                continue;
            if (result.getCollisionObject().getUserObject() == ignored.getUserObject())
                continue; // no self collision please

            RaycasterResult rd = new RaycasterResult(from.add(dir.mult(result.getHitFraction())),
                result.getHitFraction() * dir.length(),
                result.getHitNormalLocal().normalize(), // this may look wrong: TODO check if its relative to an object
                (PhysicsRigidBody) result.getCollisionObject());
            return rd;
        }

        return null;
    }

    @Override
    public PhysicsSpace getPhysicsSpace() {
        return space;
    }
}