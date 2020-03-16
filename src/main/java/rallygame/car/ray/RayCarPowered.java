package rallygame.car.ray;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import rallygame.car.data.CarDataConst;
import rallygame.helper.H;

//handles engine/drivetrain stuff
public class RayCarPowered extends RayCar {
	
	protected static final int REVERSE_GEAR_INDEX = 0;
	private static final float NITRO_COOLDOWN = 5;

	private float engineTorque;
	
	protected float accelCurrent;
	
	protected boolean ifNitro;
	private float nitroTimeout;
	private float nitroRemaining;
	protected float getNitroRemaining() { return nitroRemaining; }
	
	protected int curGear;
	protected int curRPM;
	private int gearChangeTo;
	private float gearChangeTime;
	
	public RayCarPowered(CollisionShape shape, CarDataConst carData) {
		super(shape, carData);
		this.nitroRemaining = carData.nitro_on ? carData.nitro_max : 0;
		this.curGear = 1;
	}
	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
        if (!rbEnabled())
            return;

		Quaternion w_angle = rbc.getPhysicsRotation();
		Vector3f w_velocity = rbc.getLinearVelocity();
		
		Vector3f velocity = w_angle.inverse().mult(w_velocity);
		
		engineTorque = getEngineWheelTorque(tpf);
        
        //subtract the largest ground velocity for the drive wheels, so moving on things doesn't break the transmission
        Vector3f groundVel = new Vector3f();
        for (int w_id = 0; w_id == wheels.length; w_id++) {
            if (!carData.driveFront && (w_id == 0 || w_id == 1))
                continue;
            if (!carData.driveRear && (w_id == 2 || w_id == 3))
                continue;
            Vector3f vel = w_angle.inverse().mult(wheels[w_id].collisionObject.getLinearVelocity());
            if (vel.length() > groundVel.length())
                groundVel = vel;
        };
        velocity.subtractLocal(groundVel);

		float vz = velocity.length() * Math.signum(velocity.z);
		autoTransmission(tpf, curRPM, vz);

		//split once for each 2 wheels
		float frontTorque = engineTorque/2;
		float rearTorque = engineTorque/2;
		
		/* Diff info
		 * https://forum.unity.com/threads/limited-slip-differential-modelling.517992/
		 * 
		 * logic goes:
		 * LSD is trying to keep the 2 wheels rotating at the same speed (or at least near)
		 * Maximum is the TBR (torque bias ratio), which is a ratio between the 2 input torques
		 * The only config value should be the TBR of like 1 or 2
		 * 
		 * In summary it looks like the torque to each of the tyres is the same.
		 * The lsd might actually be doing the opposite of what i think.
		 * https://github.com/VDrift/vdrift/blob/89e78a55a3cd0f5babac74ab7440f457a8848f25/src/physics/cardynamics.cpp
		 * 
		 * The previous code was actually very wrong, not just a little.
		 * 
		 * i think this eventually means the system needs to be a matrix solver state system rather than real time timestep stuff
		 * http://myselph.de/gamePhysics/equalityConstraints.html
         * 
         * but this is too much work
		 */

		if (carData.driveFront) {
			wheelTorque[0] = frontTorque;
			wheelTorque[1] = frontTorque;
		}
		if (carData.driveRear) {
			wheelTorque[2] = rearTorque;
			wheelTorque[3] = rearTorque;
		}
		if (carData.driveFront && carData.driveRear) {
			float balalnce = FastMath.clamp(carData.trans_powerBalance, 0, 1);
			//apply power balance
			wheelTorque[0] = wheelTorque[0] * (1-balalnce);
			wheelTorque[1] = wheelTorque[1] * (1-balalnce);
			
			wheelTorque[2] = wheelTorque[2] * balalnce;
			wheelTorque[3] = wheelTorque[3] * balalnce;
		}
		
		super.prePhysicsTick(space, tpf);
	}
	
	private float getEngineWheelTorque(float tpf) {
		if (!carData.driveFront && !carData.driveRear) 
			return 0;

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
		
		float nitroForce = calcNitro(tpf);

		float eTorque = (carData.lerpTorque(curRPM) + nitroForce)*accelCurrent;
		float engineDrag = 0;
		if (accelCurrent < 0.01f || curRPM > carData.e_redline) //so compression only happens on no accel
			engineDrag = (curRPM-carData.e_idle)*carData.e_compression * (Math.signum(wheelrot)); //reverse goes the other way
		
		float engineOutTorque = 0;
		if (Math.abs(curRPM) > carData.e_redline)
			engineOutTorque = -engineDrag; //kill engine if greater than redline, and only apply compression
		else //normal path
			engineOutTorque = eTorque*curGearRatio*diffRatio*carData.trans_effic - engineDrag;

		return engineOutTorque/carData.driveWheelRadius();
	}
	
	private void autoTransmission(float tpf, int rpm, float vz) {
		if (gearChangeTime != 0) {
			gearChangeTime -= tpf;
			if (gearChangeTime < 0) { // if equal probably shouldn't be trying to set the gear
				curGear = gearChangeTo;
				gearChangeTime = 0;
			}
			return;
		}

		if (curGear == REVERSE_GEAR_INDEX)
			return; //no changing out of reverse on me please...
		if (rallygame.helper.H.allTrue((w) -> { return !w.inContact; }, wheels))
			return; //if no contact, no changing of gear
        
		if (vz > carData.getGearUpSpeed(curGear) && curGear < carData.trans_gearRatios.length-1) {
			gearChangeTime = carData.auto_changeTime;
			gearChangeTo = curGear + 1;
		} else if (vz < carData.getGearDownSpeed(curGear) && curGear > 1) {
			gearChangeTime = carData.auto_changeTime;
			gearChangeTo = curGear - 1;
		}
	}

	private float calcNitro(float tpf) {
		if (!carData.nitro_on) {
			return 0;
		}
		
		float result = 0;
		if (ifNitro) {
			//reset cooldown if pressed
			this.nitroTimeout = NITRO_COOLDOWN;

			if (this.nitroRemaining > 0) {
				//apply force if there exists some
				result = carData.nitro_force;
				this.nitroRemaining -= 2 * tpf * carData.nitro_rate;
			}
		} else {
			//if not pressing, decay delay until we can increase the amount again
			this.nitroTimeout -= tpf;
			if (this.nitroTimeout <= 0) {
				this.nitroRemaining += carData.nitro_rate * tpf;
				this.nitroRemaining = Math.min(this.nitroRemaining, this.carData.nitro_max);
			}
		}

		return result;
	}

	public final String statsString() {
		return rallygame.helper.H.round3f(rbc.getPhysicsLocation(), 2) + "\nspeed:" + rallygame.helper.H.round3f(rbc.getLinearVelocity(), 2)
                + "m/s\nRPM:" + curRPM + "\nengine:" + engineTorque + "\ndrag:" + H.roundDecimal(dragDir.length(), 3) + " rr("
                + H.roundDecimal(rollingResistanceTotal(), 3) + ")" + "N";
    }
    
    private float rollingResistanceTotal() {
        float total = 0;
        for (int w_id = 0; w_id < wheels.length; w_id++) {
            total += wheels[w_id].rollingResistance;
        }
        return total;
    }
}
 