package game;

import java.util.LinkedList;

import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.dynamics.vehicle.WheelInfo.RaycastInfo;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

public class Rally extends SimpleApplication implements ActionListener {
	
	//boiler stuff
	private BulletAppState bulletAppState;
	private VehicleControl player;
	
	//skid stuff
	ParticleEmitter emit;
	private boolean ifSmoke = false;
	private Material grey;
	private LinkedList<Geometry> skidList = new LinkedList<Geometry>();
	private Vector3f wheel0Last = new Vector3f(0,0,0);
	private Vector3f wheel1Last = new Vector3f(0,0,0);
	private Vector3f wheel2Last = new Vector3f(0,0,0);
	private Vector3f wheel3Last = new Vector3f(0,0,0);
	
	//camera stuff
	private CameraNode camNode;
	private Vector3f offset;
	
	//Model Enum stuff
	private World world = World.raleigh; //Set map here
	private boolean ifNeedMaterial = true;
	private boolean worldRotate = false; //for those sideways obj files
	
	//hud stuff
	private BitmapText hudText;
	private BitmapText gear;
	private boolean toggle;
	
	//driving stuff
	private Node carNode;
	
	private int curGear = 0;
	
	private float accelCurrent = 0;
	private boolean ifAccel = false;
	private boolean ifReverse = false;
	
	private float steeringCurrent = 0;
	private int steeringDirection = 0; //-1 = right, 1 = left
	
	private float brakeCurrent = 0;
	
	//directions
	private Vector3f forward = new Vector3f();
	private Vector3f rightPlayer;
	private Vector3f leftPlayer;
	
	//contact points
	private boolean contact0;
	private boolean contact1;
	private boolean contact2;
	private boolean contact3;
	
	//debug stuff
	private Node arrowNode;
	private int frameCount = 0;
	private boolean ifDebug = false;
	
	private double totaltime = 0;
	private double distance = 0;
	
	//shadow stuff
	private final boolean ifShadow = true;
	private final boolean ifFancyShadow = false;
    private DirectionalLightShadowRenderer dlsr;
    private DirectionalLightShadowFilter dlsf;
    
    /////////////////////////////////////
    //Joystick stuff
    private Controller myJoy;
	
    
	public static void main(String[] args) {
		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(1300,720);
		settings.setFrameRate(60);
		settings.setUseJoysticks(true);
		app.setSettings(settings);

		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();
	}
	
	@Override
	public void simpleInitApp() {
		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
		
		createWorld();
				
		buildPlayer();
		initCamera();

		connectJoyStick();
		setupKeys();
		
		setupGUI();
	}

	private void createWorld() {
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		//floor
		/*
		Box floorBox = new Box(100, 0.25f, 100);
        Geometry floorGeometry = new Geometry("Floor", floorBox);
        floorGeometry.setMaterial(mat);
        floorGeometry.setLocalTranslation(0, 73, 0);
        floorGeometry.addControl(new RigidBodyControl(0));
        rootNode.attachChild(floorGeometry);
        getPhysicsSpace().add(floorGeometry);*/
        
        //imported model		
		Spatial worldNode = assetManager.loadModel(world.name);
		if (worldNode instanceof Node) {
			for (Spatial s: ((Node) worldNode).getChildren()) {
				if (ifNeedMaterial) {
					s.setMaterial(mat);
				}
				addWorldModel(s);
			}
		} else {
			Geometry worldModel = (Geometry) assetManager.loadModel(world.name);
			
			if (ifNeedMaterial) {
				worldModel.setMaterial(mat);
			}
			addWorldModel(worldModel);
		}
		
		//lights
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(0.4f));
		rootNode.addLight(al);

		DirectionalLight sun = new DirectionalLight();
		sun.setColor(ColorRGBA.White);
		sun.setDirection(new Vector3f(-.3f,-.6f,-.5f).normalizeLocal());
		rootNode.addLight(sun);
		
