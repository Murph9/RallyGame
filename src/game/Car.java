package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class Car {
	
	//model strings
	static final String carOBJModel = "assets/car4.obj";
	static final String wheelOBJModel = "assets/wheel.obj";
	
	//physical things
	static final float mass = 800;
	static final float width = 1.4f; //x size, door handle to door handle
	static final float height = 1f; //y size, roof to ground
	static final float length = 3f; //z size, from front to back
	
	//wheels
	static Vector3f wheelDirection = new Vector3f(0, -1, 0); //vertical
	static Vector3f wheelAxle = new Vector3f(-1, 0, 0); //horizontal

	static final float wheelWidth = 0.15f;
	static final float wheelRadius = 0.3f;
	
	static final float w_xOff = 0.68f; //wheels x offset (side)
	static final float w_yOff = 0f; //wheels no height
	static final float w_zOff = 1.1f; //wheels z offset (front and back)
	
	
	//suspension values for wheels
	//see for details: https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
	static final float stiffness = 60.0f;//200=f1 car
	static final float compValue = 0.3f; //(should be lower than damp)
	static final float dampValue = 0.4f;
	static final float restLength = -0.05f;
	
	static final float susCompression = compValue * 2 * FastMath.sqrt(stiffness);
	static final float susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
	
	//grip constants
	//my physics work mostly with 0.01f, but feels better with:
	static final float wheel1Slip = 0.6f;//0.9
	static final float wheel2Slip = 0.6f;//0.9
	static final float wheel3Slip = 0.6f;//0.6
	static final float wheel4Slip = 0.6f;//0.6
	
	//my physics grip constants - TODO tweak numbers
	static final float DRAG = 4.5f;
	static final float RESISTANCE = 30.0f;
	static final float CA_R = -5;
	static final float CA_F = -5;
	static final float MAX_GRIP = 1.5f;
	
	//other
	static final float MAX_ACCEL = mass*45f/400;
	static final float MAX_STEERING = 0.5f;
	static final float MAX_BRAKE = mass*50/400;
	static final Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);	
}

