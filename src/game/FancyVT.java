package game;

import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public abstract class FancyVT {
	
	static final String dir = "assets/models/";

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
	float steerFactor = 10f; //TODO ??
	
	//wheels
	Vector3f wheelDirection = new Vector3f(0, -1, 0); //vertical
	Vector3f wheelAxle = new Vector3f(-1, 0, 0); //horizontal

	float wheelWidth = 0.15f; //m
	float wheelRadius = 0.3f; //m
	float wheelMass = 75; //kg
	
	float engineMass = 40;
	float engineInertia() {
		float wheels = (wheelMass*wheelRadius*wheelRadius/2);
		if (driveFront && driveRear) {
			return engineMass + wheels*4;
		}
		return engineMass + wheels*2;
	}
	float rollFraction = 0.5f; //1 = full into roll, 0 = no roll
	
	//TODO make front and back independant
	float wheel_xOff = 0.68f; //wheels x offset (side), meters
	float wheel_yOff = 0f; //wheels y offest (height), meters
	float wheel_zOff = 1.1f; //wheels z offset (front and back), meters
	
	//suspension values for wheels
	//see for details: https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
	float stiffness = 60.0f; //200=f1 car
	float compValue = 0.6f; //(should be lower than damp)
	float dampValue = 0.7f;
	float restLength = 0f;
	
	float susCompression = compValue * 2 * FastMath.sqrt(stiffness);
	float susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
	float maxSusForce = 25*mass; //TODO '25' is a random number.
	float maxSusTravel = 50;
	
	//jme3 grip constants
	//my physics works with 0f, but feels tighter with: 1.0f
	float wheel0Slip = 0;
	float wheel1Slip = 0;
	float wheel2Slip = 0;
	float wheel3Slip = 0;
	
	//drag constants
	float DRAG = 1.5f; //squared component
	float RESISTANCE = 15.0f; //linear component

	//other (debug)
	float MAX_BRAKE = 20000;
	Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);
	
	boolean driveFront = false, driveRear = true; //this would be rear wheel drive
	
	//this one is from the notes, is a ~1999 corvette c6 
	float[] torque = new float[]{0,390,445,460,480,475,360,10}; //starts at 0 rpm, steps every 1000rpm (until done)
		//TODO maybe 500 rpm splits (will get better peaks, good for testing grip)
	
	float gearDown = 2400;//TODO find good numbers for all of these gear numbers
	float gearUp = 5500;
	float redline = 6500;
	
	float engineCompression = 0.2f; //is going to be multiplied by the RPM
	
	float transEffic = 0.75f; //TODO apparently 0.7 is common (power is lost to rotating things
	float diffRatio = 2.5f; //helps set the total drive ratio
	float[] gearRatios = new float[]{-2.9f,3.40f,2.5f,1.8f,1,0.74f,0.5f};; //reverse,gear1,gear2,g3,g4,g5,g6,...
	
	//TODO i found a porsche boxter engine curve:
//	float[] torque = new float[]{0,223,250,280,300,310,280,245,10};

	//old constants from simpler traction
	float CA_R = -5.3f;
	float CA_F = -5f;
	float MAX_GRIP = 2.5f;
}

class NormalCar extends FancyVT {
	//for using the default settings.
	//probably shouldn't have a custom constructor
	
	NormalCar() {}
}

class NormalFCar extends FancyVT {
	//Front wheel drive car
	NormalFCar() {
		driveFront = true;
		driveRear = false;
	}
}

class RallyCar extends FancyVT {
	
