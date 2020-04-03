package rallygame.service;

import com.jme3.app.state.AppState;

public interface ILoadable extends AppState {

    /**Returns the percent loaded [0-1], at one (or greater it should loaded) */
    float loadPercent();
}
