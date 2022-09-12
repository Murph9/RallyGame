package rallygame.drive;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.car.CarManager;
import rallygame.car.ai.RaceAI;
import rallygame.car.data.Car;
import rallygame.car.ray.RayCarControl;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.IDriveDone;
import rallygame.helper.Log;
import rallygame.helper.Rand;
import rallygame.service.GridPositions;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.service.checkpoint.CheckpointProgressUI;
import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;

public class DriveRace extends DriveBase implements PauseState.ICallback, ICheckpointDrive {

    // ai things
    private final int themCount = 8;

    private CheckpointProgressUI progressMenu;
    private CheckpointProgress progress;

    //racing things
    private Vector3f[] worldStarts;
    private Quaternion worldRot;
    
    public DriveRace(StaticWorldBuilder world, IDriveDone done) {
        super(done, Car.Runner, world);
        
        if (world.getTypeForDriveRace() != StaticWorld.duct2)
            throw new IllegalArgumentException();
    }
    
    @Override
    public void initialize(Application app) {
        super.initialize(app);
        
        nextState();
        
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

        this.worldStarts = new GridPositions()
                .generate(checkpoints[0], checkpoints[0].subtract(checkpoints[checkpoints.length - 1]))
                .limit(themCount + 1).toArray(i -> new Vector3f[i]);

        //buildCars and load ai
        var cm = getState(CarManager.class);
        for (int i = 0; i < this.themCount; i++) {
            RayCarControl c = cm.addCar(Rand.randFromArray(Car.values()), worldStarts[i+1], worldRot, false);
            RaceAI rAi = new RaceAI(c, this);
            c.attachAI(rAi, true);
        }
        
        progress = new CheckpointProgress(CheckpointProgress.Type.Lap, checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointProgress.GetDefaultCheckpointModel(app, 10, new ColorRGBA(0, 1, 0, 0.4f)));
        getStateManager().attach(progress);

        progressMenu = new CheckpointProgressUI(progress);
        getStateManager().attach(progressMenu);
        
        //actually init
        nextState();
    }
    
    private boolean stateChanged = false;
    private RaceState state = RaceState.NA;
    enum RaceState {
        NA,
        Init,
        Ready,
        Racing,
        Win,
    }
    private void nextState() {
        stateChanged = true;
        switch (state) {
        case NA:
            state = RaceState.Init;
            break;
        case Init:
            state = RaceState.Ready;
            break;
        case Ready:
            state = RaceState.Racing;
            break;
        case Racing:
            state = RaceState.Win;
            break;
        case Win:
            next();
            break;
        default:
            throw new IllegalStateException();
        }
    }
    

    private float stateTimeout = 0;
    @Override
    public void update(float tpf) {
        if (!isEnabled())
            return;
        
        super.update(tpf);
        var cm = getState(CarManager.class);

        progressMenu.setBasicText("State:" + state.name() + "\nStateTimeout:" + this.stateTimeout);
        
        if (stateChanged) {
            stateChanged = false;
            if (state == RaceState.Ready)
                this.stateTimeout = 4;
        }
        
        switch (state) {
        case NA:
            return;
        case Init:
            setAllCarsToStart(cm);
            break;
        case Ready:
            setAllCarsToStart(cm);
            break;
        case Racing:
            progress.update(tpf);
            break;
        case Win:
            // delay and stuff maybe
            break;
        default:
            try {
                throw new Exception("Huh?" + state);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (this.stateTimeout != -1.0f) {
            // only update when not -1 as it will reset it every frame
            this.stateTimeout -= Math.min(tpf, 1/30f); // prevent load spike cutting this short
            if (this.stateTimeout < 0) {
                this.stateTimeout = -1.0f; //stop any timeout stuff unless the state says so
                nextState();
            }
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

    private void setAllCarsToStart(CarManager cm) {
        int count = 0;
        for (RayCarControl car: cm.getAll()) {
            car.setPhysicsProperties(worldStarts[count], new Vector3f(), worldRot, new Vector3f());
            count++;
        }

        camera.resetMouseView();
    }
    
    @Override
    public void cleanup(Application app) {
        Log.p("cleaning driverace class");
        progress.cleanup(app);
        
        getStateManager().detach(progressMenu);
        progressMenu = null;
        getStateManager().detach(progress);
        progress = null;

        super.cleanup(app);
    }
    
    @Override
    protected void onEnable() {
        getState(BulletAppState.class).setEnabled(true);
        getState(ParticleAtmosphere.class).setEnabled(true);

        this.camera.setEnabled(true);
        getState(CarManager.class).setEnabled(true);
    }

    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
        getState(ParticleAtmosphere.class).setEnabled(false);

        this.camera.setEnabled(false);
        getState(CarManager.class).setEnabled(false);
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
