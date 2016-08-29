package game;


import world.World;
import world.WorldType;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.Joystick;
import com.jme3.math.*;
import com.jme3.scene.*;

import car.*;

//Long TODO's: 
//long running skidmark issue is in effect (but only for my computer)

//Bugs TODO
//minimap is still a little weird, probably need to remove some of the postprocessing stuff
//tried that and got nowhere, they are connected for some reason [you do copy the first one] (thanks for that)

//track car is slightly off the groud by a lot

public class Drive extends AbstractAppState {
	
	private BulletAppState bulletAppState;
	
	//World Model
//	public WorldType type;
	
	public World world;
	private Node worldNode;
	
	//public StaticWorld world;
//	StaticWorldBuilder sWorldB;
	
//	DynamicType wpType = DynamicType.Simple; //some value so no nulls
//	boolean needsMaterial = false;
//	public DynamicBuilder worldB;
	
	//car stuff
	public CarBuilder cb;
	private CarData car;
	
	int themCount = 0;
	private CarData them = Car.Rocket.get();
	
	//gui and camera stuff
	MyCamera camNode;
	UINode uiNode;
	MiniMap minimap;
	
	//debug stuff
	Node arrowNode;
	public int frameCount = 0;
	public boolean ifDebug = false;
	
    public Drive (CarData car, World world) {
    	super();
    	this.car = car;
    	this.world = world;
    	this.cb = new CarBuilder();
    	
    	
    	WorldType type = world.getType();
    	if (type == WorldType.NONE)
    	{
    		H.p("not sure what world type you want");
    		System.exit(-1);
    	}
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
		getPhysicsSpace().setMaxSubSteps(4);
		
		createWorld();
		
		buildCars();
		initCameras();
		
		setupGUI();
		
		connectJoyStick();
	}
    
	private void createWorld() {
		worldNode = world.init(getPhysicsSpace(), App.rally.getViewPort()); 
		/*
		if (type == WorldType.DYNAMIC) {
			
			worldB = wpType.getAndInitBuilder(getPhysicsSpace(), App.rally.getViewPort());
			App.rally.getRootNode().attachChild(worldB.getRootNode());
			
		} else if (type == WorldType.STATIC) {
			StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), world, true);
		}
		*/
		App.rally.getRootNode().attachChild(worldNode);

		arrowNode = new Node();
		App.rally.getRootNode().attachChild(arrowNode);
	}

	private void initCameras() {
	
		camNode = new MyCamera("Cam Node", App.rally.getCamera(), cb.get(0));
		App.rally.getRootNode().attachChild(camNode);
		
		minimap = new MiniMap(cb.get(0));
	}
	
	private void buildCars() {
		Vector3f start = world.getWorldStart();
		Matrix3f dir = world.getWorldRot();
		/*
		if (type == WorldType.DYNAMIC) {
			start = worldB.getWorldStart();
			dir.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
		} else if (type == WorldType.STATIC) {
			start = world.start;
		} else {
			start = new Vector3f();
		}
		*/
		cb.addCar(getPhysicsSpace(), 0, car, start, dir, true);
		
		for (int i = 1; i < themCount+1; i++) {
			start = start.add(0,0,2);
			cb.addCar(getPhysicsSpace(), i, them, start, dir, false);
		}
	}
	
	private void connectJoyStick() {
		Joystick[] joysticks = App.rally.getInputManager().getJoysticks();
		if (joysticks == null) {
			H.p("There are no joysticks :( .");
		}
	}
	
	private void setupGUI() {
		uiNode = new UINode();
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Not init below
	

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		bulletAppState.setEnabled(enabled); //we kinda don't want the physics running while paused
		
		//TODO particles and sound don't stop
	}
	
	@Override
	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);
		
		frameCount++;
		
		//update cars
		cb.update(tpf);
		
		//update world
		world.update(tpf, cb.get(0).getPhysicsLocation(), false);
		/*
		if (type == WorldType.DYNAMIC) {
			worldB.update(cb.get(0).getPhysicsLocation(), false);
		}
		*/
		if (App.rally.drive.ifDebug) {
			H.p(cb.get(0).getPhysicsLocation());
		}
		
		//Hud stuff
		uiNode.update(tpf);
		minimap.update(tpf);
		
		//camera
		camNode.myUpdate(tpf);
	}
	
	
	public void reset() {
		world.reset();
		/*
		if (type == WorldType.DYNAMIC) {
			worldB.reset();
		}
		*/
		
		arrowNode.detachAllChildren();
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void cleanup() {
		super.cleanup();
		
		H.p("cleaning drive class");
		cb.cleanup();
		cb = null;
		uiNode.cleanup();
		uiNode = null;
		
		App.rally.getStateManager().detach(bulletAppState);
		
		Node rn = App.rally.getRootNode();
		
		rn.detachChild(worldNode);
		world.cleanup();
		/*
		if (type == WorldType.DYNAMIC)
			rn.detachChild(worldB.getRootNode());
		*/
		rn.detachChild(arrowNode);
		rn.detachChild(camNode);
	}
}
