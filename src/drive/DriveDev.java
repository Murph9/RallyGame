package drive;

import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;
import world.WorldEditor;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;

import car.*;
import car.data.Car;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import game.IDriveDone;
import helper.Screen;


public class DriveDev extends DriveBase {

	private CarEditor carEditor;
	private WorldEditor worldEditor;
	private TractionCurveGraph wheelGraphs;
	
	public DriveDev(IDriveDone done) {
    	super(done, Car.Rally, new StaticWorldBuilder(StaticWorld.track2));
    }
	
	@Override
	public void initialize(Application app) {
        super.initialize(app);
        
        Screen screen = new Screen(app.getContext().getSettings());
		
    	//init input gui
		carEditor = new CarEditor(app.getInputManager(), this.cb.get(0), (data) -> { reloadCar(data);}, (car) -> { return resetCar(car); });
		carEditor.setLocalTranslation(screen.topLeft().add(0, -20, 0));
		((SimpleApplication)app).getGuiNode().attachChild(carEditor);
		
		worldEditor = new WorldEditor((a) -> { reloadWorld(a); });
		worldEditor.setLocalTranslation(screen.topRight().add(-worldEditor.width, 0, 0));
		((SimpleApplication)app).getGuiNode().attachChild(worldEditor);
		
		Vector3f size = new Vector3f(400,400,0);
		wheelGraphs = new TractionCurveGraph(app.getAssetManager(), this.cb.get(0).getCarData(), size);
		wheelGraphs.setLocalTranslation(screen.bottomRight().subtract(size.add(-5,-25,0)));
		((SimpleApplication)app).getGuiNode().attachChild(wheelGraphs);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		
		wheelGraphs.update(tpf);
	}

	private void reloadCar(CarDataConst data) {
		RayCarControl car = this.cb.get(0);
		car.setCarData(data);
		wheelGraphs.setCarDataConst(data);
	}
	private RayCarControl resetCar(Car car) {
		this.reInitPlayerCar(car);
		return this.cb.get(0);
	}
	
	public void reloadWorld(World world) {
		//reload new world
		getStateManager().detach(this.world);
		this.world = world;
		getStateManager().attach(this.world);
		
		//reset car
		this.cb.get(0).setPhysicsLocation(world.getStartPos());
		this.cb.get(0).setPhysicsRotation(world.getStartRot());
		this.cb.get(0).setLinearVelocity(new Vector3f(0, 0, 0));
		this.cb.get(0).setAngularVelocity(new Vector3f(0, 0, 0));
	}
	
	@Override
	public void cleanup(Application app) {
		((SimpleApplication)app).getGuiNode().detachChild(carEditor);
		((SimpleApplication)app).getGuiNode().detachChild(worldEditor);
		((SimpleApplication)app).getGuiNode().detachChild(wheelGraphs);

		super.cleanup(app);
	}
}
