package car.ray;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

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
	protected int curRPM = 1000;
	protected int gearChangeTo;
	protected float gearChangeTime;
	protected float clutch; //TODO use //0 = can drive, 1 = can't drive
	 
	protected float redlineKillTimeout;
	
	public RayCarPowered(CollisionShape shape, CarDataConst carData) {
		super(shape, carData);
		
		this.nitro = carData.nitro_max;
	}

	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		Vector3f w_velocity = rbc.getLinearVelocity();
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		
		engineTorque = getEngineWheelTorque(tpf, velocity.length() * Math.signum(velocity.z));
		
		float[] torques = new float[] { 0, 0, 0, 0 };

		//split once for each 2 wheels
		float frontTorque = engineTorque/2;
		float rearTorque = engineTorque/2;
		if (carData.driveFront && carData.driveRear) {
			float frontForce = (wheels[0].gripDir.z + wheels[1].gripDir.z)/2;
			float rearForce = (wheels[2].gripDir.z + wheels[3].gripDir.z)/2;
			//calc center diff
			float diff = (frontForce - rearForce)*FastMath.sign((frontForce + rearForce)/2);
			frontTorque = (engineTorque/2)*(1f - 2*FastMath.atan(carData.w_difflock*diff)/FastMath.PI); //TODO constant for center diff
			rearTorque = (engineTorque/2)*(1f + 2*FastMath.atan(carData.w_difflock*diff)/FastMath.PI);
		}
		if (carData.driveFront) {
			//calc front diff
			float diff = (wheels[0].gripDir.z - wheels[1].gripDir.z)*FastMath.sign((wheels[0].gripDir.z + wheels[1].gripDir.z)/2);
			torques[0] = frontTorque*(1f - 2*FastMath.atan(carData.w_difflock*diff)/FastMath.PI);
			torques[1] = frontTorque*(1f + 2*FastMath.atan(carData.w_difflock*diff)/FastMath.PI);
		}
		if (carData.driveRear) {
			//calc rear diff
			float diff = (wheels[2].gripDir.z - wheels[3].gripDir.z)*FastMath.sign((wheels[2].gripDir.z + wheels[3].gripDir.z)/2);
			torques[2] = rearTorque*(1f - 2*FastMath.atan(carData.w_difflock*diff)/FastMath.PI);
			torques[3] = rearTorque*(1f + 2*FastMath.atan(carData.w_difflock*diff)/FastMath.PI);
		}
		doForEachWheel((w_id) -> {
			setWheelTorque(w_id, torques[w_id]);			
		});
		
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
		
		curRPM = Math.max(curRPM, carData.e_idle); //no stall please, its bad enough that we don't have torque here
		
		autoTransmission(curRPM, vz);

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

		//TODO fake some kind of clutch at slow speeds in first (to prevent very low torques at 1000 rpm)
		
		float eTorque = (carData.lerpTorque(curRPM) + nitroForce)*accelCurrent;
		float engineDrag = 0;
		if (accelCurrent < 0.05f || curRPM > carData.e_redline) //so compression only happens on no accel
			engineDrag = (curRPM-carData.e_idle)*carData.e_compression * (Math.signum(wheelrot)); //reverse goes the other way
		
		float engineOutTorque = 0;
		if (Math.abs(curRPM) > carData.e_redline)
			engineOutTorque = -engineDrag; //kill engine if greater than redline, and only apply compression
		else //normal path
			engineOutTorque = eTorque*curGearRatio*diffRatio*carData.trans_effic - engineDrag;

		return engineOutTorque/carData.wheelData[0].radius; //TODO pick a better wheel for drive wheel size
	}
	
	private void autoTransmission(int rpm, float vz) {
		if (curGear == 0)
			return; //no changing out of reverse on me please...
		if (helper.H.allTrue((w) -> { return !w.inContact; }, wheels))
			return; //if no contact, no changing of gear
		
		float driveSpeed = (carData.trans_gearRatios[curGear]*carData.trans_finaldrive*(60/FastMath.TWO_PI))/carData.wheelData[0].radius;
		float gearUpSpeed = carData.auto_gearUp/driveSpeed; //TODO pre compute, as it doesn't change
		float gearDownSpeed = carData.auto_gearDown/driveSpeed;
		
		//TODO: error checking that there is over lap  [2-----[3--2]----3] not [2------2]--[3-----3]

		//TODO check that these values are actually working
		
		if (vz > gearUpSpeed && curGear < carData.trans_gearRatios.length-1) {
			curGear++;
			//TODO:
			/*clutch = 1;
			gearChangeTo = curGear + 1;*/
		} else if (vz < gearDownSpeed && curGear > 1) {
			curGear--;
			/*clutch = 1;
			gearChangeTo = curGear - 1;*/
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
