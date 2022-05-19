package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class Target extends PhysicsBehaviour {
    private final RayCarControl car;
    private final float strength;
    public Target(RayCarControl car, float strength) {
        this.car = car;
        this.strength = strength;
    }

    @Override
    public void accept(RigidBodyControl control, float tpf) {
        Vector3f targetDir = car.location.subtract(control.getSpatial().getLocalTranslation());
        control.applyImpulse(targetDir.normalize().mult(control.getMass() * strength * tpf), Vector3f.ZERO);
    }
}