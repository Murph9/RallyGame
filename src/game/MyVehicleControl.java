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
	Vector3f wheel0Last = new Vector3f(0,0,0);
	Vector3f wheel1Last = new Vector3f(0,0,0);
	Vector3f wheel2Last = new Vector3f(0,0,0);
	Vector3f wheel3Last = new Vector3f(0,0,0);
	
	//contact points
	boolean contact0 = false;
	boolean contact1 = false;
	boolean contact2 = false;
	boolean contact3 = false;
	
	float w0_myskid;
	float w1_myskid;
	float w2_myskid;
	float w3_myskid;
	
	boolean ifSmoke = false;
	ParticleEmitter emit;
	
	//directions
	Vector3f forward = new Vector3f();
	Vector3f right = new Vector3f();
	Vector3f left = new Vector3f();
	
	//admin stuff
	AssetManager assetManager;
	Rally rally;
	
	//car data
	Car car;
	
	//driving stuff
	boolean togglePhys = false;
	
	int curGear = 0;
	float accelCurrent = 0;
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
		this.setMaxSuspensionForce(25*car.mass);
		
		Node node1 = new Node("wheel 1 node");
		Spatial wheels1 = assetManager.loadModel(car.wheelOBJModel);
		wheels1.center();
		node1.attachChild(wheels1);
		addWheel(node1, new Vector3f(-car.w_xOff, car.w_yOff, car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		Node node2 = new Node("wheel 2 node");
		Spatial wheels2 = assetManager.loadModel(car.wheelOBJModel);
		wheels2.rotate(0, FastMath.PI, 0);
		wheels2.center();
		node2.attachChild(wheels2);
		addWheel(node2, new Vector3f(car.w_xOff, car.w_yOff, car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		Node node3 = new Node("wheel 3 node");
		Spatial wheels3 = assetManager.loadModel(car.wheelOBJModel);
		wheels3.center();
		node3.attachChild(wheels3);
		addWheel(node3, new Vector3f(-car.w_xOff-0.05f, car.w_yOff, -car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		Node node4 = new Node("wheel 4 node");
		Spatial wheels4 = assetManager.loadModel(car.wheelOBJModel);
		wheels4.rotate(0, FastMath.PI, 0);
		wheels4.center();
		node4.attachChild(wheels4);
		addWheel(node4, new Vector3f(car.w_xOff+0.05f, car.w_yOff, -car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		//Friction
		setFrictionSlip(0, car.wheel1Slip);
		setFrictionSlip(1, car.wheel2Slip);
		setFrictionSlip(2, car.wheel3Slip);
		setFrictionSlip(3, car.wheel4Slip);
		
		//attaching all the things (wheels)
		carNode.attachChild(node1);
		carNode.attachChild(node2);
		carNode.attachChild(node3);
		carNode.attachChild(node4);
		
		makeSmoke(node1);
		makeSmoke(node2);
		makeSmoke(node3);
		makeSmoke(node4);
		
		////////////////////////
		setupKeys();
//		skidNode.setShadowMode(ShadowMode.Off);
	}
	
	private void makeSmoke(Node wheelNode) {
		emit = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
	    emit.setImagesX(15); //smoke is 15x1 (1 is default for y)
	    emit.setEndColor(new ColorRGBA(1f, 1f, 1f, 0f)); //transparent
	    emit.setStartColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 0.3f)); //strong white

	    emit.setStartSize(0.5f);
	    emit.setGravity(0, -4, 0);
	    emit.setLowLife(1f);
	    emit.setHighLife(1f);
	    emit.getParticleInfluencer().setVelocityVariation(0.05f);
//	    emit.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 20, 0));
	    
	    Material mat_emit = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
	    mat_emit.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
	    emit.setMaterial(mat_emit);
	    if (ifSmoke) {
	    	wheelNode.attachChild(emit);
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
				applyImpulse(car.JUMP_FORCE, Vector3f.ZERO); //push up
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
				setPhysicsRotation(new Matrix3f());
				setLinearVelocity(Vector3f.ZERO);
				setAngularVelocity(Vector3f.ZERO);
				resetSuspension();
				
				rally.arrowNode.detachAllChildren();

				if (rally.fancyWorld) {
					//TODO wow this is a mess
					List<Spatial> ne = new LinkedList<Spatial>(rally.worldB.pieces);
					for (Spatial s: ne) {
						rally.getPhysicsSpace().remove(s.getControl(0));
						rally.worldB.detachChild(s);
						rally.worldB.pieces.remove(s);
					}
					rally.worldB.start = new Vector3f(0,0,0);
					rally.worldB.nextPos = new Vector3f(0,0,0);
					setPhysicsLocation(rally.worldB.start);
					Matrix3f p = new Matrix3f();
					p.fromAngleAxis(FastMath.DEG_TO_RAD*90, Vector3f.UNIT_Y);
					setPhysicsRotation(p);

				} else {
					setPhysicsLocation(rally.world.start);
				}
				
				
				skidNode.detachAllChildren();
				skidList.clear();
				wheel0Last = Vector3f.ZERO;
				wheel1Last = Vector3f.ZERO;
				wheel2Last = Vector3f.ZERO;
				wheel3Last = Vector3f.ZERO;
			} else {
			}
		} 
		if (binding.equals("Reverse")) {
			ifReverse = !ifReverse;
		}
	}
	//- controls
	
	
	private void specialPhysics(float tpf) {
		if (togglePhys){ return; }//no need to apply wheel forces now

		//NOTE: that z is forward, x is side
		//  but the notes say x is forward and y is sideways
		
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
		
		double slipanglefront = sideslip + rot_angle - steeringCur;
		double slipanglerear = sideslip - rot_angle;
		
		double weight = car.mass*9.81*0.5; //0.5 because its per axle
		
		//calculate grid off of the slip angle
		Vector3f force_lat_front = new Vector3f();
		if (contact0 && contact1) { //contact with front
			force_lat_front.x = (float)(slipanglefront*car.CA_F);
			force_lat_front.x = FastMath.clamp(force_lat_front.x, -car.MAX_GRIP, car.MAX_GRIP);
			w0_myskid = w1_myskid = Math.abs(force_lat_front.x)/car.MAX_GRIP;
			force_lat_front.x *= weight;
//			if (ifHandbrake) { 
//				force_lat_front.x *= 0.5;
//			}
		}
		
		Vector3f force_lat_rear = new Vector3f();
		if (contact2 && contact3) {
			force_lat_rear.x = (float)(slipanglerear*car.CA_R);
			force_lat_rear.x = FastMath.clamp(force_lat_rear.x, -car.MAX_GRIP, car.MAX_GRIP);
			w2_myskid = w3_myskid = Math.abs(force_lat_rear.x)/car.MAX_GRIP;
			force_lat_rear.x *= weight;
			if (ifHandbrake) {
				force_lat_rear.x *= 0.5f;
			}
		}
//		System.out.println(force_lat_rear.x);
		
		float accel;
		float x = accelCurrent;
		if (ifAccel) {
			accel = (accelCurrent+0.5f)*car.MAX_ACCEL; //TODO fix stall on gear 4/5
			accel = -FastMath.pow((x - 0.57f),4) - FastMath.pow((x - 0.57f),3) - (FastMath.pow((x - 0.57f),2) - 0.42f);
			accel *= car.MAX_ACCEL*4;
			accel = car.MAX_ACCEL; //TODO remove for 'geared' accel
			if (ifReverse) {
				accel *= -1;
			}
		} else {
			accel = 0;
		}
		//TODO figure out why '100' / ?maybe brake and accel can fight?
		Vector3f ftraction = new Vector3f();
		if (contact0 && contact1 && contact2 && contact3) {
			ftraction.z = 100*(accel - brakeCurrent*FastMath.sign(velocity.z));
		} //TODO make accel 'cost' grip
		
		
		//speed drag and linear resistance here
		Vector3f resistance = new Vector3f();
		resistance.z = (float)(-(car.RESISTANCE * velocity.z + car.DRAG * velocity.z * Math.abs(velocity.z)));
		resistance.x = (float)(-(car.RESISTANCE * velocity.x + car.DRAG * velocity.x * Math.abs(velocity.x))); 
		
		//put into a force
		Vector3f force = new Vector3f();
		force.z = (float) (ftraction.z + Math.sin(steeringCurrent)*force_lat_front.z + force_lat_rear.z + resistance.z);
		force.x = (float) (ftraction.x + Math.cos(steeringCurrent)*force_lat_front.x + force_lat_rear.x + resistance.x);
		
		force.mult(Math.min(Math.abs(velocity.z), 1)); //less physics while not moving far forward
		
		Vector3f w_force = w_angle.mult(force); //reset to world coords
		applyCentralForce(w_force);
		//TODO make it so each wheel is individual
		
		//* Angular Acceleration:
			//CM to front * flf.y - CM to rear * flr.y
		double torque = car.length/2 * force_lat_front.x - car.length/2 * force_lat_rear.x;
		
			//Inertia = (1/12)*mass*(w*w + h*h) == 12*(4*4+1*1)/mass
		double angular_acceleration = (car.width*car.width + car.length*car.length)*12 * torque / car.mass;
		
		applyTorque(new Vector3f(0, (float)(angular_acceleration), 0));
		
	}
	
	//////////////////////////////////////////////////////////////
	public void myUpdate(float tpf) {
		distance += getLinearVelocity().length()*tpf;
		
		WheelInfo wi0 = getWheel(0).getWheelInfo();
		RaycastInfo rayCastInfo0 = wi0.raycastInfo;
		contact0 = (rayCastInfo0.groundObject != null);
		
		WheelInfo wi1 = getWheel(1).getWheelInfo();
		RaycastInfo rayCastInfo1 = wi1.raycastInfo;
		contact1 = (rayCastInfo1.groundObject != null);
		
		WheelInfo wi2 = getWheel(2).getWheelInfo();
		RaycastInfo rayCastInfo2 = wi2.raycastInfo;
		contact2 = (rayCastInfo2.groundObject != null);
		
		WheelInfo wi3 = getWheel(3).getWheelInfo();
		RaycastInfo rayCastInfo3 = wi3.raycastInfo;
		contact3 = (rayCastInfo3.groundObject != null);
		
		specialPhysics(tpf);
		
		//skid marks
		rally.frameCount++;
		if (rally.frameCount % 4 == 0) {
			addSkidLines();
		}
		
		if (ifAccel) { //TODO or traction
			//TODO stuff with smoke thingos
		} else {
			//TODO turn off smoke thingos
		}
		
		Matrix3f playerRot = new Matrix3f();
		getPhysicsRotationMatrix(playerRot);
		
		left = playerRot.mult(Vector3f.UNIT_X);
		right = playerRot.mult(Vector3f.UNIT_X.negate());

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
	
	private void addSkidLines() {
		if (contact0) {
			addSkidLine(wheel0Last, getWheel(0).getCollisionLocation(), w0_myskid);
			wheel0Last = getWheel(0).getCollisionLocation();
		} else {
			wheel0Last = Vector3f.ZERO;
		}
		if (contact1) {
			addSkidLine(wheel1Last, getWheel(1).getCollisionLocation(), w1_myskid);
			wheel1Last = getWheel(1).getCollisionLocation();
		} else {
			wheel1Last = Vector3f.ZERO;
		}
		if (contact2) {
			addSkidLine(wheel2Last, getWheel(2).getCollisionLocation(), w2_myskid);
			wheel2Last = getWheel(2).getCollisionLocation();
		} else {
			wheel2Last = Vector3f.ZERO;
		}
		if (contact3) {
			addSkidLine(wheel3Last, getWheel(3).getCollisionLocation(), w3_myskid);
			wheel3Last = getWheel(3).getCollisionLocation();
		} else {
			wheel3Last = Vector3f.ZERO;
		}
		
		int extra = skidList.size() - 500; //so i can remove more than one (like all 4 that frame)
		for (int i = 0; i < extra; i++) {
			skidNode.detachChild(skidList.getFirst());
			skidList.removeFirst();
		}
	}
	
	private void addSkidLine(Vector3f a, Vector3f b, float grip) {
		if (a.equals(new Vector3f(0,0,0)) || b.equals(new Vector3f(0,0,0))) {
			return; //don't make a line because they aren't valid positions
		}
		a.y += 0.01;
		b.y += 0.01; //z-buffering (i.e. to stop it "fighting" with the ground)
		
		Mesh mesh = new Mesh(); //making a quad positions
		Vector3f [] vertices = new Vector3f[4];
		vertices[0] = a.add(right.mult(car.wheelWidth));
		vertices[1] = b.add(right.mult(car.wheelWidth));
		vertices[2] = a.add(left.mult(car.wheelWidth));
		vertices[3] = b.add(left.mult(car.wheelWidth));
		
		Vector2f[] texCoord = new Vector2f[4]; //texture of quad
		texCoord[0] = new Vector2f(0, 0);
		texCoord[1] = new Vector2f(0, 1);
		texCoord[2] = new Vector2f(1, 0);
		texCoord[3] = new Vector2f(1, 1);
		
		int[] indexes = { 2,0,1, 1,3,2 };
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));

		mesh.updateBound();
		
		Geometry geo = new Geometry("MyMesh", mesh); // using our custom mesh object
		
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		if (grip > 0.5) {
			Texture tex = assetManager.loadTexture("assets/stripes.png");
			mat.setTexture("ColorMap", tex);
		} else {
			mat.setColor("Color", new ColorRGBA(0,0,0,0)); //empty
		}
		
//		mat.setColor("GlowColor", ColorRGBA.Blue); TODO move somewhere else
		
//		TODO actually scale the texture alpha by grip
		
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		geo.setMaterial(mat);
		geo.setQueueBucket(Bucket.Translucent); //render order = last
		skidNode.attachChild(geo);
		skidList.add(geo);
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
		return w0_myskid+w1_myskid+w2_myskid+w3_myskid;
	}
}
