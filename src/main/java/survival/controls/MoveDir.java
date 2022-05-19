package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

public class MoveDir extends PhysicsBehaviour {
    private final Vector3f dir;
    private final float strength;

    public MoveDir(Vector3f dir, float strength) {
        this.dir = dir.normalize();
        this.strength = strength;
    }
    @Override
    public void accept(RigidBodyControl control, float tpf) {
        control.applyImpulse(dir.mult(control.getMass()*this.strength*tpf), Vector3f.ZERO);
    }
}
