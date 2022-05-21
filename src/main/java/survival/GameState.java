package survival;

import java.util.Map;

public class GameState {
    
    public float PlayerHealth;
    public float CheckpointTimer;

    public GameState(GameRules rules) {
        this.PlayerHealth = rules.PlayerMaxHealth;
        this.CheckpointTimer = rules.CheckpointTimerLength;
    }

    public void update(float tpf, GameRules rules) {
        if (this.PlayerHealth > rules.PlayerMaxHealth) {
            this.PlayerHealth = rules.PlayerMaxHealth;
        }

        this.CheckpointTimer -= tpf;

        // small health regen?
        // how to trigger game over?
    }

    public Map<String, Object> GetProperties() {
        return Map.of(
            "Health", this.PlayerHealth,
            "Checkpoint Timer", this.CheckpointTimer
        );
    }
}
