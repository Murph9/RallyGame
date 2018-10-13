package car.ray;

import com.jme3.math.FastMath;

public class CarSusDataConst {

	//travel values are relative to wheel offset pos
	public float min_travel; //[-0.3 - 0.3] upper travel length - closer to car
	public float max_travel; //[-0.3 - 0.3] lower travel length - closer to ground
	public float travelTotal() { return max_travel - min_travel; }
	public float preload_force; //[3ish] some weird gravity like unit
	public float stiffness; //[10-200]
	public float max_force; //[50*carMass]
	public float antiroll; //[???+ve] TODO use (en.wikipedia.org/wiki/Anti-roll_bar)
	public float comp; //[0.3] //should be less than relax
	public float relax; //[0.3f]
	public float compression() { return comp * 2 * FastMath.sqrt(stiffness); }
	public float relax() { return relax * 2 * FastMath.sqrt(stiffness); }
	
	public CarSusDataConst() {
		//init some defaults
		min_travel = -0.25f;
		max_travel = 0f;
		preload_force = 3f/4;
		stiffness = 20;
		max_force = 50*1000;
		antiroll = 0;
		comp = 0.2f;
		relax = 0.3f;
	}
}
