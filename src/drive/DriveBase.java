package drive;

import world.World;
import world.WorldType;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;

import car.*;
import car.data.Car;
import car.ray.RayCarControl;
import game.IDriveDone;
import helper.Log;

public class DriveBase extends BaseAppState {
	
	private final IDriveDone done;
	public DriveMenu menu;
	public World world;

	//car stuff
	public CarBuilder cb;
	protected Car car;
	
	//gui and camera stuff
	CarCamera camera;
	CarUI uiNode;
	
	//debug stuff
	public boolean ifDebug = false;
	
    public DriveBase(IDriveDone done, Car car, World world) {
		super();
		this.done = done;
    	this.car = car;
    	this.world = world;
    	
    	WorldType type = world.getType();
    	if (type == WorldType.NONE)
    	{
    		Log.e("not sure what world type you want");
    		System.exit(-15);
    	}
    }
    
    @Override
    public void initialize(Application app) {
		AppStateManager stateManager = getStateManager();
		
		stateManager.attach(world);

		this.menu = new DriveMenu(this);
    	stateManager.attach(menu);
    	
		//build player
		this.cb = getState(CarBuilder.class);
		if (!this.cb.getAll().isEmpty()) {
			Log.e("!Unusually there are cars still in car builder, please clean up.");
			this.cb.removeAll();
		}

		RayCarControl rayCar = cb.addCar(car, world.getStartPos(), world.getStartRot(), true);
		
		uiNode = new CarUI(rayCar);
		stateManager.attach(uiNode);
		
		//initCameras
		camera = new CarCamera(app.getCamera(), rayCar);
		stateManager.attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		getState(BulletAppState.class).setEnabled(true);
	}

	@Override
	protected void onEnable() {
		_setEnabled(true);
	}
	@Override
	protected void onDisable() {
		_setEnabled(false);
	}

	private void _setEnabled(boolean enabled) {
		this.world.setEnabled(enabled); //we kinda don't want the physics running while paused
		getState(BulletAppState.class).setEnabled(enabled);
		this.camera.setEnabled(enabled);
		this.cb.setEnabled(enabled);
	}
	
	public void next() {
		this.done.done(this);
	}
	
	public void reset() {
		world.reset();
	}
	
	@Override
	public void cleanup(Application app) {
		Log.p("cleaning drive class");
				
		getStateManager().detach(menu);
		menu = null;
		
		getStateManager().detach(uiNode);
		uiNode = null;
		
		getStateManager().detach(world);
		world = null;
		
		getStateManager().detach(camera);
		app.getInputManager().removeRawInputListener(camera);
		camera = null;

		cb.removeAll();
		cb = null;
	}
	
	protected void reInitPlayerCar(Car car) {
		//remove camera and ui
		AppStateManager sm = getStateManager();
		sm.detach(camera);
		getApplication().getInputManager().removeRawInputListener(camera);
		
		sm.detach(uiNode);
		
		this.cb.removeCar(cb.get(0));

		RayCarControl c = this.cb.addCar(car, world.getStartPos(), world.getStartRot(), true);
		
		//initCamera and ui again
		camera = new CarCamera(getApplication().getCamera(), c);
		sm.attach(camera);
		getApplication().getInputManager().addRawInputListener(camera);
		
		uiNode = new CarUI(c);
		sm.attach(uiNode);
	}
}
