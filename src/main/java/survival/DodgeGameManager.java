package survival;

import java.util.function.Consumer;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.CarManager;
import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import rallygame.car.data.CarDataConst;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.service.Screen;
import rallygame.service.checkpoint.BasicWaypointProgress;
import rallygame.service.checkpoint.CheckpointArrow;
import survival.wave.UpgradeType;
import survival.wave.WaveManager;

public class DodgeGameManager extends BaseAppState {

    private final GameRules rules;
    private final GameState state;
    private final boolean offerUpgrades;
    private final Drive drive;

    private Node uiRootNode;

    private CarManager cm;
    private WaveManager waveManager;
    private BasicWaypointProgress waypoints;
    private CheckpointArrow checkpointArrow;

    private Container currentSelectionWindow;
    private Container ruleWindow;
    private Label checkCount;
    private Label timer;

    public DodgeGameManager(Drive drive, boolean offerUpgrades) {
        this.drive = drive;
        this.rules = GameRules.Generate();
        this.state = new GameState(rules);
        this.offerUpgrades = offerUpgrades;
    }

    @Override
    protected void initialize(Application app) {
        cm = getState(CarManager.class);

        waypoints = new BasicWaypointProgress(cm.getPlayer());
        getStateManager().attach(waypoints);

        waveManager = new WaveManager(this, cm.getPlayer());
        getStateManager().attach(waveManager);

        var currentStateWindow = new Container();
        currentStateWindow.addChild(new Label("Checkpoints: "));
        checkCount = currentStateWindow.addChild(new Label("0"), 1);
        currentStateWindow.addChild(new Label("Time remaining: "));
        timer = currentStateWindow.addChild(new Label("0.00sec"), 1);

        uiRootNode = ((SimpleApplication) getApplication()).getGuiNode();
        uiRootNode.attachChild(currentStateWindow);
        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);

        ruleWindow = new Container();
        uiRootNode.attachChild(ruleWindow);
        new Screen(getApplication().getContext().getSettings()).topLeftMe(ruleWindow);

        checkpointArrow = new CheckpointArrow(cm.getPlayer(), (__) -> this.waypoints.getCurrentPos());
        getStateManager().attach(checkpointArrow);
    }

    @Override
    public void update(float tpf) {
        if (!this.isEnabled())
            return;
        
        if (this.state.CheckpointTimer < 0) {
            var loseWindow = new Container();
            loseWindow.addChild(new Label("You lost, pls close window"));
            uiRootNode.attachChild(loseWindow);
            new Screen(getApplication().getContext().getSettings()).centerMe(loseWindow);
            this.setEnabled(false);
            return;
        }

        if (waypoints.hitACheckpoint()) {
            if (offerUpgrades) {
                this.setEnabled(false);
                currentSelectionWindow = SelectionUI.GenerateSelectionUI(this, UpgradeType.values());
                uiRootNode.attachChild(currentSelectionWindow);
                new Screen(getApplication().getContext().getSettings()).centerMe(currentSelectionWindow);
                return;
            } else {
                updateRules(UpgradeType.WaveSpeedInc.ruleFunc);
            }
        }

        if (waypoints.noCheckpoint()) {
            this.state.CheckpointTimer = rules.CheckpointTimerLength;
            var vec2 = H.v3tov2fXZ(this.cm.getPlayer().location);
            var dir = Rand.randV2f(1, true);
            dir.normalizeLocal();
            dir.multLocal(rules.CheckpointDistance);
            waypoints.addCheckpointAt(H.v2tov3fXZ(vec2.add(dir)));
        }

        this.state.update(tpf, rules);
        
        checkCount.setText(Integer.toString(waypoints.totalCheckpoints()));
        timer.setText(Float.toString(this.state.CheckpointTimer));
        ruleWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        ruleWindow.addChild(UiHelper.generateTableOfValues(this.rules.GetProperties()));
        ruleWindow.addChild(UiHelper.generateTableOfValues(this.state.GetProperties()));
        //ruleLabel.setText(this.rules.toString()+"\n|\n"+this.state.toString());

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

        getStateManager().detach(checkpointArrow);
        checkpointArrow = null;
    }

    @Override
    protected void onEnable() {
        this.cm.setEnabled(true);
        this.waveManager.setEnabled(true);
        this.checkpointArrow.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        this.cm.setEnabled(false);
        this.waveManager.setEnabled(false);
        this.checkpointArrow.setEnabled(false);
    }

    public void updateState(Consumer<GameState> func) {
        func.accept(this.state);
    }

    public void updateRules(Consumer<GameRules> func) {
        func.accept(this.rules);
        this.setEnabled(true);

        if (currentSelectionWindow != null) {
            currentSelectionWindow.removeFromParent();
            currentSelectionWindow = null;
        }
    }

    public void updateCar(Consumer<CarDataConst> func) {
        drive.applyChange(new CarDataAdjuster(CarDataAdjustment.asFunc(func)));

        this.setEnabled(true);

        currentSelectionWindow.removeFromParent();
        currentSelectionWindow = null;
    }

    public GameRules getGameRules() {
        return this.rules;
    }
}
