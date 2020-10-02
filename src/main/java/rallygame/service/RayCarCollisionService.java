package rallygame.service;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.scene.Spatial;

import rallygame.car.CarBuilder;
import rallygame.car.ray.RayCarControl;
import rallygame.helper.Geo;
import rallygame.helper.Log;

public class RayCarCollisionService implements PhysicsCollisionListener {

    private final CarBuilder builder;
    private final IRayCarCollisionListener listener;

    public RayCarCollisionService(IRayCarCollisionListener listener, CarBuilder builder) {
        this.builder = builder;
        this.listener = listener;
    }
    
    private RayCarControl getCarFrom(Spatial node) {
        for (RayCarControl car : this.builder.getAll()) {
            if (Geo.hasParentNode(node, car.getRootNode())) {
                return car;
            }
        }
        return null;
    }

    // detect if collisions are from the player
    @Override
    public void collision(PhysicsCollisionEvent event) {
        RayCarControl carA = getCarFrom(event.getNodeA());
        RayCarControl carB = getCarFrom(event.getNodeB());

        if (carA == null || carB == null)
            return; // not 2 car collisions
        if (carA.getAI() != null && carB.getAI() != null)
            return; // both are ai
        if (carA.getAI() == null && carB.getAI() == null) {
            Log.p("Collision between players: " + carA + " " + carB);
            return; // both are a player !!!
        }

        if (carA.getAI() == null)
            listener.playerCollision(carA, carB, event.getNormalWorldOnB().negate(), event.getLocalPointB(),
                    event.getAppliedImpulse());
        else
            listener.playerCollision(carB, carA, event.getNormalWorldOnB(), event.getLocalPointA(), event.getAppliedImpulse());
    }
}
