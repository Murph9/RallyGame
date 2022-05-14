package survival;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import rallygame.car.data.CarDataConst;

public enum UpgradeType {
    MuchPOWER("MUCH POWER", null, x -> x.nitro_force *= 9),
    ShorterTimer("Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength *= 0.9f),
    IncreaseCheckpointRange("Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f),

    ReducePlayerHealth("Reduce the amount of your health", x -> x.PlayerHealth *= 0.9f), //TODO we need some kind of collision detection to take health off
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

class GameRules {
    public static final GameRules Generate() {
        return new GameRules();
    }

    public float CheckpointTimerLength;
    public float CheckpointDistance;
    public float PlayerHealth;

    private GameRules() {
        CheckpointTimerLength = 60;
        CheckpointDistance = 100;
        PlayerHealth = 40;
    }

    private Map<String, Object> GetProperties() {
        return Map.of(
            "Checkpoint distance", this.CheckpointDistance,
            "CheckTimer", this.CheckpointTimerLength,
            "PlayerHealth", this.PlayerHealth
        );
    }

    @Override
    public String toString() {
        return String.join("\n", GetProperties().entrySet().stream().map(x -> x.getKey()+": " + x.getValue().toString()).collect(Collectors.toList()));
    }
}
