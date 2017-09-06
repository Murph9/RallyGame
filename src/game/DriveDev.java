package game;

import world.World;
import world.WorldEditor;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import car.*;
import helper.H;


public class DriveDev extends DriveSimple {

	private CarEditor carEditor;
	private WorldEditor worldEditor;
	private CarWheelGraph wheelGraphs;
	
	public DriveDev (CarData car, World world) {
    	super(car, world);
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	//init input gui
		carEditor = new CarEditor(this.cb.get(0), () -> { reloadCar(); });
		carEditor.setLocalTranslation(H.screenTopLeft().add(0, -20, 0));
		App.rally.getGuiNode().attachChild(carEditor);
		
		worldEditor = new WorldEditor((a) -> { reloadWorld(a); });
		worldEditor.setLocalTranslation(H.screenTopRight().add(-worldEditor.width, 0, 0));
		App.rally.getGuiNode().attachChild(worldEditor);
		
		Vector3f size = new Vector3f(400,400,0);
		wheelGraphs = new CarWheelGraph(this.cb.get(0), size);
		wheelGraphs.setLocalTranslation(H.screenBottomRight().subtract(size));
		App.rally.getGuiNode().attachChild(wheelGraphs);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		
		wheelGraphs.update(tpf);
	}

	public void reloadCar() {
		//TODO minimap can't be reset
		
		MyPhysicsVehicle car = this.cb.get(0);
		
		Vector3f pos = car.getPhysicsLocation();
		Vector3f vel = car.getLinearVelocity();
		Quaternion q = car.getPhysicsRotation();
		
		CarData c = car.car;
		
		this.cb.removePlayer(0);
		this.cb.addCar(0, c, world.getStartPos(), world.getStartRot(), true);
		App.rally.getStateManager().attach(cb);

		App.rally.getStateManager().detach(uiNode);
		App.rally.getStateManager().detach(menu);

		menu = new DriveMenu(this);
		App.rally.getStateManager().attach(menu);
		uiNode = new CarUI(cb.get(0));
		App.rally.getStateManager().attach(uiNode);
		
		camera.setCar(cb.get(0));
		
		//keep current pos, vel, rot
		this.cb.get(0).setPhysicsLocation(pos);
		this.cb.get(0).setLinearVelocity(vel);
		this.cb.get(0).setPhysicsRotation(q);
		
		App.rally.getGuiNode().detachChild(carEditor);
		carEditor = new CarEditor(this.cb.get(0), () -> { reloadCar(); });
		carEditor.setLocalTranslation(H.screenTopLeft().add(0, -50, 0));
		App.rally.getGuiNode().attachChild(carEditor);
		
		wheelGraphs.updateMyPhysicsVehicle(this.cb.get(0));
	}
	
	public void reloadWorld(World world) {
		//reload new world
		App.rally.getStateManager().detach(this.world);
		this.world = world;
		App.rally.getStateManager().attach(this.world);
		
		//reset car
		this.cb.get(0).setPhysicsLocation(world.getStartPos());
		this.cb.get(0).setLinearVelocity(new Vector3f(0,0,0));
		this.cb.get(0).setPhysicsRotation(world.getStartRot());
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		App.rally.getGuiNode().detachChild(carEditor);
		App.rally.getGuiNode().detachChild(worldEditor);
		App.rally.getGuiNode().detachChild(wheelGraphs);
	}
}
