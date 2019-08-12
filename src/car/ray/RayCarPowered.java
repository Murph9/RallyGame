package car.ray;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import helper.Log;

//handles engine/drivetrain stuff (it is an arbitrary feature split)
public class RayCarPowered extends RayCar {
	
	protected float engineTorque;
	
	protected float accelCurrent;
	protected float steeringCurrent;
	protected float brakeCurrent;
	protected boolean handbrakeCurrent;
	protected boolean ignoreSpeedFactor;
	
	protected float steerLeft;
	protected float steerRight;

	protected float nitroTimeout;
	protected boolean ifNitro;
	protected float nitro;
	
	protected int curGear = 1;
	protected int curRPM;
	protected int gearChangeTo;
	protected float gearChangeTime;
	
	public RayCarPowered(CollisionShape shape, CarDataConst carData, Vector3f grav) {
		super(shape, carData, grav);
		
		this.nitro = carData.nitro_max;
	}

	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		Vector3f w_velocity = rbc.getLinearVelocity();
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		
		engineTorque = getEngineWheelTorque(tpf, velocity.length() * Math.signum(velocity.z));
		
		//split once for each 2 wheels
		float frontTorque = engineTorque/2;
		float rearTorque = engineTorque/2;
		
		/* TODO:
		 * https://forum.unity.com/threads/limited-slip-differential-modelling.517992/
		 * 
		 * logic goes:
		 * LSD is trying to keep the 2 wheels rotating at the same speed (or at least near)
		 * Maximum is the TBR (torque bias ratio), which is a ratio between the 2 input torques
		 * The only config value should be the TBR of like 1 or 2
		 * 
		 * In summary it looks like the torque to each of the tyres is the same.
		 * The lsd might actually be doing the opposite of what i think.
		 * And the below code is actually very wrong, not just a little.
		 * 
		 * TODO also understand:
		 * https://github.com/VDrift/vdrift/blob/89e78a55a3cd0f5babac74ab7440f457a8848f25/src/physics/cardynamics.cpp
		*/

		//i think this evenetually means the system needs to be a matrix solver state system rather than real time timestep stuff
		//http://myselph.de/gamePhysics/equalityConstraints.html
		//but this might be too much work
		
		//TODO use the w_xdiff cardata values 
		
		if (carData.driveFront) {
			wheelTorque[0] = frontTorque;
			wheelTorque[1] = frontTorque;
		}
		if (carData.driveRear) {
			wheelTorque[2] = rearTorque;
			wheelTorque[3] = rearTorque;
		}
		
