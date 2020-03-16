package rallygame.game;

import com.jme3.app.state.AppState;

public interface IDriveDone {
    /** When a gameplay state is done, they call this */
    void done(AppState state);
}