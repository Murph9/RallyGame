package survival.ability;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import rallygame.car.CarManager;

public class AbilityManager extends BaseAppState {

    private final List<Ability> abilities = new LinkedList<>();
    private final AbilityListener listener = new AbilityListener(this);

    @Override
    protected void initialize(Application app) {
        // to start with abilities set them in the state

        app.getInputManager().addRawInputListener(listener);
    }

    @Override
    protected void cleanup(Application app) {
        abilities.clear();
        app.getInputManager().removeRawInputListener(listener);
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    @Override
    public void update(float tpf) {
        for (var ab: abilities) {
            ab.update(tpf);
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

    public void accept(Consumer<List<Ability>> consumer) {
        consumer.accept(this.abilities);
    }

    public void handlePress(String string) {
        switch (string) {
            case Ability.TYPE_EXPLODE:
                triggerAbility(ExplodeAbility.class);
                break;
            case Ability.TYPE_STOP:
                triggerAbility(StopAbility.class);
            default:
                break;
        }
    }

    private void triggerAbility(Class<?> type) {
        for (var ab: abilities) {
            if (ab.ready() < 0 && ab.getClass().isAssignableFrom(type)) {
                var player = getState(CarManager.class).getPlayer();
                ab.trigger(getStateManager(), player);
                break;
            }
        }
    }
}
