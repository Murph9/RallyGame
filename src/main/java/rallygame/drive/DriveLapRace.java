package rallygame.drive;

import com.jme3.app.Application;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import rallygame.car.ai.RaceAI;
import rallygame.car.data.Car;
import rallygame.car.ray.RayCarControl;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.service.GridPositions;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.service.checkpoint.CheckpointProgressUI;
import rallygame.world.ICheckpointWorld;
import rallygame.world.wp.StaticBuilt;

public class DriveLapRace extends DriveBase implements ICheckpointDrive {

    private CheckpointProgressUI progressMenu;
    private CheckpointProgress progress;

    public DriveLapRace(IDriveDone done, ICheckpointWorld world) {
        super(done, Car.Runner, world);

        if (!(world instanceof StaticBuilt))
            Log.e("!! StaticBuilt world only please");
    }
    
    @Override
    public void initialize(Application app) {
        super.initialize(app);

        var w = (ICheckpointWorld)world;
        var checkpoints = w.checkpoints();

        var worldRot = new Quaternion();
        worldRot.fromAngleAxis(FastMath.DEG_TO_RAD * 90, new Vector3f(0, 1, 0));
        var worldStarts = new GridPositions(1.3f, 10).generate(checkpoints[0], 
                checkpoints[0].subtract(checkpoints[checkpoints.length - 1]).negate())
                .limit(2).toArray(Vector3f[]::new);

        var c = cm.addCar(Car.Rally, worldStarts[1], worldRot, false);
        RaceAI rAi = new RaceAI(c, this, false);
        rAi.setRoadWidth(15);
        c.attachAI(rAi, true);

        // Checkpoint detection and stuff
        progress = new CheckpointProgress(CheckpointProgress.Type.Sprint, checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointProgress.GetDefaultCheckpointModel(app, 10));
        getStateManager().attach(progress);

        progressMenu = new CheckpointProgressUI(progress);
        getStateManager().attach(progressMenu);
    }

    @Override
    public Vector3f getLastCheckpoint(RayCarControl car) {
        return progress.getLastCheckpoint(car);
    }

    @Override
    public Vector3f getNextCheckpoint(RayCarControl car) {
        return progress.getNextCheckpoint(car);
    }
    
    @Override
    public Vector3f[] getNextCheckpoints(RayCarControl car, int count) {
        return progress.getNextCheckpoints(car, count);
    }

    @Override
    public void cleanup(Application app) {
        Log.p("cleaning drive lap race class");
        progress.cleanup(app);
        
        getStateManager().detach(progressMenu);
        progressMenu = null;
        getStateManager().detach(progress);
        progress = null;

        super.cleanup(app);
    }
}
