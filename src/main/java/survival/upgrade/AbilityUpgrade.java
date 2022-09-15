package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import survival.ability.Ability;
import survival.ability.ExplodeAbility;

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
                if (explode.label.contains("Explode"))
                    return false;
            }
        }
        return true;
    };

    public static Upgrade<List<Ability>> AddExplodeAbility = new AbilityUpgrade(true, "Add Explode Ability", x -> x.add(new ExplodeAbility()), AbilityUpgrade.AddExplodeAbilityFunc);

    // public static Upgrade<List<Ability>> QuickerExplodeAbility = new AbilityUpgrade(true, "Add Explode Ability", x -> x.ExplodeAbilityTimerMax *= 0.95f);
    // public static Upgrade<List<Ability>> StrongerExplodeAbility = new Upgrade<>(true, "Increase Explode Ability Strength (5%)", x -> x.ExplodeAbilityStrength *= 1.05f);

    //TODO ability ideas:
    // freeze
    // stop
    // 'blink' a direction
}
