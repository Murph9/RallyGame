package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public abstract class Car {
	
	//model strings
	String carOBJModel = "assets/car4.obj";
	String wheelOBJModel = "assets/wheel.obj";
	
	//camera options
	Vector3f LOOK_AT = new Vector3f(0,0,0);
	Vector3f CAM_OFFSET = new Vector3f(0,3,-7);
	
	//physical things
	float mass = 1200;
	float width = 1.4f; //x size, door handle to door handle
	float height = 1f; //y size, roof to ground
	float length = 3f; //z size, from front to back
	
	//wheels
	Vector3f wheelDirection = new Vector3f(0, -1, 0); //vertical
	Vector3f wheelAxle = new Vector3f(-1, 0, 0); //horizontal

	float wheelWidth = 0.15f;
	float wheelRadius = 0.3f;
	
	float w_xOff = 0.68f; //wheels x offset (side)
	float w_yOff = 0f; //wheels no height
	float w_zOff = 1.1f; //wheels z offset (front and back)
	
	
	//suspension values for wheels
	//see for details: https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
	float stiffness = 60.0f;//200=f1 car
	float compValue = 0.6f; //(should be lower than damp)
	float dampValue = 0.7f;
	float restLength = 0f;
	
	float susCompression = compValue * 2 * FastMath.sqrt(stiffness);
	float susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
	
	//grip constants
	//my physics work mostly with 0.01f, but feels better with:
	float wheel1Slip = 1.0f;
	float wheel2Slip = 1.0f; //don't change these values so you spin out, make them "change due to acceleration"
	float wheel3Slip = 1.0f;
	float wheel4Slip = 1.0f;
	
	//my physics grip constants - TODO tweak numbers
	float DRAG = 4.5f;
	float RESISTANCE = 30.0f;
	float CA_R = -5;
	float CA_F = -5;
	float MAX_GRIP = 2f;
	
	//other
	float MAX_ACCEL = 90;
	float MAX_STEERING = 0.5f;
	float MAX_BRAKE = 100;
	Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);
}

class NormalCar extends Car {
	//for using the default settings.
	//kinda feels like a real car...
}

class RallyCar extends Car {
	
	RallyCar() {
		//no changes yet :(
		//TODO its not an actual rally car yet..
		
		/* just from looking at top gear rally:
		 * - oversteers easily, and feels light, probably some kind of "phantom turning force like I had before"
		 *  + snaps at a very small slip angle
		 *  + both of the CA_.. are high, return is quick and drifting is fast
		 *   - don't know if MAX_GRIP actually plays a part yet (doesn't look like it)
		 *  + might not need the stock jme3 traction
		 * - camera centered on the front of the car
		 *  + seems to follow velocity direction greatly, and some some fancy rules on slow speeds
		 * - suspension is slightly bouncy and likes being close under the car, but can extend the length of the car
		 *  + remember im looking at the "soft suspension" option
		 */
		
		mass = 1000;
		MAX_ACCEL = 140;
		MAX_BRAKE = 200;
		
		wheel1Slip = 0.01f; //don't put them on zero, their physics doesn't like it
		wheel2Slip = 0.01f;
		wheel3Slip = 0.01f;
		wheel4Slip = 0.01f;
		
		CA_F = -8; //more than 6 ok.?
		CA_R = -8; //more than 6 ok.?
		MAX_GRIP = 7.0f;
		
		stiffness  = 20.0f;
		restLength = 0.05f;
		compValue  = 0.8f;
		dampValue  = 1.2f;
		
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		
	}
}

class TrackCar extends Car { //TODO maybe massive enum?
	
	TrackCar() {
		carOBJModel = "assets/f1.obj";
		wheelOBJModel = "assets/f1_wheel.obj";
		CAM_OFFSET = new Vector3f(0,2,-6);
		
		mass = 1000;
		
		DRAG = 3f;
		RESISTANCE = 20;
		
		MAX_STEERING = 0.25f;
		MAX_ACCEL = 140;
		MAX_BRAKE = 200;
		
		stiffness  = 100.0f;
		restLength = 0.05f;
		compValue  = 0.8f;
		dampValue  = 0.9f;
		
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		
		width = 1.5f;
		height = 0.7f;
		length = 5f;
		
		w_xOff = 0.62f; //wheels x offset (side)
		w_yOff = 0.1f; //wheels y offset (height)
		w_zOff = 1.6f; //wheels z offset (front and back)
		
	}
}


