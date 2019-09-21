package car.ai;

import com.jme3.bullet.objects.PhysicsRigidBody;

import car.ray.RayCarControl;

public class DriveAtAI extends CarAI {

	private final PhysicsRigidBody driveAtThis;
	
	private float reversingTimer;
	private boolean reversing;
	
	public DriveAtAI (RayCarControl car, PhysicsRigidBody node) {
		super(car);
		
		this.driveAtThis = node;
		this.reversingTimer = 0;
		this.reversing = false;
	}

	public void update(float tpf) {
		float velocity = car.getLinearVelocity().length();
		
		//try and fix a stuck situation
		if (!reversing) {
			onEvent("Reverse", false);
			
			if (velocity < 0.5f) {
				reversingTimer += tpf;
				if (reversingTimer > 5) {
					reversing = true;
					reversingTimer = 1;
				}
			} else {
				reversingTimer = 0;
			}
	
			if (velocity < 1 && car.up.y < 0) { //if very still and not the right way up then flip over
				onEvent("Flip", true);
			}
		} else {
			//currently reversing
			onEvent("Reverse", true);
			reversingTimer -= tpf;
			if (reversingTimer < 0)
				reversing = false;
			return;
		}

		driveAt(driveAtThis.getPhysicsLocation());

		//if going to slow speed up
		if (velocity < 10) {
			onEvent("Accel", true);
			onEvent("Brake", false);
		}
	}
}
