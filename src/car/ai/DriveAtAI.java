package car.ai;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import car.MyPhysicsVehicle;
import helper.H;

public class DriveAtAI extends CarAI {

	private PhysicsRigidBody driveAtThis;
	
	public DriveAtAI (MyPhysicsVehicle car, PhysicsRigidBody node) {
		super(car);
		
		this.driveAtThis = node;
	}

	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = driveAtThis.getPhysicsLocation();

		Vector3f myforward = new Vector3f();
		car.getForwardVector(myforward);
		
		float angF = myforward.normalize().angleBetween((atPos.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((atPos.subtract(pos)).normalize());
		
		float turndeg = (angF > FastMath.QUARTER_PI) ? 1 : angF/FastMath.QUARTER_PI;

		/*
		if (ang < FastMath.PI/8) {
			H.p(car.car+"nitro o");
			onEvent("Nitro", true, 1);
		} else {
			onEvent("Nitro", false, 0);
		}
		*/
		
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
		
		//if going to slow speed up
		if (car.getLinearVelocity().length() < 10) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
			
			if (car.getLinearVelocity().length() < 1 && car.up.y < 0) { //very still
				onEvent("Flip", true, 1);
			}
		}
		
		//TODO some kind of ray cast so they can drive around things properly
		
		//hack so they don't lose too bad
		
		if (atPos.y - pos.y > 50 || atPos.subtract(pos).length() > 500) {
			car.setPhysicsLocation(atPos.add(4, 1, 0)); //spawn 3 up and left of me
			car.setLinearVelocity(driveAtThis.getLinearVelocity()); //and give them my speed
			car.setPhysicsRotation(driveAtThis.getPhysicsRotation()); //and rotation
			car.setAngularVelocity(driveAtThis.getAngularVelocity()); //and rot angle
			H.p("respawned at " + driveAtThis.getPhysicsLocation());
		}
	}

	private void onEvent(String act, boolean ifdown, float amnt) {
		car.onAction(act, ifdown, amnt);
	}
}
