package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class StopAbility extends TimedAbility {

    public StopAbility() {
        abilityTimerMax = 5;
        abilityTimer = abilityTimerMax;
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.abilityTimer = this.abilityTimerMax;
        
        player.setPhysicsProperties(null, new Vector3f(), null, new Vector3f());
        player.getRayCar().reset();
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Stop Ability", new Float[] { abilityTimer, abilityTimerMax, ready() });
    }
}
