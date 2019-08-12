package car.ray;

public class WheelDataConst {
	
	public float maxLong;
	public float maxLat;
	public float maxLongSat;
	public float maxLatSat;

	public String modelName;
	public float radius;
	public float mass;
	public float width;
	
	public WheelDataTractionConst pjk_lat;
	public WheelDataTractionConst pjk_long;
	
	public WheelDataTractionConst pjk_lat_sat; //self aligning torque
	public WheelDataTractionConst pjk_long_sat; //self aligning torque

	public WheelDataConst() {}
}