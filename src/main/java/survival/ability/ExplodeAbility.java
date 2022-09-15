package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

public class ExplodeAbility extends Ability {

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