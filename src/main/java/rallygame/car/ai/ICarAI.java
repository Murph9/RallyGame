package rallygame.car.ai;

import rallygame.game.DebugAppState;
import rallygame.service.ray.IPhysicsRaycaster;

public interface ICarAI {
    void setDebugAppState(DebugAppState debug);
    void setPhysicsRaycaster(IPhysicsRaycaster raycaster);

    void update(float tpf);
}