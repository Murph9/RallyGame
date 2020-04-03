package rallygame.drive;

import rallygame.world.IWorld;
import rallygame.world.WorldType;

import java.util.Collection;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Transform;

import rallygame.car.*;
import rallygame.car.data.Car;
import rallygame.car.ray.RayCarControl;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.service.LoadingState;

public class DriveBase extends BaseAppState implements IDrive {

    private final IDriveDone done;
    public DriveMenu menu;
    protected IWorld world;

    // car stuff
    private final Car car;
    protected CarBuilder cb;

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

        this.menu = new DriveMenu(this);
        stateManager.attach(menu);

        // build player
        this.cb = getState(CarBuilder.class);
        if (!this.cb.getAll().isEmpty()) {
            Log.e("!Unusually there are cars still in car builder, please clean up.");
            this.cb.removeAll();
        }

        RayCarControl rayCar = cb.addCar(car, world.getStartPos(), world.getStartRot(), true);

        uiNode = new CarUI(rayCar);
        stateManager.attach(uiNode);

        // initCameras
        camera = new CarCamera(app.getCamera(), rayCar);
        stateManager.attach(camera);
        app.getInputManager().addRawInputListener(camera);

        app.getStateManager().attach(new LoadingState(world, camera, uiNode, menu, cb));

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
        this.cb.setEnabled(enabled);

        getState(ParticleAtmosphere.class).setEnabled(enabled);
    }

    public void next() {
        this.done.done(this);
    }

    public void resetWorld() {
        world.reset();
    }

    public final Collection<RayCarControl> getAllCars() {
        return this.cb.getAll();
    }

    @Override
    public void cleanup(Application app) {
        Log.p("cleaning drive class");

        getStateManager().detach(menu);
        menu = null;

        getStateManager().detach(uiNode);
        uiNode = null;

        getStateManager().detach(world);
        world = null;

        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;

        cb.removeAll();
        cb = null;
    }

    protected final void reInitPlayerCar(Car car) {
        // remove camera and ui
        AppStateManager sm = getStateManager();
        sm.detach(camera);
        getApplication().getInputManager().removeRawInputListener(camera);

        sm.detach(uiNode);

        this.cb.removeCar(cb.get(0));

        RayCarControl c = this.cb.addCar(car, world.getStartPos(), world.getStartRot(), true);

        // initCamera and ui again
        camera = new CarCamera(getApplication().getCamera(), c);
        sm.attach(camera);
        getApplication().getInputManager().addRawInputListener(camera);

        uiNode = new CarUI(c);
        sm.attach(uiNode);
    }

    public Transform resetPosition(RayCarControl car) {
        return new Transform(world.getStartPos(), world.getStartRot());
    }
}
