package car.ai;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import game.App;
import helper.Log;
import world.WorldType;

public class DriveAtAI extends CarAI {

	private PhysicsRigidBody driveAtThis;
	
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
				onEvent("Flip", true, 1);
			}
		} else {
			//currently reversing
			onEvent("Reverse", true, 1);
			reversingTimer -= tpf;
			if (reversingTimer < 0)
				reversing = false;
			return;
		}
		
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = driveAtThis.getPhysicsLocation();

		Vector3f myforward = car.forward;
		
		float angF = myforward.normalize().angleBetween((atPos.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((atPos.subtract(pos)).normalize());
		
		float turndeg = (angF > FastMath.QUARTER_PI) ? 1 : angF/FastMath.QUARTER_PI;

		/*
		if (ang < FastMath.PI/8) {
			Log.p(car.car+"nitro o");
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
		if (velocity < 10) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
		}
		
		//TODO some kind of ray cast so they can drive around things properly
		
		//hack so they don't lose too bad on dynamic tracks
		if (App.rally.drive.world.getType() == WorldType.DYNAMIC
				&& atPos.y - pos.y > 50 || atPos.subtract(pos).length() > 500) {
			car.setPhysicsLocation(atPos.add(4, 1, 0)); //spawn 3 up and left of me
			car.setLinearVelocity(driveAtThis.getLinearVelocity()); //and give them my speed
			car.setPhysicsRotation(driveAtThis.getPhysicsRotation()); //and rotation
			car.setAngularVelocity(driveAtThis.getAngularVelocity()); //and rot angle
			Log.p("respawned at " + driveAtThis.getPhysicsLocation());
		}
	}
}
