package survival;

import java.util.Map;

public class GameState {
    
    public static final GameState generate() {
        var state = new GameState();
        state.CheckpointTimerLength = 60;
        state.CheckpointDistance = 100;
        state.PlayerMaxHealth = 40;
        state.WaveSpeed = 3;
        state.WaveDensity = 1;

        state.PlayerHealth = state.PlayerMaxHealth;
        state.CheckpointTimer = state.CheckpointTimerLength;
        return state;
    }

    public float CheckpointTimerLength;
    public float CheckpointDistance;
    public float PlayerMaxHealth;
    public float WaveSpeed;
    public float WaveDensity;

    public float PlayerHealth;
    public float CheckpointTimer;

    private GameState() {
    }

    public void update(float tpf) {
        if (this.PlayerHealth > PlayerMaxHealth) {
            this.PlayerHealth = PlayerMaxHealth;
        }

        this.CheckpointTimer -= tpf;
        if (this.CheckpointTimer > CheckpointTimerLength) {
            this.CheckpointTimer = CheckpointTimerLength;
        }

        // small health regen?
        // how to trigger game over?
    }

    public boolean gameOver() {
        return this.CheckpointTimer <= 0 || this.PlayerHealth <= 0;
    }

    public Map<String, Object> GetProperties() {
        return Map.of(
            "Health", this.PlayerHealth,
            "Checkpoint Timer", this.CheckpointTimer,
            "Checkpoint Distance", this.CheckpointDistance,
            "Max Checkpoint Timer", this.CheckpointTimerLength,
            "Wave Speed", this.WaveSpeed,
            "Wave Density", this.WaveDensity,
            "Player Max Health", this.PlayerMaxHealth
        );
    }
}
