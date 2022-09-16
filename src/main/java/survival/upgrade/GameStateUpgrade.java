package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;

import survival.GameState;

public class GameStateUpgrade extends Upgrade<GameState> {
    public GameStateUpgrade(boolean positive, String label, Consumer<GameState> func) {
        super(positive, label, func);
    }

    @Override
    public boolean applies(List<Upgrade<?>> existing) {
        return true; // no restrictions that i know of
    }

    public static Upgrade<GameState> ShorterTimer = new GameStateUpgrade(false, "Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength -= 3);
    public static Upgrade<GameState> IncreaseCheckpointRange = new GameStateUpgrade(false, "Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f);
    public static Upgrade<GameState> CubeCountInc = new GameStateUpgrade(false, "Increase the amount of cubes", x -> x.EntityCount += 1);
    public static Upgrade<GameState> WaveSpeedInc = new GameStateUpgrade(false, "Increase the speed waves are spawned", x -> x.WaveSpeed -= 0.2f);
    public static Upgrade<GameState> ReducePlayerHealth = new GameStateUpgrade(false, "Reduce the amount of your health", x -> x.PlayerMaxHealth -= 2);
    // public static Upgrade<GameState> Heal = new GameStateUpgrade(true, "Half Heal", x -> x.PlayerHealth += x.PlayerMaxHealth/2f);
    public static Upgrade<GameState> LongerTimer = new GameStateUpgrade(true, "Increase checkpoint time by 5 sec", x -> x.CheckpointTimerLength += 5);
}
