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
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;

//Long TODO: 
//Map maybe?
//  http://wiki.jmonkeyengine.org/doku.php/jme3:advanced:multiple_camera_views


public class Rally extends SimpleApplication {
	
	//boiler stuff
	private BulletAppState bulletAppState;
	
	//camera stuff
	private Camera camNode;
	
	//World Model Enum stuff
	World world = World.track2; //Set map here
	
	boolean dynamicWorld = true;
	WP[] type = WPFloating.values();
	boolean needsMaterial = false;
	WorldBuilder worldB;
	
	
	//hud stuff
	private BitmapText speedText;
	private Geometry speedo;
	private Geometry speedo2;
	private Geometry b0;
	private Geometry b1;
	private Geometry b2;
	private Geometry b3;
	
	//car stuff
	private Node carNode;
	MyVehicleControl player;
	private Car car = new NormalCar(); //set car here
	
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
    //Joystick stuff - TODO
    private Controller myJoy;
	
    
	public static void main(String[] args) {
		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(1300,720);
		settings.setFrameRate(60); //60
		settings.setUseJoysticks(true);
//		settings.setBitsPerPixel(16); //TODO why not?
		
		app.setSettings(settings);

		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();
	}
	
	@Override
	public void simpleInitApp() {
		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
//		bulletAppState.setDebugEnabled(true);
		
		createWorld();
				
		buildPlayer();
		initCamera();

		connectJoyStick();
		
		setupGUI();
		
		Quaternion q = new Quaternion();
		q.fromAngleAxis(FastMath.PI*1/16, Vector3f.UNIT_X);
		System.out.println(q);
	}

	private void createWorld() {
		if (dynamicWorld) {
			worldB = new WorldBuilder(this, assetManager, type, needsMaterial);
			rootNode.attachChild(worldB);
		} else {
		
			Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
			mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
			
		    //imported model		
			Spatial worldNode = assetManager.loadModel(world.name);
			if (worldNode instanceof Node) {
				for (Spatial s: ((Node) worldNode).getChildren()) {
					if (world.ifNeedsTexture) {
						s.setMaterial(mat);
					}
					addWorldModel(s);
				}
			} else {
				Geometry worldModel = (Geometry) assetManager.loadModel(world.name);
				
				if (world.ifNeedsTexture) {
					worldModel.setMaterial(mat);
				}
				addWorldModel(worldModel);
			}
		}
		//lights
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(0.4f));
		rootNode.addLight(al);

		DirectionalLight sun = new DirectionalLight();
		sun.setColor(ColorRGBA.White);
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		rootNode.addLight(sun);
		
//		bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.81f, 0)); //defaults to normal
		viewPort.setBackgroundColor(ColorRGBA.Blue);
		
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
		s.scale(world.scale);
		
		CollisionShape world = CollisionShapeFactory.createMeshShape(s);
	
		RigidBodyControl landscape = new RigidBodyControl(world, 0);
		s.addControl(landscape);
		bulletAppState.getPhysicsSpace().add(landscape);
		
