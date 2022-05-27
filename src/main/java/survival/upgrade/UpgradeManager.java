package survival.upgrade;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

// because they need an app state for actions
public class UpgradeManager extends BaseAppState {

    public UpgradeManager() {
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

        // TODO
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
