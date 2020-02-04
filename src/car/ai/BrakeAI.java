package car.ai;

import car.ray.RayCarControl;

/**
 * BrakeAI, just brakes
 */
public class BrakeAI extends CarAI {

    public BrakeAI(RayCarControl car) {
        super(car);
    }

    @Override
    public void update(float tpf) {
        justBrake();
    }
}
