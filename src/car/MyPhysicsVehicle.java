package car;

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
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.CarModelData.CarPart;
import game.App;
import game.H;

//extends:
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-jbullet/src/main/java/com/jme3/bullet/objects/PhysicsVehicle.java

public class MyPhysicsVehicle extends PhysicsVehicle {

	//skid stuff
	public Node skidNode;
	protected LinkedList<Spatial> skidList = new LinkedList<Spatial>();
	private float maxlat;
	private float maxlong;

	//sound stuff
	private AudioNode engineSound;

	//nice to use directions
	Vector3f up = new Vector3f();
	Vector3f right = new Vector3f();
	Vector3f left = new Vector3f();

	// Car controlling things
	private MyKeyListener control;
	private AI ai;

	//car data
	public CarData car;
	public Node carRootNode;
	public CarModelData model;

	//my wheel node
	public MyWheelNode[] wheel = new MyWheelNode[4];

	//state stuff
	public Vector3f vel;
	public Vector3f gForce;
	
	public int curGear = 1;
	public int curRPM = 1000;

	public float accelCurrent;
	public float steeringCurrent;
	public float brakeCurrent;

	float steerLeft;
	float steerRight;

	float nitroTimeout;
	boolean ifNitro = false;
	float nitro;

	boolean ifHandbrake = false;
	
	//TODO use
	float clutch; //0 = can drive, 1 = can't drive
	int gearChangeTo;
	float gearChangeTime;
	// /state stuff
	
	//ui stuff
	public float engineTorque;
	public String totalTraction = "";
	public float totalWheelRot;

	public float distance;
	public boolean ifLookBack = false;
	public boolean ifLookSide = false;

	float redlineKillFor;
	private float driftangle;
	public int getAngle() { return (int)(Math.abs(driftangle)*FastMath.RAD_TO_DEG); }

