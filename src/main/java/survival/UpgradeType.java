package survival;

import java.util.function.Consumer;

public enum UpgradeType {
    ShorterTimer("Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength *= 0.9f),
    //ReducePlayerHealth("Reduce the amount of your health", x -> x.PlayerHealth *= 0.9f), //TODO we need some kind of collision detection to take health off
    IncreaseCheckpointRange("Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f)
    ;

    public final String label;
    public final Consumer<GameRules> ruleFunc;
    UpgradeType(String label, Consumer<GameRules> func) {
        this.label = label;
        this.ruleFunc = func;
    }
}

class GameRules {
    public float CheckpointTimerLength = 60;
    public float CheckpointDistance = 100;

    @Override
    public String toString() {
        return "Check distance: " + this.CheckpointDistance +"\n"
                + "Check timer: " + this.CheckpointTimerLength + "\n";
    }
}
