package survival;

import com.jme3.app.Application;
import com.jme3.math.Transform;

import rallygame.car.data.Car;
import rallygame.car.data.CarDataAdjuster;
import rallygame.drive.PauseState;
import rallygame.car.ray.RayCarControl;
import rallygame.drive.DriveBase;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.world.IWorld;

public class Drive extends DriveBase implements PauseState.ICallback {

    public Drive(IDriveDone done, IWorld world, Car car) {
        super(done, car, world);
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

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

    public void applyChange(CarDataAdjuster adj) {
        var carData = this.cm.loadData(this.car, adj);
        this.reInitPlayerCar(carData);
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
