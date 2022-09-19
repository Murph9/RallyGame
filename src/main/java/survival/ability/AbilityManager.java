package survival.ability;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;

import rallygame.car.CarManager;

public class AbilityManager extends BaseAppState {

    public static final String TYPE_EXPLODE = "explode";
    public static final String TYPE_STOP = "stop";
    public static final String TYPE_FREEZE = "freeze";
    public static final String TYPE_BLINK = "blink";

    private final List<Ability> abilities = new LinkedList<>();
    private final AbilityListener listener = new AbilityListener(this);

    private AbilityUI ui;

    @Override
    protected void initialize(Application app) {
        // to start with abilities set them in the state manager

        app.getInputManager().addRawInputListener(listener);

        ui = new AbilityUI();
        getStateManager().attach(ui);
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(ui);
        ui = null;

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

    public List<Ability> getAbilities() {
        return this.abilities;
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
        this.handlePress(string, null);
    }
    public void handlePress(String string, Vector3f dir) {
        switch (string) {
            case TYPE_EXPLODE:
                triggerAbility(ExplodeAbility.class);
                break;
            case TYPE_STOP:
                triggerAbility(StopAbility.class);
                break;
            case TYPE_FREEZE:
                triggerAbility(FreezeAbility.class);
                break;
            case TYPE_BLINK:
                Vector3f blinkDir = null;
                if (dir == null) {
                    blinkDir = getState(CarManager.class).getPlayer().forward.clone();
                } else {
                    // align with car
                    blinkDir = getState(CarManager.class).getPlayer().rotation.mult(dir);
                    blinkDir.y = 0; // no going into the ground please
                }
                var blink = ((BlinkAbility)GetAbility(BlinkAbility.class));
                if (blink != null) {
                    blink.setDirection(blinkDir);
                    triggerAbility(BlinkAbility.class);
                }
            default:
                break;
        }
    }

    private void triggerAbility(Class<? extends Ability> type) {
        var ab = GetAbility(type);
        if (ab == null)
            return;
        if (ab.ready() < 0 && ab.getClass().isAssignableFrom(type)) {
            var player = getState(CarManager.class).getPlayer();
            ab.trigger(getStateManager(), player);
        }
    }
    private Ability GetAbility(Class<? extends Ability> type) {
        for (var ab: abilities) {
            if (type.isAssignableFrom(ab.getClass()))
                return ab;
        }
        return null;
    }
}
