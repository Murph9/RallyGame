package game;

import java.util.LinkedList;
import java.util.List;

import world.StaticWorldBuilder;
import world.StaticWorld;
import world.WorldBuilder;
import world.WorldType;
import world.wp.Floating;
import world.wp.WP;

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
//tried that and got no where, they are connected for some reason [you do copy the first one] (thanks for that)

//track car is slightly off the groud by a lot

public class Drive extends AbstractAppState {
	
	private BulletAppState bulletAppState;
	
	//World Model
	public WorldType type;
	
	public StaticWorld world;
	StaticWorldBuilder sWorldB;
	
	WP[] wpType = Floating.values();
	boolean needsMaterial = false;
	public WorldBuilder worldB;
	
	//car stuff
	public CarBuilder cb;
	private CarData car;
	
	int themCount = 0;
	private CarData them = Car.Hunter.get();
	
	//gui and camera stuff
	MyCamera camNode;
	UINode uiNode;
	MiniMap minimap;
	
	//debug stuff
	Node arrowNode;
	public int frameCount = 0;
	public boolean ifDebug = false;
		
    public Drive (State set) {
    	super();
    	this.car = set.getCar();
    	this.cb = new CarBuilder();
    	
    	type = set.getWorldType(); 
    	switch(type) {
    	case STATIC:
    		this.world = set.getStaticWorld();
    		break;
    	case DYNAMIC:
    		this.wpType = set.getDynamicWorld();
    		break;
    	case OTHER:
    	default:
    		H.p("not sure what world you want");
    	}
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
		
		createWorld();
		
		buildCars();
		initCameras();
		
		setupGUI();
		
		connectJoyStick();
	}
    
	private void createWorld() {
		if (type == WorldType.DYNAMIC) {
			worldB = new WorldBuilder(wpType, getPhysicsSpace(), App.rally.getViewPort());
			App.rally.getRootNode().attachChild(worldB);
			
		} else if (type == WorldType.STATIC) {
			StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), world, true);
		}

		arrowNode = new Node();
		App.rally.getRootNode().attachChild(arrowNode);
	}

	private void initCameras() {
	
		camNode = new MyCamera("Cam Node", App.rally.getCamera(), cb.get(0));
		App.rally.getRootNode().attachChild(camNode);
		
		minimap = new MiniMap(cb.get(0));
	}
	
	private void buildCars() {
		Vector3f start;
		Matrix3f dir = new Matrix3f();
		if (type == WorldType.DYNAMIC) {
			start = worldB.start;
			dir.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
		} else if (type == WorldType.STATIC) {
			start = world.start;
		} else {
			start = new Vector3f();
		}
		
		cb.addCar(getPhysicsSpace(), 0, car, start, dir, true);
		
		for (int i = 1; i < themCount+1; i++) {
			start = start.add(3,0,0);
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
		if (type == WorldType.DYNAMIC) {
			worldB.update(cb.get(0).getPhysicsLocation());
		}
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
		if (type == WorldType.DYNAMIC) {
			List<Spatial> ne = new LinkedList<Spatial>(worldB.curPieces);
			for (Spatial s: ne) {
				getPhysicsSpace().remove(s.getControl(0));
				worldB.detachChild(s);
				worldB.curPieces.remove(s);
			}
			worldB.start = new Vector3f(0,0,0);
			worldB.nextPos = new Vector3f(0,0,0);
			worldB.nextRot = new Quaternion();
		}
		
		arrowNode.detachAllChildren();
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void cleanup() {
		super.cleanup();
		
		H.p("cleaning");
		cb.cleanup();
		cb = null;
		uiNode.cleanup();
		uiNode = null;
		
		App.rally.getStateManager().detach(bulletAppState);
		
		Node rn = App.rally.getRootNode();
		if (type == WorldType.DYNAMIC)
			rn.detachChild(worldB);
		
		rn.detachChild(arrowNode);
		rn.detachChild(camNode);
	}
}
