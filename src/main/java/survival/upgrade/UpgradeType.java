package survival.upgrade;

import java.util.function.Consumer;

import rallygame.car.data.CarDataConst;
import survival.GameState;

public enum UpgradeType {
    ShorterTimer(false, "Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength -= 1),
    IncreaseCheckpointRange(false, "Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f),
    WaveSpeedInc(false, "Increase the speed waves are spawned", x -> x.WaveSpeed -= 0.05f),
    ReducePlayerHealth(false, "Reduce the amount of your health", x -> x.PlayerMaxHealth *= 0.9f),

    MuchPOWER(true, "MUCH POWER", null, x -> x.nitro_force *= 9),
    
    // ability to push boxes back
    // ability to remove boxes every now and again
    ;

    public final boolean positive;
    public final String label;
    public final Consumer<GameState> ruleFunc;
    public final Consumer<CarDataConst> carFunc;
    
    UpgradeType(boolean positive, String label, Consumer<GameState> func) {
        this(positive, label, func, null);
    }
    UpgradeType(boolean positive, String label, Consumer<GameState> __, Consumer<CarDataConst> func) {
        this.positive = positive;
        this.label = label;
        this.ruleFunc = null;
        this.carFunc = func;
    }
}
