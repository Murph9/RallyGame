package car.ray;

import java.util.function.Consumer;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.data.CarDataConst;
import car.data.CarSusDataConst;
import car.data.WheelDataTractionConst;

//doesn't extend anything, but here are some reference classes
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/scene/Spatial.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/Transform.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-bullet/src/common/java/com/jme3/bullet/control/RigidBodyControl.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-jbullet/src/main/java/com/jme3/bullet/objects/PhysicsVehicle.java
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java


//Extending traction model:
//add in a quadratic normal force forumla normal(x) = NK1 + N^2*K2 + K3
//ideally replaces E in the base formla from: https://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
//Example given: https://github.com/chrisoco/M120/blob/master/RaceCar/RCAS/src/rcas/model/MagicFormulaTireModel.java

/** Handles suspension/traction/drag and real-time data of this car */
public class RayCar implements PhysicsTickListener {
	
	//handy ids to help with removing magic numbers
	protected static int WHEEL_FL = 0, WHEEL_FR = 1, WHEEL_RL = 2, WHEEL_RR = 3;
	
	private static final Vector3f localDown = new Vector3f(0, -1, 0);
	
	protected CarDataConst carData;
	private final CarRaycaster raycaster;
    protected final RigidBodyControl rbc;
    protected boolean rbEnabled() { return rbc.isEnabled() && rbc.isInWorld() && rbc.isActive(); }
	
	//simulation variables
	protected final RayWheel[] wheels;
	private float steeringCur;
	private float brakingCur;
	private boolean handbrakeCur;
	protected final float[] wheelTorque;
	
	//debug values
	protected float rollingResistance;
	protected Vector3f dragDir;
	protected float driftAngle;
	public final Vector3f planarGForce;
	
	//hacks
	protected boolean tractionEnabled = true;
	
	public RayCar(CollisionShape shape, CarDataConst carData) {
		this.carData = carData;
		
		this.planarGForce = new Vector3f();
		this.raycaster = new CarRaycaster();
		
		this.wheels = new RayWheel[carData.wheelData.length];
		for (int i  = 0; i < wheels.length; i++) {
			wheels[i] = new RayWheel(i, carData.wheelData[i], carData.wheelOffset[i]);
		}
		this.wheelTorque = new float[4];
		this.rbc = new RigidBodyControl(shape, carData.mass);

		//a fake angular rotational, very important for driving feel
		this.rbc.setAngularDamping(0.4f);
	}

	@Override
	public void physicsTick(PhysicsSpace space, float tpf) {
		// this is intentionally left blank
	}
	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
        if (!rbEnabled())
            return;

		applySuspension(space, tpf);
		
		//TODO apply the midpoint formula or any kind of actual physics stepped simulation method
		//https://en.wikipedia.org/wiki/Midpoint_method
		if (tractionEnabled)
			applyTraction(space, tpf);
		
		applyDrag(space, tpf);
		
		//TODO give any forces back to the collision object the wheel hits
		//should use wheels[i].collisionObject
		
