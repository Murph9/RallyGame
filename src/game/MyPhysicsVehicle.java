package game;

import java.util.LinkedList;

import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.dynamics.vehicle.WheelInfo.RaycastInfo;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.bullet.util.Converter;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

//extends:
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-jbullet/src/main/java/com/jme3/bullet/objects/PhysicsVehicle.java

public class MyPhysicsVehicle extends PhysicsVehicle implements ActionListener {
	
	//skid stuff
	Node skidNode;
	LinkedList<Spatial> skidList = new LinkedList<Spatial>();
	
	//directions
	Vector3f up = new Vector3f();
	Vector3f right = new Vector3f();
	Vector3f left = new Vector3f();
	
	//admin stuff
	AssetManager assetManager;
	Rally rally;
	
	Controller myJoy;
	AI ai;
	
	//car data
	protected FancyVT car;
	protected Node carNode;
	
	MyWheelNode[] wheel = new MyWheelNode[4];
	Spatial[] wheelSpat = new Spatial[4];
	
	//driving stuff
	int curGear = 1;
	int curRPM = 0;
	
	float accelCurrent = 0;
	
	float steeringCurrent = 0;
	float steerLeft = 0;
	float steerRight= 0;
	
	float brakeCurrent = 0;
	
	boolean ifHandbrake = false;
	//- driving stuff
	
	float distance = 0;
	boolean ifLookBack = false;
	boolean ifLookSide = false;
	
	float redlineKillFor = 0;
	
