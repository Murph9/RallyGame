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
import survival.ability.AbilityManager;
import survival.hotmenu.HotMenu;
import survival.upgrade.GameStateUpgrade;
import survival.upgrade.Upgrade;
import survival.upgrade.UpgradeList;
import survival.wave.WaveManager;

public class DodgeGameManager extends BaseAppState implements PauseState.ICallback {

    private static final int checkpointCountToGetUpgrade = 3;

    private final boolean offerUpgrades;
    private final String version;
    
    private Node uiRootNode;
    private Container versionWindow;

    private CarManager cm;
    private WaveManager waveManager;
    private BasicWaypointProgress waypoints;
    private CheckpointArrow checkpointArrow;
    private StateManager stateManager;
    private HotMenu hotMenu;
    private AbilityManager abilityManager;

    private Container ruleWindow;
    private Container currentStateWindow;

    public DodgeGameManager(boolean offerUpgrades, String version) {
        this.offerUpgrades = offerUpgrades;
        this.version = version;
    }

    @Override
    protected void initialize(Application app) {
        cm = getState(CarManager.class);

        waypoints = new BasicWaypointProgress(cm.getPlayer(), checkpointCountToGetUpgrade);
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
        versionWindow.addChild(new Label("WASD or Arrows to move\nGet checkpoints\nIJKL to navigate the upgrade menu"));
        versionWindow.addChild(new Label("Version: " + this.version));
        uiRootNode.attachChild(versionWindow);
        new Screen(app.getContext().getSettings()).topRightMe(versionWindow);

        hotMenu = new HotMenu();
        getStateManager().attach(hotMenu);

        abilityManager = new AbilityManager();
        getStateManager().attach(abilityManager);
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


        int checkpointsHit = waypoints.checkpointHitsLeft();
        for (int i = 0; i< checkpointsHit; i++) {
            if (state.WaveSpeed > 1f)
                upgrade(GameStateUpgrade.CubeCountInc);
        }
        
        int upgrades = waypoints.hasUpgradeReady();
        if (upgrades > 0) {
            if (offerUpgrades) {
                this.setEnabled(false);
                // TODO the hotmenu doesn't have to pause the game to work
                hotMenu.addOptions(UpgradeList.AllPositiveApplies(getState(StateManager.class).getUpgrades()));
                return;
            }
        }

        if (waypoints.noCheckpointLoaded()) {
            state.CheckpointTimer = state.CheckpointTimerLength;
            var vec2 = H.v3tov2fXZ(this.cm.getPlayer().location);
            var dir = Rand.randV2f(1, true);
            dir.normalizeLocal();
            dir.multLocal(state.CheckpointDistance);
            waypoints.addCheckpointAt(H.v2tov3fXZ(vec2.add(dir)));
        }
        
        currentStateWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        Map<String, Object> map = Map.of("Checkpoints:", waypoints.totalCheckpoints(), "Upgrades:", waypoints.totalUpgrades());
        currentStateWindow.addChild(UiHelper.generateTableOfValues(map));
        screen.topCenterMe(currentStateWindow);

        ruleWindow.getChildren().stream().forEach(x -> x.removeFromParent());
        ruleWindow.addChild(UiHelper.generateTableOfValues(state.GetProperties()));
        ruleWindow.addChild(UiHelper.generateTableOfValues(abilityManager.GetProperties()));
        ruleWindow.addChild(UiHelper.generateTableOfValues(stateManager.getUpgrades()));

        super.update(tpf);
    }

    @Override
    protected void cleanup(Application app) {
        cm = null;

        getStateManager().detach(abilityManager);
        abilityManager = null;

        getStateManager().detach(hotMenu);
        hotMenu = null;

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
        Drive drive = getState(Drive.class);
        if (drive != null)
            drive.setEnabled(false);
        
        this.cm.setEnabled(false);
        this.waveManager.setEnabled(false);
        this.checkpointArrow.setEnabled(false);
    }

    public void upgrade(Upgrade<?> type) {        
        this.stateManager.add(type);
        this.setEnabled(true);
        hotMenu.removeAllOptions();
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
