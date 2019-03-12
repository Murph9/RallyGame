package drive;

import world.World;
import world.WorldType;

import java.util.Collection;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.Joystick;

import car.*;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;
import helper.Log;

public class DriveBase extends AbstractAppState {
	
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
    		System.exit(-1);
    	}
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);

    	Collection<PhysicsRigidBody> list = App.rally.getPhysicsSpace().getRigidBodyList();
    	if (list.size() > 0) {
    		Log.p("Someone didn't clean up after themselves...: " + list.size());
    		for (PhysicsRigidBody r: list)
    			App.rally.getPhysicsSpace().remove(r);
    	}
    	
    	app.getStateManager().attach(world);
    	app.getStateManager().attach(menu);
    	
		//build player
		this.cb = new CarBuilder();
		cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true, null);
		app.getStateManager().attach(cb);
		
		uiNode = new CarUI(cb.get(0));
		app.getStateManager().attach(uiNode);
		
		//initCameras
		camera = new CarCamera("Camera", App.rally.getCamera(), cb.get(0));
		App.rally.getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
				
		//connectJoySticks
		Joystick[] joysticks = App.rally.getInputManager().getJoysticks();
		if (joysticks == null) {
			Log.e("There are no joysticks :(");
		}
		
		App.rally.bullet.setEnabled(true);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.world.setEnabled(enabled); //we kinda don't want the physics running while paused
		App.rally.bullet.setEnabled(enabled);
		this.camera.setEnabled(enabled);
		this.cb.setEnabled(enabled);
	}
	
	@Override
	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);
	}
	
	public void next() {
		App.rally.next(this);
	}
	
	public void reset() {
		world.reset();
	}
	
	public void cleanup() {
		super.cleanup();
		Log.p("cleaning drive class");
		
		App.rally.getStateManager().detach(cb);
		cb = null;
		
		App.rally.getStateManager().detach(menu);
		menu = null;
		
		App.rally.getStateManager().detach(uiNode);
		uiNode = null;
		
		App.rally.getStateManager().detach(world);
		world = null;
		
		App.rally.getStateManager().detach(camera);
		App.rally.getInputManager().removeRawInputListener(camera);
		camera = null;
	}
	
	protected void reInitPlayerCar(Car car) {
		//remove camera and ui
		App.rally.getStateManager().detach(camera);
		App.rally.getInputManager().removeRawInputListener(camera);
		
		App.rally.getStateManager().detach(uiNode);
		
		this.cb.removeCar(0);

		RayCarControl c = this.cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true, null); 
		
		//initCamera and ui again
		camera = new CarCamera("Camera", App.rally.getCamera(), c);
		App.rally.getStateManager().attach(camera);
		App.rally.getInputManager().addRawInputListener(camera);
		
		uiNode = new CarUI(c);
		App.rally.getStateManager().attach(uiNode);
	}
}
