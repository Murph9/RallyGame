package game;


import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapFont;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.MouseAppState;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.BaseStyles;

import car.data.Car;
import car.ray.CarDataConst;
import drive.*;
import game.*;
import helper.Log;
import settings.Configuration;
import world.*;
import world.highway.HighwayWorld;
import world.lsystem.LSystemWorld;
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
//night time or something because loading looks easier to do


@SuppressWarnings("unused")
public class Main extends SimpleApplication {

	public Start start;
	public ChooseCar chooseCar;
	public ChooseMap chooseMap;
	
	public DriveBase drive;
	public SkyState sky;

	public BulletAppState bullet; //one physics space always
	
	private Car car;
	private Car them;
	private World world;
	private void loadDefaults() {
		car = Car.Roadster;
		them = Car.Runner;
		world = DynamicType.Simple.getBuilder();
		//world alernatives:
		//	new HighwayWorld();
		//	new StaticWorldBuilder(StaticWorld.track2);
		//	DynamicType.Simple.getBuilder();
	}

	public int frameCount = 0; //global frame timer
	public final Boolean IF_DEBUG = false;
	
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
		
		settings.setFrameRate(config.getFrameRate());
		app.setSettings(settings);
		app.setShowSettings(false);
//		app.setDisplayStatView(false); //defaults to on, shows the triangle count and stuff
		app.start();
	}
	
	@Override
	public void simpleInitApp() {
		App.rally = this;
		
		boolean ignoreWarnings = false;
		boolean ignoreOthers = true;
		if (ignoreWarnings) {
			Logger.getLogger("com.jme3").setLevel(Level.SEVERE); //remove warnings here
			Log.e("!!!! IGNORING IMPORTANT WARNINGS !!!!!");
		}
		if (ignoreOthers) {
			Logger.getLogger("com.jme3.scene.plugins.").setLevel(Level.SEVERE);//remove warnings here
			Log.e("!!!! IGNORING (some) IMPORTANT WARNINGS !!!!!");
		}
		inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		//initialize Lemur (GUI thing)
		GuiGlobals.initialize(this);
		//Load the 'glass' style
		BaseStyles.loadGlassStyle();
		//Set 'glass' as the default style when not specified
		GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
		//Init the lemur mouse listener
		getStateManager().attach(new MouseAppState(this));
		
		
		InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
		inputMapper.release(); //no keyboard inputs after init
		
		sky = new SkyState(); //lighting and shadow stuff is global
		getStateManager().attach(sky);
		
		start = new Start();
		getStateManager().attach(start);
		
		bullet = new BulletAppState();
//		bullet.setSpeed(0.1f); //physics per second rate
//    	bullet.setDebugEnabled(true); //show bullet wireframes
		getStateManager().attach(bullet);
		bullet.getPhysicsSpace().setAccuracy(1f/120f); //physics rate
		bullet.getPhysicsSpace().setGravity(new Vector3f(0, -9.81f, 0)); //yay its down
		
		inputManager.setCursorVisible(true);
		flyCam.setEnabled(false);
		
		//profiling in jme 3.2 (TODO add physics engine stuff to this)
//		getStateManager().attach(new DetailedProfilerState());
		
	}
	
	@Override
	public void update() {
		super.update();
		
		if (Vector3f.ZERO.length() != 0) {
			Log.e("Vector3f.ZERO is not zero!!!!, considered a fatal error.");
			System.exit(342);
		}
	}
	
	public void startDemo(AppState state) {
		getStateManager().detach(state);
		
		drive = new DriveDemo(Car.Runner, DynamicType.Valley.getBuilder()); //TODO allow other tracks
		getStateManager().attach(drive);
	}
	
	public void startDev(AppState state) {
		getStateManager().detach(state);
		
		drive = new DriveDev(Car.Runner, new StaticWorldBuilder(StaticWorld.track2));
		getStateManager().attach(drive);
	}
	
	public void startCrash(AppState state) {
		getStateManager().detach(state);
		
		drive = new DriveCrash(new StaticWorldBuilder(StaticWorld.duct2));
		getStateManager().attach(drive);
	}
	
	
	public void startAI(AppState state) {
		getStateManager().detach(state);
		
		loadDefaults();
		if (car == null || world == null) {
			System.err.println("Defaults not set.");
			System.exit(1);
		}

		drive = new DriveAI(car, Car.Runner, world);
		getStateManager().attach(drive);
	}
	public void startRace(AppState state) {
		getStateManager().detach(state); 
		getStateManager().attach(new DriveRace()); //TODO bit of a hack
	}
	
	public void startFast(AppState state) {
		//use the default option and just init straight away
		getStateManager().detach(state);
		
		loadDefaults(); //load default values
		
		if (car == null || world == null) {
			System.err.println("Main.startFast(): Defaults not set.");
			System.exit(1);
		}
		
		startDrive(car, world);
	}
	
	//HERE is the logic for the app progress.
	// its the thing you call when the app state is done with itself
	public void next(AppState app) {
		AppStateManager state = getStateManager();
		
		if (app instanceof Start) {
			state.detach(start);
			startChooseCar();

		} else if (app instanceof ChooseCar) {
			state.detach(chooseCar);
			startChooseMap();
			
		} else if (app instanceof ChooseMap) {
			state.detach(chooseMap);
			
			startDrive(chooseCar.getCarType(), chooseMap.getWorld());
			
		} else if (app instanceof DriveBase) {
			state.detach(drive);
			drive = null;
			
			//then start again
			start = new Start();
			state.attach(start);
		} else {
			Log.p("Unexpected state called me '" + app + "' - rally.next()");
			//but just start again anyway
			state.detach(app);
			drive = null;
			start = new Start();
			state.attach(start);
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
	
	private void startDrive(Car car, World world) {
		if (drive != null) return; //not sure what this is actually hoping to stop
				
		drive = new DriveBase(car, world);
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
		frameCount++; //global frame timer update
	}

	/////////////// menu
	public com.jme3.system.Timer getTimer() {
		return timer;
	}

}
