package car.ai;

import java.util.function.Function;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import helper.Log;

public class DriveAlongAI extends CarAI {

	private final Function<Vector3f, Vector3f> getGoal;
	private float maxSpeed;

	public DriveAlongAI(RayCarControl car, Function<Vector3f, Vector3f> getGoal) {
		super(car);
		
		this.maxSpeed = Float.MAX_VALUE;

		if (getGoal != null) {
			this.getGoal = getGoal;			
		} else {
			this.getGoal = (pos) -> { return new Vector3f(); }; //basically a no-op position
			Log.p("WARNING: Goal not given, using the origin.");
		}
	}

	public void setMaxSpeed(float value) {
		this.maxSpeed = value;
	}

	@Override
	public void update(float tpf) {
		Vector3f pos = this.car.getPhysicsLocation();
		Vector3f target = getGoal.apply(pos);
		if (target == null)
			return;
		
		//drive at target
		Vector3f myforward = car.forward;
		
		float angF = myforward.normalize().angleBetween((target.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((target.subtract(pos)).normalize());
		
		float turndeg = (angF > FastMath.QUARTER_PI) ? 1 : angF/FastMath.QUARTER_PI;
		
		//turn towards player
		if (ang > FastMath.HALF_PI) {
			onEvent("Left", false, 0);
			onEvent("Right", true, turndeg);
		} else {
			onEvent("Right", false, 0);
			onEvent("Left", true, turndeg);
		}
		//slow down to turn
		if (FastMath.abs(angF) < FastMath.QUARTER_PI) {
			onEvent("Brake", false, 0);
			onEvent("Accel", true, 1);
		} else {
			onEvent("Brake", true, 1);
			onEvent("Accel", false, 0);
		}
		
		//if going too slow at all speed up
		float velocity = car.getLinearVelocity().length();
		if (velocity < 4) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
		}

		if (velocity > maxSpeed) {
			onEvent("Accel", false, 0); //do not
			onEvent("Brake", false, 0);
		}
	}
}
