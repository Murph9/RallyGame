package survival;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import rallygame.game.IDriveDone;
import rallygame.service.LoadingState;
import rallygame.world.wp.WP.DynamicType;


/*
Drive next x km to next checkpoint
main problem is Fuel
Drag gets worse as you drive (requiring more fuel)
- time to next checkpoint might decrease slowly

upgrade options are related to
- improving general performance
- fuel economy
- buying fuel
- buying new car type (which adds new fuel?)

After every 10x kms there are races which you must win, ie these are the boss milestones
or maybe just a mandatory fuel sacrifice
*/

public class Flow implements IDriveDone {

    private final Application app;
    private final String version;
    private AppState curState;

    public Flow(Application survivalApp, String projectVersion) {
        app = survivalApp;
        version = projectVersion;

        AppStateManager sm = app.getStateManager();
        var world = DynamicType.Ditch.getBuilder();
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