	MyPhysicsVehicle(CollisionShape col, CarData cartype, Node carNode) {
		super(col, cartype.mass);
		this.car = cartype;
		this.carRootNode = carNode;
		
		//init the car model
		this.model = new CarModelData(cartype.carModel, cartype.wheelModel);
		this.skidNode = new Node(); //attached in car builder
		
		AssetManager am = App.rally.getAssetManager();

		this.setSuspensionCompression(car.susCompression());
		this.setSuspensionDamping(car.susDamping());
		this.setSuspensionStiffness(car.sus_stiffness);
		this.setMaxSuspensionForce(car.sus_maxForce);
		this.setMaxSuspensionTravelCm(car.sus_maxTravel);

		this.nitro = car.nitro_max;
		this.vel = new Vector3f();
		this.gForce = new Vector3f();
		
		//get wheel positions from the model data if possible
		Vector3f[] poss = new Vector3f[4];
		poss[0] = model.getPosOf(CarPart.Wheel_FL);
		poss[1] = model.getPosOf(CarPart.Wheel_FR);
		poss[2] = model.getPosOf(CarPart.Wheel_RL);
		poss[3] = model.getPosOf(CarPart.Wheel_RR);
		
		for (int i = 0; i < 4; i++) {
			if (poss[i] != null) {
				car.w_pos[i] = poss[i];
			}
		}
		
		MyWheelNode.skidTex = am.loadTexture("assets/stripes.png");
		
		//for each wheel
		for (int i = 0; i < 4; i++) {
			wheel[i] = new MyWheelNode("wheel "+i+" node", this, i);
			wheel[i].spat = am.loadModel(car.wheelModel);
			if (i % 2 == 0) //odd side needs fliping
				wheel[i].spat.rotate(0, FastMath.PI, 0);
			wheel[i].spat.center();

			wheel[i].attachChild(wheel[i].spat);

			boolean front = (i < 2) ? true : false; //only the front turns

			addWheel(wheel[i], car.w_pos[i],
					car.w_vertical, car.w_axle, car.sus_restLength, car.w_radius, front);

			//Their friction (usually zero)
			setFrictionSlip(i, car.wheelBasicSlip);

			carNode.attachChild(wheel[i]);
			
			//TODO wheels rotate the wrong direction on the other side of the car
		}

		try {
			//generate the slip* max force from the car wheel data
			double error = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals
			
			this.maxlat = VehicleGripHelper.calcSlipMax(car.w_flatdata, error);
			if (Float.isNaN(maxlat))
				throw new Exception("maxlat was: '" + maxlat +"'.");
			this.maxlong = VehicleGripHelper.calcSlipMax(car.w_flongdata, error);
			if (Float.isNaN(maxlong)) 
				throw new Exception("maxlong was: '" + maxlong +"'.");
			
		} catch (Exception e) {
			e.printStackTrace();
			H.p("error in calculating max(lat|long) values of: "+car.getClass());
			System.exit(1);
		}
		
		//then finally set the gravity to 2 times because gameplay
		setGravity(getGravity().mult(2));
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

		case "Nitro":
			if (car.nitro_on) {
				ifNitro = value;
				if (!ifNitro)
					this.nitroTimeout = 2;
			}
			break;

		case "Jump":
			if (value) {
				applyImpulse(car.JUMP_FORCE, new Vector3f(0,0,0)); //push up
				Vector3f old = getPhysicsLocation();
				old.y += 2; //and move up
				setPhysicsLocation(old);
			}
			break;

		case "Handbrake":
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


	/**Used internally (in jme), creates the actual vehicle constraint when vehicle is added to phyicsspace
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

	public void giveSound(AudioNode audio) {
		if (engineSound != null) carRootNode.detachChild(engineSound); //incase we have 2 sounds running

		engineSound = audio;
		engineSound.setLooping(true);
		engineSound.play();
		carRootNode.attachChild(engineSound);
	}

	public void makeAI() {
		this.ai = new AI(this);
	}

	public void makeControl() {
		this.control = new MyKeyListener(this);
		App.rally.getInputManager().addRawInputListener(this.control);
	}


	//TODO find SAE950311
	//Millikin & Millikin's Race Car Vehicle Dynamics
	//https://www.sae.org/images/books/toc_pdfs/R146.pdf
	////////////////////////////////////////////////////

	private void specialPhysics(float tpf) {
		//NOTE: that z is forward, x is side
		// - the reference notes say that x is forward and y is sideways so just be careful

		Matrix3f w_angle = getPhysicsRotationMatrix();
		Vector3f w_velocity = getLinearVelocity();

		//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
		float yawspeed = car.length * getAngularVelocity().y;

		Vector3f velocity = w_angle.invert().mult(w_velocity);

		//calculate g Forces on the car (calc in world then convert to local)
		gForce = w_angle.invert().mult(vel.subtract(w_velocity).mult(1/(tpf*9.81f))); //mult by inverse time step and gravity
		vel = w_velocity; //set it to the current one
		
		
		float steeringCur = steeringCurrent;
		if (velocity.z < 0) { //to flip the steering on moving in reverse
			steeringCur *= -1;
		}

		//////////////////////////////////////////////////
		//'Wheel'less forces
		//linear resistance and quadratic drag
		float rr = car.resistance(9.81f);
		float dragx = -(rr * velocity.x + car.areo_drag * velocity.x * FastMath.abs(velocity.x));
		float dragy = -(rr * velocity.y + car.areo_drag * velocity.y * FastMath.abs(velocity.y));
		float dragz = -(rr * velocity.z + car.areo_drag * velocity.z * FastMath.abs(velocity.z));

		float dragDown = -0.5f * car.areo_downforce * 1.225f * (velocity.z*velocity.z); //is the formula from wiki
		
		Vector3f totalNeutral = new Vector3f(dragx, dragy + dragDown, dragz).multLocal(tpf);
		applyCentralForce(w_angle.mult(totalNeutral));

		//////////////////////////////////
		//Wheel Forces
		Vector3f[] wf = new Vector3f[] {
				new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f(),
		};

		if (velocity.z == 0) { //to avoid all the divide by zeros
			velocity.z += 0.00001;
		}
		
		//////////////////////////////////////
		//longitudinal forces
		engineTorque = getEngineWheelTorque(tpf, velocity.z);
		float[] torques = new float[] { 0, 0, 0, 0 };

		engineTorque /= 2;
		if (car.driveFront && car.driveRear)
			engineTorque /= 2; //split up into 2 axles

		if (car.driveFront) {
			torques[0] = engineTorque;
			torques[1] = engineTorque;

			//https://en.wikipedia.org/wiki/Limited-slip_differential#Basic_principle_of_operation
			//TODO this all just feels wrong (you also lose a lot of power)
			//calculate front diff
			float diff = wheel[0].radSec - wheel[1].radSec; //positive when 0 is faster
			if (diff > 0) { //check for total != 0 because divide by zero
//				torques[0] = 0.5f*torques[0] - 0.5f*diff; //limited slip:
//				torques[1] = 0.5f*torques[1] + 0.5f*diff; //make sure the difference not greater than w_difflock
			} else {
//				torques[0] = 0.5f*torques[0] + 0.5f*diff;
//				torques[1] = 0.5f*torques[1] - 0.5f*diff;
			}
		}
		if (car.driveRear) {
			torques[2] = engineTorque;
			torques[3] = engineTorque;
			
			//calc rear diff
			float diff = wheel[2].radSec - wheel[3].radSec; //positive when 0 is faster
			if (diff > 0) { //check for total != 0 because divide by zero
//				torques[2] = 0.5f*torques[2] - 0.5f*diff; //limited slip:
//				torques[3] = 0.5f*torques[3] + 0.5f*diff; //make sure the difference not greater than w_difflock
			} else {
//				torques[2] = 0.5f*torques[2] + 0.5f*diff;
//				torques[3] = 0.5f*torques[3] - 0.5f*diff;
			}
		}


		float maxSlowLat = Float.MAX_VALUE;
		
		//http://au.mathworks.com/help/physmod/sdl/ref/tiremagicformula.html
		float slip_const = 1;
		boolean slowSpeed = false;
		float rearSteeringCur = 0;
		float slowslipspeed = 9;
		if (velocity.length() <= slowslipspeed) {
			slowSpeed = true;
			slip_const = 2;
			
			//we want the force centre inline with the rear wheels at high speed and inline with the center at slow speeds
			steeringCur /= 2;
			rearSteeringCur = -steeringCur;
			
			if (steeringCur != 0) {
				float radius = car.w_zOff*2/FastMath.sin(steeringCur);
				
				float forceToKeepCircle = car.mass*velocity.lengthSquared()/radius;
				maxSlowLat = FastMath.abs(forceToKeepCircle/4);
			}
		}		

		//http://web.archive.org/web/20050308061534/home.planet.nl/~monstrous/tutstab.html
		//http://phors.locost7.info/contents.htm		

		//for each wheel
		for (int i = 0; i < 4; i++) {
			float susforce = (float)getWheel(i).getWheelInfo().wheelsSuspensionForce;
			susforce = Math.min(susforce, car.mass*3); //[*3] HACK: to stop weird harsh physics on large normal suspension forces

			float slip_div = Math.abs(velocity.length());
			if (slowSpeed) {
				slip_div = slowslipspeed + (velocity.length()*velocity.length())/slowslipspeed;
			}
			// TODO thought about how to make slow speeds fit, try and make tyres only push with movement

			// TODO note that the bottom turn could be max of vel and radsec: http://www.menet.umn.edu/~gurkan/Tire%20Modeling%20%20Lecture.pdf 
			float slipr = slip_const*(wheel[i].radSec*car.w_radius - velocity.z);
			float slipratio = slipr/slip_div;

			if (ifHandbrake && i > 1) //rearwheels, handbrake like this keeps engine speed and still slips 
//				slipratio = (0*car.w_radius - velocity.z)/Math.abs(velocity.z); //(TODO needs work)
				wheel[i].radSec = 0;

			float slipangle = 0;
			if (i < 2) {//front
				float slipa_front = (velocity.x + yawspeed*car.w_zOff);
				slipangle = (float)(FastMath.atan(slipa_front / slip_div) - steeringCur);
			} else { //rear
				float slipa_rear = (velocity.x - yawspeed*car.w_zOff);
				slipangle = (float)(FastMath.atan(slipa_rear / slip_div) - rearSteeringCur); //slip_div is questionable here
			}
			slipangle *= slip_const;
			
			if (i == 2) //a rear wheel
				driftangle = slipangle;

			float ratiofract = slipratio/maxlong;
			float anglefract = slipangle/maxlat;
			float p = FastMath.sqrt(ratiofract*ratiofract + anglefract*anglefract);
			
			//calc the longitudinal force from the slip ratio			
			wf[i].z = (ratiofract/p)*VehicleGripHelper.tractionFormula(car.w_flongdata, p*maxlong) * susforce;
			
			//latitudinal force that is calculated off the slip angle
			wf[i].x = -(anglefract/p)*VehicleGripHelper.tractionFormula(car.w_flatdata, p*maxlat) * susforce;

			
			//prevents the force from exceeding the centripetal force TODO breaks all other forces
			//TODO also note the 4 wheel steering that the other slowspeed code does
			if (slowSpeed) {
				if (Math.abs(wf[i].x) > maxSlowLat) {
					wf[i].x = FastMath.clamp(wf[i].x, -maxSlowLat, maxSlowLat);
				}
			}
				
			
			wheel[i].skid = p;
			
			//add the wheel force after merging the forces
			float totalLongForce = torques[i] - wf[i].z - (brakeCurrent*car.brakeMaxTorque*Math.signum(wheel[i].radSec));
			if (Math.signum(totalLongForce - (brakeCurrent*car.brakeMaxTorque*Math.signum(velocity.z))) != Math.signum(totalLongForce)) {
				//we maxed out the forces with braking
				wheel[i].radSec = 0;
			} else {
				wheel[i].radSec += tpf*totalLongForce/(car.e_inertia());
			}
			
			wheel[i].susForce = susforce;
			wheel[i].gripDir = wf[i].mult(1/susforce);
		}

		//HARD TODO: better code from others 'transision' between basic and advanced friction models at slow speeds
		//	i have no static model (yet)

		//ui based values
		totalTraction = "\nf: "+(wf[0].length() + wf[1].length())+", \nb:" + (wf[2].length() + wf[3].length());
		this.totalWheelRot = 0;
		for (MyWheelNode w: wheel) {
			this.totalWheelRot += w.radSec;
		}
		this.totalWheelRot /= 4;

		//and finally apply forces
		if (wheel[0].contact)
			applyForce(w_angle.mult(wf[0]), w_angle.mult(wheel[0].getContactPoint(car.w_radius, car.rollFraction)));
		if (wheel[1].contact) 
			applyForce(w_angle.mult(wf[1]), w_angle.mult(wheel[1].getContactPoint(car.w_radius, car.rollFraction)));
		if (wheel[2].contact) 
			applyForce(w_angle.mult(wf[2]), w_angle.mult(wheel[2].getContactPoint(car.w_radius, car.rollFraction)));
		if (wheel[3].contact) 
			applyForce(w_angle.mult(wf[3]), w_angle.mult(wheel[3].getContactPoint(car.w_radius, car.rollFraction)));

	}

	private float getEngineWheelTorque(float tpf, float vz) {
		float curGearRatio = car.trans_gearRatios[curGear];//0 = reverse, >= 1 make normal sense
		float diffRatio = car.trans_finaldrive/2; //the 2 makes it fit the real world for some reason?
		
		float wheelrot = 0;
		if (clutch == 0) { //TODO let the engine rev
			if (car.driveFront)
				wheelrot = (wheel[0].radSec + wheel[1].radSec)/2; //get the drive wheels rotation speed
			if (car.driveRear) 
				wheelrot = (wheel[2].radSec + wheel[3].radSec)/2; //get the average to pretend there is a diff
			
			curRPM = (int)(wheelrot*curGearRatio*diffRatio*60*car.w_radius); //rad/(m*sec) to rad/min and the drive ratios to engine
			//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min
		} //don't set the rpm if the clutch is in

		curRPM = Math.max(curRPM, 1000); //no stall please, its bad enough that we don't have to torque here

		autoTransmission(curRPM, vz);

		float nitroForce = 0;
		if (car.nitro_on) {
			if (ifNitro && this.nitro > 0) {
				nitroForce = car.nitro_force;
				this.nitro -= 2*tpf*car.nitro_rate;
				if (this.nitro < 0)
					this.nitro = 0; //no more nitro :(
			} else if (this.nitroTimeout > 0) { //start the timeout to start growing again
				this.nitroTimeout -= tpf;
				if (this.nitroTimeout < 0)
					this.nitroTimeout = 0;
			} else {
				this.nitro += car.nitro_rate*tpf;
				if (this.nitro > car.nitro_max)
					this.nitro = this.car.nitro_max;
			}
		}

		float engineTorque = (car.lerpTorque(curRPM) + nitroForce)*accelCurrent;
		float engineDrag = 0;
		if (accelCurrent < 0.05f || curRPM > car.e_redline) { //so compression only happens on no accel
			engineDrag = curRPM*car.e_compression;
		}
		if (Math.abs(curRPM) > car.e_redline) {
			return - engineDrag; //kill engine if greater than redline, and only apply compression
		}
		
		float engineOutTorque = engineTorque*curGearRatio*diffRatio*car.trans_effic - engineDrag;

		float totalTorque = engineOutTorque/car.w_radius;
		return totalTorque;
	}

	private void autoTransmission(int rpm, float vz) {
		if (curGear == 0) return; //no changing out of reverse on me please..

		float driveSpeed = (car.trans_gearRatios[curGear]*car.trans_finaldrive/2*60);
		float gearUpSpeed = car.auto_gearUp/driveSpeed;
		float gearDownSpeed = car.auto_gearDown/driveSpeed;
		//TODO: error checking that there isn't over lap  [2-----[3--2]----3] not [2------2]--[3-----3]

		if (vz > gearUpSpeed && curGear < car.trans_gearRatios.length-1) { //TODO should also work off speed so you can redline (both up and down)
			curGear++;
		} else if (vz < gearDownSpeed && curGear > 1) {
			curGear--;
		}
	}


	//////////////////////////////////////////////////////////////
	public void myUpdate(float tpf) {
		distance += getLinearVelocity().length()*tpf;

		Matrix3f playerRot = new Matrix3f();
		getPhysicsRotationMatrix(playerRot);

		up = playerRot.mult(new Vector3f(0,1,0));
		left = playerRot.mult(new Vector3f(1,0,0));
		right = playerRot.mult(new Vector3f(-1,0,0));

		for (MyWheelNode w: wheel) {
			WheelInfo wi = getWheel(w.num).getWheelInfo();
			RaycastInfo ray = wi.raycastInfo;
			w.contact = (ray.groundObject != null);

			if (w.num % 2 == 1) 
				w.update(tpf, 1);
			else 
				w.update(tpf, -1);
		}

		//skid marks
		addSkidLines();

		//********************************************//
		//Important call here
		specialPhysics(tpf); //yay
		//********************************************//

		//wheel turning logic
		steeringCurrent = 0;
		if (steerLeft != 0) { //left
			steeringCurrent += car.w_steerAngle*steerLeft;
		}
		if (steerRight != 0) { //right
			steeringCurrent -= car.w_steerAngle*steerRight;
		}
		
//		steeringCurrent = (float)VehiclePhysicsHelper.linearise(steeringCurrent, 0.5f); 
		//TODO use function correctly
		//TODO balance number
		
		//TODO make better [please its actually amazing to use]
		float absdangle = Math.abs(driftangle);
		if (absdangle > FastMath.DEG_TO_RAD*10 && absdangle > car.w_steerAngle) { 
//			steeringCurrent = FastMath.sign(steeringCurrent)*absdangle;
		}

		//TODO 0.3 - 0.4 seconds from lock to lock seems okay from what ive seen
		
		steeringCurrent = FastMath.clamp(steeringCurrent, -FastMath.PI*3f/8f, FastMath.PI*3f/8f);
		steer(steeringCurrent);

		//update ai if any
		if (ai != null) {
			ai.update(tpf);
		}

		if (engineSound != null) {
			//if sound exists
			float pitch = FastMath.clamp(0.5f+1.5f*((float)curRPM/(float)car.e_redline), 0.5f, 2);
			engineSound.setPitch(pitch);
		}
		
	}

