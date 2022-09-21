package survival.controls;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import com.jme3.bullet.control.RigidBodyControl;

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
            //do we want bounciness back?: this.setRestitution(1);
            this.setFriction(0);
        }
    }

    @Override
    public void update(float tpf) {
        if (!enabled) return;
        
        for (var bev: this.behaviours) {
            bev.accept(this, tpf);
        }

        super.update(tpf);
    }

    @SuppressWarnings("unchecked")
    public <T extends PhysicsBehaviour> T getBehaviour(Class<T> behaviour) {
        for (var c : behaviours.toArray()) {
            if (behaviour.isAssignableFrom(c.getClass())) {
                return (T) c;
            }
        }
        return null;
    }
}
