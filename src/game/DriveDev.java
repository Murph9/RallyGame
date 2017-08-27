package game;

import world.World;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;

import car.*;
import helper.H;


public class DriveDev extends DriveSimple {

	private Container inputContainer;
	
	public DriveDev (CarData car, World world) {
    	super(car, world);
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	//init input gui
		inputContainer = new CarEditor(this.cb.get(0), () -> { reloadCar(); });
		inputContainer.setLocalTranslation(H.screenTopLeft().add(0, -50, 0));
		App.rally.getGuiNode().attachChild(inputContainer);
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}

	public void reloadCar() {
		Vector3f pos = this.cb.get(0).getPhysicsLocation();
		Vector3f vel = this.cb.get(0).getLinearVelocity();
		Quaternion q = this.cb.get(0).getPhysicsRotation();
		
		//TODO minimap can't be reset
		App.rally.getStateManager().detach(uiNode);
		App.rally.getStateManager().detach(menu);
		this.cb.removePlayer(0);
		
		cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true);
		App.rally.getStateManager().attach(cb);
		
		uiNode = new CarUI(cb.get(0));
		App.rally.getStateManager().attach(uiNode);
		
		camera.setCar(cb.get(0));
		
		//keep current pos, vel, rot
		this.cb.get(0).setPhysicsLocation(pos);
		this.cb.get(0).setLinearVelocity(vel);
		this.cb.get(0).setPhysicsRotation(q);
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		App.rally.getGuiNode().detachChild(inputContainer);
	}
	
	//TODO change world type
}
