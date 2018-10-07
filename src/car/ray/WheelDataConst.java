package car.ray;

public class WheelDataConst {
	
	public final String modelName;
	public final float radius;
	public final float mass;
	public final float width;
	
	public final WheelDataTractionConst pjk_lat;
	public final WheelDataTractionConst pjk_long;

	public WheelDataConst(String model, float radius, float mass, float width, WheelDataTractionConst wLat, WheelDataTractionConst wLong) {
		this.modelName = model;
		this.radius = radius;
		this.mass = mass;
		this.width = width;
		this.pjk_lat = wLat;
		this.pjk_long = wLong;
	}
}