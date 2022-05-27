package survival.upgrade;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import rallygame.car.ray.RayCarControl;

// because they need an app state for actions
public class UpgradeManager extends BaseAppState {

    private final RayCarControl player;

    public UpgradeManager(RayCarControl player) {
        this.player = player;
    }

    @Override
    protected void initialize(Application app) {
        
    }

    @Override
    protected void cleanup(Application app) {
        
    }

    @Override
    public void update(float tpf) {
        if (!this.isEnabled()) return;

        
    }

    @Override
    protected void onEnable() {
        // TODO start listening to keys
    }

    @Override
    protected void onDisable() {
        // TODO stop listening to keys
    }
}
