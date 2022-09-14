package survival;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import rallygame.car.data.Car;
import rallygame.game.IDriveDone;
import rallygame.service.LoadingState;
import rallygame.world.TiledFlatWorld;

/**
 TODO
Try and dodge the things
Upgrades help with it i guess
*/

public class Flow implements IDriveDone {
    private static final boolean OFFER_UPGRADES = true;
    private static final Car CAR_TYPE = Car.Survivor;

    private final Application app;
    private final String version;
    private AppState curState;

    public Flow(Application survivalApp, String projectVersion) {
        app = survivalApp;
        version = projectVersion;

        AppStateManager sm = app.getStateManager();
        var world = new TiledFlatWorld();
        var drive = new Drive(this, world, CAR_TYPE);
        var dodgeGameManager = new DodgeGameManager(OFFER_UPGRADES, version);
        var loading = new LoadingState(world, drive, dodgeGameManager);
        loading.setCallback((states) -> {
            sm.attach(drive);
            sm.attach(dodgeGameManager);
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
        sm.detach(sm.getState(DodgeGameManager.class));
    }
}
