package rallygame.car.ray;

import com.jme3.math.Vector3f;

public interface ICarPowered {
    float accelCurrent();
    float brakeCurrent();
    boolean ifHandbrake();
    int curGear();
    float nitro();
    float fuel();
    float steeringCurrent();
    int curRPM();
    float driftAngle();
    Vector3f getPlanarGForce();
    float getWheelTorque(int w_id);
}