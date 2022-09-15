package survival;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import survival.upgrade.CarDataConstUpgrade;
import survival.upgrade.GameStateUpgrade;
import survival.upgrade.Upgrade;

public class StateManager extends BaseAppState {

    private final GameState state;
    private final List<Upgrade<?>> types;

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

    public void add(Upgrade<?> type) {
        types.add(type);

        if (type instanceof CarDataConstUpgrade) {
            // the car mods must be cumulative for now, so we get all of them and apply
            var adjustments = new LinkedList<CarDataAdjustment>();
            for (var t: types) {
                if (t instanceof CarDataConstUpgrade) {
                    var t2 = (CarDataConstUpgrade)t;
                    adjustments.add(CarDataAdjustment.asFunc(t2.get()));
                }
            }
            getState(Drive.class).applyChange(new CarDataAdjuster(adjustments));
        }

        if (type instanceof GameStateUpgrade) {
            ((GameStateUpgrade)type).accept(this.state);
        }
    }

    @Override
    public void update(float tpf) {
        if (!this.isEnabled()) return;

        this.state.update(tpf);
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    public GameState getState() {
        return this.state;
    }

    public List<Upgrade<?>> getUpgrades() {
        return new LinkedList<>(this.types);
    }
}
