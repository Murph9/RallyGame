package rallygame.drive;

import rallygame.world.IWorld;
import rallygame.world.WorldType;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Transform;

import rallygame.car.CarCamera;
import rallygame.car.CarManager;
import rallygame.car.data.Car;
import rallygame.car.data.CarDataConst;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ui.CarUI;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.service.PauseState;

public class DriveBase extends BaseAppState implements IDrive, PauseState.ICallback {

    private final IDriveDone done;
    protected IWorld world;

    // car stuff
    protected final Car car;

    // gui and camera stuff
    protected CarCamera camera;
    private CarUI uiNode;

    public DriveBase(IDriveDone done, Car car, IWorld world) {
        super();
        this.done = done;
        this.car = car;
        this.world = world;

        WorldType type = world.getType();
        if (type == WorldType.NONE) {
            Log.exit(-15, "not sure what world type you want");
        }
    }

    @Override
    public void initialize(Application app) {
        AppStateManager stateManager = getStateManager();

        stateManager.attach(world);

        getState(PauseState.class).register(this);

        // build player
        var cm = getState(CarManager.class);
        if (!cm.getAll().isEmpty()) {
            Log.e("!Unusually there are cars still in car builder, please clean up.");
            cm.removeAll();
        }

        RayCarControl rayCar = cm.addCar(car, world.getStart(), true);

        uiNode = new CarUI(rayCar);
        stateManager.attach(uiNode);

        // initCameras
        camera = new CarCamera(app.getCamera(), rayCar);
        stateManager.attach(camera);
        app.getInputManager().addRawInputListener(camera);

        getState(BulletAppState.class).setEnabled(true);
    }

    @Override
    protected void onEnable() {
        _setEnabled(true);
    }

    @Override
    protected void onDisable() {
        _setEnabled(false);
    }

    private void _setEnabled(boolean enabled) {
        this.world.setEnabled(enabled); // we kinda don't want the physics running while paused
        getState(BulletAppState.class).setEnabled(enabled);
        this.camera.setEnabled(enabled);
        getState(CarManager.class).setEnabled(enabled);
        getState(ParticleAtmosphere.class).setEnabled(enabled);
    }

    public void resetWorld() {
        world.reset();
    }

    @Override
    public void cleanup(Application app) {
        Log.p("cleaning drive class");

        getState(PauseState.class).deregister(this);

        getStateManager().detach(uiNode);
        uiNode = null;

        getStateManager().detach(world);
        world = null;

        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;

        getState(CarManager.class).removeAll();
    }

    protected final void reInitPlayerCar(CarDataConst carData) {
        var cm = getState(CarManager.class);
        cm.changeTo(cm.getPlayer(), carData);

        // refresh ui node
        getApplication().getStateManager().detach(uiNode);
        getApplication().getStateManager().attach(uiNode);
    }

    public Transform resetPosition(RayCarControl car) {
        return world.getStart();
    }

    @Override
    public void pauseState(boolean value) {
        this.setEnabled(value);
    }

    @Override
    public void pauseQuit() {
        next();
    }

    @Override
    public void next() {
        this.done.done(this);
    }
}
