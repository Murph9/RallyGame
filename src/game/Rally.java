package game;


import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Arrow;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;

//Long TODOs:
//http://wiki.jmonkeyengine.org/doku.php/jme3:advanced:multiple_camera_views


public class Rally extends SimpleApplication {
	
	//boiler stuff
	private BulletAppState bulletAppState;
	
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
	
	//car stuff
	private Node carNode;
	private MyVehicleControl player;
	
	//debug stuff
	Node arrowNode;
	int frameCount = 0;
	boolean ifDebug = false;
	
	private double totaltime = 0;
	
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
		Car car = new RallyCar(); //Car Type
		
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
		rootNode.attachChild(player.skidNode);
		
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
	
	@Override
	public void simpleUpdate(float tpf) {
		//calc total position and time
		totaltime += tpf;
		
		player.myUpdate(tpf); //BIG update here
		
		if (ifDebug) {
			System.out.println(player.getPhysicsLocation() + "distance:"+player.distance + "time:" + totaltime);
		}
		
//		Matrix3f w_angle = player.getPhysicsRotationMatrix();
		Vector3f w_velocity = player.getLinearVelocity();
//		Vector3f l_velocity = w_angle.invert().mult(w_velocity).normalize();
		
		//////////////////////////////////
		float speed = player.getLinearVelocity().length();
		hudText.setText(speed + "m/s" + player.getWheelTractionInfo()); // the ui text
		
		player.curGear = (int)(speed/7);
		player.accelCurrent = speed % 7 / 7;
		float totalgrip = player.getTotalGrip();
		hudText2.setText(player.curGear+" | "+player.accelCurrent + "\n" +(player.ifAccel)+"\n"+totalgrip);
		

		Vector3f velocityVector = player.getLinearVelocity().normalize();
		player.getForwardVector(player.forward);
		
		//camera
		if (player.getLinearVelocity().length() > 2) {
			Vector3f world_v_norm = w_velocity.normalize();
			offset = new Vector3f(-world_v_norm.x*8, 3, -world_v_norm.z*8);
		}
		camNode.setLocalTranslation(offset.add(player.getPhysicsLocation()));
		camNode.lookAt(carNode.getLocalTranslation().add(0, 1.5f, 0), Vector3f.UNIT_Y);
		
		if (frameCount % 10 == 0 && ifDebug) {
			float driftAngle = velocityVector.angleBetween(player.forward);
			float driftRightAngle = FastMath.RAD_TO_DEG*(velocityVector.angleBetween(player.right)); 
//			float driftLeftAngle = FastMath.RAD_TO_DEG*(velocityVector.angleBetween(leftPlayer));
			
			Quaternion driftA = null;
			if (driftRightAngle < 90) { //are we turning right or left?
				driftA = new Quaternion().fromAngleAxis(driftAngle*2, new Vector3f(0,1,0));
			} else { //driftAngle * 2
				driftA = new Quaternion().fromAngleAxis(-driftAngle*2, new Vector3f(0,1,0));
			}
			
			putShapeArrow(ColorRGBA.Blue, player.forward.negate(), Vector3f.ZERO);
			putShapeArrow(ColorRGBA.Red, player.forward, Vector3f.ZERO);
			
			//////////////////////////////////Velocity
			putShapeArrow(ColorRGBA.Green, velocityVector, player.getPhysicsLocation());
			
			putShapeArrow(ColorRGBA.White, driftA.mult(velocityVector), player.getPhysicsLocation());
			
			////////////////////////////////// Left and right
			putShapeArrow(ColorRGBA.Cyan, player.left, player.getPhysicsLocation());
			putShapeArrow(ColorRGBA.Cyan, player.right, player.getPhysicsLocation());
		}
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


