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
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Arrow;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

public class Rally extends SimpleApplication {
	
	//boiler stuff
	private BulletAppState bulletAppState;
	
	//skid stuff
	private LinkedList<Spatial> skidList = new LinkedList<Spatial>();
	private Vector3f wheel0Last = new Vector3f(0,0,0);
	private Vector3f wheel1Last = new Vector3f(0,0,0);
	private Vector3f wheel2Last = new Vector3f(0,0,0);
	private Vector3f wheel3Last = new Vector3f(0,0,0);
	
	//camera stuff
	private CameraNode camNode;
	private Vector3f offset;
	
	//Model Enum stuff
	World world = World.duct; //Set map here
	private boolean ifNeedMaterial = true;
	private boolean worldRotate = false; //for those sideways obj files
	
	//hud stuff
	private BitmapText hudText;
	private BitmapText hudText2;
	boolean toggle;
	
	//car stuff
	private Node carNode;
	private MyVehicleControl player;
	
	//driving stuff
	int curGear = 0;
	float accelCurrent = 0;
	boolean ifAccel = false;
	boolean ifReverse = false;
	
	float steeringCurrent = 0;
	int steeringDirection = 0; //-1 = right, 1 = left
	
	float brakeCurrent = 0;
	
	//directions
	private Vector3f forward = new Vector3f();
	private Vector3f rightPlayer = new Vector3f();
	private Vector3f leftPlayer = new Vector3f();
	

	//debug stuff
	Node arrowNode;
	private int frameCount = 0;
	private boolean ifDebug = false;
	
	private double totaltime = 0;
	private double distance = 0;
	
	//shadow stuff
	private final boolean ifShadow = true;
	private final boolean ifFancyShadow = false;
    private DirectionalLightShadowRenderer dlsr;
    private DirectionalLightShadowFilter dlsf;
    
    //glowing stuff
    private final boolean ifGlow = false;
//    FilterPostProcessor fpp;
//    BloomFilter bloom;
    
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
		
