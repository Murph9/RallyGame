package survival;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import survival.upgrade.UpgradeType;

public class StateManager extends BaseAppState {

    private final GameState state;
    private final List<UpgradeType> types;

    public StateManager() {
        state = GameState.generate();
        types = new LinkedList<>();
    }

    @Override
    protected void initialize(Application app) {
        
    }

    @Override
    protected void cleanup(Application app) {
        
    }

    public void add(UpgradeType type) {
        types.add(type);

        if (type.carFunc != null) {
            // the car mods must be cumulative for now
            var adjustments = new LinkedList<CarDataAdjustment>();
            for (var t: types) {
                if (t.carFunc != null)
                    adjustments.add(CarDataAdjustment.asFunc(t.carFunc));
            }
            getState(Drive.class).applyChange(new CarDataAdjuster(adjustments));
        }
        
        if (type.stateFunc != null) {
            type.stateFunc.accept(this.state);
        }
    }

    @Override
    public void update(float tpf) {
        if (!this.isEnabled()) return;

        this.state.update(tpf);
    }

    @Override
    protected void onEnable() {
        // TODO start listening to keys
    }

    @Override
    protected void onDisable() {
        // TODO stop listening to keys
    }

    public GameState getState() {
        return this.state;
    }

    public List<UpgradeType> getUpgrades() {
        return new LinkedList<>(this.types);
    }
}
