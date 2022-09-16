package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import survival.ability.Ability;
import survival.ability.ExplodeAbility;
import survival.ability.StopAbility;

public class AbilityUpgrade extends Upgrade<List<Ability>> {
    private final Function<List<Upgrade<?>>, Boolean> applies;

    public AbilityUpgrade(boolean positive, String label, Consumer<List<Ability>> func, Function<List<Upgrade<?>>, Boolean> applies) {
        super(positive, label, func);

        this.applies = applies;
    }

    @Override
    public boolean applies(List<Upgrade<?>> existing) {
        return applies.apply(existing);
    }

    
    public static Function<List<Upgrade<?>>, Boolean> AddExplodeAbilityFunc = (existing) -> {
        for (var upgrade: existing) {
            if (upgrade instanceof AbilityUpgrade) {
                var explode = (AbilityUpgrade)upgrade;
                if (explode.label.contains("Explode")) // TODO better
                    return false;
            }
        }
        return true;
    };

    public static Function<List<Upgrade<?>>, Boolean> AddStopAbilityFunc = (existing) -> {
        for (var upgrade: existing) {
            if (upgrade instanceof AbilityUpgrade) {
                var explode = (AbilityUpgrade)upgrade;
                if (explode.label.contains("Stop")) // TODO better
                    return false;
            }
        }
        return true;
    };

    public static Upgrade<List<Ability>> AddExplodeAbility = new AbilityUpgrade(true, "Add Explode Ability (Key: L ALT or Left Stick)", x -> x.add(new ExplodeAbility()), AbilityUpgrade.AddExplodeAbilityFunc);
    public static Upgrade<List<Ability>> AddStopAbility = new AbilityUpgrade(true, "Add Stop Ability (Key: R ALT or R Bumper)", x -> x.add(new StopAbility()), AbilityUpgrade.AddStopAbilityFunc);
    // public static Upgrade<List<Ability>> QuickerExplodeAbility = new AbilityUpgrade(true, "Improve Explode Ability", x -> x.ExplodeAbilityTimerMax *= 0.95f);
    // public static Upgrade<List<Ability>> StrongerExplodeAbility = new Upgrade<>(true, "Increase Explode Ability Strength (5%)", x -> x.ExplodeAbilityStrength *= 1.05f);

    //TODO ability ideas:
    // freeze cubes (for some time)
    // 'blink' a direction
}
