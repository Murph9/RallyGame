package game;

import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public abstract class CarData {
	
	public static final String dir = "assets/models/";

	//List of not primitives:
	VehicleTuning vt = null; //TODO the original code doesn't even use it
	//grip constants
	CarWheelData wheellatdata = new NormalLatData();
	CarWheelData wheellongdata = new NormalLongData();
	
	//model strings (can be xx.obj or xx.blend)
	String carModel = dir+"car4.obj";
	String wheelModel = dir+"wheel.obj";
	
	//camera options
	Vector3f LOOK_AT = new Vector3f(0,0.5f,0); //top of car usually
	Vector3f CAM_OFFSET = new Vector3f(0,3,-7); //where the camera is
	
	//physical things
	float mass = 1200; //kg (total)
	float width = 1.4f; //x size meter, door handle to door handle
	float height = 1f; //y size meter, roof to ground
	float length = 3f; //z size meter, from front to back
	
	float steerAngle = 0.5f; //radians
	float steerFactor = 10f; //TODO meant to change the max turn angle based on speed and this
	
	//wheels axles directions
	Vector3f wheelDirection = new Vector3f(0, -1, 0); //vertical
	Vector3f wheelAxle = new Vector3f(-1, 0, 0); //horizontal

	float wheelWidth = 0.15f; //m
	float wheelRadius = 0.3f; //m
	float wheelMass = 75; //kg
	
	float engineMass = 100;
	float engineWheelInertia() {
		float wheels = (wheelMass*wheelRadius*wheelRadius/2);
		if (driveFront && driveRear) {
			return engineMass + wheels*4;
		}
		return engineMass + wheels*2;
	}
	float rollFraction = 0.5f; //1 = full into roll, 0 = no roll
	
	//TODO make front and back independant (maybe even each wheel)
	float wheel_xOff = 0.68f; //wheels x offset (side), meters
	float wheel_yOff = 0f; //wheels y offest (height), meters
	float wheel_zOff = 1.1f; //wheels z offset (front and back), meters
	
	//suspension values for wheels
	//see for details: https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
	float stiffness = 60.0f; //200=f1 car
	float compValue = 0.6f; //(should be lower than damp)
	float dampValue = 0.7f;
	float restLength = 0f;
	
	float susCompression() { return compValue * 2 * FastMath.sqrt(stiffness); }
	float susDamping() { return dampValue * 2 * FastMath.sqrt(stiffness); }
	float maxSusForce = 25*mass; //TODO '25' is a random number.
	float maxSusTravel = 50; //cms
	
	//jme3 grip constants
	//my physics works with 0f, but feels tighter with: 1.0f
	float wheelBasicSlip = 0;
	
	//drag constants
	float DRAG = 1.5f; //squared component
	float RESISTANCE = 15.0f; //linear component

	float brakeMaxTorque = 4000; 
	Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);
	
	boolean driveFront = false, driveRear = true; //this would be rear wheel drive
	
	//this one is from the notes, is a ~1999 corvette c6 
	float[] torque = new float[]{0,390,445,460,480,475,360,10}; //starts at 0 rpm, steps every 1000rpm (until done)
		//TODO maybe 500 rpm splits (will get better peaks)
	
	float gearDown = 2400; //rpm triggering a gear down
	float gearUp = 5500;
	float redline = 6500;
	
	float engineCompression = 0.2f; //is going to be multiplied by the RPM
	
	float transEffic = 0.75f; //TODO apparently 0.7 is common (power is lost to rotating things)
	float diffRatio = 2.5f; //helps set the total drive ratio
	float[] gearRatios = new float[]{-2.9f,3.40f,2.5f,1.8f,1.3f,1.0f,0.74f};; //reverse,gear1,gear2,g3,g4,g5,g6,...
	
	//TODO i found a porsche boxter engine curve:
//	float[] torque = new float[]{0,223,250,280,300,310,280,245,10};

	
	
	///////////////////
	//usefulMethods
	
	//get the max power
	float getMaxPower() {
		float max = 0;
		for (int i = 0; i < torque.length; i++) {
			max = Math.max(max, torque[i]*(1000*i)/9549);
		} //http://www.autospeed.com/cms/article.html?&title=Power-versus-Torque-Part-1&A=108647
		return max;
	}
}


