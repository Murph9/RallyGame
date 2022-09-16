package survival.ability;

import java.util.Map;

import com.jme3.app.state.AppStateManager;

import rallygame.car.ray.RayCarControl;

public abstract class Ability {
    public static final String TYPE_EXPLODE = "explode";
    public static final String TYPE_STOP = "stop";

    abstract boolean update(float tpf);
    abstract float ready();
    abstract void trigger(AppStateManager sm, RayCarControl player);
    abstract Map.Entry<String, Object> GetProperties();
}

abstract class TimedAbility extends Ability {
    protected float AbilityTimerMax;
    protected float AbilityTimer;

    public void changeTimerMax(float diff) {
        AbilityTimerMax += diff;
    }

    @Override
    public boolean update(float tpf) {
        this.AbilityTimer -= tpf;
        return false;
    }

    @Override
    public float ready() {
        return this.AbilityTimer/this.AbilityTimerMax;
    }
}