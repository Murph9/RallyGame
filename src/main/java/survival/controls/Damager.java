package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;

public class Damager extends PhysicsBehaviour {
    
    public final float amount;

    public Damager() {
        this(1);
    }
    public Damager(float amount) {
        this.amount = amount;
    }

    @Override
    public void accept(RigidBodyControl control, float aloat) {}
}
