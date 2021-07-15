package rallygame.drive;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.car.ai.CarAI;
import rallygame.car.data.Car;
import rallygame.car.ray.RayCarControl;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.service.AILearningService;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.service.checkpoint.CheckpointProgressUI;
import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;

public class DriveAILearn extends DriveBase implements PauseState.ICallback, ICheckpointDrive {

    // ai things
    private final ScheduledThreadPoolExecutor executor;

    private CheckpointProgressUI progressMenu;
    private CheckpointProgress progress;

    private Map<RayCarControl, Float> timer = new HashMap<>();

    //racing things
    private Vector3f worldStart;
    private Quaternion worldRot;
    
    public DriveAILearn(StaticWorldBuilder world, IDriveDone done) {
        super(done, Car.Runner, world);

        executor = new ScheduledThreadPoolExecutor(10);
        
        if (world.getTypeForDriveRace() != StaticWorld.duct2)
            throw new IllegalArgumentException();
    }
    
    @Override
    public void initialize(Application app) {
        super.initialize(app);

        final var th = new AILearningService(this, executor);
        executor.schedule(() -> {
            th.run();
        }, 0, TimeUnit.MICROSECONDS);

        //hard coded checkpoints for duct2 (small)
        Vector3f[] checkpoints = new Vector3f[] {
                new Vector3f(-167, -2, 81),
                new Vector3f(-237, -2, 68),
                new Vector3f(-290, -2, 29),
                new Vector3f(-321, -2, -43),
                new Vector3f(-311, -2, -130),
                new Vector3f(-255, -2, -198),
                new Vector3f(-170, -2, -215),
                new Vector3f(-91, -2, -189),
                new Vector3f(-17, -2, -105),
                new Vector3f(-1, -2, -67),

                new Vector3f(17, -2, -105),
                new Vector3f(91, -2, -189),
                new Vector3f(170, -2, -215),
                new Vector3f(255, -2, -198),
                new Vector3f(311, -2, -130),
                new Vector3f(321, -2, -43),
                new Vector3f(290, -2, 29),
                new Vector3f(237, -2, 68),
                new Vector3f(167, -2, 81),
                new Vector3f(91, -2, 77),
                new Vector3f(-91, -2, 77)
            };
        

        // generate starting positions and rotations
        this.worldRot = new Quaternion();
        this.worldRot.lookAt(checkpoints[0].subtract(checkpoints[checkpoints.length - 1]), Vector3f.UNIT_Y);
        this.worldStart = checkpoints[0];

        progress = new CheckpointProgress(CheckpointProgress.Type.Lap, checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointProgress.GetDefaultCheckpointModel(app, 10, new ColorRGBA(0, 1, 0, 0.4f)));
        getStateManager().attach(progress);

        progressMenu = new CheckpointProgressUI(progress);
        getStateManager().attach(progressMenu);
    }

    public RayCarControl addCar(Function<RayCarControl, CarAI> aiFunc) {
        try {
            return this.getApplication().enqueue(() -> {
                RayCarControl c = this.cm.addCar(Car.Runner, worldStart.add(0, 0, (float)Math.random()*30-15), worldRot, false);
                var ai = aiFunc.apply(c);
                c.attachAI(ai, true);

                progress.addCar(c);
                timer.put(c, 0f);
                return c;
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public float getScore(RayCarControl c) {
        for (var car: timer.entrySet()) {
            if (car.getKey() == c) {
                if (car.getValue() > 20) { // in sec
                    removeCar(c);
                    return progress.getStateAsFloat(car.getKey());
                }
            }
        }

        return 0;
    }

    private void removeCar(RayCarControl c) {
        this.getApplication().enqueue(() -> {
            this.timer.remove(c);
            progress.removeCar(c);
            this.cm.removeCar(c);

            System.out.println("removed car");
        });
    }
       

    @Override
    public void update(float tpf) {
        if (!isEnabled())
            return;
        
        super.update(tpf);

        for (var car: timer.entrySet()) {
            var time = car.getValue() + tpf;
            car.setValue(time);
        }
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
    public void cleanup(Application app) {
        Log.p("cleaning driverace class");
        progress.cleanup(app);
        
        getStateManager().detach(progressMenu);
        progressMenu = null;
        getStateManager().detach(progress);
        progress = null;

        executor.shutdown();

        super.cleanup(app);
    }
    
    @Override
    protected void onEnable() {
        getState(BulletAppState.class).setEnabled(true);
        getState(ParticleAtmosphere.class).setEnabled(true);

        this.camera.setEnabled(true);
        this.cm.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
        getState(ParticleAtmosphere.class).setEnabled(false);

        this.camera.setEnabled(false);
        this.cm.setEnabled(false);
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
    public void pauseState(boolean value) {
        this.setEnabled(value);
    }

    @Override
    public void quit() {
        next();
    }
}
