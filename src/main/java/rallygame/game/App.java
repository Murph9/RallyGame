package rallygame.game;


import java.util.Collection;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;

import rallygame.car.CarBuilder;
import rallygame.car.data.CarDataLoader;
import rallygame.effects.FilterManager;
import rallygame.effects.ParticleAtmosphere;
import rallygame.helper.Log;
import rallygame.service.ConstantChecker;
import rallygame.service.ObjectPlacer;
import rallygame.service.WorldGuiText;


public class App extends SimpleApplication {

	public static final Vector3f GRAVITY = new Vector3f(0, -9.81f, 0); // yay its down
	
	private AppFlow flow;

	public App() {
		super(new ParticleAtmosphere()
				, new AudioListenerState()
				, new StatsAppState()
				, new FilterManager()
				, new ObjectPlacer(true)
				, new CarBuilder(0.4f, new CarDataLoader())
                , new ConstantChecker()
				, new DebugAppState()
				, new WorldGuiText()
                
                // profiles in jme 3.2: possibly add physics engine stuff using custom sections
                //, new DetailedProfilerState() 
            );
	}

	@Override
	public void simpleInitApp() {
		inputManager.setCursorVisible(true);
		inputManager.deleteMapping(INPUT_MAPPING_EXIT); //no esc close pls

		//initialize Lemur (the GUI manager)
		GuiGlobals.initialize(this);
		//Load my Lemur style
		LemurGuiStyle.load(assetManager);
		

		//Init the Physics space with better defaults
		//BulletAppState needs to wait until after the app is initialised, so can't be called from the constructor
		BulletAppState bullet = new BulletAppState();
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

		flow.cleanup();

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
}
