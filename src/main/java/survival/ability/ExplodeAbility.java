package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;

import rallygame.car.ray.RayCarControl;
import survival.wave.WaveManager;

public class ExplodeAbility extends TimedAbility {

    private float strength;

    public ExplodeAbility() {
        strength = 35;
        abilityTimerMax = 10;
        abilityTimer = abilityTimerMax;
    }

    public void changeStrength(float diff) {
        this.strength += diff; 
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.abilityTimer = this.abilityTimerMax;
        sm.getState(WaveManager.class).applyForceFrom(player.location, strength, 50);
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Explode Ability", new Float[] { strength, abilityTimer, abilityTimerMax, ready() });
    }
}