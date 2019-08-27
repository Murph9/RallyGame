package car.ai;

import java.util.function.Function;

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
		if (target == null) {
			justBrake();
			return;
		}
		
		//drive at target
		driveAt(pos, target);


		//if going too slow at all speed up
		float velocity = car.getLinearVelocity().length();
		if (velocity < 4) {
			onEvent("Accel", true);
			onEvent("Brake", false);
		}

		if (velocity > maxSpeed) {
			onEvent("Accel", false); //do not
			onEvent("Brake", false);
		}
	}
}
