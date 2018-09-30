package car.ray;

import java.util.function.Consumer;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.ray.WheelDataConst.WheelDataTractionConst;
import game.App;
import helper.H;
import helper.HelperObj;

//doesn't extend anything, but here are some reference classes
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/scene/Spatial.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/Transform.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-bullet/src/common/java/com/jme3/bullet/control/RigidBodyControl.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-jbullet/src/main/java/com/jme3/bullet/objects/PhysicsVehicle.java
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java

//handles suspension/traction/drag
public class RayCar implements PhysicsTickListener {

	//handy ids to help with removing magic numbers
	protected static int WHEEL_FL = 0, WHEEL_FR = 1, WHEEL_RL = 2, WHEEL_RR = 3;
	
	private static final Vector3f localDown = new Vector3f(0, -1, 0);
	
	protected final CarDataConst carData;
	private final CarRaycaster raycaster;
	protected final RigidBodyControl rbc;
	
	//simulation variables
	protected final RayWheel[] wheels;
	private float steeringCur;
	private float brakingCur;
	private boolean handbrakeCur;
	protected final float[] wheelTorque;
	
	public RayCar(CollisionShape shape, CarDataConst carData) {
		this.carData = carData;
		this.carData.refresh();
		
		this.raycaster = new CarRaycaster();
		
		this.wheels = new RayWheel[carData.wheelData.length];
		for (int i  = 0; i < wheels.length; i++) {
			wheels[i] = new RayWheel(i, carData.wheelData[i], carData.wheelOffset[i]);
		}
		this.wheelTorque = new float[4];

		rbc = new RigidBodyControl(shape, carData.mass);
        
        doForEachWheel((w_id) -> {
	        try {
				//generate the slip* max force from the car wheel data
				double error = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals
				
				this.wheels[w_id].maxLat = GripHelper.calcSlipMax(carData.wheelData[w_id].pjk_lat, error);
				if (Float.isNaN(this.wheels[w_id].maxLat))
					throw new Exception("maxlat was: '" + this.wheels[w_id].maxLat +"'.");
				this.wheels[w_id].maxLong = GripHelper.calcSlipMax(carData.wheelData[w_id].pjk_long, error);
				if (Float.isNaN(this.wheels[w_id].maxLong)) 
					throw new Exception("maxlong was: '" + this.wheels[w_id].maxLong +"'.");
				
			} catch (Exception e) {
				e.printStackTrace();
				H.p("error in calculating max(lat|long) values of: " + carData.getClass());
				System.exit(1);
			}
        });
	}

	@Override
	public void physicsTick(PhysicsSpace space, float tpf) {
		// this is intentionally left blank
	}
	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		applySuspension(space, tpf);
		
		applyTraction(space, tpf);
		
		applyDrag(space, tpf);
	}
	
	private void applySuspension(PhysicsSpace space, float tpf) {
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		
		doForEachWheel((w_id) -> {
			Vector3f localPos = carData.wheelOffset[w_id];
			
			Vector3f pos = vecLocalToWorld(localPos);
			//TODO use the start position offset
			Vector3f worldDown = w_angle.mult(localDown.mult(carData.sus_max_length + carData.wheelData[w_id].radius));
			final FirstRayHitDetails col = raycaster.castRay(space, rbc, pos, worldDown);
			
//			/*
			App.rally.enqueue(() -> {
				HelperObj.use(App.rally.getRootNode(), "sus"+w_id, 
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Cyan, worldDown.normalize().mult(carData.sus_max_length), pos));
				HelperObj.use(App.rally.getRootNode(), "wheel_radius"+w_id,
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Blue, worldDown.normalize().mult(carData.wheelData[w_id].radius), pos.add(worldDown.normalize().mult(carData.sus_max_length))));
				HelperObj.use(App.rally.getRootNode(), "castPoint"+w_id, 
						H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Red, wheels[w_id].curBasePosWorld, 0.01f));
			});
