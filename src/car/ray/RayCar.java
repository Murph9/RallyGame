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

import game.App;
import helper.H;
import helper.HelperObj;
import helper.Log;

//doesn't extend anything, but here are some reference classes
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/scene/Spatial.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/Transform.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-bullet/src/common/java/com/jme3/bullet/control/RigidBodyControl.java
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-jbullet/src/main/java/com/jme3/bullet/objects/PhysicsVehicle.java
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java

//handles suspension/traction/drag
public class RayCar implements PhysicsTickListener {

	private static final boolean DEBUG = false;
	private static final boolean DEBUG_SUS = DEBUG || false; //prevents a warning done this way
	private static final boolean DEBUG_SUS2 = DEBUG || false;
	private static final boolean DEBUG_DRAG = DEBUG || false;
	
	//handy ids to help with removing magic numbers
	protected static int WHEEL_FL = 0, WHEEL_FR = 1, WHEEL_RL = 2, WHEEL_RR = 3;
	
	private static final Vector3f localDown = new Vector3f(0, -1, 0);
	
	protected CarDataConst carData;
	private final CarRaycaster raycaster;
	protected final RigidBodyControl rbc;
	
	//simulation variables
	protected final RayWheel[] wheels;
	private float steeringCur;
	private float brakingCur;
	private boolean handbrakeCur;
	protected final float[] wheelTorque;
	
	//debug values
	protected float dragValue;
	protected float driftAngle;
	public final Vector3f planarGForce;
	
