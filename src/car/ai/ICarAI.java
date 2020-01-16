package car.ai;

import game.DebugAppState;
import service.ray.IPhysicsRaycaster;

public interface ICarAI {
    void setDebugAppState(DebugAppState debug);
    void setPhysicsRaycaster(IPhysicsRaycaster raycaster);

    void update(float tpf);
}