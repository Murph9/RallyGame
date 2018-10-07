package car.ray;

import com.jme3.math.Vector3f;

import helper.H;

public class RayWheel {

	private static float ERROR = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals
	
	protected final int num;
	protected final WheelDataConst data;
	
	public boolean inContact;
	public Vector3f curBasePosWorld;
	public float susDiffLength;
	public float susForce;
	
	public float steering;
	public float radSec;
	public float skidFraction; //was 'skid'
	public Vector3f gripDir;
	
	public final float maxLong;
	public final float maxLat;
	
	public RayWheel(int num, WheelDataConst data, Vector3f offset) {
		this.num = num;
		this.data = data;
		
		//generate the slip* max force from the car wheel data
		maxLat = RayCar.GripHelper.calcSlipMax(data.pjk_lat, ERROR);
		maxLong = RayCar.GripHelper.calcSlipMax(data.pjk_long, ERROR);
		
		try {	
			if (Float.isNaN(maxLat))
				throw new Exception("maxlat was: '" + maxLat +"'.");
			if (Float.isNaN(maxLong))
				throw new Exception("maxlong was: '" + maxLong +"'.");
		} catch (Exception e) {
			e.printStackTrace();
			H.p("error in calculating max(lat|long) values of: " + num);
			System.exit(1);
		}
	}
	
	//[softly] should this class do anything else?
}
