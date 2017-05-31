package car;

import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import helper.H;
import helper.H.Duo;

public abstract class CarData {
	
	public static final String dir = "assets/models/";

	//List of not primitives:
	public VehicleTuning vt = null; //the original code doesn't even use it
	//grip constants
	public WheelData w_flatdata = new WheelDataLat();
	public WheelData w_flongdata = new WheelDataLong();
	
	//model strings (can be xx.obj or xx.blend)
	public String carModel = dir+"car4.obj";
	public String wheelModel = dir+"wheel.obj";
	
	//camera options
	public Vector3f cam_lookAt = new Vector3f(0,1.3f,0); //top of car usually
	public Vector3f cam_offset = new Vector3f(0,2.1f,-6); //where the camera is
	
	public float rollFraction = 0.5f; //1 = full into roll, 0 = no roll
	
	//physical things
	public float mass = 1200; //kg (total)
	public float width = 1.4f; //x size meter, door handle to door handle
	public float height = 1f; //y size meter, roof to ground
	public float length = 4f; //z size meter, from front to back
	
	//wheels axles directions
	public float w_steerAngle = 0.5f; //radians
	
	public Vector3f w_vertical = new Vector3f(0, -1, 0); //vertical
	public Vector3f w_axle = new Vector3f(-1, 0, 0); //horizontal

	public float w_width = 0.15f; //m
	public float w_radius = 0.3f; //m
	public float w_mass = 75; //kg
	public float w_difflock = 0.5f; //percentage speed difference on one axle //TODO not working right
	
	//TODO make front and back independant (maybe even each wheel)
	public Vector3f[] w_pos = new Vector3f[4];
	protected float w_xOff = 0.68f; //wheels x offset (side), meters
	protected float w_yOff = 0f; //wheels y offest (height), meters
	protected float w_zOff = 1.1f; //wheels z offset (front and back), meters
	protected void setw_Pos() {
		w_pos[0] = new Vector3f(w_xOff, w_yOff, w_zOff);
		w_pos[1] = new Vector3f(-w_xOff, w_yOff, w_zOff);
		w_pos[2] = new Vector3f(w_xOff, w_yOff, -w_zOff);
		w_pos[3] = new Vector3f(-w_xOff, w_yOff, -w_zOff);
	}
	
	//suspension
	//see for details: https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
	public float sus_stiffness = 50.0f; //200=f1 car
	public float sus_compValue = 0.5f; //(should be lower than damp)
	public float sus_dampValue = 0.6f;
	public float sus_restLength = 0f;
	
	public float susCompression() { return sus_compValue * 2 * FastMath.sqrt(sus_stiffness); }
	public float susDamping() { return sus_dampValue * 2 * FastMath.sqrt(sus_stiffness); }
	public float sus_maxForce = 50*mass; //TODO '50' is a random number (v2 was 25)
	public float sus_maxTravel = 50; //cms
	
	//jme3 grip constants
	//my physics works with 0f, but feels 'tighter' with it: 1.0f because i don't really have slow physics yet
	public float wheelBasicSlip = 0;
	
	//drag constants
	public float areo_drag = 1.0f; //squared component
	public float areo_lineardrag = 0.02f;
	public float areo_downforce = 0.0f; //no cars start with it

	public float brakeMaxTorque = 4000;
	public Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);
	
	public boolean driveFront = false, driveRear = true; //this would be rear wheel drive
	
	//this one is from the notes, is a ~1999 corvette c6 
	public float[] e_torque = new float[]{0,390,445,460,480,475,360,10}; //starts at 0 rpm, steps every 1000rpm (until done)
		//TODO maybe 500 rpm splits (will get better peaks)
	
	public int auto_gearDown = 2400; //rpm triggering a gear down
	public int auto_gearUp = 5500; //rpm triggering a gear up
	public int e_redline = 6500;
	
	public float e_compression = 0.1f; //is going to be multiplied by the RPM
	public float e_mass = 30; //kg, this is the interia of the engine, 100 is high	
	
	public float trans_effic = 0.75f; //TODO apparently 0.7 is common (power is lost to rotating things)
	public float trans_finaldrive = 3.0f; //helps set the total drive ratio
	public float[] trans_gearRatios = new float[]{-2.9f,3.6f,2.5f,1.8f,1.3f,1.0f,0.74f}; //reverse,gear1,gear2,g3,g4,g5,g6,...
	
	//TODO i found a porsche boxter engine curve:
//	public float[] torque = new float[]{0,223,250,280,300,310,280,245,10};

	public boolean nitro_on = true;
	public float nitro_force = 300; //TODO find a good number (this is torque as far as i can tell)
	public float nitro_rate = 1;
	public float nitro_max = 15;
	
	
	//Constructor (when we don't have model data)
	protected CarData() {
		setw_Pos();
	}
	
	////////////////////////////////////////////////////////
	//usefulMethods
	
	public float e_inertia() { //car internal engine inertia
		float wheels = (w_mass*w_radius*w_radius/2);
		if (driveFront && driveRear) {
			return e_mass + wheels*4;
		}
		return e_mass + wheels*2;
	}
	
	//linear drag component (https://en.wikipedia.org/wiki/Rolling_resistance)
	public float resistance(float gravity) {
		return gravity*mass*areo_lineardrag/w_radius;
	}
	
	//compute the torque at rpm
	//assumed to be a valid and useable rpm value
	public float lerpTorque(int rpm) {
		float RPM = (float)rpm / 1000;
		return H.lerpArray(RPM, this.e_torque);
	} 

	//get the max power and rpm
	public Duo<Float, Float> getMaxPower() {
		float max = 0;
		float maxrpm = 0;
		for (int i = 0; i < e_torque.length; i++) {
			float prevmax = max;
			max = Math.max(max, e_torque[i]*(1000*i)/9549);
			if (prevmax != max) maxrpm = i;
		} //http://www.autospeed.com/cms/article.html?&title=Power-versus-Torque-Part-1&A=108647
		return new Duo<Float, Float>(max, maxrpm*1000);
	}
}


