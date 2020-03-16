package rallygame.car.ray;

public interface ICarControlled {

    void setSteerLeft(float value);
    void setSteerRight(float value);
    void setAccel(float value);
    void setBraking(float value);
    void setNitro(boolean value);
    void jump();
    void setHandbrake(boolean value);
    void reverse(boolean value);
    void flip();
    void reset();
    void ignoreSpeedFactor(boolean value);
    void ignoreTractionModel(boolean value);
}
