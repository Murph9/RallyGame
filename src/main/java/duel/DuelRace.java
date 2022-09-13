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

import rallygame.car.CarManager;
import rallygame.car.CarCamera;
import rallygame.car.ai.BrakeAI;
import rallygame.car.ai.RaceAI;
import rallygame.car.data.CarDataConst;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ui.CarUI;
import rallygame.drive.IDrive;
import rallygame.effects.ParticleAtmosphere;
import rallygame.helper.Log;
import rallygame.service.Screen;
import rallygame.service.checkpoint.CheckpointModelFactory;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.world.ICheckpointWorld;

public class DuelRace extends BaseAppState implements IDrive {

    private final IDuelFlow flow;

    private CarManager cm;
    private DuelRaceMenu menu;
    private CheckpointProgress progress;
    private CountdownTimer countdown;

    private ICheckpointWorld world;
    private int checkpointCount;
    private CarCamera camera;
    private CarUI uiNode;

    private float raceTimer;
    private RayCarControl winner;

    private float readyStateTimeout;
    private RaceState state = RaceState.BeforeLoad;

    enum RaceState {
        BeforeLoad, WaitingForStartPress, Ready, Racing, Finished,
    }

    public DuelRace(IDuelFlow flow, ICheckpointWorld world) {
        this.flow = flow;
        this.world = world;
    }

    @Override
    protected void initialize(Application app) {
        getStateManager().attach(world);
        Vector3f[] checkpoints = world.checkpoints();
        checkpointCount = checkpoints.length;

        this.cm = getState(CarManager.class);

        DuelData data = flow.getData();

        CarDataConst yourCarData = cm.loadData(data.yourCar, data.yourAdjuster);
        RayCarControl rayCar = cm.addCar(yourCarData, world.start(0), true);

        uiNode = new CarUI(rayCar);
        getStateManager().attach(uiNode);

        CarDataConst theirCarData = cm.loadData(data.theirCar, data.theirAdjuster);
        RayCarControl car = this.cm.addCar(theirCarData, world.start(1), false);

        // Checkpoint detection and stuff
        progress = new CheckpointProgress(checkpoints, cm.getAll(), rayCar);
        progress.setCheckpointModel(CheckpointModelFactory.GetDefaultCheckpointModel(app, 10));
        getStateManager().attach(progress);

        // add AI
        car.attachAI(new RaceAI(car, progress), true);

        // initCamera
        camera = new CarCamera(app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);

        // wait until race start
        cm.setEnabled(false);

        // init menu
        this.menu = new DuelRaceMenu(this, yourCarData, theirCarData, () -> {
            DuelResultData drd = new DuelResultData();
            drd.quitGame = true;
            flow.nextState(this, drd);
        });
        getStateManager().attach(menu);

        countdown = new CountdownTimer();

        goToState(RaceState.WaitingForStartPress);
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(menu);
        menu = null;

        getStateManager().detach(progress);
        progress = null;

        getStateManager().detach(world);
        world = null;

        getStateManager().detach(uiNode);
        uiNode = null;

        this.cm.setEnabled(false);
        this.cm.removeAll();
        this.cm = null;

        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;
    }

    @Override
    protected void onEnable() {
        getState(BulletAppState.class).setEnabled(true);
        getState(ParticleAtmosphere.class).setEnabled(true);

        this.camera.setEnabled(true);
        //TODO this should be enabling/disabling the cm here
    }

    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
        getState(ParticleAtmosphere.class).setEnabled(false);

        this.camera.setEnabled(false);
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

            RayCarControl maybeWinner = progress.isThereSomeoneAtState(0, checkpointCount - 1);
            if (maybeWinner != null) {
                this.winner = maybeWinner;
                goToState(RaceState.Finished);
            }
        }
    }

    protected void startRace() {
        goToState(RaceState.Ready);
    }

    protected void quit() {
        DuelResultData d = new DuelResultData();
        d.raceResult = new DuelRaceResult();
        d.raceResult.playerWon = winner == this.cm.getPlayer();
        d.raceResult.mills = (long) (raceTimer * 1000f);

        Log.p("Winner: " + winner.getCarData().name);

        flow.nextState(this, d);
    }

    @Override
    public Transform resetPosition(RayCarControl car) {
        Vector3f pos = progress.getLastCheckpoint(car);
        Vector3f next = progress.getNextCheckpoint(car);

        Quaternion q = new Quaternion();
        q.lookAt(next.subtract(pos), Vector3f.UNIT_Y);
        return new Transform(pos, q);
    }

    private void goToState(RaceState nextState) {
        switch (nextState) {
            case BeforeLoad:
            case WaitingForStartPress:
                break;
            case Ready:
                this.readyStateTimeout = 3;
                ((SimpleApplication) getApplication()).getGuiNode().attachChild(countdown);
                break;
            case Racing:
                ((SimpleApplication) getApplication()).getGuiNode().detachChild(countdown);
                cm.setEnabled(true);
                raceTimer = 0;
                break;
            case Finished:
                menu.raceStopped(this.winner == this.cm.getPlayer());
                for (RayCarControl c : this.cm.getAll()) {
                    c.attachAI(new BrakeAI(c), true);
                }
                break;
            default:
                throw new IllegalStateException("Unknown state: " + nextState);
        }
        state = nextState;
    }

    private class CountdownTimer extends Container {
        private Label countDownLabel;

        public CountdownTimer() {
            this.addChild(new Label("", new ElementId("title")));
            countDownLabel = this.addChild(new Label(""));
        }

        public void setTime(float value) {
            countDownLabel.setText(rallygame.helper.H.roundDecimal(value, 1) + " sec");
        }
    }

    @Override
    public void next() {
    }

    @Override
    public void resetWorld() {
    }
}