	///////////////////////////////////////////////////////////
	private void addSkidLines() {
		if (ai != null) return;

		if (App.rally.drive.frameCount % 4 == 0) {
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
		if (App.rally.drive == null) return;

		setPhysicsRotation(new Matrix3f());
		setLinearVelocity(new Vector3f());
		setAngularVelocity(new Vector3f());

		
		setPhysicsLocation(App.rally.drive.world.getWorldStart());
		setPhysicsRotation(App.rally.drive.world.getWorldRot());
		setAngularVelocity(new Vector3f());
		
		App.rally.drive.reset();
/*
		if (App.rally.drive.type == WorldType.DYNAMIC) {
			setPhysicsLocation(App.rally.drive.worldB.getWorldStart());
			Matrix3f p = new Matrix3f();
			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
			setPhysicsRotation(p);

			setAngularVelocity(new Vector3f());
		} else {
			setPhysicsLocation(App.rally.drive.world.start);
		}
*/	
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

	public void cleanup() {
		//TODO more
		this.ai = null;
		if (this.control != null) {
			App.rally.getInputManager().removeRawInputListener(this.control);
			this.control = null;
		}

		if (this.engineSound != null) {
			carRootNode.detachChild(this.engineSound);
			App.rally.getAudioRenderer().stopSource(engineSound);
			engineSound = null;
		}
	}
}

class VehicleGripHelper {
	//http://www.gamedev.net/topic/462784-simplified-pacejka-magic-formula/

	//There were fancy versions of the Pacejka's Formula here but there were removed
	//Try the git repositiory to get them back. (it should say 'removed' in the git message)

	/** Pacejka's Formula simplified from the bottom of:.
	 * http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	 * @param w Pick the lateral or longitudial version to send.
	 * @param slip Slip angle or slip ratio (it doesn't matter except for one value changes on it)
	 * @return The force expected
	 */
	static float tractionFormula(WheelData w, float slip) {
		return w.D * FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
	}

	//returns the slip value that gives the closest to 1 from the magic formula (should be called twice, lat and long)
	static float calcSlipMax(WheelData w, double error) {
		double lastX = 0.2f; //our first guess (usually finishes about 0.25f)
		double nextX = lastX + 5*error; //just so its a larger diff that error

		while (Math.abs(lastX - nextX) > error) {
			lastX = nextX;
			nextX = iterate(w, lastX, error);
		}
		return (float)nextX;
	}
	private static double iterate(WheelData w, double x, double error) {
		return x - ((tractionFormula(w, (float)x)-w.D) / dtractionFormula(w, (float)x, error)); 
		//-1 because we are trying to find a max (which happens to be 1)
	}
	private static double dtractionFormula(WheelData w, double slip, double error) {
		return (tractionFormula(w, (float)(slip+error)) - tractionFormula(w , (float)(slip-error)))/ (2*error);
	}
	
	//steering factor from: http://www.racer.nl/reference/carphys.htm#linearity
	//TODO use
	static double linearise(double input, double factor) {
		return input*factor + (1 - factor) * Math.pow(input, 3);
	}
	
	//TODO could look into the brush tyre model
}