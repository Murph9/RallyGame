package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import survival.ability.Ability;
import survival.ability.ExplodeAbility;
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

    public static Upgrade<List<Ability>> AddExplodeAbility = new AbilityUpgrade(true, true, "Add Explode Ability (Key: L ALT or Left Stick)", x -> x.add(new ExplodeAbility()));
    public static Upgrade<List<Ability>> AddStopAbility = new AbilityUpgrade(true, true, "Add Stop Ability (Key: R ALT or R Bumper)", x -> x.add(new StopAbility()));
    // public static Upgrade<List<Ability>> QuickerExplodeAbility = new AbilityUpgrade(true, false, "Improve Explode Ability", x -> x.ExplodeAbilityTimerMax *= 0.95f);
    // public static Upgrade<List<Ability>> StrongerExplodeAbility = new Upgrade<>(true, false, "Increase Explode Ability Strength (5%)", x -> x.ExplodeAbilityStrength *= 1.05f);

    //TODO ability ideas:
    // freeze cubes (for some time)
    // 'blink' a direction
}