		//TODO use the velocity of the object in calculations (suspension and traction)
	}
	
	private void applySuspension(PhysicsSpace space, float tpf) {
		Vector3f w_pos = rbc.getPhysicsLocation();
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		
		//Do suspension ray cast
		doForEachWheel((w_id) -> {
			Vector3f localPos = carData.wheelOffset[w_id];
			
			CarSusDataConst sus = carData.susByWheelNum(w_id);
			float susTravel = sus.travelTotal();
			
			//cast ray from suspension min, to max + radius (radius because bottom out check might as well be the size of the wheel)
			wheels[w_id].rayStartWorld = vecLocalToWorld(localPos.add(localDown.mult(sus.min_travel)));
			wheels[w_id].rayDirWorld = w_angle.mult(localDown.mult(susTravel + carData.wheelData[w_id].radius));
			FirstRayHitDetails col = raycaster.castRay(space, rbc, wheels[w_id].rayStartWorld, wheels[w_id].rayDirWorld);
						
			if (col == null) { //suspension ray found nothing, extend all the way and don't apply a force (set to 0)
				wheels[w_id].inContact = false;
				wheels[w_id].susRayLength = susTravel;
				wheels[w_id].curBasePosWorld = localDown.mult(susTravel + carData.wheelData[w_id].radius);
				return; //no force
			}
			
			wheels[w_id].hitNormalInWorld = col.hitNormalInWorld;
			wheels[w_id].collisionObject = col.obj;
			wheels[w_id].susRayLength = col.dist - carData.wheelData[w_id].radius; //remove the wheel radius
			wheels[w_id].curBasePosWorld = col.pos;
			wheels[w_id].inContact = true; //wheels are still touching..
		});
		
		//Do suspension forces
		doForEachWheel((w_id) -> {
			CarSusDataConst sus = carData.susByWheelNum(w_id);
			if (!wheels[w_id].inContact) {
				wheels[w_id].susForce = 0;
				return;
			}
			
			if (wheels[w_id].susRayLength < 0) { //suspension bottomed out 
				//TODO do some proper large sus/damp force to stop vehicle motion/rotation down
				wheels[w_id].susForce = (sus.preload_force+sus.travelTotal())*sus.stiffness*1000;
				Vector3f f = w_angle.invert().mult(wheels[w_id].hitNormalInWorld.mult(wheels[w_id].susForce * tpf));
				rbc.applyImpulse(f, wheels[w_id].curBasePosWorld.subtract(w_pos));
				return;
			}
			
			float denominator = wheels[w_id].hitNormalInWorld.dot(w_angle.mult(localDown)); //loss due to difference between collision and localdown (cos ish)
			Vector3f relpos = wheels[w_id].curBasePosWorld.subtract(rbc.getPhysicsLocation()); //pos of sus contact point relative to car origin
			Vector3f velAtContactPoint = getVelocityInLocalPoint(relpos); //get sus vel at point on ground
			
			float projVel = wheels[w_id].hitNormalInWorld.dot(velAtContactPoint); //percentage of normal force that applies to the current motion
			float projected_rel_vel = 0;
			float clippedInvContactDotSuspension = 0;
			if (denominator >= -0.1f) {
				projected_rel_vel = 0f;
				clippedInvContactDotSuspension = 1f / 0.1f;
			} else {
				float inv = -1f / denominator;
				projected_rel_vel = projVel * inv;
				clippedInvContactDotSuspension = inv;
			}
			
			// Calculate spring distance from its zero length, as it should be outside the suspension range
			float springDiff = sus.travelTotal() - wheels[w_id].susRayLength;
			
			// Spring
			wheels[w_id].susForce = (sus.preload_force + sus.stiffness * springDiff) * clippedInvContactDotSuspension;
			
			// Damper
			float susp_damping = (projected_rel_vel < 0f) ? sus.compression() : sus.relax();
			wheels[w_id].susForce -= susp_damping * projected_rel_vel;
			
			//Sway bars https://forum.miata.net/vb/showthread.php?t=25716
			int w_id_other = w_id == 0 ? 1 : w_id == 1 ? 0 : w_id == 2 ? 3 : 2; //fetch the id or the other side
			float swayDiff = wheels[w_id_other].susRayLength - wheels[w_id].susRayLength;
			wheels[w_id].susForce += swayDiff * sus.antiroll;
			
			wheels[w_id].susForce *= 1000; //kN
			
			//applyImpulse (force = world space, pos = relative to local)
			Vector3f f = wheels[w_id].hitNormalInWorld.mult(wheels[w_id].susForce * tpf);
			rbc.applyImpulse(f, wheels[w_id].curBasePosWorld.subtract(w_pos)); 
		});

	}
	
	private void applyTraction(PhysicsSpace space, float tpf) {
		Vector3f w_pos = rbc.getPhysicsLocation();
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		Vector3f w_velocity = rbc.getLinearVelocity();
		Vector3f w_angVel = rbc.getAngularVelocity();
		
		planarGForce.set(Vector3f.ZERO); //reset
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		if (velocity.z == 0) //NaN on divide avoidance strategy
			velocity.z += 0.0001f;
		
		float steeringCur = this.steeringCur;
		if (velocity.z < 0) { //to flip the steering on moving in reverse
			steeringCur *= -1;
		}
		
		final float slip_div = Math.abs(velocity.length());
		final float steeringFake = steeringCur;
		doForEachWheel((w_id) -> {
			Vector3f wheel_force = new Vector3f();
			
			// Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
			float angVel = 0;
			if (!Float.isNaN(w_angVel.y))
				angVel = w_angVel.y;
			
			float slipr = wheels[w_id].radSec * carData.wheelData[w_id].radius - velocity.z;
			float slipratio = slipr/slip_div;
			
			if (handbrakeCur && !isFrontWheel(w_id)) //rearwheels only
				wheels[w_id].radSec = 0;
			
			float slipangle = 0;
			if (isFrontWheel(w_id)) {
				float slipa_front = velocity.x + carData.wheelOffset[w_id].z * angVel;
				slipangle = (float)(FastMath.atan2(slipa_front, slip_div) - steeringFake);
			} else { //so rear
				float slipa_rear = velocity.x + carData.wheelOffset[w_id].z * angVel;
				driftAngle = slipa_rear; //set drift angle as the rear amount
				slipangle = (float)(FastMath.atan2(slipa_rear, slip_div)); //slip_div is questionable here
			}
			
			//merging the forces into a traction circle
			//normalise based on their independant max values 
			float ratiofract = slipratio/carData.wheelData[w_id].maxLong;
			float anglefract = slipangle/carData.wheelData[w_id].maxLat;
			float p = FastMath.sqrt(ratiofract*ratiofract + anglefract*anglefract);
			if (p == 0)
				p = 0.001f;
			
			wheels[w_id].skidFraction = p;
			
			//calc the longitudinal force from the slip ratio
			wheel_force.z = (ratiofract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_long, p*carData.wheelData[w_id].maxLong) * GripHelper.loadFormula(carData.wheelData[w_id].pjk_long, this.wheels[w_id].susForce);
			//calc the latitudinal force from the slip angle
			wheel_force.x = -(anglefract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_lat, p*carData.wheelData[w_id].maxLat) * GripHelper.loadFormula(carData.wheelData[w_id].pjk_lat, this.wheels[w_id].susForce);
			
			// braking and abs
			float brakeCurrent2 = brakingCur;
			if (Math.abs(ratiofract) >= 1 && velocity.length() > 2 && brakingCur == 1)
				brakeCurrent2 = 0; //abs (which i think works way too well gameplay wise)
			
			//add the wheel force after merging the forces
			float totalLongForce = wheelTorque[w_id] - wheel_force.z - (brakeCurrent2*carData.brakeMaxTorque*Math.signum(wheels[w_id].radSec));
			float totalLongForceTorque = tpf*totalLongForce/(carData.e_inertia()) * carData.wheelData[w_id].radius;
			if (brakingCur != 0 && Math.signum(wheels[w_id].radSec) != Math.signum(wheels[w_id].radSec + totalLongForceTorque))
				wheels[w_id].radSec = 0; //maxed out the forces with braking, so prevent wheels from moving
			else
				wheels[w_id].radSec += totalLongForceTorque; //so the radSec can be used next frame, to calculate slip ratio
			
			wheels[w_id].gripDir = wheel_force;
			rbc.applyImpulse(w_angle.mult(wheel_force).mult(tpf), wheels[w_id].curBasePosWorld.subtract(w_pos));
			
			planarGForce.addLocal(wheel_force);
		});
		
		planarGForce.multLocal(1/carData.mass); //F=m*a => a=F/m
	}
	
	private void applyDrag(PhysicsSpace space, float tpf) {
		//rolling resistance (https://en.wikipedia.org/wiki/Rolling_resistance)
		Vector3f w_pos = rbc.getPhysicsLocation();
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		Vector3f w_velocity = rbc.getLinearVelocity();
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		rollingResistance = 0;
		doForEachWheel((w_id) -> {
			if (!wheels[w_id].inContact)
				return;
			
			//apply rolling resistance in the negative direction
			Vector3f wheel_force = new Vector3f(0, 0, FastMath.sign(velocity.z)* - carData.rollingResistance(w_id, wheels[w_id].susForce));
			rollingResistance += Math.abs(wheel_force.z); //for debug reasons
			rbc.applyImpulse(w_angle.mult(wheel_force).mult(tpf), wheels[w_id].curBasePosWorld.subtract(w_pos));
		});

		//quadratic drag
		dragDir = carData.quadraticDrag(w_velocity);
		
		float dragDown = -0.5f * carData.areo_downforce * 1.225f * (w_velocity.z*w_velocity.z); //formula for downforce from wikipedia
		rbc.applyCentralForce(dragDir.add(0, dragDown, 0)); //apply downforce after
	}
	
	/////////////////
	//control methods
	public float getSteering() {
		return this.steeringCur; 
	}
	public void setSteering(float value) {
		this.steeringCur = value;
		
		this.wheels[0].steering = value;
		this.wheels[1].steering = value;
	}
	public float getBraking() {
		return this.brakingCur;
	}
	public void setBraking(float value) {
		this.brakingCur = value;
	}
	public boolean getHandbrake() {
		return this.handbrakeCur;
	}
	public void setHandbrake(boolean value) {
		this.handbrakeCur = value;
	}
	
	public void setWheelTorque(int w_id, float torque) {
		wheelTorque[w_id] = torque;
	}
	public float getWheelTorque(int w_id) {
		if (w_id < 0 || w_id > 4)
			return 0;
		return wheelTorque[w_id];
	}
	
	/////////////////
	//helper functions
	protected void doForEachWheel(Consumer<Integer> func) {
		if (func == null)
			throw new NullPointerException("func is null");
		
		for (int i = 0; i < wheels.length; i++)
			func.accept(i);
	}
	
	private Vector3f vecLocalToWorld(Vector3f in) {
		Vector3f out = new Vector3f();
		return rbc.getPhysicsRotationMatrix().mult(out.set(in), out).add(rbc.getPhysicsLocation());
	}

	private Vector3f getVelocityInLocalPoint(Vector3f rel_pos) {
		//http://www.letworyinteractive.com/blendercode/dd/d16/btRigidBody_8h_source.html#l00366
		Vector3f vel = rbc.getLinearVelocity();
		Vector3f ang = rbc.getAngularVelocity();
		return vel.add(ang.cross(rel_pos));
	}

	private boolean isFrontWheel(int w_id) {
		return w_id < 2;
	}


	public static class GripHelper {
		private static float ERROR = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals
		
		//http://www.gamedev.net/topic/462784-simplified-pacejka-magic-formula/

		//There were fancy versions of the Pacejka's Formula here but there were removed
		//Try the git repositiory to get them back. (it should say 'removed' in the git message)

		/** Pacejka's Formula simplified from the bottom of:.
		 * http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
		 * @param w Pick the lateral or longitudial version to send.
		 * @param slip Slip angle or slip ratio (it doesn't matter except for one value changes on it)
		 * @return The force expected
		 */
		public static float tractionFormula(WheelDataTractionConst w, float slip) {
			return FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
		}
		public static float loadFormula(WheelDataTractionConst w, float load) {
			return Math.max(0, w.D1 * (1 - w.D2 * load) * load);
		}
		public static float calcMaxLoad(WheelDataTractionConst w) {
			return loadFormula(w, dloadFormula(w));
		}
		private static float dloadFormula(WheelDataTractionConst w) {
			return 1 / (2f * w.D2);
		}

		//returns the slip value that gives the closest to 1 from the magic formula (should be called twice, lat and long)
		public static float calcSlipMax(WheelDataTractionConst w) {
			double lastX = 0.2f; //our first guess (usually finishes about 0.25f)
			double nextX = lastX + 10*ERROR; //just so its a larger diff that error

			while (Math.abs(lastX - nextX) > ERROR) {
				lastX = nextX;
				nextX = iterate(w, lastX, ERROR);
			}
			
			if (!Double.isNaN(nextX))
				return (float)nextX;

			//attempt guess type 2 (numerical) (must be between 0 and 2)
			double max = -1;
			float pos = -1;
			for (int i = 0; i < 200; i++) {
				float value = tractionFormula(w, i/100f);
				if (value > max) {
					max = value;
					pos = i/100f;
				}
			}
			
			if (max < 0)
				return Float.NaN;
			
			return pos;
		}
		private static double iterate(WheelDataTractionConst w, double x, double error) {
			return x - ((tractionFormula(w, (float)x)-1) / dtractionFormula(w, (float)x, error)); 
			//-1 because we are trying to find a max (which happens to be 1)
		}
		private static double dtractionFormula(WheelDataTractionConst w, double slip, double error) {
			return (tractionFormula(w, (float)(slip+error)) - tractionFormula(w , (float)(slip-error)))/ (2*error);
		}
	}
}