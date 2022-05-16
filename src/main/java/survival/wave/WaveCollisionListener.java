package survival.wave;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.scene.Spatial;

import rallygame.car.ray.RayCarControl;
import survival.controls.BaseControl;

public class WaveCollisionListener implements PhysicsCollisionListener {

    private final WaveManager manager;
    private final RayCarControl player;

    public WaveCollisionListener(WaveManager manager, RayCarControl player) {
        this.manager = manager;
        this.player = player;
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        var rigidA = event.getObjectA();
        var rigidB = event.getObjectB();
        if (rigidA == player.getPhysicsObject()) {
            collideWithPlayer(event, rigidB, event.getNodeB());
        } else if (rigidB == player.getPhysicsObject()) {
            collideWithPlayer(event, rigidA, event.getNodeA());
        }
    }

    private void collideWithPlayer(PhysicsCollisionEvent event, PhysicsCollisionObject other, Spatial node) {
        var control = node.getControl(BaseControl.class);
        if (control == null)
            return;
        manager.controlCollision(control);
    }
    
}