		rootNode.attachChild(s);
	}

	private void initCamera() {
		flyCam.setEnabled(false);
		
		camNode = new Camera("Cam Node", cam, player);
		rootNode.attachChild(camNode);
	}
	
	private void buildPlayer() {
		Spatial carmodel = assetManager.loadModel(car.carModel); //use it as car. for now
		carmodel.setShadowMode(ShadowMode.CastAndReceive);
		
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
		if (dynamicWorld) {
			player.setPhysicsLocation(worldB.start);
			Matrix3f p = player.getPhysicsRotationMatrix();
			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, Vector3f.UNIT_Y);
			player.setPhysicsRotation(p);
		} else {
			player.setPhysicsLocation(world.start);
		}
	}
	
	private void connectJoyStick() {
		myJoy = new Controller();
		
		inputManager.addRawInputListener(new JoystickEventListner(myJoy, inputManager));
	}
	
	private void setupGUI() {
		speedText = new BitmapText(guiFont, false);		  
		speedText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		speedText.setColor(ColorRGBA.White);								// font color
		speedText.setText("");												// the text
		speedText.setLocalTranslation(settings.getWidth()-200, 300, 0); // position
		guiNode.attachChild(speedText);
		
		BitmapText the50 = new BitmapText(guiFont, false);
		the50.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		the50.setColor(ColorRGBA.DarkGray);								// font color
		the50.setText("50");												// the text
		the50.setLocalTranslation(settings.getWidth()-70, 120, 0); // position
		guiNode.attachChild(the50);
		
		BitmapText the1 = new BitmapText(guiFont, false);
		the1.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		the1.setColor(ColorRGBA.Gray);								// font color
		the1.setText("1");												// the text
		the1.setLocalTranslation(settings.getWidth()-170, 120, 0); // position
		guiNode.attachChild(the1);
		
		//TODO make better
		Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.DarkGray);
		
		Arrow s = new Arrow(Vector3f.UNIT_X);
		speedo = new Geometry("Speed", s);
		speedo.setLocalTranslation(settings.getWidth()-75, 0, 0);
		speedo.scale(100);
		speedo.setMaterial(m);
		guiNode.attachChild(speedo);
		
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Gray);
		
		speedo2 = new Geometry("RPM", s);
		speedo2.setLocalTranslation(settings.getWidth()-175, 0, 0);
		speedo2.scale(100);
		speedo2.setMaterial(m);
		guiNode.attachChild(speedo2);
		
		
		int width = settings.getWidth() - 350;
		Box b = new Box(10, 10, 1);
		b0 = new Geometry("frontleft", b);
		b0.setLocalTranslation(width, 50, 0);
		b0.setMaterial(m);
		guiNode.attachChild(b0);
		
		b1 = new Geometry("frontright", b);
		b1.setLocalTranslation(width +25, 50, 0);
		b1.setMaterial(m);
		guiNode.attachChild(b1);
		
		b2 = new Geometry("rearleft", b);
		b2.setLocalTranslation(width, 25, 0);
		b2.setMaterial(m);
		guiNode.attachChild(b2);
		
		b3 = new Geometry("rearright", b);
		b3.setLocalTranslation(width + 25, 25, 0);
		b3.setMaterial(m);
		guiNode.attachChild(b3);
		
	}
	
	public PhysicsSpace getPhysicsSpace(){
		return bulletAppState.getPhysicsSpace();
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/* Physics below */
	
	@Override
	public void simpleUpdate(float tpf) {
		totaltime += tpf; //calc total time
		
		player.myUpdate(tpf); //BIG update here
		if (dynamicWorld) {
			worldB.update(player.getPhysicsLocation());
		}
			
		if (ifDebug) {
			System.out.println(player.getPhysicsLocation() + "distance:"+player.distance + "time:" + totaltime);
		}
		//////////////////////////////////
		//Hud stuff
		
		Vector3f velocityVector = player.getLinearVelocity().normalize();
		player.getForwardVector(player.forward);
		
		float speed = player.getLinearVelocity().length();
		player.curGear = (int)(speed/7);
		player.accelCurrent = speed % 7 / 7;
		float totalgrip = player.getTotalGrip();
		
		speedText.setText(speed + "m/s\ngear:" + player.curGear + "\naccel:" + player.accelCurrent+ "\ngrip:" + totalgrip); // the ui text
		
		speedo.setLocalRotation(Quaternion.IDENTITY);
		speedo.rotate(0, 0, FastMath.PI);
		speedo.rotate(0, 0, -speed*FastMath.DEG_TO_RAD*90/50);
		
		speedo2.setLocalRotation(Quaternion.IDENTITY);
		speedo2.rotate(0, 0, FastMath.PI);
		speedo2.rotate(0, 0, -player.accelCurrent*FastMath.HALF_PI);
		
		Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wn0.skid,player.wn0.skid,player.wn0.skid,1));
		b0.setMaterial(m);
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wn1.skid,player.wn1.skid,player.wn1.skid,1));
		b1.setMaterial(m);
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wn2.skid,player.wn2.skid,player.wn2.skid,1));
		b2.setMaterial(m);
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wn3.skid,player.wn3.skid,player.wn3.skid,1));
		b3.setMaterial(m);
		
		/////////////////////////////////
		//camera
		camNode.myUpdate(tpf);
		
		//////////////////////////////////
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
	
	void putShapeArrow(ColorRGBA color, Vector3f dir, Vector3f pos) {
		Arrow arrow = new Arrow(dir);
		arrow.setLineWidth(1); // make arrow thicker
		Geometry arrowG = createShape(arrow, color);
		arrowG.setLocalTranslation(pos);
		arrowG.setShadowMode(ShadowMode.Off);
		arrowNode.attachChild(arrowG);
	}
	
	Geometry createShape(Mesh shape, ColorRGBA color){
		Geometry g = new Geometry("coordinate axis", shape);
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", color);
		g.setMaterial(mat);
		g.setShadowMode(ShadowMode.Off);
		return g;
	}
	
}
