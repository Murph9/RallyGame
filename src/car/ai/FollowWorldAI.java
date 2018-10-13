package car.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import helper.Log;
import world.wp.DefaultBuilder;

public class FollowWorldAI extends CarAI {

	private DefaultBuilder world;
	private float lastTurn;
	
	public FollowWorldAI (RayCarControl car, DefaultBuilder world) {
		super(car);
		
		this.world = world;
	}

	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = world.getNextPieceClosestTo(pos);
		if (atPos == null)
			return; //don't know
		
		Matrix3f w_angle = car.getPhysicsRotationMatrix();
		Vector3f velocity = w_angle.invert().mult(car.vel);
		int reverse = (velocity.z < 0 ? -1 : 1);
		
		Vector3f myforward = car.forward;
		
		float angF = myforward.normalize().angleBetween((atPos.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((atPos.subtract(pos)).normalize());
		
		//get attempted turn angle as pos or negative
		float nowTurn = angF*Math.signum(FastMath.HALF_PI-ang); //TODO wobbling

		Log.p(nowTurn, angF, ang, lastTurn);
		lastTurn = FastMath.interpolateLinear(tpf*10, lastTurn, nowTurn);
		
		//turn towards
		if (lastTurn < 0) {
			onEvent("Left", false, 0);
			onEvent("Right", true, Math.abs(lastTurn)*reverse);
		} else {
			onEvent("Right", false, 0);
			onEvent("Left", true, Math.abs(lastTurn)*reverse);
		}
		//slow down to turn
		if (FastMath.abs(angF) < FastMath.QUARTER_PI) {
			onEvent("Brake", false, 0);
			onEvent("Accel", true, 1);
		} else {
			onEvent("Brake", true, 1);
			onEvent("Accel", false, 0);
		}
		
		//if going too slow speed up
		if (car.getLinearVelocity().length() < 10) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
			
			if (car.getLinearVelocity().length() < 2 && car.up.y < 0) { //very still
				onEvent("Flip", true, 1);
			}
		}
		
		//TODO some kind of ray cast so they can drive around things properly
	}
}
