package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;

import rallygame.car.ray.RayCarControl;
import survival.wave.WaveManager;

public class FreezeAbility extends TimedAbility {

    private float length;

    public FreezeAbility() {
        length = 5;
        abilityTimerMax = 10;
        abilityTimer = abilityTimerMax;
    }

    public void changeLength(float diff) {
        this.length += diff; 
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.abilityTimer = this.abilityTimerMax;
        sm.getState(WaveManager.class).freezeAll(length);
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Freeze Ability", new Float[] { length, abilityTimer, abilityTimerMax, ready() });
    }
}