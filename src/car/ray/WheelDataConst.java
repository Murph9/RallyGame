package car.ray;

public class WheelDataConst {
	
	public final String modelName;
	public final float radius;
	public final float mass;
	public final float width;
	
	public WheelDataTractionConst pjk_lat;
	public WheelDataTractionConst pjk_long;
	
	public WheelDataTractionConst pjk_lat_sat; //self aligning torque
	public WheelDataTractionConst pjk_long_sat; //self aligning torque

	public WheelDataConst(String model, float radius, float mass, float width) {
		this.modelName = model;
		this.radius = radius;
		this.mass = mass;
		this.width = width;
	}
}