	public RayCar(CollisionShape shape, CarDataConst carData) {
		this.carData = carData;
		this.carData.load();
		if (!this.carData.loaded) {
			try {
				throw new Exception("Car data not correctly loaded");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		this.planarGForce = new Vector3f();
		
		this.raycaster = new CarRaycaster();
		
		this.wheels = new RayWheel[carData.wheelData.length];
		for (int i  = 0; i < wheels.length; i++) {
			wheels[i] = new RayWheel(i, carData.wheelData[i], carData.wheelOffset[i]);
		}
		this.wheelTorque = new float[4];

		rbc = new RigidBodyControl(shape, carData.mass);
		
		// TODO check that rest suspension position is within min and max
		Vector3f grav = new Vector3f();
		App.rally.bullet.getPhysicsSpace().getGravity(grav); //becuase the rigid body doesn't have gravity yet
		Log.p("TODO: sus min/max range calc for both front and rear" + carData.susF.preload_force);
	}

	@Override
	public void physicsTick(PhysicsSpace space, float tpf) {
		// this is intentionally left blank
	}
	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		applySuspension(space, tpf);
		
		//TODO apply the midpoint formula
		//https://en.wikipedia.org/wiki/Midpoint_method
		applyTraction(space, tpf);
		
		applyDrag(space, tpf);
		
		//TODO give a force back to the collision object
	}
	
	private void applySuspension(PhysicsSpace space, float tpf) {
		Vector3f w_pos = rbc.getPhysicsLocation();
		Matrix3f w_angle = rbc.getPhysicsRotationMatrix();
		
		doForEachWheel((w_id) -> {
			Vector3f localPos = carData.wheelOffset[w_id];
			
			CarSusDataConst sus = carData.susByWheelNum(w_id);
			float susTravel = sus.travelTotal();
			
			//cast ray from suspension min, to max + radius (radius because bottom out check might as well be the size of the wheel)
			Vector3f startPos = vecLocalToWorld(localPos.add(localDown.mult(sus.min_travel)));
			Vector3f dir = w_angle.mult(localDown.mult(susTravel + carData.wheelData[w_id].radius));
			final FirstRayHitDetails col = raycaster.castRay(space, rbc, startPos, dir);
			
			if (DEBUG_SUS) {
				App.rally.enqueue(() -> {
					HelperObj.use(App.rally.getRootNode(), "sus_wheel_radius"+w_id,
							H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Blue, dir.normalize().mult(carData.wheelData[w_id].radius), startPos));
					HelperObj.use(App.rally.getRootNode(), "sus"+w_id,
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Cyan, dir.normalize().mult(susTravel), startPos.add(dir.normalize().mult(carData.wheelData[w_id].radius))));
					
					HelperObj.use(App.rally.getRootNode(), "col_point"+w_id,
						H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Red, wheels[w_id].curBasePosWorld, 0.01f));
				});
			}
			
			if (col == null) { //suspension ray found nothing, extend all the way and don't apply a force (set to 0)
				wheels[w_id].inContact = false;
				wheels[w_id].susRayLength = susTravel;
				wheels[w_id].curBasePosWorld = localDown.mult(susTravel + carData.wheelData[w_id].radius);
				wheels[w_id].susForce = 0;
				return; //no force
			}
			
			wheels[w_id].susRayLength = col.dist - carData.wheelData[w_id].radius; //remove the wheel radius
			wheels[w_id].curBasePosWorld = col.pos;
			wheels[w_id].inContact = true; //wheels are still touching..
			
			if (wheels[w_id].susRayLength < 0) { //suspension bottomed out 
				//TODO do some proper large sus/damp force...
				wheels[w_id].susForce = (sus.preload_force+sus.travelTotal())*sus.stiffness*1000;
				Vector3f f = w_angle.invert().mult(col.hitNormalInWorld.mult(wheels[w_id].susForce * tpf));
				rbc.applyImpulse(f, wheels[w_id].curBasePosWorld.subtract(w_pos));
				return;
			}
			
			float denominator = col.hitNormalInWorld.dot(w_angle.mult(localDown)); //loss due to difference between collision and localdown (cos ish)
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
			
			// Calculate spring distance from its zero length, as it should be outside the suspension range
			float springDiff = sus.travelTotal() - wheels[w_id].susRayLength;
			
			// Spring
			wheels[w_id].susForce = (sus.preload_force + sus.stiffness * springDiff) * clippedInvContactDotSuspension;
			
			// Damper
			float susp_damping = (projected_rel_vel < 0f) ? sus.compression() : sus.relax();
			wheels[w_id].susForce -= susp_damping * projected_rel_vel;
			
			// Limit: no negative forces or stupid high numbers pls
			wheels[w_id].susForce = FastMath.clamp(wheels[w_id].susForce * 1000, 0, sus.max_force);
			
			
			Vector3f f = w_angle.invert().mult(col.hitNormalInWorld.mult(wheels[w_id].susForce * tpf));
			rbc.applyImpulse(f, wheels[w_id].curBasePosWorld.subtract(w_pos));
			
			if (DEBUG_SUS2) {
				App.rally.enqueue(() -> {
					HelperObj.use(App.rally.getRootNode(), "normalforcearrow" + w_id, 
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Black, col.hitNormalInWorld, col.pos));
					
					HelperObj.use(App.rally.getRootNode(), "normalforcearrow inv" + w_id,
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.White, col.hitNormalInWorld.mult(-denominator), col.pos));
					
					HelperObj.use(App.rally.getRootNode(), "forcearrow" + w_id, 
						H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Cyan, f.mult(1/(carData.mass)), vecLocalToWorld(localPos)));
				});
			}
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
		
		//a 'few' slow speed hacks
		float maxSlowLat = Float.MAX_VALUE;
		float slowslipspeed = 10; //TODO some proportion of car.w_Off, so cars don't feel so weird at slow speeds
		boolean isSlowSpeed = velocity.length() <= slowslipspeed;
		if (isSlowSpeed && steeringCur != 0) {
			float radius = carData.wheelOffset[0].z/FastMath.sin(steeringCur);
			
			float forceToKeepCircle = (carData.mass*rbc.getGravity().y/4)*velocity.lengthSquared()/radius;
			maxSlowLat = FastMath.abs(forceToKeepCircle/4);
		}
		
		final float slip_div = Math.abs(velocity.length());
		final float slip_const = (isSlowSpeed ? 2 : 1);
		final float steeringFake = steeringCur;
		final float maxSlowLatFake = maxSlowLat;
		doForEachWheel((w_id) -> {
			Vector3f wheel_force = new Vector3f();
			
			//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
			float angVel = 0;
			if (!Float.isNaN(w_angVel.y))
				angVel = w_angVel.y;
			
			float slipr = slip_const*wheels[w_id].radSec * carData.wheelData[w_id].radius - velocity.z;
			float slipratio = slipr/slip_div;
			
			if (handbrakeCur && !isFrontWheel(w_id)) //rearwheels only
				wheels[w_id].radSec = 0;
			
			float slipangle = 0;
			if (isFrontWheel(w_id)) { //TODO this can be computed out of the loop
				float slipa_front = velocity.x + carData.wheelOffset[w_id].z * angVel;
				slipangle = (float)(FastMath.atan2(slipa_front, slip_div) - steeringFake);
			} else { //rear
				float slipa_rear = velocity.x + carData.wheelOffset[w_id].z * angVel;
				driftAngle = slipa_rear; //set drift angle as the rear amount
				slipangle = (float)(FastMath.atan2(slipa_rear, slip_div)); //slip_div is questionable here
			}
			slipangle *= slip_const;
			
			//start work on merging the forces into a traction circle
			float ratiofract = slipratio/this.wheels[w_id].maxLong;
			float anglefract = slipangle/this.wheels[w_id].maxLat;
			float p = FastMath.sqrt(ratiofract*ratiofract + anglefract*anglefract);
			
			//calc the longitudinal force from the slip ratio
			wheel_force.z = (ratiofract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_long, p*this.wheels[w_id].maxLong) * this.wheels[w_id].susForce; //TODO normalise susforce to prevent very large forces
			
			//latitudinal force that is calculated off the slip angle
			wheel_force.x = -(anglefract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_lat, p*this.wheels[w_id].maxLat) * this.wheels[w_id].susForce;
			
			//prevents the force from exceeding the centripetal force 'sometimes'
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
			if (Math.abs(ratiofract) >= 1 && velocity.length() > 2 && brakingCur == 1)
				brakeCurrent2 = 0; //abs (which i think works way too well)
			
			//self aligning torque
			if (!isSlowSpeed) { //this doesn't play nice with slow speed physics
				//TODO not convinced that long(z) direction traction has a self aligning torque
				//wheel_force.z += (ratiofract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_long_sat, p*this.wheels[w_id].maxLong) * this.wheels[w_id].susForce;
				wheel_force.x += (anglefract/p)*GripHelper.tractionFormula(carData.wheelData[w_id].pjk_lat_sat, p*this.wheels[w_id].maxLat) * this.wheels[w_id].susForce;
			}
			
			//add the wheel force after merging the forces
			float totalLongForce = wheelTorque[w_id] - wheel_force.z - (brakeCurrent2*carData.brakeMaxTorque*Math.signum(wheels[w_id].radSec));
			float nextRadSec = wheels[w_id].radSec + tpf*totalLongForce/(carData.e_inertia());
			if (brakingCur != 0 && Math.signum(wheels[w_id].radSec) != Math.signum(nextRadSec))
				wheels[w_id].radSec = 0; //maxed out the forces with braking, so prevent wheels from moving
			else
				wheels[w_id].radSec += tpf*totalLongForce/(carData.e_inertia());
			
			wheels[w_id].gripDir = wheel_force.normalize();
			rbc.applyImpulse(w_angle.mult(wheel_force).mult(tpf), wheels[w_id].curBasePosWorld.subtract(w_pos));
			
			planarGForce.addLocal(wheel_force);
		});
		
		planarGForce.multLocal(1/carData.mass);
	}
	
	private void applyDrag(PhysicsSpace space, float tpf) {
		//linear resistance and quadratic drag (https://en.wikipedia.org/wiki/Automobile_drag_coefficient#Drag_area)

		//TODO was removed because i need the suspension normal force to caculate correctly
		//use: (and apply at wheel pos)
		doForEachWheel((w_id) -> { carData.rollingResistance(9.81f, w_id); }); 
		
		Vector3f w_velocity = rbc.getLinearVelocity();
		float dragx = -(1.225f * carData.areo_drag * carData.areo_crossSection * w_velocity.x * FastMath.abs(w_velocity.x));
		float dragy = -(1.225f * carData.areo_drag * carData.areo_crossSection * w_velocity.y * FastMath.abs(w_velocity.y));
		float dragz = -(1.225f * carData.areo_drag * carData.areo_crossSection * w_velocity.z * FastMath.abs(w_velocity.z));
		//TODO change cross section for each xyz direction to make a realistic drag feeling
		
		Vector3f totalNeutral = new Vector3f(dragx, dragy, dragz);
		dragValue = totalNeutral.length(); //for debug reasons
		
		float dragDown = -0.5f * carData.areo_downforce * 1.225f * (w_velocity.z*w_velocity.z); //formula for downforce from wikipedia
		rbc.applyCentralForce(totalNeutral.add(0, dragDown, 0)); //apply downforce after
		
		//TODO rotational drag (or at least a fake one)
		
		if (DEBUG_DRAG) {
			App.rally.enqueue(() -> {
				HelperObj.use(App.rally.getRootNode(), "dragarrow",
					H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Black, totalNeutral, rbc.getPhysicsLocation()));
			});
		}
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