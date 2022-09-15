package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

public abstract class Ability {
    public static final String TYPE_EXPLODE = "explode";

    abstract String type();
    abstract boolean update(float tpf);
    abstract Map.Entry<String, Object> GetProperties();
}

class ExplodeAbility extends Ability {

    private float ExplodeAbilityTimerMax;
    private float ExplodeAbilityTimer;
    private float ExplodeAbilityStrength;

    public ExplodeAbility() {
        ExplodeAbilityStrength = 35;
        ExplodeAbilityTimerMax = 10;
        ExplodeAbilityTimer = ExplodeAbilityTimerMax;
    }

    @Override
    public String type() {
        return TYPE_EXPLODE;
    }

    @Override
    public boolean update(float tpf) {
        this.ExplodeAbilityTimer -= tpf;
        if (this.ExplodeAbilityTimer < 0) {
            this.ExplodeAbilityTimer = this.ExplodeAbilityTimerMax;
            return true;
        }

        return false;
    }

    public float getStrength() {
        return ExplodeAbilityStrength;
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("Explode Ability", new Float[]{ExplodeAbilityStrength, ExplodeAbilityTimer, ExplodeAbilityTimerMax});
    }
}
