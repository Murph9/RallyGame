package car.ray;

public interface ICarPowered {
    float accelCurrent();
    float brakeCurrent();
    boolean ifHandbrake();
    int curGear();
    float nitro();
    float steeringCurrent();
    int curRPM();
    float driftAngle();
}