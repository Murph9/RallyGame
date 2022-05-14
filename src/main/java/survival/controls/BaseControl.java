package survival.controls;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class BaseControl extends RigidBodyControl {
    
    private final List<PhysicsBehaviour> behaviours = new LinkedList<>();

    public BaseControl(float mass, PhysicsBehaviour... behaviours) {
        super(mass);
        this.behaviours.addAll(Lists.newArrayList(behaviours));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            //this.setRestitution(1);
            this.setFriction(0);
        }
    }

    @Override
    public void update(float tpf) {
        for (var bev: this.behaviours) {
            bev.accept(this, tpf);
        }

        super.update(tpf);
    }

    public static PhysicsBehaviour HoverAt(float height) {
        return new HoverAtBehaviour(height);
    }

    public static PhysicsBehaviour Target(RayCarControl target, float strength) {
        return (c, tpf) -> {
            Vector3f targetDir = target.location.subtract(c.getSpatial().getLocalTranslation());
            c.applyImpulse(targetDir.normalize().mult(c.getMass()*strength*tpf), Vector3f.ZERO);
        };
    }

    public static PhysicsBehaviour MaxSpeed(float speed) {
        return (c, tpf) -> {
            var vel = c.getLinearVelocity();
            if (vel.length() > speed) {
                vel.normalizeLocal().multLocal(speed);
            }
    
            c.setLinearVelocity(vel);
        };
    }
    public static PhysicsBehaviour Move(Vector3f dir, float strength) {
        return (c, tpf) -> {
            c.applyImpulse(dir.normalize().mult(c.getMass()*strength*tpf), Vector3f.ZERO);
        };
    }
}

class HoverAtBehaviour implements PhysicsBehaviour {
    
    private final float height;
    private boolean gravitySet;

    public HoverAtBehaviour(float height) {
        this.height = height;
    }

    @Override
    public void accept(RigidBodyControl control, Float tpf) {
        
        if (this.gravitySet) {
            control.setGravity(Vector3f.ZERO);
            this.gravitySet = true;
        }

        var loc = control.getPhysicsLocation().clone();
        if (loc.y < this.height) {
            loc.y = this.height;
        }
        control.setPhysicsLocation(loc);

        var vel = control.getLinearVelocity();
        vel.y = 0; // prevent weird gravity mess
        control.setLinearVelocity(vel);
    }
}
