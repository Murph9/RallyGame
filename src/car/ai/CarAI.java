package car.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;

public abstract class CarAI {
	
	protected RayCarControl car;

	public CarAI(RayCarControl car) {
		this.car = car;

		//ignore all turning speed factor code for AIs
		car.onAction("IgnoreSteeringSpeedFactor", true, 1);
	}
	
	public abstract void update(float tpf);
	
	protected void onEvent(String act, boolean ifdown) {
		onEvent(act, ifdown, ifdown ? 1 : 0);
	}
	protected void onEvent(String act, boolean ifdown, float amnt) {
		car.onAction(act, ifdown, amnt);
	}

	/** Only brake, no accel and no steering */
	protected void justBrake() {
		onEvent("Left", false);
		onEvent("Right", false);
		
		onEvent("Accel", false);
		onEvent("Brake", true);
	}

	/**
	 * Helper method direct the AI towards a point
	 * @param curPos Current AI car position
	 * @param targetPos Target location
	 */
	protected void driveAt(Vector3f curPos, Vector3f targetPos) {
		Matrix3f w_angle = car.getPhysicsRotationMatrix();
		Vector3f velocity = w_angle.invert().mult(car.vel);
		int reverse = (velocity.z < 0 ? -1 : 1);

		Vector3f w_forward = new Vector3f(car.forward); // this is already in world space
		w_forward.y = 0; // don't care for vertical directions
		w_forward.normalizeLocal();

		Vector3f target = targetPos.subtract(curPos);
		target.y = 0; // still no caring about the vertical
		target.normalizeLocal();

		// angle between target and direction
		float angF = w_forward.angleBetween(target);
		// and get the sign for the angle
		float ang = car.left.normalize().angleBetween(target);

		// get attempted turn angle as pos or negative
		float nowTurn = angF * Math.signum(FastMath.HALF_PI - ang);

		// turn towards
		if (nowTurn < 0) {
			onEvent("Left", false);
			onEvent("Right", true, Math.abs(nowTurn) * reverse);
		} else {
			onEvent("Left", true, Math.abs(nowTurn) * reverse);
			onEvent("Right", false);
		}

		// accel or brake?
		if (FastMath.abs(angF) < FastMath.PI / 8) { // eighth pi
			onEvent("Brake", false);
			onEvent("Accel", true);
		} else if (FastMath.abs(angF) < FastMath.QUARTER_PI) {
			// slow down to turn
			onEvent("Accel", false);
			onEvent("Brake", false);
		} else {
			// slow down a lot to turn a lot
			onEvent("Brake", true);
			onEvent("Accel", false);
		}

		/*
		 * TODO: calculate the max turning at the current speed
		 * Math will include: 
		 * pjk.D (4 wheel average)
		 * speed
		 * gravity with car mass
		 * some centripetal formula
		 * 
		 * ---
		 * Calculate if you are going to overshoot the target 
		 */
	}

	//TODO helper ray cast method, to find out what to avoid
}
