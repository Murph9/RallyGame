package survival.ability;

import java.util.Map;

import com.jme3.app.state.AppStateManager;

import rallygame.car.ray.RayCarControl;

public abstract class Ability {
    abstract boolean update(AppStateManager sm, float tpf);
    abstract float ready();
    abstract void trigger(AppStateManager sm, RayCarControl player);
    abstract Map.Entry<String, Object> GetProperties();
}

abstract class TimedAbility extends Ability {
    protected float abilityTimerMax;
    protected float abilityTimer;

    public void changeTimerMax(float diff) {
        abilityTimerMax += diff;
    }

    @Override
    public boolean update(AppStateManager sm, float tpf) {
        this.abilityTimer -= tpf;
        return false;
    }

    @Override
    public float ready() {
        return this.abilityTimer/this.abilityTimerMax;
    }
}