	MyPhysicsVehicle(CollisionShape col, FancyVT cartype, Node carNode, Rally rally) {
		super(col, cartype.mass);
		this.car = cartype;
		this.rally = rally;
		this.carNode = carNode;
		this.assetManager = rally.getAssetManager();
		
		this.skidNode = new Node(); //attached in car builder
		
		this.setSuspensionCompression(car.susCompression);
		this.setSuspensionDamping(car.susDamping);
		this.setSuspensionStiffness(car.stiffness);
		this.setMaxSuspensionForce(car.maxSusForce);
		this.setMaxSuspensionTravelCm(car.maxSusTravel);
		
		wheel[0] = new MyWheelNode("wheel 0 node", this, 0);
		wheelSpat[0] = assetManager.loadModel(car.wheelModel);
		wheelSpat[0].center();
		wheel[0].attachChild(wheelSpat[0]);
		addWheel(wheel[0], new Vector3f(-car.wheel_xOff, car.wheel_yOff, car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		wheel[1] = new MyWheelNode("wheel 1 node", this, 1);
		wheelSpat[1] = assetManager.loadModel(car.wheelModel);
		wheelSpat[1].rotate(0, FastMath.PI, 0);
		wheelSpat[1].center();
		wheel[1].attachChild(wheelSpat[1]);
		addWheel(wheel[1], new Vector3f(car.wheel_xOff, car.wheel_yOff, car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		wheel[2] = new MyWheelNode("wheel 2 node", this, 2);
		wheelSpat[2] = assetManager.loadModel(car.wheelModel);
		wheelSpat[2].center();
		wheel[2].attachChild(wheelSpat[2]);
		addWheel(wheel[2], new Vector3f(-car.wheel_xOff-0.05f, car.wheel_yOff, -car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		wheel[3] = new MyWheelNode("wheel 3 node", this, 3);
		wheelSpat[3] = assetManager.loadModel(car.wheelModel);
		wheelSpat[3].rotate(0, FastMath.PI, 0);
		wheelSpat[3].center();
		wheel[3].attachChild(wheelSpat[3]);
		addWheel(wheel[3], new Vector3f(car.wheel_xOff+0.05f, car.wheel_yOff, -car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		//Their friction
		setFrictionSlip(0, car.wheel0Slip);
		setFrictionSlip(1, car.wheel1Slip);
		setFrictionSlip(2, car.wheel2Slip);
		setFrictionSlip(3, car.wheel3Slip);
		
		
		for (MyWheelNode w: wheel) {
			//attaching all the things (wheels)
			carNode.attachChild(w);
		}
		
		////////////////////////
		if (carNode.getName().equals("0")) //yes its a little hardcoded..
			setupKeys();
	}
	
	/**Used internally, creates the actual vehicle constraint when vehicle is added to phyicsspace
     */
	@Override
    public void createVehicle(PhysicsSpace space) {
        physicsSpace = space;
        if (space == null) {
            return;
        }
        rayCaster = new DefaultVehicleRaycaster(space.getDynamicsWorld());
        vehicle = new FancyRcV(car, rBody, rayCaster); //<-- i added it here for the change of constructor
        vehicle.setCoordinateSystem(0, 1, 2);
        for (VehicleWheel wheel : wheels) {
            wheel.setWheelInfo(vehicle.addWheel(Converter.convert(wheel.getLocation()), Converter.convert(wheel.getDirection()), Converter.convert(wheel.getAxle()),
                    wheel.getRestLength(), wheel.getRadius(), tuning, wheel.isFrontWheel()));
        }
    }
	
    
    public void giveAI(AI a) {
    	this.ai = a;
    	InputManager i = rally.getInputManager();
    	
    	i.addRawInputListener(a);
    	
    	i.addListener(this, "Left"); 
		i.addListener(this, "Right");
		i.addListener(this, "Accel");
		i.addListener(this, "Brake");
		i.addListener(this, "Jump");
		i.addListener(this, "Handbrake");
		i.addListener(this, "Reset");
		i.addListener(this, "Physics");
		i.addListener(this, "LookBack");
		i.addListener(this, "Reverse");
    }
    
	//controls
	private void setupKeys() {
		InputManager i = rally.getInputManager();
		i.addRawInputListener(new MyKeyListener(this)); //my input class, practice for controller class
//		i.addRawInputListener(new JoystickEventListner(this));
		
		//maps to:
		i.addListener(this, "Left"); 
		i.addListener(this, "Right");
		i.addListener(this, "Accel");
		i.addListener(this, "Brake");
		i.addListener(this, "Jump");
		i.addListener(this, "Handbrake");
		i.addListener(this, "Reset");
		i.addListener(this, "Physics");
		i.addListener(this, "LookBack");
		i.addListener(this, "Reverse");
	}
	
	public void onAction(String binding, boolean value, float tpf) {
		//value == 'if pressed (down) - we get one back when its unpressed (up)'
		//tpf is being used as the value for joysticks. deal with it
		switch(binding) {
		case "Left": 
			steerLeft = tpf;
			break;
			
		case "Right": 
			steerRight = tpf;
			break;
			
		case "Accel":
			accelCurrent = tpf;
			break;
			
		case "Brake":
			brakeCurrent = car.MAX_BRAKE*tpf;
			break;
			
		case "Jump":
			if (value) {
				applyImpulse(car.JUMP_FORCE, new Vector3f(0,0,0)); //push up
				Vector3f old = getPhysicsLocation();
				old.y += 2; //and move up
				setPhysicsLocation(old);
			}
			break;
			
		case "HandBrake":
			ifHandbrake = value;
			break;
			
		case "Flip":
			if (value) this.flipMe();
			break;
			
		case "Reset":
			if (value) this.reset();
			break;
			
		case "Reverse":
			if (value) curGear = 0;
			else curGear = 1;
			break;
			
		case "Lookback":
			ifLookBack = value;
			break;
			
		case "Lookside":
			ifLookSide = value;
			break;
			
		default:
			//nothing
			System.err.println("unknown key: "+binding);
			break;
		}
	}
	//end controls
	
	//TODO Things taken out of physics:
	//- handbrake (there is a chance that the longtitudinal magic should fix this
	
	////////////////////////////////////////////////////
	
	private void specialPhysics(float tpf) {
		//NOTE: that z is forward, x is side
		// - the reference notes say that x is forward and y is sideways so just be careful

		Matrix3f w_angle = getPhysicsRotationMatrix();
		Vector3f w_velocity = getLinearVelocity();
		
		//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
		double yawspeed = car.length * getAngularVelocity().y;
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		
		float steeringCur = steeringCurrent;
		if (velocity.z < 0) { //to flip the steering on moving in reverse
			steeringCur *= -1;
		}

		if (velocity.z == 0) { //to avoid the divide by zero below
			velocity.z += 0.0001;
		}
		//important angles
		double[] slips = VehiclePhysicsHelper.calcSlipAngle(velocity.x, velocity.z, yawspeed, steeringCur);
		double slipanglefront = slips[0];
		double slipanglerear = slips[1];
		UINode.angle.setText(H.roundDecimal(Math.abs(FastMath.RAD_TO_DEG*slipanglerear), 0)+" '");

		//////////////////////////////////////////////////
		//'Wheel'less forces (TODO braking in wheels)
		float braking = 0;
		if (wheel[0].contact || wheel[1].contact || wheel[2].contact || wheel[3].contact) {
			braking = -brakeCurrent*FastMath.sign(velocity.z);
		}
		Vector3f brakeVec = new Vector3f(0,0,1).multLocal(braking);
		
		//linear resistance and quadratic drag
		float dragx = (float)(-(car.RESISTANCE * velocity.x + car.DRAG * velocity.x * Math.abs(velocity.x)));
		float dragy = (float)(-(car.RESISTANCE * velocity.y + car.DRAG * velocity.y * Math.abs(velocity.y)));
		float dragz = (float)(-(car.RESISTANCE * velocity.z + car.DRAG * velocity.z * Math.abs(velocity.z)));
		
		Vector3f totalNeutral = new Vector3f(dragx, dragy, dragz);
		applyCentralForce(w_angle.mult(totalNeutral.add(brakeVec)));
		
		//////////////////////////////////
		double weightperwheel = car.mass*(-getGravity().y/1000)*0.25; //0.25 because its per wheel
		
		Vector3f fl = new Vector3f(); //front left
		Vector3f fr = new Vector3f(); //front right
		Vector3f rl = new Vector3f(); //rear  left
		Vector3f rr = new Vector3f(); //rear  right
		
		//latitudinal forces that are calculated off the slip angle
		fl.x = fr.x = -(float)VehiclePhysicsHelper.tractionFormula(car.wheellatdata, (float)slipanglefront, (float)weightperwheel);
		rl.x = rr.x = -(float)VehiclePhysicsHelper.tractionFormula(car.wheellongdata, (float)slipanglerear, (float)weightperwheel);

		//////////////////////////////////////
		//longitudinal forces
		float wheelRot = velocity.z/(2*FastMath.PI*car.wheelRadius); //w = v/(2*Pi*r) -> rad/sec
		float engineForce = getEngineWheelForce(wheelRot, tpf)*accelCurrent; 
		
		float accel = engineForce-Math.signum(velocity.z)*car.engineCompression*curRPM;

//		H.p(VehicleHelper.longitudinalForce(car, )); //TODO wait until we have the slip ratio
		
		
		accel /= 2; //per wheel because at least 2 wheels
		if (car.driveFront && car.driveRear) { 
			accel /= 2; //if its split up between the front and rear axle
		}
			
		if (car.driveFront) {
			fl.z = fr.z = accel;
		}
		if (car.driveRear) {
			rl.z = rr.z = accel;
		}

		float latforceOnWheel = (float)weightperwheel*car.MAX_GRIP;
		
		//TODO remove when skid ratio comes in, (and mix them together)
		wheel[0].skid = FastMath.clamp((latforceOnWheel-fl.length())/latforceOnWheel, 0, 1);
		fl = new Vector3f(H.clamp(fl, latforceOnWheel));
		
		wheel[1].skid = FastMath.clamp((latforceOnWheel-fr.length())/latforceOnWheel, 0, 1);
		fr = new Vector3f(H.clamp(fr, latforceOnWheel));
		
		wheel[2].skid = FastMath.clamp((latforceOnWheel-rl.length())/latforceOnWheel, 0, 1);
		rl = new Vector3f(H.clamp(rl, latforceOnWheel));
		
		wheel[3].skid = FastMath.clamp((latforceOnWheel-rr.length())/latforceOnWheel, 0, 1);
		rr = new Vector3f(H.clamp(rr, latforceOnWheel));
		

		//TODO stop the wobble (hint the basic vehicle code does this through impulses)
		float lim = 5;
		if (Math.abs(velocity.z) <= lim) {
			fl.mult(velocity.length()*velocity.length()/(lim*2));
			fr.mult(velocity.length()*velocity.length()/(lim*2));
			rl.mult(velocity.length()*velocity.length()/(lim*2));
			rr.mult(velocity.length()*velocity.length()/(lim*2));
		}
		
		
		//and finally apply forces
		if (wheel[0].contact) 
			applyForce(w_angle.mult(fl), w_angle.mult(wheel[0].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[1].contact) 
			applyForce(w_angle.mult(fr), w_angle.mult(wheel[1].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[2].contact) 
			applyForce(w_angle.mult(rl), w_angle.mult(wheel[2].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[3].contact) 
			applyForce(w_angle.mult(rr), w_angle.mult(wheel[3].getContactPoint(car.wheelRadius, car.rollFraction)));
		
	}
	
	private float getEngineWheelForce(float wheelrot, float tpf) {
		float curGearRatio = car.gearRatios[curGear];//0 = reverse, >= 1 normal make sense
		float diffRatio = car.diffRatio;
		curRPM = (int)(wheelrot*curGearRatio*diffRatio*60); //rad/sec to rad/min and the drive ratios to engine
			//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min

		redlineKillFor -= tpf;
		
		if (redlineKillFor > 0) {
			return 0;
		}
		
		if (Math.abs(curRPM) > car.redline) {
			redlineKillFor = car.redlineCutTime;
			return 0; //kill engine if greater than redline
		}
		
		autoTransmission(curRPM);
		
		float engineTorque = lerpTorque(curRPM);
		float driveTorque = engineTorque*curGearRatio*diffRatio*car.transEffic;
		
		float totalTorque = driveTorque/car.wheelRadius;
		return totalTorque;
	}
	
	private void autoTransmission(int rpm) {
		if (curGear == 0) return; //no changing out of reverse on me please..
		
		if (rpm > car.gearUp && curGear < car.gearRatios.length-1) {
			curGear++;
		} else if (rpm < car.gearDown && curGear > 1) {
			curGear--;
		}
	}
	
	private float lerpTorque(int rpm) {
		if (rpm < 1000) rpm = 1000; //prevent stall values
		float RPM = (float)rpm / 1000;
		return H.lerpArray(RPM, car.torque);
	}
	
	//////////////////////////////////////////////////////////////
	public void myUpdate(float tpf) {
		distance += getLinearVelocity().length()*tpf;
		
		Matrix3f playerRot = new Matrix3f();
		getPhysicsRotationMatrix(playerRot);
		
		up = playerRot.mult(new Vector3f(0,1,0));
		left = playerRot.mult(new Vector3f(1,0,0));
		right = playerRot.mult(new Vector3f(1,0,0).negate());
		
		for (MyWheelNode w: wheel) {
			WheelInfo wi = getWheel(w.num).getWheelInfo();
			RaycastInfo ray = wi.raycastInfo;
			w.contact = (ray.groundObject != null);
			
			w.update(tpf);
		}
		
		//skid marks
		addSkidLines();
		
		specialPhysics(tpf); //yay
		
		for (Spatial w: wheelSpat) {
			//TODO rotate the wheels
		}
		
		//wheel turning logic -TODO
			//trying to turn less at high speed
		steeringCurrent = 0;
		float speedFactor = 1;
//		speedFactor = car.steerFactor/(getLinearVelocity().length()*10);
		if (steerLeft != 0) { //left
			steeringCurrent += car.steerAngle*speedFactor*steerLeft; //maxangle*speedFactor*turnfraction
		}
		if (steerRight != 0) { //right
			steeringCurrent -= car.steerAngle*speedFactor*steerRight; //maxangle*speedFactor*turnfraction
		}
		steer(steeringCurrent);
		H.pUI(steeringCurrent);
	}
	
	///////////////////////////////////////////////////////////
	private void addSkidLines() {
		if (ai != null) return;
		
		if (rally.frameCount % 4 == 0) {
			for (MyWheelNode w: wheel) {
				w.addSkidLine();
			}
			
			int extra = skidList.size() - 500; //so i can remove more than one (like all 4 that frame)
			for (int i = 0; i < extra; i++) {
				skidNode.detachChild(skidList.getFirst());
				skidList.removeFirst();
			}
		}
	}
	

	private void reset() {
		setPhysicsRotation(new Matrix3f());
		setLinearVelocity(new Vector3f());
		setAngularVelocity(new Vector3f());

		rally.reset();
		
		if (rally.dynamicWorld) {
			setPhysicsLocation(rally.worldB.start);
			Matrix3f p = new Matrix3f();
			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
			setPhysicsRotation(p);

			setAngularVelocity(new Vector3f());
		} else {
			setPhysicsLocation(rally.world.start);
		}
		
		skidNode.detachAllChildren();
		skidList.clear();
		for (MyWheelNode w: wheel) {
			w.last = new Vector3f(0,0,0);
		}
	}
	
	private void flipMe() {
		if (this.up.y > 0) return; //if already the right way up
		
		Quaternion q = getPhysicsRotation(); 
		q.multLocal(new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0,0,1)));
		setPhysicsRotation(q);
		setPhysicsLocation(getPhysicsLocation().add(new Vector3f(0,1,0)));
	}
	
	@Override
	public float getCurrentVehicleSpeedKmHour() {
		return vehicle.getCurrentSpeedKmHour();
	}
}

class VehiclePhysicsHelper {
	
	static double[] calcSlipAngle(double vx, double vz, double yawspeed, double steeringCur) {
		double[] out = new double[]{
				Math.atan((vx + yawspeed) / Math.abs(vz)) - steeringCur, 
				Math.atan((vx - yawspeed) / Math.abs(vz))};
		return out;
	}
	
	//http://www.gamedev.net/topic/462784-simplified-pacejka-magic-formula/
	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	
	//There were fancy versions of the Pacejka's Formula here but there were removed.
		//Try the git repositiory to get them back. (i should say 'removed' in the git message)
	
	/** Pacejka's Formula simplified from the bottom of:.
	 * http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	 * @param w Pick the lateral or longitudial version to send.
	 * @param slip Slip angle or slip ratio (it doesn't matter except for one value changes on it)
	 * @param Fz Load on the tire
	 * @return The force expected
	 */
	static float tractionFormula(CarWheelData w, float slip, float Fz) {
		return Fz * w.D * FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
	}
}