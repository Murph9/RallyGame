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
import helper.H;

//TODO there is another appstate to try here called (base|basic?)appstate
public class DriveSimple extends AbstractAppState {
	
	private BulletAppState bulletAppState;
	
	public World world;
	private Node worldNode;
	
	//car stuff
	public CarBuilder cb;
	private CarData car;
	
	//gui and camera stuff
	CarCamera camera;
	UINode uiNode;
	MiniMap minimap;
	
	//debug stuff
	Node arrowNode;
	public int frameCount = 0;
	public boolean ifDebug = false;
	
    public DriveSimple (CarData car, World world) {
    	super();
    	this.car = car;
    	this.world = world;
    	this.cb = new CarBuilder();
    	App.rally.getStateManager().attach(cb);
    	
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
    	//bulletAppState.setDebugEnabled(true); //TODO 3.1 beta-1 still broken
		app.getStateManager().attach(bulletAppState);
		getPhysicsSpace().setMaxSubSteps(4);
		
		createWorld();
		
		buildCars();
		initCameras();
		
		//setup GUI
		uiNode = new UINode(cb.get(0));
		App.rally.getStateManager().attach(uiNode);
		
		connectJoyStick();
	}
    
	private void createWorld() {
		worldNode = world.init(getPhysicsSpace(), App.rally.getViewPort()); 
		App.rally.getRootNode().attachChild(worldNode);

		arrowNode = new Node();
		App.rally.getRootNode().attachChild(arrowNode);
	}

	private void initCameras() {
	
		camera = new CarCamera("Camera", App.rally.getCamera(), cb.get(0));
		App.rally.getRootNode().attachChild(camera);
		App.rally.getInputManager().addRawInputListener(camera);
		
		minimap = new MiniMap(cb.get(0));
	}
	
	private void buildCars() {
		Vector3f start = world.getWorldStart();
		Matrix3f dir = world.getWorldRot();
		cb.addCar(getPhysicsSpace(), 0, car, start, dir, true);
	}
	
	private void connectJoyStick() {
		Joystick[] joysticks = App.rally.getInputManager().getJoysticks();
		if (joysticks == null) {
			H.e("There are no joysticks :( .");
		}
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
		
		//update world
		world.update(tpf, cb.get(0).getPhysicsLocation(), false);

		//Hud stuff
		minimap.update(tpf);
		
		//camera
		camera.myUpdate(tpf);
	}
	
	
	public void reset() {
		world.reset();
		arrowNode.detachAllChildren();
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void cleanup() {
		super.cleanup();
		
		H.p("cleaning drive class");
		App.rally.getStateManager().detach(cb);
		cb = null;
		
		App.rally.getStateManager().detach(uiNode);
		uiNode = null;
		
		App.rally.getStateManager().detach(bulletAppState);
		
		Node rn = App.rally.getRootNode();
		
		rn.detachChild(worldNode);
		world.cleanup();

		rn.detachChild(arrowNode);
		rn.detachChild(camera);
		App.rally.getInputManager().removeRawInputListener(camera);
	}
}
