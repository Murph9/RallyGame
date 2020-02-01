package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.RaceAI;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import drive.ICheckpointDrive;
import helper.Log;
import service.checkpoint.CheckpointProgress;

public class DuelRace extends BaseAppState implements ICheckpointDrive {

    private final IDuelFlow flow;

    private CarBuilder cb;
    private DuelRaceMenu menu;
    private CheckpointProgress progress;

    private DuelWorld world;
    private CarCamera camera;
    private CarUI uiNode;

    private float raceTimer;
    private RayCarControl winner;

    public DuelRace(IDuelFlow flow) {
        this.flow = flow;
    }

    @Override
    protected void initialize(Application app) {
        world = new DuelWorld();
        world.loadCheckpoints(app.getAssetManager());
        getStateManager().attach(world);
        Vector3f[] checkpoints = world.checkpoints();

        this.cb = getState(CarBuilder.class);

        DuelData data = flow.getData();

        CarDataConst yourCarData = cb.loadData(data.yourCar, data.yourAdjuster);
        RayCarControl rayCar = cb.addCar(yourCarData, world.start(0), true);

        uiNode = new CarUI(rayCar);
        getStateManager().attach(uiNode);

        CarDataConst theirCarData = cb.loadData(data.theirCar, data.theirAdjuster);
        RayCarControl car = this.cb.addCar(theirCarData, world.start(1), false);
        car.attachAI(new RaceAI(car, this), true);

        // initCamera
        camera = new CarCamera(app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);

        //wait until race start
        cb.setEnabled(false);

        //init menu
        this.menu = new DuelRaceMenu(this, yourCarData, theirCarData, () -> {
            DuelResultData drd = new DuelResultData();
            drd.quitGame = true;
            flow.nextState(this, drd);
        });
        getStateManager().attach(menu);


        //Checkpoint detection and stuff
        progress = new CheckpointProgress(checkpoints, cb.getAll(), rayCar);
        // progress.attachVisualModel(false);
        getStateManager().attach(progress);
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(progress);
        progress = null;
        
        getStateManager().detach(world);
        world = null;

        getStateManager().detach(uiNode);
        uiNode = null;

        this.cb.setEnabled(false);
        this.cb.removeAll();
        this.cb = null;

        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;
    }

    @Override
    protected void onEnable() {
        getState(BulletAppState.class).setEnabled(true);
    }
    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!isEnabled())
            return;
        if (winner != null)
            return;

        raceTimer += Math.min(tpf, 1/30f); //limit time step being large due to loading
        menu.setState(raceTimer);

        RayCarControl maybeWinner = progress.isThereAWinner(0, 1);
        if (maybeWinner != null) {
            this.winner = maybeWinner;
            this.cb.setEnabled(false);
            this.menu.raceStopped(this.winner == this.cb.get(0));
        }
    }

    void startRace() {
        cb.setEnabled(true);
        raceTimer = 0;
    }

    void quit() {
        DuelResultData d = new DuelResultData();
        d.raceResult = new DuelRaceResult();
        d.raceResult.playerWon = winner == this.cb.get(0);
        d.raceResult.mills = (long) (raceTimer * 1000f);

        Log.p("Winner: " + winner.getCarData().name);
        
        cb.setEnabled(false);
        flow.nextState(this, d);
    }

    @Override
    public Vector3f getNextCheckpoint(RayCarControl car) {
        return progress.getNextCheckpoint(car);
    }

    public Transform resetPosition(RayCarControl car) {
        Vector3f pos = progress.getLastCheckpoint(car);
        Vector3f next = progress.getNextCheckpoint(car);

        Quaternion q = new Quaternion();
        q.lookAt(next.subtract(pos), Vector3f.UNIT_Y);
        return new Transform(pos, q);
    }
}
