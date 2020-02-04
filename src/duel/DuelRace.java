package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.RaceAI;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import drive.ICheckpointDrive;
import helper.Log;
import service.Screen;
import service.checkpoint.CheckpointProgress;

public class DuelRace extends BaseAppState implements ICheckpointDrive {

    private final IDuelFlow flow;

    private CarBuilder cb;
    private DuelRaceMenu menu;
    private CheckpointProgress progress;
    private CountdownTimer countdown;

    private DuelWorld world;
    private CarCamera camera;
    private CarUI uiNode;

    private float raceTimer;
    private RayCarControl winner;

    private float readyStateTimeout;
    private RaceState state = RaceState.BeforeLoad;
    enum RaceState {
        BeforeLoad, WaitingForStartPress, Ready, Racing, Finished,
    }

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

        countdown = new CountdownTimer();
        
        //Checkpoint detection and stuff
        progress = new CheckpointProgress(checkpoints, cb.getAll(), rayCar);
        progress.setCheckpointSize(10);
        progress.attachVisualModel(false);
        getStateManager().attach(progress);


        goToState(RaceState.WaitingForStartPress);
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

        // limit time step being large due to loading
        float tpfLagless = Math.min(tpf, 1 / 30f);

        if (state == RaceState.Ready) {
            this.readyStateTimeout -= tpfLagless;
            countdown.setTime(readyStateTimeout);
            new Screen(getApplication().getContext().getSettings()).centerMe(countdown);
            if (this.readyStateTimeout < 0) {
                goToState(RaceState.Racing);
            }
        }

        if (state == RaceState.Racing) {
            raceTimer += tpfLagless;
            menu.setState(raceTimer);

            RayCarControl maybeWinner = progress.isThereAWinner(0, 2);
            if (maybeWinner != null) {
                this.winner = maybeWinner;
                goToState(RaceState.Finished);
            }
        }
    }

    void startRace() {
        goToState(RaceState.Ready);
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

    //#region state logic:
    private void goToState(RaceState nextState) {
        switch (nextState) {
        case BeforeLoad:
        case WaitingForStartPress:
            break;
        case Ready:
            this.readyStateTimeout = 3;
            ((SimpleApplication)getApplication()).getGuiNode().attachChild(countdown);
            break;
        case Racing:
            ((SimpleApplication) getApplication()).getGuiNode().detachChild(countdown);
            cb.setEnabled(true);
            raceTimer = 0;
            break;
        case Finished:
            this.cb.setEnabled(false);
            this.menu.raceStopped(this.winner == this.cb.get(0));
            break;
        default:
            throw new IllegalStateException("Unknown state: " + nextState);
        }
        state = nextState;
    }
    //#endregion


    private class CountdownTimer extends Container {
        private Label countDownLabel;
        public CountdownTimer() {
            this.addChild(new Label("", new ElementId("title")));
            countDownLabel = this.addChild(new Label(""));
        }

        public void setTime(float value) {
            countDownLabel.setText(helper.H.roundDecimal(value, 1) + " sec");
        }
    }
}
