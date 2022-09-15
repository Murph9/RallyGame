package survival.ability;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import rallygame.car.CarManager;
import survival.wave.WaveManager;

public class AbilityManager extends BaseAppState {

    private final List<Ability> abilities = new LinkedList<>();

    @Override
    protected void initialize(Application app) {
        abilities.add(new ExplodeAbility());
    }

    @Override
    protected void cleanup(Application app) {
        abilities.clear();
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    @Override
    public void update(float tpf) {
        //TODO use to get data var state = getState(StateManager.class).getState();

        for (var ab: abilities) {
            var result = ab.update(tpf);
            if (result && ab instanceof ExplodeAbility) {
                // a little bit hard coded
                var value = ((ExplodeAbility)ab).getStrength();
                if (value > 0) { // TODO ability manager maybe?, with controller
                    var cm = getState(CarManager.class);
                    getState(WaveManager.class).applyForceFrom(cm.getPlayer().location, value, 50);
                }
            }
        }
    }

    public Map<String, Object> GetProperties() {
        var map = new HashMap<String, Object>();
        for (var a: abilities) {
            var props = a.GetProperties();
            map.put(props.getKey(), props.getValue());
        }
        return map;
    }
}
