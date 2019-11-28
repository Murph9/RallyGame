package game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
import car.ai.DriveAlongAI;
import car.data.Car;
import car.ray.RayCarControl;
import drive.IFlow;
import world.wp.DefaultBuilder;
import world.wp.WP.DynamicType;

public class Start extends BaseAppState {

	//TODO version number on screen somewhere

	//TODO maybe add some other cars to preview

	private final IFlow flow;

	private DefaultBuilder world;

	private CarBuilder cb;
	private Car carType;
	
	private StartCamera camera;
	private Container myWindow;
	
	public Start(IFlow flow) {
		super();
		this.flow = flow;

		carType = Car.Runner;
		world = DynamicType.Valley.getBuilder();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		App myapp = ((App) app);

		getStateManager().attach(world);
		
		// build player
		this.cb = getState(CarBuilder.class);
		RayCarControl car = cb.addCar(this.carType, world.getStartPos(), world.getStartRot(), true);

		//attach basic ai, for the view
		DriveAlongAI ai = new DriveAlongAI(car, (vec) -> world.getNextPieceClosestTo(vec));
		ai.setMaxSpeed(27.7778f); // 27.7 = 100 km/h
		car.attachAI(ai, true);
		
		this.camera = new StartCamera("start camera", app.getCamera(), car);
		getStateManager().attach(this.camera);
		
		getState(BulletAppState.class).setEnabled(true);

		//UI
		myWindow = new Container();
		myapp.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		
        myWindow.addChild(new Label("Main Menu"));
		
		Map<String, Runnable> buttonActions = generateButtonMappings(flow);
		
		for (Entry<String, Runnable> action: buttonActions.entrySet()) {
			final Runnable method = action.getValue();
			Button startFast = myWindow.addChild(new Button(action.getKey()));
			startFast.addClickCommands(source -> {
				method.run();
				myapp.getGuiNode().detachChild(myWindow);
			});
		}
        
        Button exit = myWindow.addChild(new Button("Exit"));
        exit.addClickCommands(source -> myapp.stop());
	}
	
	public void update(float tpf) {
		if (!isEnabled() || !isInitialized())
			return;
		
		super.update(tpf);
	}
	
	@Override
	protected void onEnable() {
		this.cb.setEnabled(true);
	}
	@Override
	protected void onDisable() {
		this.cb.setEnabled(false);
	}

	@Override
	public void cleanup(Application app) {
		getState(BulletAppState.class).setEnabled(false);

		getStateManager().detach(camera);
		camera = null;
		
		myWindow.removeFromParent();
		myWindow = null;
		
		cb.removeAll();
		cb = null;
		
		getStateManager().detach(world);
		world = null;
	}

	private Map<String, Runnable> generateButtonMappings(IFlow flow) {
		Map<String, Runnable> buttonActions = new LinkedHashMap<String, Runnable>();
		for (AppFlow.StartType t: AppFlow.StartType.values()) {
			buttonActions.put(t.name(), () -> { flow.startCallback(t); });
		}
		return buttonActions;
	}

	class StartCamera extends AbstractAppState {
		// render() based camera to prevent jumpiness

		private final Camera c;
		private final RayCarControl rcc;

		private final Vector3f offset;
		private final Vector3f lookAtHeight;

		private float angle;

		public StartCamera(String name, Camera c, RayCarControl rcc) {
			super();

			this.c = c;
			this.rcc = rcc;

			this.offset = new Vector3f(0, rcc.getCarData().cam_offsetHeight, rcc.getCarData().cam_offsetLength);
			this.lookAtHeight = new Vector3f(0, rcc.getCarData().cam_lookAtHeight, 0);

			this.c.setLocation(rcc.getRootNode().getLocalTranslation().add(offset));
			this.c.lookAt(rcc.getRootNode().getLocalTranslation().add(lookAtHeight), Vector3f.UNIT_Y);
		}

		@Override
		public void update(float tpf) {
			if (!isEnabled())
				return;
			super.update(tpf);

			angle += tpf / 8;
		}

		@Override
		public void render(RenderManager rm) {
			Vector3f movedOffset = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y).mult(this.offset);
			Vector3f camPos = rcc.getRootNode().getLocalTranslation().add(movedOffset);

			this.c.setLocation(camPos);
			this.c.lookAt(rcc.getRootNode().getLocalTranslation().add(lookAtHeight), Vector3f.UNIT_Y);
		}
	}
}
