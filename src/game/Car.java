package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public abstract class Car {
	
	//model strings (can be xx.obj or xx.blend)
	String carModel = "assets/car4.obj";
	String wheelModel = "assets/wheel.obj";
	
	//camera options
	Vector3f LOOK_AT = new Vector3f(0,1f,0); //top of car usually
	Vector3f CAM_OFFSET = new Vector3f(0,3,-7); //where the camera is
	
	//physical things
	float mass = 1200; //kg (total)
	float width = 1.4f; //x size meter, door handle to door handle
	float height = 1f; //y size meter, roof to ground
	float length = 3f; //z size meter, from front to back
	
	//wheels
	Vector3f wheelDirection = new Vector3f(0, -1, 0); //vertical
	Vector3f wheelAxle = new Vector3f(-1, 0, 0); //horizontal

	float wheelWidth = 0.15f; //m
	float wheelRadius = 0.3f; //m
	float wheelMass = 75; //kg
	float MAX_STEERING = 0.5f; //radians
	float rollFraction = 1; //1 = full into roll, 0 = no roll
	
	float[] slipRatioCurve = new float[]{0, 6000, 5990, 5000, 3000, 1000}; //TODO use (maybe?)
	
	float w_xOff = 0.68f; //wheels x offset (side), meters
	float w_yOff = 0f; //wheels y offest (height), meters
	float w_zOff = 1.1f; //wheels z offset (front and back), meters
	
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
	
	//grip constants
	//my physics works with 0.01f, but feels better with: 1.0f
	float wheel0Slip = 1.0f;
	float wheel1Slip = 1.0f;
	float wheel2Slip = 1.0f;
	float wheel3Slip = 1.0f;
	
	//my physics grip constants
	float DRAG = 2.5f; //squared component
	float RESISTANCE = 30.0f; //linear component
	float CA_R = -5f;
	float CA_F = -5f;
	float MAX_LAT_GRIP = 2.5f;
	float MAX_LONG_GRIP = 2.5f;
	
	//other (debug)
	float MAX_ACCEL = 9000; //TODO take it out?
	float MAX_BRAKE = 10000;
	Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);
	
	boolean driveFront = false, driveRear = true; //this would be rear wheel drive
	
	//this one is from the notes, is a corevette c6 ~1999
	float[] torque = new float[]{0,390,445,460,480,475,360,10}; //starts at 0 rpm, steps every 1000rpm (until done)
		//TODO maybe 500 rpm splits (will get better peaks, good for testing grip)
	
	float gearDown = 2400;//TODO find good numbers for all of these gear numbers
	float gearUp = 5500;
	
	float transEffic = 0.75f; //TODO apparently 0.7 is common (power is lost to rotating things
	float diffRatio = 3.42f; //helps set the total drive ratio
	float[] gearRatios = new float[]{-2.9f,2.66f,1.78f,1.3f,1,0.74f,0.5f};; //reverse,gear1,gear2,g3,g4,g5,g6,...
	
	//TODO i found a porsche boxter engine curve:
//	float[] torque = new float[]{0,223,250,280,300,310,280,245,10};
}

class NormalCar extends Car {
	//for using the default settings.
	//probably shouldn't have a custom constructor
}

class RallyCar extends Car {
	
	RallyCar() {
		carModel = "assets/car4raid_1.obj"; //...well it is now?
		wheelModel = "assets/wheelraid1.obj";
		
		/* just from looking at top gear rally:
		 * - oversteers easily, and feels light,
		 *  + snaps at a very small slip angle
		 *   - don't know if MAX_GRIP actually plays a part yet (doesn't look like it)
		 *  + might not need the stock jme3 traction
		 * - camera centered on the front of the car
		 *  + seems to follow velocity direction greatly, and some some fancy rules on slow speeds
		 * - suspension is slightly bouncy and likes being close under the car, but can extend the length of the car
		 *  + remember im looking at the "soft suspension" option
		 */
		//////////////////////////////////////////////////////////////////////
		//DON'T TOUCH THESE UNTIL YOU GET THE ACTUAL PHYSICS OF THE CAR RIGHT
		//////////////////////////////////////////////////////////////////////		
		mass = 1000;
		
		wheel0Slip = 0.01f; //if you make it zero relaly slow speeds get weird
		wheel1Slip = 0.01f;
		wheel2Slip = 0.01f;
		wheel3Slip = 0.01f;
		
		wheelWidth = 0.25f;
		wheelRadius = 0.4f;
		
		driveFront = true;
		driveRear = true;
		
		w_xOff = 0.7f;
		w_yOff = 0.2f;
		w_zOff = 1.1f;
		
		CA_F = -7;
		CA_R = -6f;
		MAX_LAT_GRIP = 3f;
		
		stiffness  = 35.0f;
		restLength = 0.15f;
		compValue  = 0.6f;
		dampValue  = 0.7f;
		rollFraction = 0.4f;
		
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		maxSusForce = 25000;
		
		torque = new float[]{0,720,880,920,960,973,720,10}; //starts at 0 rpm, steps every 1000rpm (until done)
		diffRatio = 4f;
	}
}

class TrackCar extends Car {
	
	TrackCar() {
		carModel = "assets/f1.blend";
		wheelModel = "assets/f1_wheel.blend";
		CAM_OFFSET = new Vector3f(0,2.5f,-6);
		
		mass = 1000;
		
		DRAG = 0.3f; //engine is stopping before these values...
		RESISTANCE = 5;
		
		MAX_STEERING = 0.25f;

		CA_F = -7;
		CA_R = -6.5f;
		MAX_LAT_GRIP = 3f;
		
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
		
		w_xOff = 0.62f;
		w_yOff = 0.05f;
		w_zOff = 1.6f;
		
		//TODO found via internet
		torque = new float[]{0,300,500,500,550,608,595,580,560,540,525,500,440,400,350,0};
		gearDown = 6000;
		gearUp = 13500;
		
		diffRatio = 5.5f;
		gearRatios = new float[]{-10f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};; //reverse,gear1,gear2,g3,g4,g5,g6,...
	}
}


