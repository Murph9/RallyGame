package rallygame.service;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Spatial;

public class GhostObjectCollisionListener implements PhysicsCollisionListener {

    public interface IListener {
        void ghostCollision(GhostControl control, RigidBodyControl control2);
    }

    private final IListener listener;

    public GhostObjectCollisionListener(IListener listener) {
        this.listener = listener;
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        Spatial a = event.getNodeA();
        Spatial b = event.getNodeB();
        if (a == null || b == null)
            return; //something didn't add it to the physics space correctly, so ignore it it will come up somewhere else

        var aGhost = a.getControl(GhostControl.class);
        var bGhost = b.getControl(GhostControl.class);
        if (aGhost != null && bGhost != null) {
            return; // ignore ghost | ghost collisions
        }
        if (aGhost == null && bGhost == null) {
            return; // ignore non-ghost | non-ghost collisions
        }

        PhysicsCollisionObject ac = event.getObjectA();
        PhysicsCollisionObject bc = event.getObjectB();

        if (isMovingBody(bc) && aGhost != null) {
            listener.ghostCollision((GhostControl) ac, (RigidBodyControl) bc);
        }

        if (isMovingBody(ac) && bGhost != null) {
            listener.ghostCollision((GhostControl) bc, (RigidBodyControl) ac);
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