		super.prePhysicsTick(space, tpf);
	}
	
	private float getEngineWheelTorque(float tpf, float vz) {
		float curGearRatio = carData.trans_gearRatios[curGear];//0 = reverse, >= 1 make normal sense
		float diffRatio = carData.trans_finaldrive;
		
		float wheelrot = 0;
		//get the drive wheels rotation speed
		if (carData.driveFront && carData.driveRear)
			wheelrot = (wheels[0].radSec + wheels[1].radSec + wheels[2].radSec + wheels[3].radSec)/4;
		else if (carData.driveFront)
			wheelrot = (wheels[0].radSec + wheels[1].radSec)/2;
		else if (carData.driveRear) 
			wheelrot = (wheels[2].radSec + wheels[3].radSec)/2;
		
		curRPM = (int)(wheelrot*curGearRatio*diffRatio*(60/FastMath.TWO_PI)); //rad/(m*sec) to rad/min and the drive ratios to engine
		//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min
		
		//no stall please, its bad enough that we don't have torque here
		//'fake clutch'
		curRPM = Math.max(curRPM, carData.e_idle);
		

		if (gearChangeTime != 0) {
			gearChangeTime -= tpf;
			if (gearChangeTime > 0) {
				return 0; //because not connected to wheels
			} else if (gearChangeTime < 0) { //if equal probably shouldn't be trying to set the gear
				curGear = gearChangeTo;
				gearChangeTime = 0;
			}
		} else { //only check while NOT changing gear 
			autoTransmission(curRPM, vz);
		}
		
		float nitroForce = 0;
		if (carData.nitro_on) { //TODO this is not bug free [surprising with its length huh?]
			if (ifNitro && this.nitro > 0) {
				nitroForce = carData.nitro_force;
				this.nitro -= 2*tpf*carData.nitro_rate;
				if (this.nitro < 0)
					this.nitro = 0; //no more nitro :(
			} else if (this.nitroTimeout > 0) { //start the timeout to start growing again
				this.nitroTimeout -= tpf;
				if (this.nitroTimeout < 0)
					this.nitroTimeout = 0;
			} else {
				this.nitro += carData.nitro_rate*tpf;
				if (this.nitro > carData.nitro_max)
					this.nitro = this.carData.nitro_max;
			}
		}

		
		float eTorque = (carData.lerpTorque(curRPM) + nitroForce)*accelCurrent;
		float engineDrag = 0;
		if (accelCurrent < 0.05f || curRPM > carData.e_redline) //so compression only happens on no accel
			engineDrag = (curRPM-carData.e_idle)*carData.e_compression * (Math.signum(wheelrot)); //reverse goes the other way
		
		float engineOutTorque = 0;
		if (Math.abs(curRPM) > carData.e_redline)
			engineOutTorque = -engineDrag; //kill engine if greater than redline, and only apply compression
		else //normal path
			engineOutTorque = eTorque*curGearRatio*diffRatio*carData.trans_effic - engineDrag;

		return engineOutTorque/driveWheelRadius();
	}

	private float driveWheelRadius() {
		if (carData.driveFront && carData.driveRear)
			return (carData.wheelData[0].radius + carData.wheelData[1].radius + carData.wheelData[2].radius + carData.wheelData[3].radius)/4f;
		if (carData.driveFront)
			return (carData.wheelData[0].radius + carData.wheelData[1].radius)/2f;
		if (carData.driveRear)
			return (carData.wheelData[2].radius + carData.wheelData[3].radius)/2f;

		Log.e("No drive wheels found");
		return 1;
	}
	
	private void autoTransmission(int rpm, float vz) {
		if (curGear == 0)
			return; //no changing out of reverse on me please...
		if (helper.H.allTrue((w) -> { return !w.inContact; }, wheels))
			return; //if no contact, no changing of gear
		
		float driveSpeed = (carData.trans_gearRatios[curGear]*carData.trans_finaldrive*(60/FastMath.TWO_PI))/carData.wheelData[0].radius;
		float gearUpSpeed = carData.auto_gearUp/driveSpeed; //TODO pre compute, as it doesn't change (you can also find the optimal gear change point based on the torque curve)
		float gearDownSpeed = carData.auto_gearDown/driveSpeed;
		
		//TODO: error checking that there is over lap  [2-----[3--2]----3] not [2------2]--[3-----3]
		
		if (vz > gearUpSpeed && curGear < carData.trans_gearRatios.length-1) {
			gearChangeTime = carData.auto_changeTime;
			gearChangeTo = curGear + 1;
		} else if (vz < gearDownSpeed && curGear > 1) {
			gearChangeTime = carData.auto_changeTime;
			gearChangeTo = curGear - 1;
		}
	}
	
	public PoweredState getPoweredState() {
		return new PoweredState(this);
	}
	
	public class PoweredState {
		//TODO please explain the rationale behind this class
		public final float accelCurrent;
		public final float brakeCurrent;
		public final boolean ifHandbrake;
		public final int curGear;
		public final float nitro;
		public final float steeringCurrent;
		public final int curRPM;
		public final float driftAngle;
		public PoweredState(RayCarPowered rayCarPowered) {
			accelCurrent = rayCarPowered.accelCurrent;
			brakeCurrent = rayCarPowered.brakeCurrent;
			curGear = rayCarPowered.curGear;
			nitro = rayCarPowered.nitro;
			steeringCurrent = rayCarPowered.steeringCurrent;
			curRPM = rayCarPowered.curRPM;
			ifHandbrake = rayCarPowered.handbrakeCurrent;
			driftAngle = rayCarPowered.driftAngle;
		}
	}
}
