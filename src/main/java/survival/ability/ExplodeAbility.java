package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;

import rallygame.car.ray.RayCarControl;
import survival.wave.WaveManager;

public class ExplodeAbility extends TimedAbility {

    private float ExplodeAbilityStrength;

    public ExplodeAbility() {
        ExplodeAbilityStrength = 35;
        AbilityTimerMax = 10;
        AbilityTimer = AbilityTimerMax;
    }

    public void changeStrength(float diff) {
        this.ExplodeAbilityStrength += diff; 
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.AbilityTimer = this.AbilityTimerMax;
        sm.getState(WaveManager.class).applyForceFrom(player.location, ExplodeAbilityStrength, 50);
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Explode Ability", new Float[] { ExplodeAbilityStrength, AbilityTimer, AbilityTimerMax, ready() });
    }
}