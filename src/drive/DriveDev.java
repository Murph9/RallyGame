package drive;

import world.World;
import world.WorldEditor;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;

import car.*;
import car.data.Car;
import car.ray.CarDataConst;
import car.ray.RayCarControl;
import helper.H;


public class DriveDev extends DriveBase {

	private CarEditor carEditor;
	private WorldEditor worldEditor;
	private TractionCurveGraph wheelGraphs;
	
	public DriveDev(Car car, World world) {
    	super(car, world);
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	//init input gui
		carEditor = new CarEditor(app.getInputManager(), this.cb.get(0), (data) -> { reloadCar(data);}, (car) -> { return resetCar(car); });
		carEditor.setLocalTranslation(H.screenTopLeft().add(0, -20, 0));
		this.app.getGuiNode().attachChild(carEditor);
		
		worldEditor = new WorldEditor((a) -> { reloadWorld(a); });
		worldEditor.setLocalTranslation(H.screenTopRight().add(-worldEditor.width, 0, 0));
		this.app.getGuiNode().attachChild(worldEditor);
		
		Vector3f size = new Vector3f(400,400,0);
		wheelGraphs = new TractionCurveGraph(this.app.getAssetManager(), this.cb.get(0), size);
		wheelGraphs.setLocalTranslation(H.screenBottomRight().subtract(size.add(-5,-25,0)));
		this.app.getGuiNode().attachChild(wheelGraphs);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		
		wheelGraphs.update(tpf);
	}

	private void reloadCar(CarDataConst data) {
		this.cb.setCarData(0, data);
		wheelGraphs.updateMyPhysicsVehicle(this.cb.get(0));
	}
	private RayCarControl resetCar(Car car) {
		this.reInitPlayerCar(car);
		return this.cb.get(0);
	}
	
	public void reloadWorld(World world) {
		//reload new world
		this.app.getStateManager().detach(this.world);
		this.world = world;
		this.app.getStateManager().attach(this.world);
		
		//reset car
		this.cb.get(0).setPhysicsLocation(world.getStartPos());
		this.cb.get(0).setLinearVelocity(new Vector3f(0,0,0));
		this.cb.get(0).setPhysicsRotation(world.getStartRot());
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		this.app.getGuiNode().detachChild(carEditor);
		this.app.getGuiNode().detachChild(worldEditor);
		this.app.getGuiNode().detachChild(wheelGraphs);
	}
}
