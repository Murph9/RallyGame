package drive;

import world.World;
import world.WorldType;

import java.util.Collection;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.Joystick;

import car.*;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;
import game.ParticleAtmosphere;
import helper.Log;

public class DriveBase extends AbstractAppState {
	
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
    public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);

		this.app = (SimpleApplication)app;

    	Collection<PhysicsRigidBody> list = ((App)app).getPhysicsSpace().getRigidBodyList();
    	if (list.size() > 0) {
    		Log.p("Someone didn't clean up after themselves...: " + list.size());
    		for (PhysicsRigidBody r: list)
				((App)app).getPhysicsSpace().remove(r);
    	}
		
		stateManager.attach(world);
    	stateManager.attach(menu);
    	
		//build player
		this.cb = new CarBuilder((App)this.app);
		cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true, null);
		stateManager.attach(cb);
		
		uiNode = new CarUI(cb.get(0));
		stateManager.attach(uiNode);
		
		//Particle emitter
		ParticleAtmosphere particles = new ParticleAtmosphere(cb.get(0).getRootNode());
		stateManager.attach(particles);
		
		
		//initCameras
		camera = new CarCamera("Camera", app.getCamera(), cb.get(0));
		stateManager.attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		//connectJoySticks
		Joystick[] joysticks = app.getInputManager().getJoysticks();
		if (joysticks == null) {
			Log.e("There are no joysticks :(");
		}
		
		((App)app).setPhysicsSpaceEnabled(true);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.world.setEnabled(enabled); //we kinda don't want the physics running while paused
		((App)app).setPhysicsSpaceEnabled(enabled);
		this.camera.setEnabled(enabled);
		this.cb.setEnabled(enabled);
	}
	
	@Override
	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);
	}
	
	public void next() {
		((App)app).next(this);
	}
	
	public void reset() {
		world.reset();
	}
	
	public void cleanup() {
		super.cleanup();
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
		
		this.cb.removeCar(0);

		RayCarControl c = this.cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true, null); 
		
		//initCamera and ui again
		camera = new CarCamera("Camera", app.getCamera(), c);
		app.getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		uiNode = new CarUI(c);
		app.getStateManager().attach(uiNode);
	}
}
