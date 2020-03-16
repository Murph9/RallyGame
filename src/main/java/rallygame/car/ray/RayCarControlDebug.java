package rallygame.car.ray;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import rallygame.game.DebugAppState;

public class RayCarControlDebug {

    private final boolean DEBUG;
    private final boolean DEBUG_SUS;
    private final boolean DEBUG_SUS2;
    private final boolean DEBUG_TRACTION;
    private final boolean DEBUG_DRAG;

    private final RayCarControl car;

    public RayCarControlDebug(RayCarControl car, boolean ifDebug) {
        this.car = car;
        this.DEBUG = ifDebug;
        this.DEBUG_SUS = this.DEBUG || false;
        this.DEBUG_SUS2 = this.DEBUG || false;
        this.DEBUG_TRACTION = this.DEBUG || false;
        this.DEBUG_DRAG = this.DEBUG || false;
    }

    public void update(Application app) {
        
        if (DEBUG) {
            Quaternion w_angle = car.rbc.getPhysicsRotation();
            car.doForEachWheel((w_id) -> {

                float susTravel = car.carData.susByWheelNum(w_id).travelTotal();

                DebugAppState debug = app.getStateManager().getState(DebugAppState.class);

                if (DEBUG_SUS) {
                    debug.drawArrow("sus_wheel_radius" + w_id, ColorRGBA.Blue, car.wheels[w_id].rayStartWorld,
                            car.wheels[w_id].rayDirWorld.normalize().mult(car.carData.wheelData[w_id].radius));
                    debug.drawArrow("sus" + w_id, ColorRGBA.Cyan,
                            car.wheels[w_id].rayStartWorld
                                    .add(car.wheels[w_id].rayDirWorld.normalize().mult(car.carData.wheelData[w_id].radius)),
                            car.wheels[w_id].rayDirWorld.normalize().mult(susTravel));
                    debug.drawBox("col_point" + w_id, ColorRGBA.Red, car.wheels[w_id].curBasePosWorld, 0.01f);
                }

                if (DEBUG_SUS2) {
                    debug.drawArrow("normalforcearrow" + w_id, ColorRGBA.Black, car.wheels[w_id].curBasePosWorld,
                            car.wheels[w_id].hitNormalInWorld);
                }

                if (DEBUG_TRACTION) {
                    debug.drawArrow("tractionDir" + w_id, ColorRGBA.White, car.wheels[w_id].curBasePosWorld,
                            w_angle.mult(car.wheels[w_id].gripDir.mult(1 / car.carData.mass)));
                }

                Vector3f w_pos = car.rbc.getPhysicsLocation();
                if (DEBUG_DRAG) {
                    debug.drawArrow("dragarrow", ColorRGBA.Black, w_pos, car.dragDir);
                }
            });
        }
    }
}