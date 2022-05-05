package survival;

import java.util.LinkedList;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.car.data.Car;
import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import rallygame.car.ray.RayCarControl;
import rallygame.drive.DriveBase;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.world.wp.DefaultBuilder;

public class Drive extends DriveBase implements DefaultBuilder.IPieceChanged {

    private final static Car CAR_TYPE = Car.Survivor;

    private final LinkedList<Vector3f> initCheckpointBuffer = new LinkedList<>();
    private CheckpointProgress progress;
    private GameManager menu;

    public Drive(IDriveDone done, DefaultBuilder world) {
        super(done, CAR_TYPE, world);

        world.registerListener(this);
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        Vector3f[] checkpoints = new Vector3f[] {
            new Vector3f(0, 0, 0),
            new Vector3f(10, 0, 0) //starting vector to get the cars to drive forward
        };

        progress = new CheckpointProgress(CheckpointProgress.Type.Sprint, checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointProgress.GetDefaultCheckpointModel(app, 25, new ColorRGBA(0, 1, 0, 0.4f)));
        getStateManager().attach(progress);

        menu = new GameManager(this, progress);
        getStateManager().attach(menu);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        // add any buffered checkpoints
        if (progress.isInitialized() && !initCheckpointBuffer.isEmpty()) {
            for (Vector3f p : initCheckpointBuffer)
                progress.addCheckpoint(p);
            initCheckpointBuffer.clear();
        }

        if (progress.isInitialized())
            progress.update(tpf);
    }

    @Override
    public void resetWorld() {
        // nothing, because it breaks the checkpoints
    }

    @Override
    public Transform resetPosition(RayCarControl car) {
        Vector3f pos = progress.getLastCheckpoint(car);
        Vector3f next = progress.getNextCheckpoint(car);

        Quaternion q = new Quaternion();
        q.lookAt(next.subtract(pos), Vector3f.UNIT_Y);
        return new Transform(pos, q);
    }

    @Override
    public void pieceAdded(Vector3f pos) {
        if (!progress.isInitialized())   {
            initCheckpointBuffer.add(pos);
            return;
        }
        progress.addCheckpoint(pos);
    }

    @Override
    public void pieceRemoved(Vector3f pos) {
        if (!progress.isInitialized())
            throw new IllegalStateException("Can't remove a piece until its initialized");
        progress.setMinCheckpoint(pos);
    }

    @Override
    public void cleanup(Application app) {
        Log.p("cleaning survivor drive class");
        progress.cleanup(app);
        
        getStateManager().detach(progress);
        progress = null;

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
}
