package game;


import java.util.LinkedList;
import java.util.List;

import world.StaticWorldBuilder;
import world.WP;
import world.Floating;
import world.StaticWorld;
import world.WorldBuilder;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapFont;
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
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;

//Long TODO's: 
//i broke skidmarks again, will need to fix that :(

//ENGINE stuff

//Bugs TODO
//maps a little weird still, probably need to remove some of the postprocessing stuff
 //tried that and got no where, they are connected for some reason [you do copy the first one]

//track car is slightly off the groud by a lot

////Idea for game modes:
//being chased. (them spawning all lightning sci-fi like)
//  time based
//  score based (closeness to them)
//  touch all of them once
//the infinite road thing


public class Rally extends SimpleApplication {
	
	//boiler stuff
	private BulletAppState bulletAppState;
	
	//World Model
	StaticWorld world = StaticWorld.duct2; //Set map here
	StaticWorldBuilder sWorldB;
	
	boolean dynamicWorld = false;
	WP[] type = Floating.values();
	boolean needsMaterial = false;
	WorldBuilder worldB;
	
	//car stuff
	CarBuilder cb;
	private ExtendedVT car = new Runner();
	
	int themCount = 10;
	private ExtendedVT them = new Hunter();
	
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
    private final boolean ifGlow = false;
//    FilterPostProcessor fpp;
//    BloomFilter bloom;
    
    
	public static void main(String[] args) {
		int fps = 60; //default is 60
		
		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280,720); //1280,720 //1920,1080
		settings.setFrameRate(fps);
		settings.setUseJoysticks(true);
		settings.setVSync(false);
		
		app.setSettings(settings);
		app.setTimer(new NanoT60(fps));
		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();
	}
	
	@Override
	public void simpleInitApp() {
		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
//		bulletAppState.setDebugEnabled(true); //TODO this causes a illegal argument exception
//		getPhysicsSpace().enableDebug(assetManager); //so does this
		
		createWorld();
		
		buildPlayers();
		initCameras();
		
		setupGUI();
		
		connectJoyStick();
		
		//Just getting numbers for rotations
//		Quaternion q = new Quaternion();
//		q.fromAngleAxis(FastMath.HALF_PI/2, new Vector3f(0,1,0));
//		System.out.println(q);
	}

	private void createWorld() {
		if (dynamicWorld) {
			worldB = new WorldBuilder(this, assetManager, type, viewPort, needsMaterial);
			rootNode.attachChild(worldB);
			
		} else {
			sWorldB = new StaticWorldBuilder(this, world, ifShadow);
		}
		
		//lights
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(0.3f));
		rootNode.addLight(al);

		DirectionalLight sun = new DirectionalLight();
		sun.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		rootNode.addLight(sun);
		
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

	private void initCameras() {
		flyCam.setEnabled(false);
		
		camNode = new MyCamera("Cam Node", cam, cb.get(0));
		rootNode.attachChild(camNode);
		
		minimap = new MiniMap(this, cb.get(0));
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
		cb.addPlayer(0, this, car, start, dir, false);
		
		for (int i = 1; i < themCount+1; i++) {
			start = start.add(3,0,0);
			cb.addPlayer(i, this, them, start, dir, true);
		}
	}
	
	private void connectJoyStick() {
		Joystick[] joysticks = inputManager.getJoysticks();
		if (joysticks == null) {
			H.p("There are no joysticks :( .");
		}
	}
	
	private void setupGUI() {
		uiNode = new UINode(this);
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/* Not init below */
	
	@Override
	public void simpleUpdate(float tpf) {
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
	
	public BitmapFont getFont() {
		return guiFont;
	}
	public AppSettings getSettings() {
		return settings;
	}
	public com.jme3.renderer.Camera getCam() {
		return cam;
	}
	
	public PhysicsSpace getPhysicsSpace(){
		return bulletAppState.getPhysicsSpace();
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
	}
	
}

class NanoT60 extends NanoTimer {
	
	private float frames;
	NanoT60(float frames) {
		super();
		this.frames = frames;
	}
	
	public void setFrames(float frames) {
		//for setting slow mo or something..
	}
	
	@Override
	public float getTimePerFrame() {
		//return tpf;
	    return 1f/frames; //frame time for 60fps
	}
}
