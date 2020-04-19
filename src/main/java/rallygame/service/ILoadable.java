package rallygame.service;

import com.jme3.app.state.AppState;

public interface ILoadable extends AppState {

    /**Returns the percent loaded [0-1], at one (or greater it should loaded) */
    LoadResult loadPercent();

    class LoadResult {
        public final float percent;
        public final String message;

        public LoadResult(float percent, String message) {
            this.percent = percent;
            this.message = message;
        }
    }
}
