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

import car.CarBuilder;
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
	
	private AppFlow flow;

	public App() {
		super(new ParticleAtmosphere()
				, new AudioListenerState()
				, new StatsAppState()
				, new FilterManager()
				, new CarBuilder()
				, new DebugAppState(true)
				//, new DetailedProfilerState() //profiling in jme 3.2: TODO add physics engine stuff using custom sections
				);
	}

	@Override
	public void simpleInitApp() {
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
		flow = new AppFlow(this);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (Vector3f.ZERO.length() != 0) {
			Log.e("Vector3f.ZERO is not zero!!!!, considered a fatal error.");
			System.exit(342);
		}
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
