package survival;

import java.util.function.Consumer;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.CarManager;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.service.Screen;
import rallygame.service.checkpoint.BasicWaypointProgress;

public class DodgeGameManager extends BaseAppState {

    private final GameRules rules;

    private Node uiRootNode;

    private float time;
    private CarManager cm;
    private WaveManager waveManager;
    private BasicWaypointProgress waypoints;

    private Container currentSelectionWindow;
    private Label ruleLabel;
    private Label checkCount;
    private Label timer;

    public DodgeGameManager() {
        rules = new GameRules();
    }

    @Override
    protected void initialize(Application app) {
        cm = getState(CarManager.class);

        waveManager = new WaveManager();
        getStateManager().attach(waveManager);

        waypoints = new BasicWaypointProgress(cm.getPlayer());
        getStateManager().attach(waypoints);

        var currentStateWindow = new Container();
        currentStateWindow.addChild(new Label("Checkpoints: "));
        checkCount = currentStateWindow.addChild(new Label("0"), 1);
        currentStateWindow.addChild(new Label("Time remaining: "));
        timer = currentStateWindow.addChild(new Label("0.00sec"), 1);

        uiRootNode = ((SimpleApplication) getApplication()).getGuiNode();
        uiRootNode.attachChild(currentStateWindow);
        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);

        var ruleWindow = new Container();
        ruleLabel = ruleWindow.addChild(new Label("Checkpoints: "));
        uiRootNode.attachChild(ruleWindow);
        new Screen(getApplication().getContext().getSettings()).topLeftMe(ruleWindow);
    }

    @Override
    public void update(float tpf) {
        if (!this.isEnabled())
            return;
        
        if (time < 0) {
            if (time > 1e5) {
                //i.e. never happen but i want to keep the line here
                waveManager.addType(WaveType.SingleFollow, cm.getPlayer());
            }
            
            var loseWindow = new Container();
            loseWindow.addChild(new Label("You lost, pls close window"));
            this.setEnabled(false);
            return;
        }

        if (waypoints.hitACheckpoint()) {
            this.setEnabled(false);
            currentSelectionWindow = SelectionUI.GenerateSelectionUI(this, UpgradeType.values());
            uiRootNode.attachChild(currentSelectionWindow);
            new Screen(getApplication().getContext().getSettings()).centerMe(currentSelectionWindow);
            return;
        }

        if (waypoints.noCheckpoint()) {
            time = rules.CheckpointTimerLength;
            var vec2 = H.v3tov2fXZ(this.cm.getPlayer().location);
            var dir = Rand.randV2f(1, true);
            dir.normalizeLocal();
            dir.multLocal(rules.CheckpointDistance);
            waypoints.addCheckpointAt(H.v2tov3fXZ(vec2.add(dir)));
        }

        this.time -= tpf;
        checkCount.setText(waypoints.totalCheckpoints()+"");
        timer.setText(this.time+"");
        ruleLabel.setText(this.rules.toString());

        super.update(tpf);
    }

    @Override
    protected void cleanup(Application app) {
        cm = null;

        uiRootNode.removeFromParent();
        uiRootNode = null;

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

    public void updateRules(Consumer<GameRules> func) {
        func.accept(this.rules);
        this.setEnabled(true);

        currentSelectionWindow.removeFromParent();
        currentSelectionWindow = null;
    }
}

