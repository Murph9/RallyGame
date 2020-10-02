package rallygame.service;

import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public interface IRayCarCollisionListener {
    void playerCollision(RayCarControl player, RayCarControl them, Vector3f normalInWorld,
            Vector3f themLocalPos, float appliedImpulse);
}