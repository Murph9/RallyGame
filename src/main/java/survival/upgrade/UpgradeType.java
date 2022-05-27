package survival.upgrade;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import rallygame.car.data.CarDataConst;
import survival.GameState;

public enum UpgradeType {
    ShorterTimer(false, "Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength -= 3),
    IncreaseCheckpointRange(false, "Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f),
    WaveSpeedInc(false, "Increase the speed waves are spawned", x -> x.WaveSpeed -= 0.1f),
    ReducePlayerHealth(false, "Reduce the amount of your health", x -> x.PlayerMaxHealth -= 2),

    MuchPOWER(true, "MUCH POWER (5% nitro increase) [ctrl]", null, x -> x.nitro_force *= 1.05f),
    ImproveGrip(true, "Improve Grip (3%)", null, (data) -> {
        for (int i = 0; i < 4; i++) {
            data.wheelData[i].pjk_lat.D1 *= 1.03f;
            data.wheelData[i].pjk_long.D1 *= 1.03f;
        }
    }),
    ImproveEngine(true, "Improve Engine (4%)", null, (data) -> {
        for (int i = 0; i < data.e_torque.length; i++) {
            data.e_torque[i] *= 1.04f;
        }
    })
    
    // ability to push boxes back
    // ability to remove boxes every now and again
    ;

    public final boolean positive;
    public final String label;
    public final Consumer<GameState> stateFunc;
    public final Consumer<CarDataConst> carFunc;
    
    UpgradeType(boolean positive, String label, Consumer<GameState> func) {
        this(positive, label, func, null);
    }
    UpgradeType(boolean positive, String label, Consumer<GameState> stateFunc, Consumer<CarDataConst> carFunc) {
        this.positive = positive;
        this.label = label;
        this.stateFunc = stateFunc;
        this.carFunc = carFunc;
    }

    public static List<UpgradeType> getAllPositive() {
        var result = new LinkedList<UpgradeType>();
        for (var type: UpgradeType.values()) {
            if (type.positive)
                result.add(type);
        }

        return result;
    }

    public static List<UpgradeType> getAllNegative() {
        var result = new LinkedList<UpgradeType>();
        for (var type: UpgradeType.values()) {
            if (!type.positive)
                result.add(type);
        }

        return result;
    }
}
