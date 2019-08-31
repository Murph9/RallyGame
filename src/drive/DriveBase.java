package drive;

import world.World;
import world.WorldType;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.Joystick;

import car.*;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;
import helper.Log;

public class DriveBase extends BaseAppState {
	
	public SimpleApplication app;

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
	
    public DriveBase(Car car, World world) {
    	super();
    	this.car = car;
    	this.world = world;
    	this.menu = new DriveMenu(this);
    	
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
    	stateManager.attach(menu);
    	
		//build player
		this.cb = new CarBuilder((App)app);
		RayCarControl rayCar = cb.addCar(car, world.getStartPos(), world.getStartRot(), true, null);
		stateManager.attach(cb);
		
		uiNode = new CarUI(rayCar);
		stateManager.attach(uiNode);
		
		//initCameras
		camera = new CarCamera("Camera", app.getCamera(), rayCar);
		stateManager.attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		//connectJoySticks
		Joystick[] joysticks = app.getInputManager().getJoysticks();
		if (joysticks == null) {
			Log.e("There are no joysticks :(");
		}
		
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
		((App)app).next(this);
	}
	
	public void reset() {
		world.reset();
	}
	
	@Override
	public void cleanup(Application app) {
		Log.p("cleaning drive class");
		
		app.getStateManager().detach(cb);
		cb = null;
		
		app.getStateManager().detach(menu);
		menu = null;
		
		app.getStateManager().detach(uiNode);
		uiNode = null;
		
		app.getStateManager().detach(world);
		world = null;
		
		app.getStateManager().detach(camera);
		app.getInputManager().removeRawInputListener(camera);
		camera = null;
	}
	
	protected void reInitPlayerCar(Car car) {
		//remove camera and ui
		app.getStateManager().detach(camera);
		app.getInputManager().removeRawInputListener(camera);
		
		app.getStateManager().detach(uiNode);
		
		this.cb.removeCar(cb.get(0));

		RayCarControl c = this.cb.addCar(car, world.getStartPos(), world.getStartRot(), true, null); 
		
		//initCamera and ui again
		camera = new CarCamera("Camera", app.getCamera(), c);
		app.getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		uiNode = new CarUI(c);
		app.getStateManager().attach(uiNode);
	}
}
