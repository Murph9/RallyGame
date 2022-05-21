package survival.controls;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

public class HoverAt extends PhysicsBehaviour {
    
    private final float height;
    private boolean gravitySet;

    public HoverAt(float height) {
        this.height = height;
    }

    @Override
    public void accept(RigidBodyControl control, float tpf) {
        
        if (this.gravitySet) {
            control.setGravity(Vector3f.ZERO);
            this.gravitySet = true;
        }

        var loc = control.getPhysicsLocation().clone();
        if (loc.y < this.height) {
            loc.y = this.height;
            control.setPhysicsLocation(loc);
        }

        var vel = control.getLinearVelocity();
        vel.y = 0; // prevent weird gravity mess
        control.setLinearVelocity(vel);
    }
}
