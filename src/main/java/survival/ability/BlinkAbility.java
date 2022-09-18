package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;

public class BlinkAbility extends TimedAbility {
    private Vector3f dir;
    private float distance;

    public BlinkAbility() {
        abilityTimerMax = 1;
        abilityTimer = abilityTimerMax;
        distance = 25;
    }

    public void setDirection(Vector3f dir) {
        this.dir = dir.normalize();
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.abilityTimer = this.abilityTimerMax;
        var newPos = player.location.add(dir.mult(distance));
        player.setPhysicsProperties(newPos, null, null, null);
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Blink Ability", new Float[] { abilityTimer, abilityTimerMax, ready(), distance });
    }
}
