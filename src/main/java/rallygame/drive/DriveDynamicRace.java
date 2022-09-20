package rallygame.drive;

import java.util.LinkedList;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
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
import rallygame.service.GridPositions;
import rallygame.service.checkpoint.CheckpointModelFactory;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.service.checkpoint.CheckpointProgressUI;
import rallygame.world.wp.DefaultBuilder;


public class DriveDynamicRace extends DriveBase {

    // ai things
    private final int themCount = 1;

    private CheckpointProgressUI progressMenu;
    private CheckpointProgress progress;

    //racing things
    private Vector3f[] worldStarts;
    private Quaternion worldRot;
    
    private LinkedList<Vector3f> initCheckpointBuffer;

    public DriveDynamicRace(DefaultBuilder world, IDriveDone done) {
        super(done, Car.Runner, world);
        world.setDistFunction(() -> {
            var cm = getState(CarManager.class);
            return cm.getAll().stream().map(x -> x.location).toArray(Vector3f[]::new);
        });

        initCheckpointBuffer = new LinkedList<>();
    }
    
    @Override
    public void initialize(Application app) {
        super.initialize(app);
        
        nextState();
        
        Vector3f[] checkpoints = new Vector3f[] {
                new Vector3f(0, 0, 0),
                new Vector3f(10, 0, 0) //starting vector to get the cars to drive forward
            };
        

        // generate starting positions and rotations
        this.worldRot = new Quaternion();
        worldRot.fromAngleAxis(FastMath.DEG_TO_RAD * 90, new Vector3f(0, 1, 0));

        this.worldStarts = new GridPositions(1.3f, 10).generate(checkpoints[0], 
                checkpoints[0].subtract(checkpoints[checkpoints.length - 1]).negate())
                .limit(themCount+1).toArray(i -> new Vector3f[i]);

        var cm = getState(CarManager.class);
        var aiCars = new LinkedList<RayCarControl>();
        //buildCars and load ai
        for (int i = 0; i < this.themCount; i++) {
            RayCarControl c = cm.addCar(Car.LeMans, worldStarts[i+1], worldRot, false);
            aiCars.add(c);
        }
        
        progress = new CheckpointProgress((DefaultBuilder)world, checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointModelFactory.GetDefaultCheckpointModel(app, 4, new ColorRGBA(0, 1, 0, 0.4f)));
        getStateManager().attach(progress);

        progressMenu = new CheckpointProgressUI(progress);
        getStateManager().attach(progressMenu);

        for (var aiCar: aiCars) {
            RaceAI rAi = new RaceAI(aiCar, progress, false);
            aiCar.attachAI(rAi, true);
        }

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

        //add any buffered checkpoints
        if (progress.isInitialized() && !initCheckpointBuffer.isEmpty()) {
            for (Vector3f p : initCheckpointBuffer)
                progress.addCheckpoint(p);
            initCheckpointBuffer.clear();
        }

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
}
