package car.ray;

import com.jme3.math.FastMath;

public class CarSusDataConst {

	//travel values are relative to wheel offset pos
	public float min_travel; //[-0.3 - 0.3] upper travel length - closer to car
	public float max_travel; //[-0.3 - 0.3] lower travel length - closer to ground
	public float travelTotal() { return max_travel - min_travel; }
	
	public float preload_force; //kg/mm [3ish]
	public float stiffness; //kg/mm [10-200]
	public float max_force; //kg/mm [50*carMass]
	public float antiroll; //[???+ve] TODO use (en.wikipedia.org/wiki/Anti-roll_bar)
	
	public float comp; //[0.2] //should be less than relax
	public float relax; //[0.3f]
	public float compression() { return comp * 2 * FastMath.sqrt(stiffness); }
	public float relax() { return relax * 2 * FastMath.sqrt(stiffness); }
	
	public CarSusDataConst() {}
}
