package game;


import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.MouseAppState;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.BaseStyles;

import car.data.Car;
import car.ray.CarDataConst;
import drive.*;
import effects.EdgeMaskFilter;
import effects.FilterManager;
import effects.ParticleAtmosphere;
import game.*;
import helper.Log;
import settings.Configuration;
import world.*;
import world.highway.HighwayWorld;
import world.lsystem.LSystemWorld;
import world.track.TrackWorld;
import world.wp.WP.DynamicType;

@SuppressWarnings("unused")
public class App extends SimpleApplication {

	public static final Vector3f GRAVITY = new Vector3f(0, -9.81f, 0); // yay its down
	public static final Boolean IF_DEBUG = false;
	public static App CUR;

	private Start start;
	private ChooseCar chooseCar;
	private ChooseMap chooseMap;
	
	public DriveBase drive;

	private Car car;
	private Car them;
	private World world;
	private void loadDefaults() {
		car = Car.Runner;
		them = Car.Rally;
		world = new StaticWorldBuilder(StaticWorld.track2);
		//world alernatives:
		//new HighwayWorld();
		//new TrackWorld();
		//new StaticWorldBuilder(StaticWorld.track2);
		//DynamicType.Simple.getBuilder();
	}

	public App() {
		super(new ParticleAtmosphere()
				, new AudioListenerState()
				, new StatsAppState()
				, new FilterManager()
				//, new DetailedProfilerState() //profiling in jme 3.2: TODO add physics engine stuff using custom sections
				);
	}

	@Override
	public void simpleInitApp() {
		CUR = this;

		boolean ignoreWarnings = false;
		if (ignoreWarnings) {
			Logger.getLogger("com.jme3").setLevel(Level.SEVERE); //remove warnings here
			Log.e("!!!! IGNORING IMPORTANT WARNINGS !!!!!");
		}
		Logger.getLogger("com.jme3.scene.plugins.blender").setLevel(Level.WARNING); //ignore blender warnings

		inputManager.setCursorVisible(true);
		inputManager.deleteMapping(INPUT_MAPPING_EXIT); //no esc close pls

		//initialize Lemur (the GUI manager)
		GuiGlobals.initialize(this);
		//Load my Lemur style
		LemurGuiStyle.load(assetManager);
		

		//Init the Physics space with better defaults
		//BulletAppState needs to wait until after the app is initialised, so can't be called from the constructor
		BulletAppState bullet = new BulletAppState();
		// bullet.setSpeed(0.1f); //physics per second rate
		// bullet.setDebugEnabled(true); //show bullet wireframes
		bullet.setThreadingType(ThreadingType.PARALLEL);
		getStateManager().attach(bullet);
		bullet.getPhysicsSpace().setAccuracy(1f / 120f); // physics rate
		bullet.getPhysicsSpace().setGravity(GRAVITY);

		///////
		//Game logic start:
		start = new Start();
		getStateManager().attach(start);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (Vector3f.ZERO.length() != 0) {
			Log.e("Vector3f.ZERO is not zero!!!!, considered a fatal error.");
			System.exit(342);
		}

		//TODO this can check if a state is active
		BaseAppState a = getStateManager().getState(BaseAppState.class);
		boolean printAppState = false;
		if (printAppState)
			Log.p(a);
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
	
	public void startMainRoad(AppState state) {
		getStateManager().detach(state);
		
		drive = new DriveMainRoadGetaway();
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
	public AppSettings getSettings() {
		return settings;
	}
	public PhysicsSpace getPhysicsSpace() {
		return getStateManager().getState(BulletAppState.class).getPhysicsSpace();
	}

	@Override
	public void destroy() {
		super.destroy();

		Log.p("Closing " + this.getClass().getName());

		PhysicsSpace space = getPhysicsSpace();
		Collection<PhysicsRigidBody> list = space.getRigidBodyList();
		if (list.size() > 0) {
			Log.e("Someone didn't clean up after themselves: " + list.size() + " physics bodies remain. (or this was called too early)");
			for (PhysicsRigidBody r : list)
				Log.p(r);
			for (PhysicsRigidBody r : list)
				space.remove(r);
		}
	}

	/////////////// menu
	public com.jme3.system.Timer getTimer() {
		return timer;
	}
}
