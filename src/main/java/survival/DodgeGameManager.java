package survival;

import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.CarManager;
import rallygame.drive.PauseState;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.service.Screen;
import rallygame.service.checkpoint.BasicWaypointProgress;
import rallygame.service.checkpoint.CheckpointArrow;
import survival.upgrade.SelectionUI;
import survival.upgrade.UpgradeType;
import survival.wave.WaveManager;

public class DodgeGameManager extends BaseAppState implements PauseState.ICallback {

    private final boolean offerUpgrades;
    private final String version;
    
    private Node uiRootNode;
    private Container versionWindow;

    private CarManager cm;
    private WaveManager waveManager;
    private BasicWaypointProgress waypoints;
    private CheckpointArrow checkpointArrow;
    private StateManager stateManager;

    private Container currentSelectionWindow;
    private Container ruleWindow;
    private Container currentStateWindow;

    public DodgeGameManager(boolean offerUpgrades, String version) {
        this.offerUpgrades = offerUpgrades;
        this.version = version;
    }

    @Override
    protected void initialize(Application app) {
        cm = getState(CarManager.class);

        waypoints = new BasicWaypointProgress(cm.getPlayer());
        getStateManager().attach(waypoints);

        waveManager = new WaveManager(cm.getPlayer());
        getStateManager().attach(waveManager);

        stateManager = new StateManager();
        getStateManager().attach(stateManager);

        uiRootNode = ((SimpleApplication) getApplication()).getGuiNode();

        currentStateWindow = new Container();
        uiRootNode.attachChild(currentStateWindow);
        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);

        ruleWindow = new Container();
        uiRootNode.attachChild(ruleWindow);
        new Screen(getApplication().getContext().getSettings()).topLeftMe(ruleWindow);

        checkpointArrow = new CheckpointArrow(cm.getPlayer(), (__) -> this.waypoints.getCurrentPos());
        getStateManager().attach(checkpointArrow);

        versionWindow = new Container();
        versionWindow.addChild(new Label("WASD or Arrows to move\nGet checkpoints"));
        versionWindow.addChild(new Label("Version: " + this.version));
        uiRootNode.attachChild(versionWindow);
        new Screen(app.getContext().getSettings()).topRightMe(versionWindow);
    }

    @Override
    public void update(float tpf) {
        if (!this.isEnabled())
            return;
        
        var screen = new Screen(getApplication().getContext().getSettings());

        var state = this.stateManager.getState();
        if (state.gameOver()) {
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
                upgrade(UpgradeType.WaveSpeedInc);
            }
        }

        if (waypoints.noCheckpoint()) {
            state.CheckpointTimer = state.CheckpointTimerLength;
            var vec2 = H.v3tov2fXZ(this.cm.getPlayer().location);
            var dir = Rand.randV2f(1, true);
            dir.normalizeLocal();
            dir.multLocal(state.CheckpointDistance);
            waypoints.addCheckpointAt(H.v2tov3fXZ(vec2.add(dir)));
        }
        
        currentStateWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        currentStateWindow.addChild(UiHelper.generateTableOfValues(Map.of("Checkpoints:", waypoints.totalCheckpoints(), "Fuel:", cm.getPlayer().fuel())));
        screen.topCenterMe(currentStateWindow);

        ruleWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        ruleWindow.addChild(UiHelper.generateTableOfValues(state.GetProperties()));
        ruleWindow.addChild(UiHelper.generateTableOfValues(stateManager.getUpgrades()));

        super.update(tpf);
    }

    @Override
    protected void cleanup(Application app) {
        cm = null;

        versionWindow.removeFromParent();
        versionWindow = null;

        uiRootNode.removeFromParent();
        uiRootNode = null;

        getStateManager().detach(waypoints);
        waypoints = null;

        getStateManager().detach(waveManager);
        waveManager = null;

        getStateManager().detach(checkpointArrow);
        checkpointArrow = null;

        getStateManager().detach(stateManager);
        stateManager = null;

        getStateManager().detach(getState(Drive.class));
    }

    @Override
    protected void onEnable() {
        getState(Drive.class).setEnabled(true);
        this.cm.setEnabled(true);
        this.waveManager.setEnabled(true);
        this.checkpointArrow.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        getState(Drive.class).setEnabled(false);
        this.cm.setEnabled(false);
        this.waveManager.setEnabled(false);
        this.checkpointArrow.setEnabled(false);
    }

    public void upgrade(UpgradeType type) {
        var curFuel = this.cm.getPlayer().fuel();
        
        this.stateManager.add(type);
        this.setEnabled(true);
        if (type.carFunc != null) {
            // this would reset fuel value so we have to set it manually...
            this.cm.getPlayer().setFuel(curFuel);
            //TODO clean this weird fuel value fix
        }

        if (currentSelectionWindow != null) {
            currentSelectionWindow.removeFromParent();
            currentSelectionWindow = null;
        }
    }

    
    
    @Override
    public void pauseState(boolean value) {
        this.setEnabled(value);
    }

    @Override
    public void quit() {
        getState(Drive.class).next();
    }
}
