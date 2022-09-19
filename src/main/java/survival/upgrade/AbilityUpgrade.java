package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import survival.ability.Ability;
import survival.ability.BlinkAbility;
import survival.ability.ExplodeAbility;
import survival.ability.FreezeAbility;
import survival.ability.StopAbility;

public class AbilityUpgrade extends Upgrade<List<Ability>> {
    private final Function<List<Upgrade<?>>, Boolean> applies;
    private final boolean unique;

    public AbilityUpgrade(boolean positive, boolean unique, String label, Consumer<List<Ability>> func) {
        this(positive, unique, label, func, null);
    }
    public AbilityUpgrade(boolean positive, boolean unique, String label, Consumer<List<Ability>> func, Function<List<Upgrade<?>>, Boolean> applies) {
        super(positive, label, func);

        this.applies = applies;
        this.unique = unique;
    }

    @Override
    public boolean applies(List<Upgrade<?>> existing) {
        if (unique) {
            return !existing.contains(this);
        }

        if (applies == null)
            return true;
        return applies.apply(existing);
    }

    public static Upgrade<List<Ability>> AddExplodeAbility = new AbilityUpgrade(true, true, "Add Explode Ability", x -> x.add(new ExplodeAbility()));
    public static Upgrade<List<Ability>> QuickerExplodeAbility = new AbilityUpgrade(true, false, "Quicker cooldown for Explode Ability", x -> {
        for (var u: x) {
            if (u instanceof ExplodeAbility)
                ((ExplodeAbility)u).changeTimerMax(-0.5f);
        }
    }, y -> y.contains(AddExplodeAbility));
    public static Upgrade<List<Ability>> StrongerExplodeAbility = new AbilityUpgrade(true, false, "Increase Explode Ability Strength (5%)", x -> {
        for (var u: x) {
            if (u instanceof ExplodeAbility)
                ((ExplodeAbility)u).changeStrength(1f);
        }
    }, y -> y.contains(AddExplodeAbility));

    public static Upgrade<List<Ability>> AddStopAbility = new AbilityUpgrade(true, true, "Add Stop Ability", x -> x.add(new StopAbility()));

    public static Upgrade<List<Ability>> AddFreezeAbility = new AbilityUpgrade(true, true, "Add Freeze Ability", x -> x.add(new FreezeAbility()));

    public static Upgrade<List<Ability>> AddBlinkAbility = new AbilityUpgrade(true, true, "Add Blink", x -> x.add(new BlinkAbility()));
}
