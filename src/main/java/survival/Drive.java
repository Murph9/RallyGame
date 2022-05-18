package survival;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Transform;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.data.Car;
import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import rallygame.drive.PauseState;
import rallygame.car.ray.RayCarControl;
import rallygame.drive.DriveBase;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.service.Screen;
import rallygame.world.IWorld;

public class Drive extends DriveBase implements PauseState.ICallback {
    private static final boolean OFFER_UPGRADES = false;
    private static final Car CAR_TYPE = Car.Survivor;
    private final String version;

    private DodgeGameManager manager;
    private Container versionWindow;

    public Drive(IDriveDone done, IWorld world, String version) {
        super(done, CAR_TYPE, world);
        this.version = version;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        this.manager = new DodgeGameManager(this, OFFER_UPGRADES);
        getStateManager().attach(manager);

        versionWindow = new Container();
        versionWindow.addChild(new Label("WASD or Arrows to move\nGet checkpoints"));
        versionWindow.addChild(new Label("Version: " + this.version));
        ((SimpleApplication)app).getGuiNode().attachChild(versionWindow);
        new Screen(app.getContext().getSettings()).topRightMe(versionWindow);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
    }

    @Override
    public void resetWorld() {
        // nothing, because it breaks the checkpoints
    }

    @Override
    public Transform resetPosition(RayCarControl car) {
        return new Transform(car.location, car.rotation); // no change
    }

    @Override
    public void cleanup(Application app) {
        Log.p("cleaning survivor drive class");
        super.cleanup(app);

        versionWindow.removeFromParent();
        versionWindow = null;
    }

    public void increaseGrip() {
        var adj = new CarDataAdjuster(CarDataAdjustment.asFunc((data) -> {
            for (int i = 0; i < 4; i++) {
                data.wheelData[i].pjk_lat.D1 *= 1.15;
                data.wheelData[i].pjk_long.D1 *= 1.15;
            }
        }));

        var carData = this.cm.loadData(CAR_TYPE, adj);
        this.reInitPlayerCar(carData);
    }

    public void increasePower() {
        var adj = new CarDataAdjuster(CarDataAdjustment.asFunc((data) -> {
            for (int i = 0; i < data.e_torque.length; i++) {
                data.e_torque[i] *= 1.15f;
            }
        }));

        var carData = this.cm.loadData(CAR_TYPE, adj);
        this.reInitPlayerCar(carData);
    }

    public void applyChange(CarDataAdjuster adj) {
        var carData = this.cm.loadData(CAR_TYPE, adj);
        this.reInitPlayerCar(carData);
    }

    @Override
    protected void onEnable() {
        manager.setEnabled(true);
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        manager.setEnabled(false);
        super.onDisable();
    }

    @Override
    public void pauseState(boolean value) {
        this.setEnabled(value);
    }

    @Override
    public void quit() {
        next();
    }
}
