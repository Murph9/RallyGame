package rallygame.drive;

import java.util.LinkedList;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.ai.RaceAI;
import rallygame.car.data.Car;
import rallygame.car.ray.RayCarControl;
import rallygame.effects.LoadModelWrapper;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.IDriveDone;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.service.GridPositions;
import rallygame.service.Screen;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.service.checkpoint.CheckpointProgressUI;
import rallygame.world.wp.DefaultBuilder;


public class DynamicCopyRace extends DriveBase implements PauseState.ICallback, ICheckpointDrive, DefaultBuilder.IPieceChanged {

    private final Car[] carList = new Car[]{
            Car.Rocket,
            Car.Ultra,
            Car.Rally,
            Car.LeMans,
            Car.Runner,
            Car.Normal,
            Car.Roadster,
            Car.Ricer,
            Car.Hunter
    };

    private CheckpointProgressUI progressMenu;
    private CheckpointProgress progress;

    //racing things
    private Vector3f[] worldStarts;
    private Quaternion worldRot;
    
    private LinkedList<Vector3f> initCheckpointBuffer;
    
    private Label timer;
    private float time;

    //TODO design:
    //- catch up to first by copying cars as you go
    //- win when you are first

    public DynamicCopyRace(DefaultBuilder world, IDriveDone done) {
        super(done, Car.Runner, world);
        world.registerListener(this);
        world.setDistFunction(() -> this.cb.getAll().stream().map(x -> x.location).toArray(Vector3f[]::new));

        initCheckpointBuffer = new LinkedList<>();
    }
    
    @Override
    public void initialize(Application app) {
        super.initialize(app);
        
        nextState();
        
        //hard coded checkpoints for duct2 (small)
        Vector3f[] checkpoints = new Vector3f[] {
                new Vector3f(0, 0, 0),
                new Vector3f(10, 0, 0) // starting vector to get the cars to drive forward
            };
        

        // generate starting positions and rotations
        this.worldRot = new Quaternion();
        worldRot.fromAngleAxis(FastMath.DEG_TO_RAD * 90, new Vector3f(0, 1, 0));

        this.worldStarts = new GridPositions(1.3f, 6).generate(checkpoints[0], 
                checkpoints[0].subtract(checkpoints[checkpoints.length - 1]).negate())
                .limit(carList.length + 1).toArray(i -> new Vector3f[i]);

        //buildCars and load ai
        for (int i = carList.length - 1; i >= 0; i--) {
            RayCarControl c = this.cb.addCar(carList[i], worldStarts[i+1], worldRot, false);
            RaceAI rAi = new RaceAI(c, this, false);
            c.attachAI(rAi, true);
        }
        
        // Something bigger to spawn on
        Geometry gBox = new Geometry("a bigger starting box", new Box(20, 0.25f, 10));
        gBox.setLocalTranslation(-30, -0.1f, 0);
        Spatial boxG = LoadModelWrapper.create(app.getAssetManager(), gBox, ColorRGBA.Red);
        boxG.addControl(new RigidBodyControl(0));
        ((SimpleApplication) app).getRootNode().attachChild(boxG);
        getState(BulletAppState.class).getPhysicsSpace().add(boxG);

        // init checkpoints
        progress = new CheckpointProgress(CheckpointProgress.Type.Sprint, checkpoints, cb.getAll(), cb.get(0));
        progress.setCheckpointModel(CheckpointProgress.GetDefaultCheckpointModel(app, 4, new ColorRGBA(0, 1, 0, 0.4f)));
        getStateManager().attach(progress);

        progressMenu = new CheckpointProgressUI(progress);
        getStateManager().attach(progressMenu);

        //show score
        var container = new Container();
        this.timer = container.addChild(new Label("Score: 0 sec"));
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(container);
        new Screen(getApplication().getContext().getSettings()).topLeftMe(container);

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
            state = RaceState.Init;
            break;
        default:
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void end() {
        var container = new Container();
        container.addChild(new Label("Score: " + H.roundDecimal(time, 2) + " sec"));
        Button button = container.addChild(new Button("Okay"));
        button.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                next();
                ((SimpleApplication) getApplication()).getGuiNode().detachChild(container);
            }
        });
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(container);
    }

    private float stateTimeout = 0;
    @Override
    public void update(float tpf) {
        if (!isEnabled())
            return;
        super.update(tpf);

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
            setAllCarsToStart();
            break;
        case Ready:
            setAllCarsToStart();
            break;
        case Racing:
            progress.update(tpf);
            checkWinners();
            time += tpf;
            timer.setText(H.roundDecimal(time, 2) + " sec");
            break;
        case Win:
            // delay and stuff maybe
            this.cb.setEnabled(false);
            end();
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

    private void checkWinners() {
        // TODO check if player is winning, end then call nextState();
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

    private void setAllCarsToStart() {
        int count = cb.getCount() - 1;
        for (RayCarControl car: cb.getAll()) {
            car.setPhysicsProperties(worldStarts[count], new Vector3f(), worldRot, new Vector3f());
            count--;
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
        this.cb.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
        getState(ParticleAtmosphere.class).setEnabled(false);

        this.camera.setEnabled(false);
        this.cb.setEnabled(false);
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
            throw new IllegalStateException("Think about it, how?");

        progress.setMinCheckpoint(pos);
    }
}
