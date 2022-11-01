package survival.ability;

import java.util.AbstractMap;
import java.util.Map;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;

import rallygame.car.CarManager;
import rallygame.car.ray.RayCarControl;

public class GravityAbility extends TimedAbility {
    // TODO would like this ability to be more like 'more grip' but hey

    private float length;
    private Vector3f gravTemp;

    public GravityAbility() {
        length = 2.5f;
        abilityTimerMax = 10;
        abilityTimer = abilityTimerMax;
    }

    @Override
    public boolean update(AppStateManager sm, float tpf) {
        if (gravTemp != null && abilityTimer <= 0) {
            sm.getState(CarManager.class).getPlayer().getPhysicsObject().setGravity(gravTemp);
            gravTemp = null;
        }

        return super.update(sm, tpf);
    }

    public void changeLength(float diff) {
        this.length += diff; 
    }

    @Override
    public void trigger(AppStateManager sm, RayCarControl player) {
        this.abilityTimer = this.abilityTimerMax;
        
        gravTemp = player.getPhysicsObject().getGravity();
        player.getPhysicsObject().setGravity(gravTemp.mult(2f));
    }

    @Override
    public Map.Entry<String, Object> GetProperties() {
        return new AbstractMap.SimpleEntry<>("More Gravity Ability", new Float[] { length, abilityTimer, abilityTimerMax, ready() });
    }
}