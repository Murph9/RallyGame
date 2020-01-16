package car.ai;

import car.ray.RayCarControl;
import game.DebugAppState;
import car.ray.RayCar.GripHelper;
import service.ray.IPhysicsRaycaster;

public class NewCarAI implements ICarAI {

    private final RayCarControl car;
    private final ICarAIAction[] actions;
    private final float BEST_LAT_FORCE;
    private final float BEST_LONG_FORCE;

    private IPhysicsRaycaster raycaster;
    private DebugAppState debug;

    public NewCarAI(RayCarControl car, ICarAIAction... actions) {
        this.car = car;
        this.actions = actions;

        BEST_LAT_FORCE = GripHelper.calcMaxLoad(car.getCarData().wheelData[0].pjk_lat);
        BEST_LONG_FORCE = GripHelper.calcMaxLoad(car.getCarData().wheelData[0].pjk_long);

        // ignore all turning speed factor code for AIs
        car.onAction("IgnoreSteeringSpeedFactor", true, 1);
    }

    @Override
    public void setPhysicsRaycaster(IPhysicsRaycaster raycaster) {
        this.raycaster = raycaster;
    }
    
    @Override
    public void setDebugAppState(DebugAppState debug) {
        this.debug = debug;
    }

    protected void onEvent(String act, boolean ifdown) {
        onEvent(act, ifdown, ifdown ? 1 : 0);
    }

    protected void onEvent(String act, boolean ifdown, float amnt) {
        car.onAction(act, ifdown, amnt);
    }

    @Override
    public void update(float tpf) {
        NewCarAIData data = new NewCarAIData(car, raycaster, debug, BEST_LAT_FORCE, BEST_LONG_FORCE);

        for (ICarAIAction action : actions)
            if (action.update(tpf, this, data))
                break;
    }
}

class NewCarAIData {
    public final RayCarControl car;
    public final IPhysicsRaycaster raycaster;
    public final DebugAppState debug;
    public final float BEST_LAT_FORCE;
    public final float BEST_LONG_FORCE;

    public NewCarAIData(RayCarControl car, IPhysicsRaycaster raycaster, DebugAppState debug, float BEST_LAT_FORCE,
            float BEST_LONG_FORCE) {
        this.car = car;
        this.raycaster = raycaster;
        this.debug = debug;
        this.BEST_LAT_FORCE = BEST_LAT_FORCE;
        this.BEST_LONG_FORCE = BEST_LONG_FORCE;
    }
}

interface ICarAIAction {
    /** do i run */
    void setEnabled(boolean value);
    /** returns if quit */
    boolean update(float tpf, NewCarAI ai, NewCarAIData data);
}

class JustBrakeCarAction implements ICarAIAction {

    private boolean enabled = true;

    @Override
    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public boolean update(float tpf, NewCarAI ai, NewCarAIData data) {
        if (!enabled)
            return false;
        
        ai.onEvent("Left", false);
        ai.onEvent("Right", false);

        ai.onEvent("Accel", false);
        ai.onEvent("Brake", true);

        return false;
    }
}
