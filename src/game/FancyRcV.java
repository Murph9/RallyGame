package game;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;

//copied from:
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java

//For writing the physics engine.

public class FancyRcV extends RaycastVehicle {

	Vector3f fwd = new Vector3f();
	private int indexRightAxis = 0;
	private int indexUpAxis = 2;
	private int indexForwardAxis = 1;
	
	private float curkm = 0;
	
	public FancyRcV(CarData tuning, RigidBody chassis, VehicleRaycaster raycaster) {
		super(tuning.vt, chassis, raycaster);
	}
	
	//extend methods so that I can write 'extended' ones
	
	@Override
	public void updateFriction(float timeStep) {
		//empty
	}
	
	@Override //because i can't set the private variable
	public float getCurrentSpeedKmHour() {
		return curkm;
	}
	
	
	@Override
	public void updateVehicle(float step) {
		for (int i = 0; i < getNumWheels(); i++) {
			updateWheelTransform(i, false);
		}
		
		Vector3f tmp = new Vector3f();

		curkm = 3.6f * getRigidBody().getLinearVelocity(tmp).length();

		Transform chassisTrans = getChassisWorldTransform(new Transform());

		Vector3f forwardW = new Vector3f();
		forwardW.set(
				chassisTrans.basis.getElement(0, indexUpAxis),
				chassisTrans.basis.getElement(1, indexForwardAxis),
				chassisTrans.basis.getElement(2, indexRightAxis));

		if (forwardW.dot(getRigidBody().getLinearVelocity(tmp)) < 0f) {
			curkm *= -1f;
		}

		//
		// simulate suspension
		//

		int i = 0;
		for (i = 0; i < wheelInfo.size(); i++) {
			@SuppressWarnings("unused")
			float depth;
			depth = rayCast(wheelInfo.getQuick(i));
		}

		updateSuspension(step);

		for (i = 0; i < wheelInfo.size(); i++) {
			// apply suspension force
			WheelInfo wheel = wheelInfo.getQuick(i);

			float suspensionForce = wheel.wheelsSuspensionForce;

			if (suspensionForce > wheel.maxSuspensionForce) {
				suspensionForce = wheel.maxSuspensionForce;
			}
			Vector3f impulse = new Vector3f();
			impulse.scale(suspensionForce * step, wheel.raycastInfo.contactNormalWS);
			Vector3f relpos = new Vector3f();
			relpos.sub(wheel.raycastInfo.contactPointWS, getRigidBody().getCenterOfMassPosition(tmp));

			getRigidBody().applyImpulse(impulse, relpos);
		}

		updateFriction(step);

		
		//the code for rotating tyres has been commented out:
		/*
		for (i = 0; i < wheelInfo.size(); i++) {
			WheelInfo wheel = wheelInfo.getQuick(i);
			Vector3f relpos = new Vector3f();
			relpos.sub(wheel.raycastInfo.hardPointWS, getRigidBody().getCenterOfMassPosition(tmp));
			Vector3f vel = getRigidBody().getVelocityInLocalPoint(relpos, new Vector3f());

			if (wheel.raycastInfo.isInContact) {
				Transform chassisWorldTransform = getChassisWorldTransform(new Transform());

				Vector3f fwd = new Vector3f();
				fwd.set(
						chassisWorldTransform.basis.getElement(0, indexUpAxis),
						chassisWorldTransform.basis.getElement(1, indexForwardAxis),
						chassisWorldTransform.basis.getElement(2, indexRightAxis));

				float proj = fwd.dot(wheel.raycastInfo.contactNormalWS);
				tmp.scale(proj, wheel.raycastInfo.contactNormalWS);
				fwd.sub(tmp);

				float proj2 = fwd.dot(vel);

				wheel.deltaRotation = (proj2 * step) / (wheel.wheelsRadius);
				wheel.rotation += wheel.deltaRotation;
			}
			else {
				wheel.rotation += wheel.deltaRotation;
			}

			wheel.deltaRotation *= 0.99f; // damping of rotation when not in contact
		}*/
	}
}
