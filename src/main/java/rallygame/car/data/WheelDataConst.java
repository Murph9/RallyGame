package rallygame.car.data;

import java.io.Serializable;

public class WheelDataConst implements Serializable {

	public String modelName;

	public float radius;
	public float mass;
	public float width;

    public String tractionType;
    public WheelTraction traction;
}
