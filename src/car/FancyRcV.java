package car;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;

//copied from:
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java

//For re-writing the physics engine.

public class FancyRcV extends RaycastVehicle {

	Vector3f fwd = new Vector3f();
	private int indexRightAxis = 0;
	private int indexUpAxis = 2;
	private int indexForwardAxis = 1;
	
	private float curkm = 0;
	
	private RigidBody mychassis;
	
	public FancyRcV(CarData tuning, RigidBody chassis, VehicleRaycaster raycaster) {
		super(tuning.vt, chassis, raycaster);
		
		mychassis = chassis; //because i can't access internal methods...
	}
	
	//extend methods so that I can write 'special' ones
	@Override
	public void updateSuspension(float deltaTime) {
		float chassisMass = 1f / mychassis.getInvMass();
		float last_length_diff = 0;
		WheelInfo last_wheel_info = null;
		
		for (int w_it = 0; w_it < getNumWheels(); w_it++) {
			WheelInfo wheel_info = wheelInfo.getQuick(w_it);
			float length_diff = 0;
			
			if (wheel_info.raycastInfo.isInContact) {
				float force;
				//	Spring
				{
					float susp_length = wheel_info.getSuspensionRestLength();
					float current_length = wheel_info.raycastInfo.suspensionLength;

					length_diff = (susp_length - current_length);
					last_length_diff = length_diff;

					force = wheel_info.suspensionStiffness * length_diff * wheel_info.clippedInvContactDotSuspension;
				}

				// Damper
				{
					float projected_rel_vel = wheel_info.suspensionRelativeVelocity;
					{
						float susp_damping;
						if (projected_rel_vel < 0f)
							susp_damping = wheel_info.wheelsDampingCompression;
						else
							susp_damping = wheel_info.wheelsDampingRelaxation;
						force -= susp_damping * projected_rel_vel;
					}
				}

				// RESULT
				wheel_info.wheelsSuspensionForce = force * chassisMass;
				if (wheel_info.wheelsSuspensionForce < 0f) {
					wheel_info.wheelsSuspensionForce = 0f;
				}
			}
			else {
				wheel_info.wheelsSuspensionForce = 0f;
			}
			
			//sway/roll bars (for each pair of wheels)
			//TODO still limited by the max suspension force
			if (w_it % 2 == 1) {//every second wheel
				float roll_bar_force = (length_diff - last_length_diff)*20000f;
				if (wheel_info.raycastInfo.isInContact)
					wheel_info.wheelsSuspensionForce += roll_bar_force*chassisMass;
				if (last_wheel_info.raycastInfo.isInContact)
					last_wheel_info.wheelsSuspensionForce -= roll_bar_force*chassisMass;
			}
			
			last_length_diff = 0;
			last_wheel_info = wheel_info;
		}
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
		
		//note: friction was removed from here (and the method voided)
	}
	@Override
	public void updateFriction(float timeStep) {
		//empty
	}
}
