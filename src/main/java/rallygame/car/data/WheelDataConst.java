package rallygame.car.data;

import java.io.Serializable;

public class WheelDataConst implements Serializable {
	
	public float maxLong;
	public float maxLat;

	public String modelName;
	public float radius;
	public float mass;
	public float width;
	
	public WheelDataTractionConst pjk_lat;
	public WheelDataTractionConst pjk_long;
}
