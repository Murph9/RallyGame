package service;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Spatial;

public class GhostObjectCollisionListener implements PhysicsCollisionListener {

    private final IGhostObjectCollisionApply applier;

    public GhostObjectCollisionListener(IGhostObjectCollisionApply applier) {
        this.applier = applier;
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        Spatial a = event.getNodeA();
        PhysicsCollisionObject ac = event.getObjectA();
        Spatial b = event.getNodeB();
        PhysicsCollisionObject bc = event.getObjectB();

        if (a.getControl(GhostControl.class) != null && b.getControl(GhostControl.class) != null) {
            return; // ignore ghost | ghost collisions
        }

        if (a.getControl(GhostControl.class) == null && b.getControl(GhostControl.class) == null) {
            return; // ignore non-ghost | non-ghost collisions
        }

        if (a.getControl(GhostControl.class) != null && isMovingBody(bc)) {
            applier.ghostCollision((GhostControl) ac, (RigidBodyControl) bc);
        }

        if (b.getControl(GhostControl.class) != null && isMovingBody(ac)) {
            applier.ghostCollision((GhostControl) bc, (RigidBodyControl) ac);
        }
    }

    private boolean isMovingBody(PhysicsCollisionObject obj) {
        if (!(obj instanceof RigidBodyControl)) {
            return false; // if its not a rigid body, no idea what to do
        }

        RigidBodyControl control = (RigidBodyControl) obj;
        if (control.isKinematic() || control.getMass() == 0)
            return false; // only concerned about 'moving' collisions

        return true;
    }
}