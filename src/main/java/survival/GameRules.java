package survival;

import java.util.Map;
import java.util.stream.Collectors;

public class GameRules {
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