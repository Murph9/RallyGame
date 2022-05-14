package survival;

import com.jme3.app.Application;
import com.jme3.math.Transform;

import rallygame.car.data.Car;
import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import rallygame.car.ray.RayCarControl;
import rallygame.drive.DriveBase;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.world.IWorld;

public class Drive extends DriveBase {
    private static final boolean OFFER_UPGRADES = true;
    private static final Car CAR_TYPE = Car.Survivor;

    // TODO listen to pauses
    private DodgeGameManager manager;

    public Drive(IDriveDone done, IWorld world) {
        super(done, CAR_TYPE, world);
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        this.manager = new DodgeGameManager(this, OFFER_UPGRADES);
        getStateManager().attach(manager);
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
}
