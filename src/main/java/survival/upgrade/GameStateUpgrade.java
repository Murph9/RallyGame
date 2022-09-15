package survival.upgrade;

import java.util.function.Consumer;

import survival.GameState;

public class GameStateUpgrade extends Upgrade<GameState> {
    public GameStateUpgrade(boolean positive, String label, Consumer<GameState> func) {
        super(positive, label, func);
    }
}