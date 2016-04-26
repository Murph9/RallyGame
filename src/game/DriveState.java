package game;

import java.util.LinkedList;
import java.util.List;

import world.StaticWorldBuilder;
import world.WP;
import world.Floating;
import world.StaticWorld;
import world.WorldBuilder;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.Joystick;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.*;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

//Long TODO's: 
//long running skidmark issue is in effect

//Bugs TODO
//minimap is still a little weird, probably need to remove some of the postprocessing stuff
//tried that and got no where, they are connected for some reason [you do copy the first one] (thanks for that)

//track car is slightly off the groud by a lot

public class DriveState extends AbstractAppState {
	
	private BulletAppState bulletAppState;
	
	//World Model
	StaticWorld world;
	StaticWorldBuilder sWorldB;
	
	DirectionalLight sun;
	AmbientLight amb;
	
	boolean dynamicWorld = false;
	WP[] type = Floating.values();
	boolean needsMaterial = false;
	WorldBuilder worldB;
	
	//car stuff
	CarBuilder cb;
	private CarData car;
	
	int themCount = 3;
	private CarData them = Car.Hunter.get();
	
	//gui and camera stuff
	MyCamera camNode;
	UINode uiNode;
	MiniMap minimap;
	
	//debug stuff
	Node arrowNode;
	int frameCount = 0;
	public boolean ifDebug = false;
		
	//shadow stuff
	private final boolean ifShadow = true;
	private final boolean ifFancyShadow = false;
    private DirectionalLightShadowRenderer dlsr;
    private DirectionalLightShadowFilter dlsf;
    
    //glowing stuff
    private final boolean ifGlow = false; //TODO does this even work?
//    FilterPostProcessor fpp;
//    BloomFilter bloom;
    
    public DriveState (CarData car, StaticWorld world) {
    	super();
    	this.car = car;
    	this.world = world;
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
		
		createWorld();
		
		buildPlayers();
		initCameras();
		
		setupGUI();
		
		connectJoyStick();
	}
    
	private void createWorld() {
		if (dynamicWorld) {
			worldB = new WorldBuilder(type, App.rally.getViewPort(), needsMaterial);
			App.rally.getRootNode().attachChild(worldB);
			
		} else {
			StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), world, ifShadow);
		}
		
		//lights
		amb = new AmbientLight();
		amb.setColor(ColorRGBA.Blue.mult(0.3f));
		App.rally.getRootNode().addLight(amb);

		sun = new DirectionalLight();
		sun.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		App.rally.getRootNode().addLight(sun);
		
		App.rally.getViewPort().setBackgroundColor(ColorRGBA.Blue);
		
		arrowNode = new Node();
		App.rally.getRootNode().attachChild(arrowNode);
		
		if (ifShadow) {
	        //Shadows and lights
	        dlsr = new DirectionalLightShadowRenderer(App.rally.getAssetManager(), 2048, 3);
	        dlsr.setLight(sun);
	        dlsr.setLambda(0.55f);
	        dlsr.setShadowIntensity(0.6f);
	        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
	        App.rally.getViewPort().addProcessor(dlsr);
	
	        dlsf = new DirectionalLightShadowFilter(App.rally.getAssetManager(), 2048, 3);
	        dlsf.setLight(sun);
	        dlsf.setLambda(0.55f);
	        dlsf.setShadowIntensity(0.6f);
	        dlsf.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
	        dlsf.setEnabled(false);
	
	        FilterPostProcessor fpp = new FilterPostProcessor(App.rally.getAssetManager());
	        fpp.addFilter(dlsf);
	
	        if (ifFancyShadow) {
			    fpp = new FilterPostProcessor(App.rally.getAssetManager());
			    SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
			    fpp.addFilter(ssaoFilter);
	        }
	        
	        if (ifGlow) { //TODO this does weird things
	        	BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
	        	fpp.addFilter(bloom);
	        }
	        
	        App.rally.getViewPort().addProcessor(fpp);
        }
	}

	private void initCameras() {
	
		camNode = new MyCamera("Cam Node", App.rally.getCamera(), cb.get(0));
		App.rally.getRootNode().attachChild(camNode);
		
		minimap = new MiniMap(cb.get(0));
	}
	
	private void buildPlayers() {
		Vector3f start;
		Matrix3f dir = new Matrix3f();
		if (dynamicWorld) {
			start = worldB.start;
			dir.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
		} else {
			start = world.start;
		}
		
		cb = new CarBuilder();
		cb.addPlayer(getPhysicsSpace(), 0, car, start, dir, true);
		
		for (int i = 1; i < themCount+1; i++) {
			start = start.add(3,0,0);
			cb.addPlayer(getPhysicsSpace(), i, them, start, dir, false);
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
		
		cb.update(tpf);
		
		//////////////////////////////////
		//Hud stuff
		uiNode.update(tpf);
		minimap.update(tpf);
		
		/////////////////////////////////
		//camera
		camNode.myUpdate(tpf);
	}
	
	
	public void reset() {
		if (dynamicWorld) {
			List<Spatial> ne = new LinkedList<Spatial>(worldB.curPieces);
			for (Spatial s: ne) {
				getPhysicsSpace().remove(s.getControl(0));
				worldB.detachChild(s);
				worldB.curPieces.remove(s);
			}
			worldB.start = new Vector3f(0,0,0);
			worldB.nextPos = new Vector3f(0,0,0);
			worldB.nextRot = new Quaternion();
		} else {
			
		}
		arrowNode.detachAllChildren();
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void cleanup() {
		cb.cleanup();
	}
}
