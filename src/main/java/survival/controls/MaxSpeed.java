package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;

public class MaxSpeed extends PhysicsBehaviour {

    private final float speed;
    public MaxSpeed(float speed) {
        this.speed = speed;
    }
    @Override
    public void accept(RigidBodyControl control, float aloat) {
        var vel = control.getLinearVelocity();
        if (vel.length() > speed) {
            vel.normalizeLocal().multLocal(speed);
        }

        control.setLinearVelocity(vel);
    }
}
