package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class FollowControl extends RigidBodyControl {

    private final RayCarControl target;
    private final float height;
    private final float strength;
    private final float maxSpeed;

    private boolean gravitySet;

    public FollowControl(float mass, RayCarControl target, float height) {
        this(mass, target, height, 10, 30);
    }

    public FollowControl(float mass, RayCarControl target, float height, float strength) {
        this(mass, target, height, strength, 30);
    }

    public FollowControl(float mass, RayCarControl target, float height, float strength, float maxSpeed) {
        super(mass);
        this.target = target;
        this.height = height;
        this.strength = strength;
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (this.gravitySet) {
            this.setGravity(Vector3f.ZERO);
            this.gravitySet = true;
        }

        Vector3f targetDir = target.location.subtract(this.spatial.getLocalTranslation());
        this.applyImpulse(targetDir.normalize().mult(this.getMass()*this.strength*tpf), Vector3f.ZERO);

        var loc = this.getPhysicsLocation().clone();
        loc.y = this.height;
        this.setPhysicsLocation(loc);
        
        var vel = this.getLinearVelocity();
        vel.y = 0; // prevent weird gravity mess
        if (vel.length() > this.maxSpeed) {
            vel.normalizeLocal().multLocal(this.maxSpeed);
        }

        this.setLinearVelocity(vel);
    }
}