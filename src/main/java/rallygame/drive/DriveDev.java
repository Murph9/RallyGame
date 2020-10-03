package rallygame.drive;

import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;
import rallygame.world.IWorld;
import rallygame.world.WorldEditor;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;

import rallygame.car.data.Car;
import rallygame.car.data.CarDataConst;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ui.CarEditor;
import rallygame.car.ui.TractionCurveGraph;
import rallygame.game.IDriveDone;
import rallygame.service.Screen;
import rallygame.service.Screen.HorizontalPos;
import rallygame.service.Screen.VerticalPos;


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
		carEditor = new CarEditor(app.getInputManager(), this.cm.getPlayer(), (data) -> { reloadCar(data);}, (car) -> { return resetCar(car); });
		carEditor.setLocalTranslation(screen.get(HorizontalPos.Left, VerticalPos.Top).add(0, -20, 0));
		((SimpleApplication)app).getGuiNode().attachChild(carEditor);
		
        worldEditor = new WorldEditor((a) -> { reloadWorld(a); });
		worldEditor.setLocalTranslation(screen.get(HorizontalPos.Right, VerticalPos.Top).add(-worldEditor.width, 0, 0));
		((SimpleApplication)app).getGuiNode().attachChild(worldEditor);
		
		Vector3f size = new Vector3f(400,400,0);
        wheelGraphs = new TractionCurveGraph(app.getAssetManager(), this.cm.getPlayer().getCarData(), size);
        wheelGraphs.setLocalTranslation(screen.get(HorizontalPos.Right, VerticalPos.Bottom, size));
		((SimpleApplication)app).getGuiNode().attachChild(wheelGraphs);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		
		wheelGraphs.update(tpf);
	}

	private void reloadCar(CarDataConst data) {
		this.reInitPlayerCar(data);
	}
	private RayCarControl resetCar(Car car) {
		this.reInitPlayerCar(car);
		return this.cm.getPlayer();
	}
	
	public void reloadWorld(IWorld world) {
		//reload new world
		getStateManager().detach(this.world);
		this.world = world;
		getStateManager().attach(this.world);
		
        //reset car
        this.cm.getPlayer().reset();
	}
	
	@Override
	public void cleanup(Application app) {
		((SimpleApplication)app).getGuiNode().detachChild(carEditor);
		((SimpleApplication)app).getGuiNode().detachChild(worldEditor);
		((SimpleApplication)app).getGuiNode().detachChild(wheelGraphs);

		super.cleanup(app);
	}
}