	RallyCar() {
		carModel = dir+"car4raid_1.obj";
		wheelModel = dir+"wheelraid1.obj";
		
		mass = 1400;
		
		wheel0Slip = 0.01f; //if you make it zero relaly slow speeds get weird
		wheel1Slip = 0.01f;
		wheel2Slip = 0.01f;
		wheel3Slip = 0.01f;
		
		wheelWidth = 0.25f;
		wheelRadius = 0.4f;
		
		driveFront = true;
		driveRear = true;
		
		wheel_xOff = 0.7f;
		wheel_yOff = 0.2f;
		wheel_zOff = 1.1f;
		
		CA_R = -5.3f;
		CA_F = -5f;
		MAX_GRIP = 2.5f;
		
		stiffness  = 35.0f;
		restLength = 0.15f;
		compValue  = 0.4f;
		dampValue  = 0.7f;
		rollFraction = 0.6f;
		
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		maxSusForce = 25000;
		
		torque = new float[]{0,420,580,620,660,673,420,10}; //starts at 0 rpm, steps every 1000rpm (until done)
		
		gearDown = 2900;
		gearUp = 5700;
		redline = 6500;
		
		transEffic = 0.75f;
		diffRatio = 3f;
		gearRatios = new float[]{-3.5f,3.66f,2.5f,1.9f,1.4f,1.02f,0.7f};
	}
}

class TrackCar extends FancyVT {
	
	TrackCar() {
		carModel = dir+"f1.blend";
		wheelModel = dir+"f1_wheel.blend";
		CAM_OFFSET = new Vector3f(0,2.5f,-6);
		
		mass = 900;
		
		DRAG = 0.3f; //engine is stopping before these values...
		RESISTANCE = 5;
		
		steerAngle = 0.25f;

		CA_F = -7;
		CA_R = -6.5f;
		MAX_GRIP = 3f;
		
		stiffness  = 200.0f;
		restLength = 0.05f;
		compValue  = 0.8f;
		dampValue  = 0.9f;
		
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		
		width = 1.5f;
		height = 0.7f;
		length = 5f;
		rollFraction = 0.2f;
		
		wheel_xOff = 0.62f;
		wheel_yOff = 0.12f;
		wheel_zOff = 1.63f;
		
		//TODO found via internet (f1 '09)
		torque = new float[]{0, 300,500,500,550,608, 595,580,560,540,525, 500,440,410,360,350};
		gearDown = 9000;
		gearUp = 13500;
		redline = 15000;
		
		diffRatio = 3.2f;
		gearRatios = new float[]{-5f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};
	}
}

class Rocket extends FancyVT {
	
	Rocket() {
		mass = 900;
		steerAngle = 0.22f;
		
		DRAG = 0.1f;
		RESISTANCE = 5;
		rollFraction = 0f;
		
		CA_F = -10;
		CA_R = -9.5f;
		MAX_GRIP = 20f;
		MAX_BRAKE = 50000;
		
		torque = new float[]{0, 300,500,500,550,608, 595,580,560,540,525, 500,440,410,360,250};
		for (int i = 0; i < torque.length; i++) {
			torque[i] *= 2;
		}
		gearDown = 9000;
		gearUp = 13500;
		redline = 15000;
		
		diffRatio = 2.5f;
		gearRatios = new float[]{-5f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};
		
		wheellatdata = new RocketWheel();
		wheellongdata = new RocketWheel();
	}
}

///////////////////////////////////////
//for the runing mode

class Runner extends FancyVT {
	
	Runner() {
		
	}
}

class Hunter extends FancyVT {
	
	Hunter() {
		carModel = dir+"sa_hummer.blend";
		wheelModel = dir+"sa_hummer_wheel.blend";
		
		mass = 2500;
		
		wheel_xOff = 1.0f;
		wheel_yOff = -0.45f;
		wheel_zOff = 1.85f;
		
		wheelRadius = 0.4f;

		rollFraction = 0.1f;
		
		CA_F = -7;
		CA_R = -6;
		MAX_GRIP = 3f;
				
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		maxSusForce = 55000;
		
		torque = new float[]{0,520,680,720,760,773,520,110};
		
		gearDown = 2900;
		gearUp = 5700;
		redline = 6500;
		
		transEffic = 0.75f;
		diffRatio = 3f;
		gearRatios = new float[]{-3.5f,3.66f,2.5f,1.9f,1.4f,1.02f,0.7f};
	}
}

