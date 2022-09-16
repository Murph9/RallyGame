package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class StopAbility extends TimedAbility {

    public StopAbility() {
        AbilityTimerMax = 5;
        AbilityTimer = AbilityTimerMax;
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.AbilityTimer = this.AbilityTimerMax;
        
        player.setPhysicsProperties(null, new Vector3f(), null, new Vector3f());
        player.getRayCar().reset();
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Stop Ability", new Float[] { AbilityTimer, AbilityTimerMax, ready() });
    }
}
