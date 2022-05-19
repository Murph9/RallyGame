package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;

public abstract class PhysicsBehaviour {
    public abstract void accept(RigidBodyControl control, float tpf);
}
