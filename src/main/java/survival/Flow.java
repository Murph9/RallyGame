package survival;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import rallygame.game.IDriveDone;
import rallygame.service.LoadingState;
import rallygame.world.TiledFlatWorld;

/**
Try and dodge the things
Upgrades help with it i guess

TODO:
- damage
- killing 'decayed' objects
- killing objects that fell off
*/



public class Flow implements IDriveDone {

    private final Application app;
    private final String version;
    private AppState curState;

    public Flow(Application survivalApp, String projectVersion) {
        app = survivalApp;
        version = projectVersion;

        AppStateManager sm = app.getStateManager();
        var world = new TiledFlatWorld();
        var drive = new Drive(this, world);
        var loading = new LoadingState(world, drive);
        loading.setCallback((states) -> {
            sm.attach(drive);
        });

        sm.attach(loading);
    }
    
    public void cleanup() {
        if (curState != null)
            app.getStateManager().detach(curState);
    }

    @Override
    public void done(AppState state) {
        AppStateManager sm = app.getStateManager();
        sm.detach(sm.getState(Drive.class));
    }
}