//			*/
			if (col == null) { //no suspension ray found, extend all the way and set force to 0
				wheels[w_id].inContact = false;
				wheels[w_id].susDiffLength = carData.sus_max_length;
				wheels[w_id].curBasePosWorld = localDown.mult(carData.sus_max_length + carData.wheelData[w_id].radius);
				wheels[w_id].susForce = 0;
				return; //no force
			}
			
			float susDist = col.dist - carData.wheelData[w_id].radius;
			if (susDist < 0) {
				wheels[w_id].inContact = false;
				wheels[w_id].susDiffLength = 0;
				wheels[w_id].curBasePosWorld = col.pos;
				wheels[w_id].susForce = 0; //TODO suspension bottomed out, do some large force...
				return; //no force
			}
						
			wheels[w_id].inContact = true;
			wheels[w_id].curBasePosWorld = col.pos;

			float denominator = col.hitNormalInWorld.dot(w_angle.invert().mult(localDown)); //loss due to difference between collision and localdown (cos ish)
			Vector3f relpos = wheels[w_id].curBasePosWorld.subtract(rbc.getPhysicsLocation()); //pos of sus contact point relative to car
			Vector3f velAtContactPoint = getVelocityInLocalPoint(relpos); //get sus vel at point on ground
			
			float projVel = col.hitNormalInWorld.dot(velAtContactPoint); //percentage of normal force that applies to the current motion
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
			
			//TODO 0 length -> 0 force, which isn't valid for our new physics. needs to have some offset as max sus travel isn't max spring travel
			wheels[w_id].susDiffLength = (carData.sus_max_length - susDist);
			// Spring
			wheels[w_id].susForce = carData.sus_stiffness * wheels[w_id].susDiffLength * clippedInvContactDotSuspension;
			
			// Damper
			float susp_damping = (projected_rel_vel < 0f) ? carData.susCompression() : carData.susRelax();
			wheels[w_id].susForce -= susp_damping * projected_rel_vel;
			
			// Limit: no negative forces or stupid high numbers
			wheels[w_id].susForce = FastMath.clamp(wheels[w_id].susForce * carData.mass, 0, carData.sus_max_force);
			
			Vector3f f = w_angle.mult(col.hitNormalInWorld.mult(wheels[w_id].susForce * tpf));
			rbc.applyImpulse(f, w_angle.mult(localPos));
			/*
			App.rally.enqueue(() -> {
				HelperObj.use(App.rally.getRootNode(), "normalforcearrow" + w_id, 
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Black, 
								col.hitNormalInWorld, col.pos));
				
				HelperObj.use(App.rally.getRootNode(), "normalforcearrow inv" + w_id,
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.White,
								col.hitNormalInWorld.mult(-denominator), col.pos));
				
				HelperObj.use(App.rally.getRootNode(), "forcearrow" + w_id, 
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Cyan, 
								f.mult(1/(2*carData.mass)), vecLocalToWorld(localPos)));
			});
//			*/			
		});
	}
	
	private void applyTraction(PhysicsSpace space, float tpf) {
		Vector3f w_pos = rbc.getPhysicsLocation();
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		Vector3f w_velocity = rbc.getLinearVelocity();
		Vector3f w_angVel = rbc.getAngularVelocity();
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		if (velocity.z == 0) //NaN on divide avoidance strategy
			velocity.z += 0.0001f;
		
		float steeringCur = this.steeringCur;
		if (velocity.z < 0) { //to flip the steering on moving in reverse
			steeringCur *= -1;
		}
		
		//a 'few' slow speed hacks
		float maxSlowLat = Float.MAX_VALUE;
		float slowslipspeed = 10; //TODO some proportion of car.w_Off, so cars don't feel so weird at slow speeds
		boolean isSlowSpeed = velocity.length() <= slowslipspeed;
		float rearSteeringCur = 0;
		if (isSlowSpeed) {
			//we want the force centre inline with the center at slow speeds
			rearSteeringCur = -steeringCur;
			
			if (steeringCur != 0) {
				float radius = carData.wheelOffset[0].z*2/FastMath.sin(steeringCur);
				
				float forceToKeepCircle = (carData.mass*rbc.getGravity().y/4)*velocity.lengthSquared()/radius;
				maxSlowLat = FastMath.abs(forceToKeepCircle/4);
			}
		}
		
		final float slip_div = Math.abs(velocity.length());
		final float slip_const = (isSlowSpeed ? 2 : 1);
		final float steeringFake = steeringCur;
		final float steeringFakeRear = rearSteeringCur;
		final float maxSlowLatFake = maxSlowLat;
		doForEachWheel((w_id) -> {
			if (!this.wheels[w_id].inContact || wheels[w_id].susForce <= 0)
				return; //yay no math to do
			
			Vector3f wheel_force = new Vector3f();
			
			//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
			float yawspeed = 0;
			if (!Float.isNaN(w_angVel.y))
				yawspeed = carData.wheelOffset[w_id].z * w_angVel.normalize().y; //TODO check
			
			float slipr = slip_const*wheels[w_id].radSec * carData.wheelData[w_id].radius - velocity.z;
			float slipratio = slipr/slip_div;
			
			if (handbrakeCur && w_id > 1) //rearwheels only
				wheels[w_id].radSec = 0;
			
			float slipangle = 0;
			if (isFrontWheel(w_id)) {
				float slipa_front = (velocity.x + yawspeed*carData.wheelOffset[w_id].z);
				slipangle = (float)(FastMath.atan(slipa_front / slip_div) - steeringFake);
			} else { //rear
				float slipa_rear = (velocity.x - yawspeed*carData.wheelOffset[w_id].z);
				slipangle = (float)(FastMath.atan(slipa_rear / slip_div) - steeringFakeRear); //slip_div is questionable here
			}
			slipangle *= slip_const;
			
			//start work on merging the forces into a traction circle
			float ratiofract = slipratio/this.wheels[w_id].maxLong;
			float anglefract = slipangle/this.wheels[w_id].maxLat;
			float p = FastMath.sqrt(ratiofract*ratiofract + anglefract*anglefract);
			
			//calc the longitudinal force from the slip ratio
			wheel_force.z = (ratiofract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_long, p*this.wheels[w_id].maxLong) * wheels[w_id].susForce;
			
			//latitudinal force that is calculated off the slip angle
			wheel_force.x = -(anglefract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_lat, p*this.wheels[w_id].maxLat) * wheels[w_id].susForce;
			
			//prevents the force from exceeding the centripetal force
			//TODO fix?
			if (isSlowSpeed && Math.abs(wheel_force.x) > maxSlowLatFake) {
				//[1-N/M] means the clamp is smaller for smaller speeds
				float clampValue = Math.abs(maxSlowLatFake * (1-slowslipspeed/velocity.length()));
				wheel_force.x = FastMath.clamp(wheel_force.x, -clampValue, clampValue);
			}
			//prevent the x forces from being larger than the force that would stop us from changing our x velocity sign
			if (isSlowSpeed) { //does cause us to not be able to turn for like ~0.2sec sometimes
				float limit = Math.max(Math.abs(velocity.x)*(carData.mass/4)/tpf, 1000);
				wheel_force.x = FastMath.clamp(wheel_force.x, -limit, limit);
			}
			
			wheels[w_id].skidFraction = p;

			// braking and abs
			float brakeCurrent2 = brakingCur;
			if (wheels[w_id].skidFraction > 1 && velocity.length() > 2 && brakingCur == 1)
				brakeCurrent2 = 0; //abs (which works way too well i think)
			
			//add the wheel force after merging the forces
			float totalLongForce = wheelTorque[w_id] - wheel_force.z - (brakeCurrent2*carData.brakeMaxTorque*Math.signum(wheels[w_id].radSec));
			float nextRadSec = wheels[w_id].radSec + tpf*totalLongForce/(carData.e_inertia(w_id));
			if (brakingCur != 0 && Math.signum(wheels[w_id].radSec) != Math.signum(nextRadSec)) //TODO find the real first variable in this
				wheels[w_id].radSec = 0; //maxed out the forces with braking, so prevent wheels from moving
			else
				wheels[w_id].radSec += tpf*totalLongForce/(carData.e_inertia(w_id));
			
			rbc.applyImpulse(w_angle.mult(wheel_force).mult(tpf), wheels[w_id].curBasePosWorld.subtract(w_pos));
		});
	}
	
	private void applyDrag(PhysicsSpace space, float tpf) {
		//linear resistance and quadratic drag (https://en.wikipedia.org/wiki/Automobile_drag_coefficient#Drag_area)
		//float rollingResistance = carData.rollingResistance(9.81f); //TODO was removed because i need the suspension normal force to caculate correctly
		
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		Vector3f w_velocity = rbc.getLinearVelocity();
		
		float dragx = -(1.225f * carData.areo_drag * carData.areo_crossSection * w_velocity.x * FastMath.abs(w_velocity.x));
		float dragy = -(1.225f * carData.areo_drag * carData.areo_crossSection * w_velocity.y * FastMath.abs(w_velocity.y));
		float dragz = -(1.225f * carData.areo_drag * carData.areo_crossSection * w_velocity.z * FastMath.abs(w_velocity.z));
		//TODO change cross section for each xyz to make a realistic drag feeling
		
		Vector3f totalNeutral = new Vector3f(dragx, dragy, dragz);
		//TODO: report this out: float dragForce = totalNeutral.length();
		
		float dragDown = -0.5f * carData.areo_downforce * 1.225f * (w_velocity.z*w_velocity.z); //formula for downforce from wikipedia
		rbc.applyCentralForce(w_angle.mult(totalNeutral.add(0, dragDown, 0))); //apply downforce after
	}
	
	/////////////////
	//control methods
	public float getSteering() {
		return this.steeringCur; 
	}
	public void setSteering(float value) {
		this.steeringCur = value;
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
	private Vector3f vecWorldToLocal(Vector3f in) {
		Vector3f out = new Vector3f();
		in.subtract(rbc.getPhysicsLocation(), out);
		rbc.getPhysicsRotation().inverse().mult(out, out);
		return out;
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
			return w.D * FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
		}

		//returns the slip value that gives the closest to 1 from the magic formula (should be called twice, lat and long)
		public static float calcSlipMax(WheelDataTractionConst w, double error) {
			double lastX = 0.2f; //our first guess (usually finishes about 0.25f)
			double nextX = lastX + 5*error; //just so its a larger diff that error

			while (Math.abs(lastX - nextX) > error) {
				lastX = nextX;
				nextX = iterate(w, lastX, error);
			}
			return (float)nextX;
		}
		private static double iterate(WheelDataTractionConst w, double x, double error) {
			return x - ((tractionFormula(w, (float)x)-w.D) / dtractionFormula(w, (float)x, error)); 
			//-1 because we are trying to find a max (which happens to be 1)
		}
		private static double dtractionFormula(WheelDataTractionConst w, double slip, double error) {
			return (tractionFormula(w, (float)(slip+error)) - tractionFormula(w , (float)(slip-error)))/ (2*error);
		}
	}
}