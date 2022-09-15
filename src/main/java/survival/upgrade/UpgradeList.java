package survival.upgrade;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class UpgradeList {
    public static List<Upgrade<?>> All = Arrays.asList(
        GameStateUpgrade.ShorterTimer,
        GameStateUpgrade.IncreaseCheckpointRange,
        GameStateUpgrade.WaveSpeedInc,
        GameStateUpgrade.ReducePlayerHealth,
        GameStateUpgrade.LongerTimer,

        CarDataConstUpgrade.ImproveEngine,

        AbilityUpgrade.AddExplodeAbility
    );

    public static List<Upgrade<?>> AllPositive = All.stream().filter(x -> x.positive).collect(Collectors.toList());

    public static List<Upgrade<?>> AllPositiveApplies(List<Upgrade<?>> applied) {
        var list = new LinkedList<Upgrade<?>>();
        for (var upgrade: AllPositive) {
            if (upgrade.applies(applied))
                list.add(upgrade);
        }
        return list;
    }
}