		rootNode.setShadowMode(ShadowMode.CastAndReceive);
		bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.81f, 0));
		
		arrowNode = new Node();
		rootNode.attachChild(arrowNode);
		
		if (ifShadow) {
	        //Shadows and lights
	        dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
	        dlsr.setLight(sun);
	        dlsr.setLambda(0.55f);
	        dlsr.setShadowIntensity(0.6f);
	        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
//	        dlsr.displayFrustum();
	        viewPort.addProcessor(dlsr);
	
	        dlsf = new DirectionalLightShadowFilter(assetManager, 2048, 3);
	        dlsf.setLight(sun);
	        dlsf.setLambda(0.55f);
	        dlsf.setShadowIntensity(0.6f);
	        dlsf.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
	        dlsf.setEnabled(false);	
	
	        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
	        fpp.addFilter(dlsf);
	
	        viewPort.addProcessor(fpp);
	        
	        if (ifFancyShadow) {
			    fpp = new FilterPostProcessor(assetManager);
			    SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
			    fpp.addFilter(ssaoFilter);
			    viewPort.addProcessor(fpp);
	        }
        }
        
		
	}
	
	private void addWorldModel(Spatial s) {
		System.out.println("Adding: "+ s.getName());
		
		s.move(0,-5,0);
		if (worldRotate) {
			s.rotate(-90*FastMath.DEG_TO_RAD, 0, 0);
		}
		s.scale(world.scale);
		
		CollisionShape world = CollisionShapeFactory.createMeshShape(s);
	
		RigidBodyControl landscape = new RigidBodyControl(world, 0);
		s.addControl(landscape);
		bulletAppState.getPhysicsSpace().add(landscape);
		
		rootNode.attachChild(s);
	}

	private void initCamera() {
		flyCam.setEnabled(false);
		
		camNode = new CameraNode("Cam Node", cam);
		camNode.setControlDir(ControlDirection.SpatialToCamera);
	
//		carNode.attachChild(camNode);
		rootNode.attachChild(camNode);
		camNode.setLocalTranslation(new Vector3f(0, 3, -8));
		camNode.lookAt(carNode.getLocalTranslation().add(0, 1.5f, 0), Vector3f.UNIT_Y);
		
		offset = new Vector3f(0,3,-8);
	}
	
	private void buildPlayer() {
		Spatial carmodel = assetManager.loadModel(Car.carOBJModel);
		
		//create a compound shape and attach CollisionShape for the car body at 0,1,0
		//this shifts the effective center of mass of the BoxCollisionShape to 0,-1,0
		CompoundCollisionShape compoundShape = new CompoundCollisionShape();
		compoundShape.addChildShape(CollisionShapeFactory.createDynamicMeshShape(carmodel), Vector3f.ZERO);
		
		carNode = new Node("vehicleNode");
		player = new VehicleControl(compoundShape, Car.mass);
		carNode.addControl(player);
		carNode.attachChild(carmodel);

		player.setSuspensionCompression(Car.susCompression);
		player.setSuspensionDamping(Car.susDamping);
		player.setSuspensionStiffness(Car.stiffness);
		player.setMaxSuspensionForce(25*Car.mass);
		
		Node node1 = new Node("wheel 1 node");
		Spatial wheels1 = assetManager.loadModel(Car.wheelOBJModel);
		wheels1.center();
		node1.attachChild(wheels1);
		player.addWheel(node1, new Vector3f(-Car.w_xOff, Car.w_yOff, Car.w_zOff),
				Car.wheelDirection, Car.wheelAxle, Car.restLength, Car.radius, true);

		Node node2 = new Node("wheel 2 node");
		Spatial wheels2 = assetManager.loadModel(Car.wheelOBJModel);
		wheels2.rotate(0, FastMath.PI, 0);
		wheels2.center();
		node2.attachChild(wheels2);
		player.addWheel(node2, new Vector3f(Car.w_xOff, Car.w_yOff, Car.w_zOff),
				Car.wheelDirection, Car.wheelAxle, Car.restLength, Car.radius, true);

		Node node3 = new Node("wheel 3 node");
		Spatial wheels3 = assetManager.loadModel(Car.wheelOBJModel);
		wheels3.center();
		node3.attachChild(wheels3);
		player.addWheel(node3, new Vector3f(-Car.w_xOff-0.05f, Car.w_yOff, -Car.w_zOff),
				Car.wheelDirection, Car.wheelAxle, Car.restLength, Car.radius, false);

		Node node4 = new Node("wheel 4 node");
		Spatial wheels4 = assetManager.loadModel(Car.wheelOBJModel);
		wheels4.rotate(0, FastMath.PI, 0);
		wheels4.center();
		node4.attachChild(wheels4);
		player.addWheel(node4, new Vector3f(Car.w_xOff+0.05f, Car.w_yOff, -Car.w_zOff),
				Car.wheelDirection, Car.wheelAxle, Car.restLength, Car.radius, false);

		//Friction
		player.setFrictionSlip(0, Car.wheel1Slip);
		player.setFrictionSlip(1, Car.wheel2Slip);
		player.setFrictionSlip(2, Car.wheel3Slip);
		player.setFrictionSlip(3, Car.wheel4Slip);
		
		//attaching all the things (wheels)
		carNode.attachChild(node1);
		carNode.attachChild(node2);
		carNode.attachChild(node3);
		carNode.attachChild(node4);
		
		rootNode.attachChild(carNode);
		
		getPhysicsSpace().add(player);
		player.setPhysicsLocation(world.start);
		
		grey = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		grey.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.7f));
		grey.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		makeSmoke(node1);
		makeSmoke(node2);
		makeSmoke(node3);
		makeSmoke(node4);
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
	
	private void connectJoyStick() {
		myJoy = new Controller();
		
		inputManager.addRawInputListener(new JoystickEventListner(myJoy, inputManager));
	}
	
	private void setupKeys() {
		inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
		inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
		inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
		inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
		inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
		inputManager.addMapping("Impluse", new KeyTrigger(KeyInput.KEY_LCONTROL));
		inputManager.addMapping("Reverse", new KeyTrigger(KeyInput.KEY_LSHIFT));
		
		inputManager.addListener(this, "Lefts");
		inputManager.addListener(this, "Rights");
		inputManager.addListener(this, "Ups");
		inputManager.addListener(this, "Downs");
		inputManager.addListener(this, "Space");
		inputManager.addListener(this, "Reset");
		inputManager.addListener(this, "Impluse");
		inputManager.addListener(this, "Reverse");
		
		//TODO use the Controller class
	}
	
	private void setupGUI() {
		hudText = new BitmapText(guiFont, false);		  
		hudText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		hudText.setColor(ColorRGBA.White);									// font color
//		hudText.setText(player.getCurrentVehicleSpeedKmHour()+"km/h");	// the text
		hudText.setText("");												// the text
		hudText.setLocalTranslation(0, 200, 0); 	// position
		guiNode.attachChild(hudText);
		
		gear = new BitmapText(guiFont, false);
		gear.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		gear.setColor(ColorRGBA.White);									// font color
		gear.setText("");												// the text
		gear.setLocalTranslation(0, 300, 0); 	// position
		guiNode.attachChild(gear);
	}
	
	private PhysicsSpace getPhysicsSpace(){
		return bulletAppState.getPhysicsSpace();
	}
	
	public void onAction(String binding, boolean value, float tpf) {
		if (binding.equals("Lefts")) {
			if (value) {
				steeringDirection = 1;
			} else {
				steeringDirection = 0;
			}
			player.steer(steeringCurrent);
		} else if (binding.equals("Rights")) {
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

		} else if (binding.equals("Downs")) {
			if (value) {
				brakeCurrent += Car.MAX_BRAKE;
			} else {
				brakeCurrent -= Car.MAX_BRAKE;
			}
			
		} else if (binding.equals("Space")) {
			if (value) {
				player.applyImpulse(Car.JUMP_FORCE, Vector3f.ZERO);
				Vector3f old = player.getPhysicsLocation();
				old.y += 2;
				player.setPhysicsLocation(old);
			}
			
		} else if (binding.equals("Impluse")) {
			if (value) {
				toggle = !toggle;
				System.out.println("impluse");
			}
			
		} else if (binding.equals("Reset")) {
			if (value) {
				player.setPhysicsLocation(world.start);
				player.setPhysicsRotation(new Matrix3f());
				player.setLinearVelocity(Vector3f.ZERO);
				player.setAngularVelocity(Vector3f.ZERO);
				player.resetSuspension();
				
				arrowNode.detachAllChildren();
				
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
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/* Physics below */

	public void carPhysics(float tpf) {
		if (toggle){
			//no need to apply wheel forces now
			//TODO still apply drag though
			return;
		}
		
		//note that z is forward, x is side
		//  and the notes say x is forward and y is sideways
		Matrix3f w_angle = player.getPhysicsRotationMatrix();
		Vector3f w_velocity = player.getLinearVelocity();
		
		//* Linear Accelerations: = car.length * car.yawrate (in rad/sec)
		double yawspeed = Car.length * player.getAngularVelocity().y;
		
		Vector3f velocity = w_angle.invert().mult(w_velocity);
		
		double rot_angle = 0, sideslip = 0;
		if (velocity.z > 0.001) { //no divide by zero errors
			rot_angle = Math.atan(yawspeed / velocity.z);
			sideslip = Math.atan(velocity.x / velocity.z);
		}
		float steeringCur = steeringCurrent;
		if (velocity.z < 0) { //need to flip the steering on reverse
			steeringCur *= -1;
		}
		double slipanglefront = sideslip + rot_angle - steeringCur;
		double slipanglerear = sideslip - rot_angle;
		
		double weight = Car.mass*9.81*0.5; //0.5 because its per axle
		
		//calculate grid off of the slip angle - TODO change skid colour on hitting max grip
		Vector3f force_lat_front = new Vector3f();
		if (contact0 && contact1) { //contact with front
			force_lat_front.x = (float)(slipanglefront*Car.CA_F);
			force_lat_front.x = FastMath.clamp(force_lat_front.x, -Car.MAX_GRIP, Car.MAX_GRIP); 
			force_lat_front.x *= weight;
		}
		Vector3f force_lat_rear = new Vector3f();
		if (contact2 && contact3) {
			force_lat_rear.x = (float)(slipanglerear*Car.CA_R);
			force_lat_rear.x = FastMath.clamp(force_lat_rear.x, -Car.MAX_GRIP, Car.MAX_GRIP);
			force_lat_rear.x *= weight;
		}
		
		float accel;
		float x = accelCurrent;
		if (ifAccel) {
			accel = (accelCurrent+0.5f)*Car.MAX_ACCEL; //TODO fix stall on gear 4/5
			accel = -FastMath.pow((x - 0.57f),4) - FastMath.pow((x - 0.57f),3) - (FastMath.pow((x - 0.57f),2) - 0.42f);
			accel *= Car.MAX_ACCEL*4;
//			accel = Car.MAX_ACCEL; //TODO remove for 'geared' accel
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
		}
		
		//speed drag and linear resistance here
		Vector3f resistance = new Vector3f();
		resistance.z = (float)(-(Car.RESISTANCE * velocity.z + Car.DRAG * velocity.z * Math.abs(velocity.z)));
		resistance.x = (float)(-(Car.RESISTANCE * velocity.x + Car.DRAG * velocity.x * Math.abs(velocity.x))); 
		
		//put into a force
		Vector3f force = new Vector3f();
		force.z = (float) (ftraction.z + Math.sin(steeringCurrent)*force_lat_front.z + force_lat_rear.z + resistance.z);
		force.x = (float) (Math.cos(steeringCurrent)*force_lat_front.x + force_lat_rear.x + resistance.x); //ftraction.x = 0
		
		force.mult(Math.min(Math.abs(velocity.z), 1)); //less physics while not moving far forward
		
		Vector3f w_force = w_angle.mult(force); //reset to world coords
		player.applyCentralForce(w_force);
		//TODO make it so each wheel is individual
		
		//* Angular Acceleration:
			//CM to front * flf.y - CM to rear * flr.y
		double torque = Car.length/2 * force_lat_front.x - Car.length/2 * force_lat_rear.x;
		
			//Inertia = (1/12)*mass*(w*w + h*h) == 12*(4*4+1*1)/mass
		double angular_acceleration = (Car.width*Car.width + Car.length*Car.length)*12 * torque / Car.mass;
		
		player.applyTorque(new Vector3f(0, (float)(angular_acceleration), 0));
		
	}
	
	@Override
	public void simpleUpdate(float tpf) {
		//calc total position and time
		distance += player.getLinearVelocity().length()*tpf;
		totaltime += tpf;
//		System.out.println(player.getLinearVelocity().length() + " " + totaltime);
		
		WheelInfo wi0 = player.getWheel(0).getWheelInfo();
		RaycastInfo rayCastInfo0 = wi0.raycastInfo;
		contact0 = (rayCastInfo0.groundObject != null);
		
		WheelInfo wi1 = player.getWheel(1).getWheelInfo();
		RaycastInfo rayCastInfo1 = wi1.raycastInfo;
		contact1 = (rayCastInfo1.groundObject != null);
		
		WheelInfo wi2 = player.getWheel(2).getWheelInfo();
		RaycastInfo rayCastInfo2 = wi2.raycastInfo;
		contact2 = (rayCastInfo2.groundObject != null);
		
		WheelInfo wi3 = player.getWheel(3).getWheelInfo();
		RaycastInfo rayCastInfo3 = wi3.raycastInfo;
		contact3 = (rayCastInfo3.groundObject != null);
		
		carPhysics(tpf);
		
		if (ifDebug) {
			System.out.println(player.getPhysicsLocation() + "distance:"+distance + "time:" + totaltime);
		}
		
		if (ifAccel) { //TODO or traction
			//TODO stuff with smoke thingos
		} else {
			//TODO turn off smoke thingos
		}
		
		Matrix3f w_angle = player.getPhysicsRotationMatrix();
		Vector3f w_velocity = player.getLinearVelocity();		
		Vector3f l_velocity = w_angle.invert().mult(w_velocity).normalize();
		
		//////////////////////////////////
		float speed = player.getLinearVelocity().length();
		hudText.setText(speed + "m/s" + getWheelTractionInfo()); // the ui text
		curGear = (int)(speed/7);
		accelCurrent = speed % 7 / 7;
		gear.setText(curGear+" | "+accelCurrent + "\n" +(ifAccel));
		

		Vector3f velocityVector = player.getLinearVelocity().normalize();
		player.getForwardVector(forward);
		
		//camera
		if (player.getLinearVelocity().length() > 2) {
			Vector3f w_v_norm = w_velocity.normalize();
			offset = new Vector3f(-w_v_norm.x*8, 3, -w_v_norm.z*8);
//			offset = new Vector3f(0, 3, -8);
		}
		camNode.setLocalTranslation(offset.add(player.getPhysicsLocation()));
		camNode.lookAt(carNode.getLocalTranslation().add(0, 1.5f, 0), Vector3f.UNIT_Y);
		
		
		Matrix3f playerRot = new Matrix3f();
		player.getInterpolatedPhysicsRotation(playerRot);
		
		leftPlayer = playerRot.mult(Vector3f.UNIT_X);
		rightPlayer = playerRot.mult(Vector3f.UNIT_X.negate());
		
		float driftAngle = velocityVector.angleBetween(forward);
		float driftRightAngle = FastMath.RAD_TO_DEG*(velocityVector.angleBetween(rightPlayer)); 
//		float driftLeftAngle = FastMath.RAD_TO_DEG*(velocityVector.angleBetween(leftPlayer));
		
		Quaternion driftA = null;
		if (driftRightAngle < 90) { //are we turning right or left?
			driftA = new Quaternion().fromAngleAxis(driftAngle*2, new Vector3f(0,1,0));
		} else { //driftAngle * 2
			driftA = new Quaternion().fromAngleAxis(-driftAngle*2, new Vector3f(0,1,0));
		}
		
		if (frameCount % 10 == 0 && ifDebug) {
			putShapeArrow(ColorRGBA.Blue, forward.negate(), Vector3f.ZERO);
			putShapeArrow(ColorRGBA.Red, forward, Vector3f.ZERO);
			
			//////////////////////////////////Velocity
			putShapeArrow(ColorRGBA.Green, velocityVector, player.getPhysicsLocation());
			
			putShapeArrow(ColorRGBA.White, driftA.mult(velocityVector), player.getPhysicsLocation());
			
			////////////////////////////////// Left and right
			putShapeArrow(ColorRGBA.Cyan, leftPlayer, player.getPhysicsLocation());
			putShapeArrow(ColorRGBA.Cyan, rightPlayer, player.getPhysicsLocation());
		}
		frameCount++;
		
		if (frameCount % 4 == 0) {
			//skid marks
			if (contact0) {
				addSkidLine(wheel0Last, player.getWheel(0).getCollisionLocation());
				wheel0Last = player.getWheel(0).getCollisionLocation();
			} else {
				wheel0Last = Vector3f.ZERO;
			}
			if (contact1) {
				addSkidLine(wheel1Last, player.getWheel(1).getCollisionLocation());
				wheel1Last = player.getWheel(1).getCollisionLocation();
			} else {
				wheel1Last = Vector3f.ZERO;
			}
			if (contact2) {
				addSkidLine(wheel2Last, player.getWheel(2).getCollisionLocation());
				wheel2Last = player.getWheel(2).getCollisionLocation();
			} else {
				wheel2Last = Vector3f.ZERO;
			}
			if (contact3) {
				addSkidLine(wheel3Last, player.getWheel(3).getCollisionLocation());
				wheel3Last = player.getWheel(3).getCollisionLocation();
			} else {
				wheel3Last = Vector3f.ZERO;
			}
			
			int extra = skidList.size() - 500; //so i can remove more than one (like all 4 that frame)
			for (int i = 0; i < extra; i++) {
				arrowNode.detachChild(skidList.getFirst());
				skidList.removeFirst();
			}
		}
		
		//turning torque (from the driver)
		if (steeringDirection > 0) { //left
			steeringCurrent = Math.min(0.05f+steeringCurrent, Car.MAX_STEERING);
		}
		if (steeringDirection < 0) { //right
			steeringCurrent = Math.max(-0.05f+steeringCurrent, -Car.MAX_STEERING);
		}
		if (steeringDirection == 0 && steeringCurrent != 0) { //decay the turning angle slightly
			steeringCurrent -= FastMath.sign(steeringCurrent)*0.04f;
		}
		if (Math.abs(steeringCurrent) < 0.05 ) steeringCurrent = 0;
		player.steer(steeringCurrent);
		
	}
	
	private void addSkidLine(Vector3f a, Vector3f b) {
		if (a.equals(new Vector3f(0,0,0)) || b.equals(new Vector3f(0,0,0))) {
			return; //don't make a line because its not valid
		}
		a.y += 0.01;
		b.y += 0.01; //z-buffering
		
		Mesh mesh = new Mesh(); //making a quad
		Vector3f [] vertices = new Vector3f[4];
		vertices[0] = a.add(rightPlayer.mult(0.15f));
		vertices[1] = b.add(rightPlayer.mult(0.15f));
		vertices[2] = a.add(leftPlayer.mult(0.15f));
		vertices[3] = b.add(leftPlayer.mult(0.15f));
		int[] indexes = { 2,0,1, 1,3,2 };
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));
		mesh.updateBound();
		
		Geometry geo = new Geometry("OurMesh", mesh); // using our custom mesh object
		geo.setMaterial(grey);
		arrowNode.attachChild(geo);
		skidList.add(geo);
	}
	
	private void putShapeArrow(ColorRGBA color, Vector3f dir, Vector3f pos) {
		Arrow arrow = new Arrow(dir);
		arrow.setLineWidth(1); // make arrow thicker
		Geometry arrowG = createShape(arrow, color);
		arrowG.setLocalTranslation(player.getPhysicsLocation());
		arrowG.setShadowMode(ShadowMode.Off);
		arrowNode.attachChild(arrowG);
	}
	
	private Geometry createShape(Mesh shape, ColorRGBA color){
		  Geometry g = new Geometry("coordinate axis", shape);
		  Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		  mat.getAdditionalRenderState().setWireframe(true);
		  mat.setColor("Color", color);
		  g.setMaterial(mat);
		  g.setShadowMode(ShadowMode.Off);
		  return g;
	}
	
	private String getWheelTractionInfo() {
		String out = "";
		out = "\n" + player.getWheel(0).getSkidInfo() + "\n" +
		player.getWheel(1).getSkidInfo() + "\n" + 
		player.getWheel(2).getSkidInfo() + "\n" +
		player.getWheel(3).getSkidInfo();
		return out;
	}

}


