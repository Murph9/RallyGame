package game;

import java.util.LinkedList;

import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.dynamics.vehicle.WheelInfo.RaycastInfo;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
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
	private float maxlat;
	private float maxlong;	
	
	//sound stuff
	AudioNode engineSound; //TODO
	
	//directions
	Vector3f up = new Vector3f();
	Vector3f right = new Vector3f();
	Vector3f left = new Vector3f();
	
	Controller myJoy;
	AI ai;
	
	//car data
	protected FancyVT car;
	protected Node carNode;
	
	//my wheel node
	MyWheelNode[] wheel = new MyWheelNode[4];
	
	//state stuff
	int curGear = 1;
	int curRPM = 0;
	
	float accelCurrent = 0;
	float steeringCurrent = 0;
	float brakeCurrent = 0;
	
	float steerLeft = 0;
	float steerRight= 0;
	
	float engineTorque = 0;
	float totalTraction = 0;
	float wheelRot = 0;
	
	boolean ifHandbrake = false;
	//- driving stuff
	
	float distance = 0;
	boolean ifLookBack = false;
	boolean ifLookSide = false;
	
	float redlineKillFor = 0;
	
	MyPhysicsVehicle(CollisionShape col, FancyVT cartype, Node carNode) {
		super(col, cartype.mass);
		this.car = cartype;
		this.carNode = carNode;
		AssetManager am = App.rally.getAssetManager();
		
		this.skidNode = new Node(); //attached in car builder
		
		this.setSuspensionCompression(car.susCompression);
		this.setSuspensionDamping(car.susDamping);
		this.setSuspensionStiffness(car.stiffness);
		this.setMaxSuspensionForce(car.maxSusForce);
		this.setMaxSuspensionTravelCm(car.maxSusTravel);
		
		wheel[0] = new MyWheelNode("wheel 0 node", this, 0);
		wheel[0].spat = am.loadModel(car.wheelModel);
		wheel[0].spat.center();
		wheel[0].attachChild(wheel[0].spat);
		addWheel(wheel[0], new Vector3f(-car.wheel_xOff, car.wheel_yOff, car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		wheel[1] = new MyWheelNode("wheel 1 node", this, 1);
		wheel[1].spat = am.loadModel(car.wheelModel);
		wheel[1].spat.rotate(0, FastMath.PI, 0);
		wheel[1].spat.center();
		wheel[1].attachChild(wheel[1].spat);
		addWheel(wheel[1], new Vector3f(car.wheel_xOff, car.wheel_yOff, car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		wheel[2] = new MyWheelNode("wheel 2 node", this, 2);
		wheel[2].spat = am.loadModel(car.wheelModel);
		wheel[2].spat.center();
		wheel[2].attachChild(wheel[2].spat);
		addWheel(wheel[2], new Vector3f(-car.wheel_xOff-0.05f, car.wheel_yOff, -car.wheel_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		wheel[3] = new MyWheelNode("wheel 3 node", this, 3);
		wheel[3].spat = am.loadModel(car.wheelModel);
		wheel[3].spat.rotate(0, FastMath.PI, 0);
		wheel[3].spat.center();
		wheel[3].attachChild(wheel[3].spat);
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
		if (carNode.getName().equals("0")) {//yes its a little hardcoded..
			setupKeys();
			
			//only player gets sound
			engineSound = new AudioNode(am, "assets/sound/engine2.wav");
			engineSound.setLooping(true);
			engineSound.play();
		}
		
		//generate the slip* max force from the car wheel data
		this.maxlat = VehiclePhysicsHelper.calcSlipMax(car.wheellatdata);
		this.maxlong = VehiclePhysicsHelper.calcSlipMax(car.wheellongdata);
		
		H.p(this.maxlat + " " + this.maxlong);
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
    	InputManager i = App.rally.getInputManager();
    	
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
		InputManager i = App.rally.getInputManager();
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
			brakeCurrent = tpf;
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

	//TODO find SAE950311 
	//Millikin & Millikin's Race Car Vehicle Dynamics
	
	//TODO good info to actually read:
	//https://www.sae.org/images/books/toc_pdfs/R146.pdf
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

		//////////////////////////////////////////////////
		//'Wheel'less forces		
		//linear resistance and quadratic drag
		float dragx = (float)(-(car.RESISTANCE * velocity.x + car.DRAG * velocity.x * Math.abs(velocity.x)));
		float dragy = (float)(-(car.RESISTANCE * velocity.y + car.DRAG * velocity.y * Math.abs(velocity.y)));
		float dragz = (float)(-(car.RESISTANCE * velocity.z + car.DRAG * velocity.z * Math.abs(velocity.z)));
		
		Vector3f totalNeutral = new Vector3f(dragx, dragy, dragz);
		applyCentralForce(w_angle.mult(totalNeutral));
		
		//////////////////////////////////
		//Wheel Forces
		Vector3f[] wf = new Vector3f[] {
				new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f(),
		};
		
		if (velocity.z == 0) { //to avoid the divide by zero below
			velocity.z += 0.0001;
		}
		
		//////////////////////////////////////
		//longitudinal forces
		engineTorque = getEngineWheelTorque(tpf, velocity.z);
		float[] torques = new float[] { 0, 0, 0, 0 };
		
		engineTorque /= 2; //probably on at least one axle
		if (car.driveFront && car.driveRear)
			engineTorque /= 2; //split up into 2 more wheels
		
		if (car.driveFront) {
			torques[0] = engineTorque;
			torques[1] = engineTorque;
		}
		if (car.driveRear) {
			torques[2] = engineTorque;
			torques[3] = engineTorque;
		}
		
		//braking
		if (brakeCurrent != 0) {
			wheel[0].radSec = 0;
			wheel[1].radSec = 0; //TODO do it properly
			wheel[2].radSec = 0;
			wheel[3].radSec = 0;
		}
		
		//http://web.archive.org/web/20050308061534/home.planet.nl/~monstrous/tutstab.html
		//http://phors.locost7.info/contents.htm

		//LARGE LOOP
		//for each wheel
		for (int i = 0; i < 4; i++) {
			float susforce = (float)getWheel(i).getWheelInfo().wheelsSuspensionForce;
			
			float slipratio = (wheel[i].radSec*car.wheelRadius - velocity.z)/Math.abs(velocity.z);
			//calc the longitudinal force from the slip ratio
			wf[i].z = VehiclePhysicsHelper.tractionFormula(car.wheellongdata, slipratio) * susforce;
			
			float totalLongForce = torques[i] - wf[i].z - brakeCurrent*3000; //TODO magic number
			wheel[i].radSec += tpf*totalLongForce/(car.engineInertia()/4); //TODO actually get the total wheels or something
			
			float slipangle = 0;
			if (i < 2) //front
				slipangle = VehiclePhysicsHelper.calcFrontSlipAngle(velocity.x, velocity.z, yawspeed, steeringCur);
			else //rear
				slipangle = VehiclePhysicsHelper.calcRearSlipAngle(velocity.x, velocity.z, yawspeed);
			
			//calculate the max lat traction value here
			float tractionLeft = slipratio / this.maxlong;
			if (tractionLeft > 1) slipangle = 0; //TODO a bit hard but it will do
			
			//latitudinal force that is calculated off the slip angle
			wf[i].x = -VehiclePhysicsHelper.tractionFormula(car.wheellatdata, slipangle) * susforce;
			
			wheel[i].skid = FastMath.sqrt(slipangle*slipangle + slipratio*slipratio);
		}
		
		//TODO merge the lat and long traction values
				
		//TODO: !HARD! better code from others 'transision' between static and kinetic friction models
			// i have no static model
		
		//TODO calculate the skid mark values

		//TODO stop the wobble (hint the basic vehicle code does this through impulses (integration of forces)
			// try 'rolling resistance function'
		
		float lim = 5;
		float velz = velocity.z + 0.01f;
		if (Math.abs(velz) <= lim) {
//			fl.multLocal(velz*velz/(lim*lim));
//			fr.multLocal(velz*velz/(lim*lim));
//			rl.multLocal(velz*velz/(lim*lim));
//			rr.multLocal(velz*velz/(lim*lim));
			//TODO doesn't work
		}
		
		//ui based values
		totalTraction = wf[0].length() + wf[1].length() + wf[2].length() + wf[3].length();  
		this.wheelRot = 0;
		for (MyWheelNode w: wheel) {
			this.wheelRot += w.radSec;
		}
		this.wheelRot /= 4;
		
		//and finally apply forces
		if (wheel[0].contact)
			applyForce(w_angle.mult(wf[0]), w_angle.mult(wheel[0].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[1].contact) 
			applyForce(w_angle.mult(wf[1]), w_angle.mult(wheel[1].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[2].contact) 
			applyForce(w_angle.mult(wf[2]), w_angle.mult(wheel[2].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[3].contact) 
			applyForce(w_angle.mult(wf[3]), w_angle.mult(wheel[3].getContactPoint(car.wheelRadius, car.rollFraction)));
		
	}
	
	private float getEngineWheelTorque(float tpf, float vz) {
		float wheelrot = 0;
		if (car.driveFront)
			wheelrot = wheel[0].radSec; //get the drive wheels rotation speed
		if (car.driveRear) 
			wheelrot = wheel[2].radSec; //because both the wheels are rpm locked it doesn't matter which one
		
		float curGearRatio = car.gearRatios[curGear];//0 = reverse, >= 1 normal make sense
		float diffRatio = car.diffRatio;
		
		curRPM = (int)(wheelrot*curGearRatio*diffRatio*60*car.wheelRadius); //rad/(m*sec) to rad/min and the drive ratios to engine
			//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min
		
		curRPM = H.clamp(curRPM, 800, 50000);//make sure we never stall (or get rediculous)
		//TODO use engine compression value

		if (Math.abs(curRPM) > car.redline) {
			return 0; //kill engine if greater than redline
		}
		
		autoTransmission(curRPM, vz);
		float engineTorque = lerpTorque(curRPM)*accelCurrent;
		
		float engineOutTorque = engineTorque*curGearRatio*diffRatio*car.transEffic;
		
		float totalTorque = engineOutTorque/car.wheelRadius;
		return totalTorque;
	}
	
	private void autoTransmission(int rpm, float vz) {
		if (curGear == 0) return; //no changing out of reverse on me please..
		
		float gearUpSpeed = car.gearUp/(car.gearRatios[curGear]*car.diffRatio*60);
		float gearDownSpeed = car.gearDown/(car.gearRatios[curGear]*car.diffRatio*60);
		//TODO: error checking on making sure the one we change to doesn't instantly change back
		
		
		if (vz > gearUpSpeed && curGear < car.gearRatios.length-1) {
			curGear++;
		} else if (vz < gearDownSpeed && curGear > 1) {
			curGear--;
		}
	}

	//assumed to be a valid and useable rpm value
	private float lerpTorque(int rpm) {
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
		
			w.update(tpf, curRPM);
		}
		
		//skid marks
		addSkidLines();
		
		specialPhysics(tpf); //yay
		
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
		
		//sound
		float pitch = FastMath.clamp(0.5f+1.5f*(curRPM/car.redline), 0.5f, 2);
		engineSound.setPitch(pitch);
	}
	
	///////////////////////////////////////////////////////////
	private void addSkidLines() {
		if (ai != null) return;
		
		if (App.rally.frameCount % 4 == 0) {
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

		App.rally.reset();
		
		if (App.rally.dynamicWorld) {
			setPhysicsLocation(App.rally.worldB.start);
			Matrix3f p = new Matrix3f();
			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
			setPhysicsRotation(p);

			setAngularVelocity(new Vector3f());
		} else {
			setPhysicsLocation(App.rally.world.start);
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
	
	static float calcFrontSlipAngle(double vx, double vz, double yawspeed, double steeringCur) {
		return (float)(Math.atan((vx + yawspeed) / Math.abs(vz)) - steeringCur);
	}
	static float calcRearSlipAngle(double vx, double vz, double yawspeed) {
		return (float)(Math.atan((vx - yawspeed) / Math.abs(vz)));
	}
	//http://www.gamedev.net/topic/462784-simplified-pacejka-magic-formula/
	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	
	//There were fancy versions of the Pacejka's Formula here but there were removed.
		//Try the git repositiory to get them back. (i should say 'removed' in the git message)
	
	/** Pacejka's Formula simplified from the bottom of:.
	 * http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	 * @param w Pick the lateral or longitudial version to send.
	 * @param slip Slip angle or slip ratio (it doesn't matter except for one value changes on it)
	 * @return The force expected
	 */
	static float tractionFormula(CarWheelData w, float slip) {
		return w.D * FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
	}
	
	//returns the slip value that gives the closest to 1 from the magic formula
	static float calcSlipMax(CarWheelData w) {
		double error = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals
		
		double lastX = 0.2f; //0.2 is always our first guess (12 deg or so is good)
		double nextX = lastX + 10*error; //just so its a larger diff that error
		
		while (Math.abs(lastX - nextX) > error) {
			lastX = nextX;
			nextX = iterate(w, lastX, error);
		}
		
		return (float)nextX;
	}
	private static double iterate(CarWheelData w, double x, double error) {
		return x - ((tractionFormula(w, (float)x)-1) / dtractionFormula(w, (float)x, error)); 
		//-1 because we are trying to find a max (which happens to be 1)
	}
	private static double dtractionFormula(CarWheelData w, double slip, double error) {
		return (tractionFormula(w, (float)(slip+error)) - tractionFormula(w , (float)(slip-error)))/ (2*error);
	}
	
	
	//TODO fix (use http://phors.locost7.info/phors25.htm)
	//basically gets fraction of maximum for each force and limits the total value by it
	static float[] mergeSlips(float slipangle, float slipratio) {
		//TODO calculate the slip* max values, and use it to limit the other
		
		//TODO will also help us get the skid stuff too
		
		return new float[] { };
	}
}