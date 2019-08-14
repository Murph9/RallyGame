package car.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import world.wp.DefaultBuilder;

public class FollowWorldAI extends CarAI {

	private DefaultBuilder world;
	private float maxSpeed;
	
	public FollowWorldAI (RayCarControl car, DefaultBuilder world) {
		super(car);
		
		this.world = world;
		this.maxSpeed = Float.MAX_VALUE;
	}

	public void setMaxSpeed(float value) {
		this.maxSpeed = value;
	}

	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = world.getNextPieceClosestTo(pos);
		if (atPos == null)
			return; //don't know
		
		Matrix3f w_angle = car.getPhysicsRotationMatrix();
		Vector3f velocity = w_angle.invert().mult(car.vel);
		int reverse = (velocity.z < 0 ? -1 : 1);
		
		Vector3f w_forward = new Vector3f(car.forward); //this is already in world space
		w_forward.y = 0; //don't care for vertical directions
		w_forward.normalizeLocal();
		
		Vector3f target = atPos.subtract(pos);
		target.y = 0; //still no caring about the vertical
		target.normalizeLocal();

		//angle between target and direction
		float angF = w_forward.angleBetween(target);
		//and get the sign for the angle
		float ang = car.left.normalize().angleBetween(target);
		
		//get attempted turn angle as pos or negative
		float nowTurn = angF*Math.signum(FastMath.HALF_PI-ang);
	
		//turn towards
		if (nowTurn < 0) {
			onEvent("Left", false, 0);
			onEvent("Right", true, Math.abs(nowTurn)*reverse);
		} else {
			onEvent("Right", false, 0);
			onEvent("Left", true, Math.abs(nowTurn)*reverse);
		}
		
		//accel or brake?
		if (FastMath.abs(angF) < FastMath.PI/8) { //eighth pi
			onEvent("Brake", false, 0);
			onEvent("Accel", true, 1);
		} else if (FastMath.abs(angF) < FastMath.QUARTER_PI) {
			// slow down to turn
			onEvent("Accel", false, 0);
			onEvent("Brake", false, 0);
		} else {
			// slow down a lot to turn a lot
			onEvent("Brake", true, 1);
			onEvent("Accel", false, 0);
		}
		/*
		 * TODO: calculate the max turning at the current speed
		 * Math will include: 
		 * pjk.D (4 wheel average)
		 * speed
		 * gravity with car mass
		 * some centripetal formula
		 */
		
		float carSpeed = car.getLinearVelocity().length();

		//if going too slow speed up
		if (carSpeed < 3) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
			
			if (carSpeed < 2 && car.up.y < 0) { //very still
				onEvent("Flip", true, 1);
			}
		}
		
		if (carSpeed > maxSpeed) {
			onEvent("Accel", false, 0); //do not
			onEvent("Brake", false, 0);
		}

		//TODO some kind of ray cast so they can drive around things properly
	}
}
