package game;

import java.util.LinkedList;
import java.util.List;

import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.dynamics.vehicle.WheelInfo.RaycastInfo;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

public class MyVehicleControl extends VehicleControl implements ActionListener {
	
	//skid stuff
	Node skidNode = new Node();
	LinkedList<Spatial> skidList = new LinkedList<Spatial>();
	
	//directions
	Vector3f forward = new Vector3f();
	Vector3f right = new Vector3f();
	Vector3f left = new Vector3f();
	
	//admin stuff
	AssetManager assetManager;
	Rally rally;
	
	//car data
	Car car;
	
	MyWheelNode wn0;
	MyWheelNode wn1;
	MyWheelNode wn2;
	MyWheelNode wn3;
	
	//driving stuff
	boolean togglePhys = false;
	boolean ifSmoke = false;

	int curGear = 1; //TODO set this
	int curRPM = 0;
	
	boolean ifAccel = false;
	boolean ifReverse = false;
	
	float steeringCurrent = 0;
	int steeringDirection = 0; //-1 = right, 1 = left
	
	float brakeCurrent = 0;
	
	boolean ifHandbrake = false;
	//- driving stuff
	
	float distance = 0;
	
	MyVehicleControl(CollisionShape col, Car car, AssetManager assetManager, Node carNode, Rally rally) {
		super(col, car.mass);
		this.car = car;
		this.assetManager = assetManager;
		this.rally = rally;
		
		this.setSuspensionCompression(car.susCompression);
		this.setSuspensionDamping(car.susDamping);
		this.setSuspensionStiffness(car.stiffness);
		this.setMaxSuspensionForce(car.maxSusForce);
		
		
		wn0 = new MyWheelNode("wheel 0 node", this, 0);
		Spatial wheel0 = assetManager.loadModel(car.wheelModel);
		wheel0.center();
		wn0.attachChild(wheel0);
		addWheel(wn0, new Vector3f(-car.w_xOff, car.w_yOff, car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		wn1 = new MyWheelNode("wheel 1 node", this, 1);
		Spatial wheel1 = assetManager.loadModel(car.wheelModel);
		wheel1.rotate(0, FastMath.PI, 0);
		wheel1.center();
		wn1.attachChild(wheel1);
		addWheel(wn1, new Vector3f(car.w_xOff, car.w_yOff, car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		wn2 = new MyWheelNode("wheel 2 node", this, 2);
		Spatial wheel2 = assetManager.loadModel(car.wheelModel);
		wheel2.center();
		wn2.attachChild(wheel2);
		addWheel(wn2, new Vector3f(-car.w_xOff-0.05f, car.w_yOff, -car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		wn3 = new MyWheelNode("wheel 3 node", this, 3);
		Spatial wheel3 = assetManager.loadModel(car.wheelModel);
		wheel3.rotate(0, FastMath.PI, 0);
		wheel3.center();
		wn3.attachChild(wheel3);
		addWheel(wn3, new Vector3f(car.w_xOff+0.05f, car.w_yOff, -car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		//Friction
		setFrictionSlip(0, car.wheel0Slip);
		setFrictionSlip(1, car.wheel1Slip);
		setFrictionSlip(2, car.wheel2Slip);
		setFrictionSlip(3, car.wheel3Slip);
		
		//attaching all the things (wheels)
		carNode.attachChild(wn0);
		carNode.attachChild(wn1);
		carNode.attachChild(wn2);
		carNode.attachChild(wn3);
		
		makeSmoke(wn0);
		makeSmoke(wn1);
		makeSmoke(wn2);
		makeSmoke(wn3);
		
		////////////////////////
		setupKeys();
//		skidNode.setShadowMode(ShadowMode.Off);
	}
	
	private void makeSmoke(MyWheelNode wheelNode) {
		wheelNode.smoke = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
		wheelNode.smoke.setImagesX(15); //smoke is 15x1 (1 is default for y)
		wheelNode.smoke.setEndColor(new ColorRGBA(1f, 1f, 1f, 0f)); //transparent
		wheelNode.smoke.setStartColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 0.3f)); //strong white

		wheelNode.smoke.setStartSize(0.5f);
		wheelNode.smoke.setGravity(0, -4, 0);
		wheelNode.smoke.setLowLife(1f);
		wheelNode.smoke.setHighLife(1f);
		wheelNode.smoke.getParticleInfluencer().setVelocityVariation(0.05f);
//	    wheelNode.smoke.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 20, 0));
	    
	    Material mat_emit = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
	    mat_emit.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
	    wheelNode.smoke.setMaterial(mat_emit);
	    if (ifSmoke) {
	    	wheelNode.attachChild(wheelNode.smoke);
	    }
	}
	
	//controls
	private void setupKeys() {
		rally.getInputManager().addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
		rally.getInputManager().addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
		rally.getInputManager().addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
		rally.getInputManager().addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
		rally.getInputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_Q));
		rally.getInputManager().addMapping("Handbrake", new KeyTrigger(KeyInput.KEY_SPACE));
		rally.getInputManager().addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
		rally.getInputManager().addMapping("Impluse", new KeyTrigger(KeyInput.KEY_LCONTROL));
		rally.getInputManager().addMapping("Reverse", new KeyTrigger(KeyInput.KEY_LSHIFT));
		
		rally.getInputManager().addListener(this, "Lefts");
		rally.getInputManager().addListener(this, "Rights");
		rally.getInputManager().addListener(this, "Ups");
		rally.getInputManager().addListener(this, "Downs");
		rally.getInputManager().addListener(this, "Jump");
		rally.getInputManager().addListener(this, "Handbrake");
		rally.getInputManager().addListener(this, "Reset");
		rally.getInputManager().addListener(this, "Impluse");
		rally.getInputManager().addListener(this, "Reverse");
		
		//TODO use the Controller class
	}
	
	public void onAction(String binding, boolean value, float tpf) {
		//value == 'if pressed'
		//value = value of press, with keys its always 0 or 1
		//controllers on the otherhand...
		
		if (binding.equals("Lefts")) {
			if (value) {
				steeringDirection = 1;
			} else {
				steeringDirection = 0;
			}
			steer(steeringCurrent);
		} 
		if (binding.equals("Rights")) {
			if (value) {
				steeringDirection = -1;
			} else {
				steeringDirection = 0;
			}
		}
		
		if (binding.equals("Ups")) {
			if (value) {
				ifAccel = true;
			} else {
				ifAccel = false;
			}

		} 
		if (binding.equals("Downs")) {
			if (value) {
				brakeCurrent += car.MAX_BRAKE;
			} else {
				brakeCurrent -= car.MAX_BRAKE;
			}
			
		} 
		if (binding.equals("Jump")) {
			if (value) {
				applyImpulse(car.JUMP_FORCE, new Vector3f(0,0,0)); //push up
				Vector3f old = getPhysicsLocation();
				old.y += 2; //and move up
				setPhysicsLocation(old);
			}
			
		} 
		if (binding.equals("Handbrake")) {
			ifHandbrake = !ifHandbrake;
			
		} 
		if (binding.equals("Impluse")) {
			if (value) {
				togglePhys = !togglePhys;
				System.out.println("physics = "+!togglePhys);
			}
		} 
		if (binding.equals("Reset")) {
			if (value) {
				reset();
			} else {
			}
		} 
		if (binding.equals("Reverse")) {
			ifReverse = !ifReverse;
		}
	}
	//end controls
	
	////////////////////////////////////////////////////
	
	private void specialPhysics(float tpf) {
		if (togglePhys){ return; }//no need to apply wheel forces now
		//TODO fix backwards and slow speeds
		
		//NOTE: that z is forward, x is side
		//  the reference ntoes say that x is forward and y is sideways (just be careful)
		
		Matrix3f w_angle = getPhysicsRotationMatrix();
		Vector3f w_velocity = getLinearVelocity();
		
		//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
		double yawspeed = car.length * getAngularVelocity().y;
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		
		double rot_angle = 0, sideslip = 0;
		if (velocity.z > 0.001) { //no divide by zero errors
			rot_angle = Math.atan(yawspeed / velocity.z);
			sideslip = Math.atan(velocity.x / velocity.z);
		}
		
		float steeringCur = steeringCurrent;
		if (velocity.z < 0) { //need to flip the steering on moving in reverse
			steeringCur *= -1;
		}
		
		//important angles
		double slipanglefront = sideslip + rot_angle - steeringCur;
		double slipanglerear = sideslip - rot_angle;
		
		//////////////////////////////////
		double weight = car.mass*9.81*0.25; //0.25 because its per wheel
		
		Vector3f force_lat_front_left = new Vector3f();
		Vector3f force_lat_front_right = new Vector3f();
		Vector3f force_lat_rear_left = new Vector3f();
		Vector3f force_lat_rear_right = new Vector3f();
		
		//calculate off of the slip angle
		if (wn0.contact) { //front left
			force_lat_front_left.x = (float)(slipanglefront*car.CA_F);
			force_lat_front_left.x = FastMath.clamp(force_lat_front_left.x, -car.MAX_GRIP, car.MAX_GRIP); //TODO clamp later
			wn0.skid = Math.abs(force_lat_front_left.x)/car.MAX_GRIP;
			force_lat_front_left.x *= weight;
		} else { wn0.skid = 0; }
		if (wn1.contact) { //front right
			force_lat_front_right.x = (float)(slipanglefront*car.CA_F);
			force_lat_front_right.x = FastMath.clamp(force_lat_front_right.x, -car.MAX_GRIP, car.MAX_GRIP);
			wn1.skid = Math.abs(force_lat_front_right.x)/car.MAX_GRIP;
			force_lat_front_right.x *= weight;
		} else { wn1.skid = 0; }

		if (wn2.contact) { //rear left
			force_lat_rear_left.x = (float)(slipanglerear*car.CA_R);
			force_lat_rear_left.x = FastMath.clamp(force_lat_rear_left.x, -car.MAX_GRIP, car.MAX_GRIP);
			wn2.skid = Math.abs(force_lat_rear_left.x)/car.MAX_GRIP;
			force_lat_rear_left.x *= weight;
			if (ifHandbrake) force_lat_rear_left.x *= 0.5f;
		} else { wn2.skid = 0; }
		if (wn3.contact) { //rear right
			force_lat_rear_right.x = (float)(slipanglerear*car.CA_R);
			force_lat_rear_right.x = FastMath.clamp(force_lat_rear_right.x, -car.MAX_GRIP, car.MAX_GRIP);
			wn3.skid = Math.abs(force_lat_rear_right.x)/car.MAX_GRIP;
			force_lat_rear_right.x *= weight;
			if (ifHandbrake) force_lat_rear_right.x *= 0.5f;
		} else { wn3.skid = 0; }
		
		float accel;
		if (ifAccel) {
			accel = getForceFromEngineToWheels(velocity.z);//*accelCurrent;
//			accel = car.MAX_ACCEL; //TODO remove for 'geared' accel
			if (ifReverse) {
				accel *= -1;
			}
		} else {
			getForceFromEngineToWheels(velocity.z); //because the rpm and gear still need to be updated
			accel = 0;
		}
		
		//TODO figure out why '100' / ?maybe brake and accel can fight?
		float ftractionz = 0;
		if (wn2.contact && wn3.contact) {
			ftractionz = accel - 100*(brakeCurrent*FastMath.sign(velocity.z));
		} //TODO make accel 'cost' grip, maybe try and just do the physics per wheel
		
		//linear resistance and quadratic drag here
		float dragy = (float)(-(car.RESISTANCE * velocity.z + car.DRAG * velocity.z * Math.abs(velocity.z)));
		float dragx = (float)(-(car.RESISTANCE * velocity.x + car.DRAG * velocity.x * Math.abs(velocity.x))); 

		////////////////////////////////////
		//put them into global and 4 x direction forces:
		Vector3f totalNeutral = new Vector3f(dragx,0,dragy);
		applyCentralForce(w_angle.mult(totalNeutral)); //non wheel based forces on the car

		ftractionz /= 2; //because 2 wheels an axel
		if (car.driveFront && car.driveRear) 
			ftractionz /= 2; //if its split up between the front and rear axle
		
		//    wn0      wn1      wn2      wn3
		float flx,flz, frx,frz, rlx,rlz, rrx,rrz;
		
		flx = FastMath.cos(steeringCurrent)*force_lat_front_left.x;
		frx = FastMath.cos(steeringCurrent)*force_lat_front_right.x;
		if (car.driveFront) {
			flz = FastMath.sin(steeringCurrent)*force_lat_front_left.z + ftractionz;
			frz = FastMath.sin(steeringCurrent)*force_lat_front_right.z + ftractionz;
		} else {
			flz = FastMath.sin(steeringCurrent)*force_lat_front_left.z;
			frz = FastMath.sin(steeringCurrent)*force_lat_front_right.z;
		}
		
		rlx = force_lat_rear_left.x;
		rrx = force_lat_rear_right.x;
		if (car.driveRear) {
			rlz = force_lat_rear_left.z + ftractionz;
			rrz = force_lat_rear_right.z + ftractionz;
		} else {
			rlz = force_lat_rear_left.z;
			rrz = force_lat_rear_right.z;
		}
		
		Vector3f frontl = w_angle.mult(new Vector3f(flx, 0, flz));
		Vector3f frontr = w_angle.mult(new Vector3f(frx, 0, frz));
		Vector3f rearl = w_angle.mult(new Vector3f(rlx, 0, rlz));
		Vector3f rearr = w_angle.mult(new Vector3f(rrx, 0, rrz));
		
		if (wn0.contact)
			applyForce(frontl, w_angle.mult(wn0.getLocalTranslation())); //apply forces
		if (wn1.contact)
			applyForce(frontr, w_angle.mult(wn1.getLocalTranslation())); //TODO add in the height of the wheel
		if (wn2.contact)
			applyForce(rearl, w_angle.mult(wn2.getLocalTranslation()));
		if (wn3.contact)
			applyForce(rearr, w_angle.mult(wn3.getLocalTranslation()));
		
		////////////////////
		//TODO actually do something about engine torque
		
		//basic overall forumla for acceleration
//		drive = direction*(engineTorque*throttle)*gearratio*diffratio*transmissioneffiency/wheelradius
		
		//slip ratio and traction force
//		o = (w*wheelradius - velocity.z)/(abs(velocity.z))
//		I get the feeling the CAR_MAXGRIP is actually for this section
		
		//torque on the drive wheels overview
//		totaltorque = drivetorque + tractiontorque + braketorque
//		angular accel = totaltorque/rearwheelsinertia (rear wheel inetria = (wheelmass*radius^2)/2 )
//		rearwheelangularvelocity += anguaraccel*tpf
	
		//the notes specifically say that this situation kind of seems circular.
		//just use the last values: new = old + change*timestep
		
	}
	
	private float getForceFromEngineToWheels(float speedForward) {
		float wheelrot = speedForward/(2*FastMath.PI*car.wheelRadius); //w = v/(2*Pi*r) -> rad/sec
		
		float curGearRatio = car.gearRatios[1+curGear];
		float diffRatio = car.gearRatios[0];
		curRPM = (int)(wheelrot*curGearRatio*diffRatio*60); //TODO change from gear 1
			//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min
		if (curRPM > 5500 && curGear < car.gearRatios.length-2) { //TODO test end range
			curGear++;
		}
		//TODO find good numbers for all of these IFS
		//TODO put them in the Car class
		if (curRPM < 2400 && curGear > 1) { 
			curGear--;
		}
		
		float engineTorque = lerpTorque(curRPM);
		float driveTorque = engineTorque*curGearRatio*diffRatio;//TODO*trasmissionefficency;
		
		return driveTorque/car.wheelRadius;
	}
	
	//////////////////////////////////////////////////////////////
	public void myUpdate(float tpf) {
		distance += getLinearVelocity().length()*tpf;
		
		WheelInfo wi0 = getWheel(0).getWheelInfo();
		RaycastInfo rayCastInfo0 = wi0.raycastInfo;
		wn0.contact = (rayCastInfo0.groundObject != null);
		
		WheelInfo wi1 = getWheel(1).getWheelInfo();
		RaycastInfo rayCastInfo1 = wi1.raycastInfo;
		wn1.contact= (rayCastInfo1.groundObject != null);
		
		WheelInfo wi2 = getWheel(2).getWheelInfo();
		RaycastInfo rayCastInfo2 = wi2.raycastInfo;
		wn2.contact = (rayCastInfo2.groundObject != null);
		
		WheelInfo wi3 = getWheel(3).getWheelInfo();
		RaycastInfo rayCastInfo3 = wi3.raycastInfo;
		wn3.contact = (rayCastInfo3.groundObject != null);
		
		specialPhysics(tpf); //yay 
		
		//skid marks
		rally.frameCount++;
		if (rally.frameCount % 4 == 0) {
			addSkidLines();
		}
				
		Matrix3f playerRot = new Matrix3f();
		getPhysicsRotationMatrix(playerRot);
		
		left = playerRot.mult(new Vector3f(1,0,0));
		right = playerRot.mult(new Vector3f(1,0,0).negate());

		//wheel turning logic
		if (steeringDirection > 0) { //left
			steeringCurrent = Math.min(0.05f+steeringCurrent, car.MAX_STEERING);
		}
		if (steeringDirection < 0) { //right
			steeringCurrent = Math.max(-0.05f+steeringCurrent, -car.MAX_STEERING);
		}
		if (steeringDirection == 0 && steeringCurrent != 0) { //decay the turning angle slightly
			steeringCurrent -= FastMath.sign(steeringCurrent)*0.04f;
		}
		if (Math.abs(steeringCurrent) < 0.05 ) steeringCurrent = 0;
		steer(steeringCurrent);
	}
	
	///////////////////////////////////////////////////////////
	private void addSkidLines() {
		wn0.addSkidLine();
		wn1.addSkidLine();
		wn2.addSkidLine();
		wn3.addSkidLine();
		
		int extra = skidList.size() - 500; //so i can remove more than one (like all 4 that frame)
		for (int i = 0; i < extra; i++) {
			skidNode.detachChild(skidList.getFirst());
			skidList.removeFirst();
		}
	}
	
	////////////////////
	//getters
	public String getWheelTractionInfo() {
		String out = "";
		out = "\n" + getWheel(0).getSkidInfo() + "\n" +
		getWheel(1).getSkidInfo() + "\n" + 
		getWheel(2).getSkidInfo() + "\n" +
		getWheel(3).getSkidInfo();
		return out;
	}
	
	public float getTotalGrip() {
		MyWheelNode[] n = {wn0, wn1, wn2, wn3};
		float total = 0;
		for (MyWheelNode a: n) {
			total += a.skid;
		}
		return total;
	}

	private void reset() { //TODO clean
		setPhysicsRotation(new Matrix3f());
		setLinearVelocity(new Vector3f(0,0,0));
		setAngularVelocity(new Vector3f(0,0,0));
		resetSuspension();
		
		rally.arrowNode.detachAllChildren();

		if (rally.dynamicWorld) {
			//TODO wow this is a mess
			List<Spatial> ne = new LinkedList<Spatial>(rally.worldB.pieces);
			for (Spatial s: ne) {
				rally.getPhysicsSpace().remove(s.getControl(0));
				rally.worldB.detachChild(s);
				rally.worldB.pieces.remove(s);
			}
			rally.worldB.start = new Vector3f(0,0,0);
			rally.worldB.nextPos = new Vector3f(0,0,0);
			rally.worldB.nextRot = new Quaternion();
			
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
		wn0.last = new Vector3f(0,0,0);
		wn1.last = new Vector3f(0,0,0);
		wn2.last = new Vector3f(0,0,0);
		wn3.last = new Vector3f(0,0,0);
	}

	private float lerpTorque(int rpm) {
		if (rpm < 0) rpm = 0; //prevent array issues
		
		int intrpm = (rpm / 1000); //basically divide by 1000 and round down
		float remrpm = (float)(rpm % 1000)/1000;
		float before = torqueFromArray(intrpm);
		float after = torqueFromArray(intrpm+1);
		return FastMath.interpolateLinear(remrpm, before, after);
	}
	
	private float torqueFromArray(int i) {
		if (i < 1) i = 1;
		if (i > car.torque.length-1) i =car.torque.length-1; 
		return car.torque[i];
	}
}


class MyWheelNode extends Node {

	MyVehicleControl mvc;
	int num;
	
	Material mat;
	int[] indexes = { 2,0,1, 1,3,2 };
	
	boolean contact;
	Vector3f last;
	
	float skid;
	ParticleEmitter smoke;
	
	public MyWheelNode(String name, MyVehicleControl mvc, int num) {
		super(name);
		this.mvc = mvc;
		this.num = num;
		this.last = new Vector3f(new Vector3f(0,0,0));
		
		this.setShadowMode(ShadowMode.CastAndReceive);
	}

	public void addSkidLine() {
		if (contact) {
			addSkidLine(last, mvc.getWheel(num).getCollisionLocation(), skid);
			last = mvc.getWheel(num).getCollisionLocation();
		} else {
			last = new Vector3f(0,0,0);
		}
	}
	
	private void addSkidLine(Vector3f a, Vector3f b, float grip) {
		if (a.equals(new Vector3f(0,0,0)) || b.equals(new Vector3f(0,0,0))) {
			return; //don't make a line because they aren't valid positions
		}
		a.y += 0.05;
		b.y += 0.05; //z-buffering (i.e. to stop it "fighting" with the ground)
		
		Mesh mesh = new Mesh(); //making a quad positions
		Vector3f [] vertices = new Vector3f[4];
		vertices[0] = a.add(mvc.right.mult(mvc.car.wheelWidth));
		vertices[1] = b.add(mvc.right.mult(mvc.car.wheelWidth));
		vertices[2] = a.add(mvc.left.mult(mvc.car.wheelWidth));
		vertices[3] = b.add(mvc.left.mult(mvc.car.wheelWidth));
		
		Vector2f[] texCoord = new Vector2f[4]; //texture of quad
		texCoord[0] = new Vector2f(0, 0);
		texCoord[1] = new Vector2f(0, 1);
		texCoord[2] = new Vector2f(1, 0);
		texCoord[3] = new Vector2f(1, 1);
		
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));

		mesh.updateBound();
		
		Geometry geo = new Geometry("MyMesh", mesh);
		
		Material mat = new Material(mvc.assetManager, "Common/MatDefs/Light/Lighting.j3md");
		
		Texture tex = mvc.assetManager.loadTexture("assets/stripes.png");
		mat.setTexture("DiffuseMap", tex);
		mat.setTexture("NormalMap", tex);
		mat.setBoolean("UseMaterialColors", true);
		
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		mat.setColor("Diffuse", new ColorRGBA(0,0,0,grip));
		
		mat.setBoolean("UseMaterialColors", true);
		geo.setMaterial(mat);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		mvc.skidNode.attachChild(geo);
		mvc.skidList.add(geo);
		
//		mat.setColor("GlowColor", ColorRGBA.Blue); TODO move somewhere else
	}
}