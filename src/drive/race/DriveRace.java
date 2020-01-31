package drive.race;

import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.RaceAI;
import car.data.Car;
import car.ray.RayCarControl;
import drive.PauseState;
import game.IDriveDone;
import helper.H;
import helper.Log;
import service.GridPositions;
import world.StaticWorld;
import world.StaticWorldBuilder;

//TODO DriveRace can't be converted to DriveBase as the world must be initialised before this
public class DriveRace extends BaseAppState implements PauseState.ICallback {

    public DriveRaceUI menu;
    
    //things that should be in a world class
    private Node rootNode = new Node("root");
    
    private final Car playerCarType;
    private final StaticWorldBuilder world;
    private final IDriveDone done;
    
    // ai things
    private final int themCount = 15;

    private CarBuilder cb;
    private DriveRaceProgress progress;
    private PauseState pauseState;

    //gui and camera stuff
    private CarCamera camera;
    private CarUI uiNode;
    
    //racing things
    private Vector3f[] worldStarts;
    private Matrix3f worldRot;
    
    public DriveRace(StaticWorldBuilder world, IDriveDone done) {
        super();
        this.playerCarType = Car.Runner;
        this.world = world;
        this.done = done;

        if (this.world.getTypeForDriveRace() != StaticWorld.duct2)
            throw new IllegalArgumentException();
    }
    
    @Override
    public void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);
        
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
        Quaternion q = new Quaternion();
        q.lookAt(checkpoints[0].subtract(checkpoints[checkpoints.length - 1]), Vector3f.UNIT_Y);
        this.worldRot = q.toRotationMatrix();

        GridPositions gridPositions = new GridPositions(3, 10);
        List<Vector3f> startPositions = gridPositions.generate(themCount+1, checkpoints[0], 
                checkpoints[0].subtract(checkpoints[checkpoints.length - 1]));

        this.worldStarts = startPositions.toArray(new Vector3f[0]);

        //buildCars
        this.cb = getState(CarBuilder.class);
        RayCarControl rayCar = cb.addCar(playerCarType, worldStarts[0], worldRot, true);

        uiNode = new CarUI(rayCar);
        getStateManager().attach(uiNode);
        
        //load ai
        for (int i = 0; i < this.themCount; i++) {
            RayCarControl c = this.cb.addCar(H.randFromArray(Car.values()), worldStarts[i+1], worldRot, false);
            RaceAI rAi = new RaceAI(c, this);
            c.attachAI(rAi, true);
        }
        
        //initCameras
        camera = new CarCamera(app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);
        
        getState(BulletAppState.class).setEnabled(true);

        pauseState = new PauseState(this);
        getStateManager().attach(pauseState);

        progress = new DriveRaceProgress(checkpoints, cb.getAll(), rayCar);
        getStateManager().attach(progress);

        menu = new DriveRaceUI(progress);
        getStateManager().attach(menu);
        
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
            done.done(this);
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

        menu.setBasicText("State:" + state.name() + "\nStateTimeout:" + this.stateTimeout);
        
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

    public Transform resetTransform(RayCarControl car) {
        Vector3f pos = progress.getLastCheckpoint(car);
        Vector3f next = progress.getNextCheckpoint(car);

        Quaternion q = new Quaternion();
        q.lookAt(next.subtract(pos), Vector3f.UNIT_Y);
        return new Transform(pos, q);
    }

    private void setAllCarsToStart() {
        int count = 0;
        for (RayCarControl car: cb.getAll()) {
            car.setPhysicsProperties(worldStarts[count], new Vector3f(), new Quaternion().fromRotationMatrix(worldRot), new Vector3f());
            count++;
        }

        camera.resetMouseView();
    }
    
    @Override
    public void cleanup(Application app) {
        Log.p("cleaning driverace class");
        progress.cleanup(app);
        
        getStateManager().detach(menu);
        menu = null;
        getStateManager().detach(progress);
        progress = null;
        getStateManager().detach(pauseState);
        pauseState = null;
        
        getStateManager().detach(uiNode);
        uiNode = null;
                
        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;
        
        ((SimpleApplication)app).getRootNode().detachChild(rootNode);

        cb.removeAll();
        cb = null;
    }
    
    @Override
    protected void onEnable() {
        getState(BulletAppState.class).setEnabled(true);
        this.camera.setEnabled(true);
        this.cb.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
        this.camera.setEnabled(false);
        this.cb.setEnabled(false);
    }
    
    public Vector3f getNextCheckpoint(RayCarControl car) {
        return progress.getNextCheckpoint(car);
    }

    @Override
    public void pauseState(boolean value) {
        this.setEnabled(value);
    }

    @Override
    public void quit() {
        System.exit(-111);
    }
}