		setupGUI();
	}

	private void createWorld() {
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
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
	
	        if (ifFancyShadow) {
			    fpp = new FilterPostProcessor(assetManager);
			    SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
			    fpp.addFilter(ssaoFilter);
	        }
	        
	        if (ifGlow) { //TODO this does weird things
	        	BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
	        	fpp.addFilter(bloom);
	        }
	        
	        viewPort.addProcessor(fpp);
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

	private void initCamera() { //TODO make a lag free camera (thats locked to the car)
		flyCam.setEnabled(false);
		
		camNode = new CameraNode("Cam Node", cam);
		camNode.setControlDir(ControlDirection.SpatialToCamera);
	
		rootNode.attachChild(camNode);
		camNode.setLocalTranslation(new Vector3f(0, 3, -8));
		camNode.lookAt(carNode.getLocalTranslation().add(0, 1.5f, 0), Vector3f.UNIT_Y);
		
		offset = new Vector3f(0,3,-8);
	}
	
	private void buildPlayer() {
		Car car = new TrackCar(); //Car Type
		
		Spatial carmodel = assetManager.loadModel(car.carOBJModel); //use it as car. for now
		//create a compound shape and attach CollisionShape for the car body at 0,1,0
		//this shifts the effective center of mass of the BoxCollisionShape to 0,-1,0
		CompoundCollisionShape compoundShape = new CompoundCollisionShape();
		compoundShape.addChildShape(CollisionShapeFactory.createDynamicMeshShape(carmodel), Vector3f.ZERO);
		
		
		carNode = new Node("vehicleNode");
		player = new MyVehicleControl(compoundShape, car, assetManager, carNode, this);
		
		carNode.addControl(player);
		carNode.attachChild(carmodel);
		
		rootNode.attachChild(carNode);
		getPhysicsSpace().add(player);
		player.setPhysicsLocation(world.start);
	}
	
	private void connectJoyStick() {
		myJoy = new Controller();
		
		inputManager.addRawInputListener(new JoystickEventListner(myJoy, inputManager));
	}
	
	private void setupGUI() {
		hudText = new BitmapText(guiFont, false);		  
		hudText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		hudText.setColor(ColorRGBA.White);									// font color
//		hudText.setText(player.getCurrentVehicleSpeedKmHour()+"km/h");	// the text
		hudText.setText("");												// the text
		hudText.setLocalTranslation(0, 200, 0); 	// position
		guiNode.attachChild(hudText);
		
		hudText2 = new BitmapText(guiFont, false);
		hudText2.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		hudText2.setColor(ColorRGBA.White);									// font color
		hudText2.setText("");												// the text
		hudText2.setLocalTranslation(0, 300, 0); 	// position
		guiNode.attachChild(hudText2);
	}
	
	private PhysicsSpace getPhysicsSpace(){
		return bulletAppState.getPhysicsSpace();
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/* Physics below */

	public void carPhysics(float tpf) {
		if (toggle){ return; }//no need to apply wheel forces now
		
		//NOTE: that z is forward, x is side
		//  but the notes say x is forward and y is sideways
		
		Matrix3f w_angle = player.getPhysicsRotationMatrix();
		Vector3f w_velocity = player.getLinearVelocity();
		
		//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
		double yawspeed = player.car.length * player.getAngularVelocity().y;
		
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
		
		double weight = player.car.mass*9.81*0.5; //0.5 because its per axle
		
		//calculate grid off of the slip angle
		Vector3f force_lat_front = new Vector3f();
		if (player.contact0 && player.contact1) { //contact with front
			force_lat_front.x = (float)(slipanglefront*player.car.CA_F);
			force_lat_front.x = FastMath.clamp(force_lat_front.x, -player.car.MAX_GRIP, player.car.MAX_GRIP);
			player.w0_myskid = player.w1_myskid = Math.abs(force_lat_front.x)/player.car.MAX_GRIP;
			force_lat_front.x *= weight;
		}
		Vector3f force_lat_rear = new Vector3f();
		if (player.contact2 && player.contact3) {
			force_lat_rear.x = (float)(slipanglerear*player.car.CA_R);
			force_lat_rear.x = FastMath.clamp(force_lat_rear.x, -player.car.MAX_GRIP, player.car.MAX_GRIP);
			player.w2_myskid = player.w3_myskid = Math.abs(force_lat_rear.x)/player.car.MAX_GRIP;
			force_lat_rear.x *= weight;
		}
		
		float accel;
		float x = accelCurrent;
		if (ifAccel) {
			accel = (accelCurrent+0.5f)*player.car.MAX_ACCEL; //TODO fix stall on gear 4/5
			accel = -FastMath.pow((x - 0.57f),4) - FastMath.pow((x - 0.57f),3) - (FastMath.pow((x - 0.57f),2) - 0.42f);
			accel *= player.car.MAX_ACCEL*4;
			accel = player.car.MAX_ACCEL; //TODO remove for 'geared' accel
			if (ifReverse) {
				accel *= -1;
			}
			
		} else {
			accel = 0;
		}
		//TODO figure out why '100' / ?maybe brake and accel can fight?
		Vector3f ftraction = new Vector3f();
		if (player.contact0 && player.contact1 && player.contact2 && player.contact3) {
			ftraction.z = 100*(accel - brakeCurrent*FastMath.sign(velocity.z));
		} //TODO make accel 'cost' grip
		
		//speed drag and linear resistance here
		Vector3f resistance = new Vector3f();
		resistance.z = (float)(-(player.car.RESISTANCE * velocity.z + player.car.DRAG * velocity.z * Math.abs(velocity.z)));
		resistance.x = (float)(-(player.car.RESISTANCE * velocity.x + player.car.DRAG * velocity.x * Math.abs(velocity.x))); 
		
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
		double torque = player.car.length/2 * force_lat_front.x - player.car.length/2 * force_lat_rear.x;
		
			//Inertia = (1/12)*mass*(w*w + h*h) == 12*(4*4+1*1)/mass
		double angular_acceleration = (player.car.width*player.car.width + player.car.length*player.car.length)*12 * torque / player.car.mass;
		
		player.applyTorque(new Vector3f(0, (float)(angular_acceleration), 0));
		
	}
	
	@Override
	public void simpleUpdate(float tpf) {
		//calc total position and time
		distance += player.getLinearVelocity().length()*tpf;
		totaltime += tpf;
		
		
		//TODO change to plater.update(tpf)
		WheelInfo wi0 = player.getWheel(0).getWheelInfo();
		RaycastInfo rayCastInfo0 = wi0.raycastInfo;
		player.contact0 = (rayCastInfo0.groundObject != null);
		
		WheelInfo wi1 = player.getWheel(1).getWheelInfo();
		RaycastInfo rayCastInfo1 = wi1.raycastInfo;
		player.contact1 = (rayCastInfo1.groundObject != null);
		
		WheelInfo wi2 = player.getWheel(2).getWheelInfo();
		RaycastInfo rayCastInfo2 = wi2.raycastInfo;
		player.contact2 = (rayCastInfo2.groundObject != null);
		
		WheelInfo wi3 = player.getWheel(3).getWheelInfo();
		RaycastInfo rayCastInfo3 = wi3.raycastInfo;
		player.contact3 = (rayCastInfo3.groundObject != null);
		
		carPhysics(tpf);
		
		if (ifDebug) {
			System.out.println(player.getPhysicsLocation() + "distance:"+distance + "time:" + totaltime);
		}
		
		if (ifAccel) { //TODO or traction
			//TODO stuff with smoke thingos
		} else {
			//TODO turn off smoke thingos
		}
		
//		Matrix3f w_angle = player.getPhysicsRotationMatrix();
		Vector3f w_velocity = player.getLinearVelocity();		
//		Vector3f l_velocity = w_angle.invert().mult(w_velocity).normalize();
		
		//////////////////////////////////
		float speed = player.getLinearVelocity().length();
		hudText.setText(speed + "m/s" + player.getWheelTractionInfo()); // the ui text
		
		curGear = (int)(speed/7);
		accelCurrent = speed % 7 / 7;
		float totalgrip = player.getTotalGrip();
		hudText2.setText(curGear+" | "+accelCurrent + "\n" +(ifAccel)+"\n"+totalgrip);
		

		Vector3f velocityVector = player.getLinearVelocity().normalize();
		player.getForwardVector(forward);
		
		//camera
		if (player.getLinearVelocity().length() > 2) {
			Vector3f world_v_norm = w_velocity.normalize();
			offset = new Vector3f(-world_v_norm.x*8, 3, -world_v_norm.z*8);
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
		
		//skid marks
		frameCount++;
		if (frameCount % 4 == 0) {
			addSkidLines();
		}
		
		//turning torque (from the driver)
		if (steeringDirection > 0) { //left
			steeringCurrent = Math.min(0.05f+steeringCurrent, player.car.MAX_STEERING);
		}
		if (steeringDirection < 0) { //right
			steeringCurrent = Math.max(-0.05f+steeringCurrent, -player.car.MAX_STEERING);
		}
		if (steeringDirection == 0 && steeringCurrent != 0) { //decay the turning angle slightly
			steeringCurrent -= FastMath.sign(steeringCurrent)*0.04f;
		}
		if (Math.abs(steeringCurrent) < 0.05 ) steeringCurrent = 0;
		player.steer(steeringCurrent);
		
	}
	
	private void addSkidLines() { //TODO move to player
		if (player.contact0) {
			addSkidLine(wheel0Last, player.getWheel(0).getCollisionLocation(), player.w0_myskid);
			wheel0Last = player.getWheel(0).getCollisionLocation();
		} else {
			wheel0Last = Vector3f.ZERO;
		}
		if (player.contact1) {
			addSkidLine(wheel1Last, player.getWheel(1).getCollisionLocation(), player.w1_myskid);
			wheel1Last = player.getWheel(1).getCollisionLocation();
		} else {
			wheel1Last = Vector3f.ZERO;
		}
		if (player.contact2) {
			addSkidLine(wheel2Last, player.getWheel(2).getCollisionLocation(), player.w2_myskid);
			wheel2Last = player.getWheel(2).getCollisionLocation();
		} else {
			wheel2Last = Vector3f.ZERO;
		}
		if (player.contact3) {
			addSkidLine(wheel3Last, player.getWheel(3).getCollisionLocation(), player.w3_myskid);
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
	
	private void addSkidLine(Vector3f a, Vector3f b, float grip) {
		if (a.equals(new Vector3f(0,0,0)) || b.equals(new Vector3f(0,0,0))) {
			return; //don't make a line because its not a valid position
		}
		a.y += 0.01;
		b.y += 0.01; //z-buffering (i.e. to stop it "fighting" with the ground)
		
		Mesh mesh = new Mesh(); //making a quad positions
		Vector3f [] vertices = new Vector3f[4];
		vertices[0] = a.add(rightPlayer.mult(player.car.wheelWidth));
		vertices[1] = b.add(rightPlayer.mult(player.car.wheelWidth));
		vertices[2] = a.add(leftPlayer.mult(player.car.wheelWidth));
		vertices[3] = b.add(leftPlayer.mult(player.car.wheelWidth));
		
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
		
		if (ifGlow) {
			mat.setColor("GlowColor", ColorRGBA.Blue);
		}
		//TODO fix shadows on texture
		//TODO actually scale the texture alpha by grip
		
		//don't try to set the color again (it doesn't work)
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		geo.setMaterial(mat);
		geo.setQueueBucket(Bucket.Translucent); //render order = last
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
	
}


