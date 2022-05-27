package survival;

import java.util.Map;
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
import survival.upgrade.SelectionUI;
import survival.upgrade.UpgradeManager;
import survival.upgrade.UpgradeType;
import survival.wave.WaveManager;

public class DodgeGameManager extends BaseAppState {

    private final GameState state;
    private final boolean offerUpgrades;
    private final Drive drive;

    private Node uiRootNode;

    private CarManager cm;
    private WaveManager waveManager;
    private BasicWaypointProgress waypoints;
    private CheckpointArrow checkpointArrow;
    private UpgradeManager upgrades;

    private Container currentSelectionWindow;
    private Container ruleWindow;
    private Container currentStateWindow;

    public DodgeGameManager(Drive drive, boolean offerUpgrades) {
        this.drive = drive;
        this.state = GameState.generate();
        this.offerUpgrades = offerUpgrades;
    }

    @Override
    protected void initialize(Application app) {
        cm = getState(CarManager.class);

        waypoints = new BasicWaypointProgress(cm.getPlayer());
        getStateManager().attach(waypoints);

        waveManager = new WaveManager(cm.getPlayer());
        getStateManager().attach(waveManager);

        upgrades = new UpgradeManager(cm.getPlayer());
        getStateManager().attach(upgrades);

        uiRootNode = ((SimpleApplication) getApplication()).getGuiNode();

        currentStateWindow = new Container();
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
        
        var screen = new Screen(getApplication().getContext().getSettings());

        if (this.state.CheckpointTimer <= 0 || this.state.PlayerHealth <= 0) {
            var loseWindow = new Container();
            loseWindow.addChild(new Label("You lost by either running of time or health.\nPlease restart game to replay."));
            uiRootNode.attachChild(loseWindow);
            screen.centerMe(loseWindow);
            this.setEnabled(false);
            return;
        }

        if (waypoints.hitACheckpoint()) {
            if (offerUpgrades) {
                this.setEnabled(false);
                currentSelectionWindow = SelectionUI.GenerateSelectionUI(this, UpgradeType.values());
                uiRootNode.attachChild(currentSelectionWindow);
                screen.centerMe(currentSelectionWindow);
                return;
            } else {
                updateState(UpgradeType.WaveSpeedInc.ruleFunc);
            }
        }

        if (waypoints.noCheckpoint()) {
            this.state.CheckpointTimer = state.CheckpointTimerLength;
            var vec2 = H.v3tov2fXZ(this.cm.getPlayer().location);
            var dir = Rand.randV2f(1, true);
            dir.normalizeLocal();
            dir.multLocal(state.CheckpointDistance);
            waypoints.addCheckpointAt(H.v2tov3fXZ(vec2.add(dir)));
        }

        this.state.update(tpf);
        
        currentStateWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        currentStateWindow.addChild(UiHelper.generateTableOfValues(Map.of("Checkpoints:", Integer.toString(waypoints.totalCheckpoints()), "Fuel:", cm.getPlayer().fuel())));
        screen.topCenterMe(currentStateWindow);

        ruleWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        ruleWindow.addChild(UiHelper.generateTableOfValues(this.state.GetProperties()));

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

        getStateManager().detach(upgrades);
        upgrades = null;
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

    public GameState getGameRules() {
        return this.state;
    }
}

