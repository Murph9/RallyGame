package survival;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.CarManager;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.service.Screen;
import rallygame.service.checkpoint.BasicWaypointProgress;

public class DodgeGameManager extends BaseAppState {

    private float time;
    private CarManager cm;
    private WaveManager waveManager;
    private BasicWaypointProgress waypoints;

    private Container currentStateWindow;
    private Label checkCount;

    @Override
    protected void initialize(Application app) {
        cm = getState(CarManager.class);

        waveManager = new WaveManager();
        getStateManager().attach(waveManager);

        waypoints = new BasicWaypointProgress(cm.getPlayer());
        getStateManager().attach(waypoints);

        currentStateWindow = new Container();
        currentStateWindow.addChild(new Label("Checkpoints: "));
        checkCount = currentStateWindow.addChild(new Label("0.00sec"), 1);

        ((SimpleApplication) getApplication()).getGuiNode().attachChild(currentStateWindow);
        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);
    }

    @Override
    public void update(float tpf) {
        this.time += tpf;

        if (time > 1) {
            waveManager.addType(WaveType.SingleFollow, cm.getPlayer());
            time = 0;
        }

        if (waypoints.hitACheckpoint()) {
            this.setEnabled(false);
            //TODO do UI things
            return;
        }

        if (waypoints.noCheckpoint()) {
            waypoints.addCheckpointAt(H.v2tov3fXZ(Rand.randV2f(100, true))); //TODO obvs to do better than this
        }

        checkCount.setText(waypoints.totalCheckpoints()+"");

        super.update(tpf);
    }

    @Override
    protected void cleanup(Application app) {
        cm = null;

        getStateManager().detach(waypoints);
        waypoints = null;

        getStateManager().detach(waveManager);
        waveManager = null;
    }

    @Override
    protected void onEnable() {
        this.cm.setEnabled(true);
        this.waveManager.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        this.cm.setEnabled(false);
        this.waveManager.setEnabled(false);
    }
}

