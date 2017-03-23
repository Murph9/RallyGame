package game;


import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapFont;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.BaseStyles;

import car.Car;
import car.CarData;
import game.*;
import helper.H;
import settings.Configuration;
import world.*;
import world.highway.HighwayWorld;
import world.wp.WP.DynamicType;

////TODO Ideas for game modes:
//being chased. (with them spawning all lightning sci-fi like?)
//  time based
//  score based (closeness to them)
//  touch all of them in one run
//    or get them all the same colour (like the pads in mario galaxy)
//  get them to fall in a hole
//  follow points for being close
//the infinite road thing
//  overtake as many as you can
//  like the crew
//    get away from the start
//    stay on road at speed thing

//kind of like the the snowboarding phone game, where you do challenges on the way down
//	might work well going down one road

//Using eclipse: why isn't this a default?
//http://stackoverflow.com/questions/3915961/how-to-view-hierarchical-package-structure-in-eclipse-package-explorer

//TODO
//still need to get fog working..
//at night time or something because loading looks easier
//stop the car sound on the menus [please]

//Long TODO's:
//long running skidmark issue is in effect (but only for my computer)

//Bugs TODO:
//minimap is still a little weird, probably need to remove some of the water postprocessing stuff
//tried that and got nowhere, they are connected for some reason [you do copy the first one] (thanks for that)


@SuppressWarnings("unused")
public class Main extends SimpleApplication {

	public Start start;
	public ChooseCar chooseCar;
	public ChooseMap chooseMap;
	
	public DriveSimple drive;
	public SkyState sky;

	public BulletAppState bullet; //one physics space always
	
	private final CarData defaultCar = Car.Runner.get();
	private final CarData defaultThem = Car.Runner.get();
	private final World defaultWorld = new HighwayWorld();
			//Options:
			//	new HighwayWorld();
			//	new StaticWorldBuilder(StaticWorld.track2);
			//	DynamicType.Simple.getBuilder();
	
	private CarData car;
	private World world;
	
	public static void main(String[] args) {
		Configuration config = Configuration.Read();
		
		Main app = new Main();
		AppSettings settings = new AppSettings(true);
		if (config.ifFullscreen()) {
			settings.setFullscreen(true);
			//TODO untested
			//will probably cause some resolution issues if it doesn't match up nice
		} else {
			settings.setResolution(config.getWidth(),config.getHeight());
		}
		settings.setFrameRate(config.getFrameRate());
		settings.setUseJoysticks(true);
		settings.setTitle(config.getTitle());
		settings.setVSync(config.ifVsnyc());
		
//		settings.setFrameRate(config.getFrameRate());
		app.setSettings(settings);
		app.setTimer(new NanoTN(config.getFrameRate()));
		app.setShowSettings(false);
//		app.setDisplayStatView(false); //shows the triangle count and stuff
		app.start();

		///////////////////////
		//Just getting numbers for rotations
		Quaternion q = new Quaternion();
		q = q.fromAngleAxis(FastMath.DEG_TO_RAD*(-8), new Vector3f(0,0,1));
//		H.p(q);
		
		//note in the 0,0,0 -> a,b,c
		//b = turning right and left
		//a = barrel roll
		//c = up and down a hill
	}

	@Override
	public void simpleInitApp() {
		App.rally = this;
		
		boolean ignoreWarnings = false;
		boolean ignoreOthers = true;
		if (ignoreWarnings) {
			Logger.getLogger("com.jme3").setLevel(Level.SEVERE); //remove warnings here
			H.e("!!!! IGNORING IMPORTANT WARNINGS !!!!!");
		}
		if (ignoreOthers) {
			Logger.getLogger("com.jme3.scene.plugins.").setLevel(Level.SEVERE);//remove warnings here
			H.e("!!!! IGNORING (some) IMPORTANT WARNINGS !!!!!");	
		}
		inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		//initialize Lemur (GUI thing)
		GuiGlobals.initialize(this);
		//Load the 'glass' style
		BaseStyles.loadGlassStyle();
		//Set 'glass' as the default style when not specified
		GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
		
		InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
		inputMapper.release(); //TODO no keyboard inputs please (causes weird issues)
		
		sky = new SkyState(); //lighting and shadow stuff is global
		getStateManager().attach(sky);
		
		start = new Start();
		getStateManager().attach(start);
		
		bullet = new BulletAppState();
    	//bullet.setDebugEnabled(true); //TODO 3.1 beta-1 still broken
		//getPhysicsSpace().setMaxSubSteps(4);
		getStateManager().attach(bullet);
		
		inputManager.setCursorVisible(true);
		flyCam.setEnabled(false);

		//set the default option
		car = defaultCar;
		world = defaultWorld;
	}
	
	public void startDemo(AppState state) {
		getStateManager().detach(state);
		
		drive = new DriveDemo(defaultCar, DynamicType.Valley.getBuilder()); //make something else it can drive on
		getStateManager().attach(drive);
	}
	
	public void startAI(AppState state) {
		getStateManager().detach(state);
		
		if (car == null || world == null || defaultThem == null) {
			System.err.println("Defaults not set.");
			System.exit(1);
		}

		drive = new DriveAI(defaultCar, defaultThem, defaultWorld);
		getStateManager().attach(drive);
	}
	
	public void startFast(AppState state) {
		//use the default option and just init straight away
		getStateManager().detach(state);
		
		if (car == null || world == null) {
			System.err.println("Defaults not set.");
			System.exit(1);
		}
		
		startDrive(car, world);
	}
	
	//HERE is the logic for the app progress.
	// its the thing you call when the app state is done with itself
	public void next(AppState app) {
		AppStateManager state = getStateManager();
		
		if (app instanceof Start) {
//			App.rally.stop();
//			return;
			
			state.detach(start);
			startChooseCar();
		} else if (app instanceof ChooseCar) {
			state.detach(chooseCar);
			startChooseMap();
			
		} else if (app instanceof ChooseMap) {
			state.detach(chooseMap);
			
			startDrive(chooseCar.getCarData(), chooseMap.getWorld());
			
		} else if (app instanceof DriveSimple) {
			state.detach(drive);
			drive = null;
			
			//then start again
			start = new Start();
			state.attach(start);
		} else {
			H.p("Unexpected state called me '" + app + "' - rally.next()");
		}
	}
	
	private void startChooseCar() {
		chooseCar = new ChooseCar();
		getStateManager().attach(chooseCar);
	}
	
	private void startChooseMap() {
		chooseMap = new ChooseMap();
		getStateManager().attach(chooseMap);
	}
	
	private void startDrive(CarData car, World world) {
		if (drive != null) return; //not sure what this is actually hoping to stop
				
		drive = new DriveSimple(car, world);
		getStateManager().attach(drive);
	}

	/////////////////////
	public BitmapFont getFont() {
		return guiFont;
	}
	public AppSettings getSettings() {
		return settings;
	}
	public PhysicsSpace getPhysicsSpace() {
		return bullet.getPhysicsSpace();
	}

	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		stateManager.update(tpf);
	}

	/////////////// menu
	public com.jme3.system.Timer getTimer() {
		return timer;
	}

}

class NanoTN extends NanoTimer {

	private float frames;
	NanoTN(float frames) {
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
