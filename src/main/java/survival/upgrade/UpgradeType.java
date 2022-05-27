package survival.upgrade;

import java.util.function.Consumer;

import rallygame.car.data.CarDataConst;
import survival.GameRules;

public enum UpgradeType {
    MuchPOWER("MUCH POWER", null, x -> x.nitro_force *= 9),
    ShorterTimer("Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength -= 1),
    IncreaseCheckpointRange("Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f),
    WaveSpeedInc("Increase the speed waves are spawned", x -> x.WaveSpeed -= 0.05f),

    ReducePlayerHealth("Reduce the amount of your health", x -> x.PlayerMaxHealth *= 0.9f),

    // ability to push boxes back
    // ability to remove boxes every now and again
    ;

    public final String label;
    public final Consumer<GameRules> ruleFunc;
    public final Consumer<CarDataConst> carFunc;
    
    UpgradeType(String label, Consumer<GameRules> func) {
        this.label = label;
        this.ruleFunc = func;
        this.carFunc = null;
    }
    UpgradeType(String label, Consumer<GameRules> __, Consumer<CarDataConst> func) {
        this.label = label;
        this.ruleFunc = null;
        this.carFunc = func;
    }
}
