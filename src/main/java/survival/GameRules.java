package survival;

import java.util.Map;

public class GameRules {
    public static final GameRules Generate() {
        return new GameRules();
    }

    public float CheckpointTimerLength;
    public float CheckpointDistance;
    public float PlayerMaxHealth;
    public float WaveSpeed;
    public float WaveDensity;

    private GameRules() {
        CheckpointTimerLength = 60;
        CheckpointDistance = 100;
        PlayerMaxHealth = 40;
        WaveSpeed = 3;
        WaveDensity = 1;
    }

    public Map<String, Object> GetProperties() {
        return Map.of(
            "Checkpoint Distance", this.CheckpointDistance,
            "Checkpoint Timer", this.CheckpointTimerLength,
            "Wave Speed", this.WaveSpeed,
            "Wave Density", this.WaveDensity,
            "Player Max Health", this.PlayerMaxHealth
        );
    }
}
