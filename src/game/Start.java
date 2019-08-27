package game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
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
import world.wp.DefaultBuilder;
import world.wp.WP.DynamicType;

public class Start extends AbstractAppState {

	//TODO version number on screen somewhere

	//TODO maybe add some other cars to preview

	private SimpleApplication app;
	private DefaultBuilder world;

	private CarBuilder cb;
	private Car carType;
	
	private StartCamera camera;
	private Container myWindow;
	
	public Start() {
		super();

		carType = Car.Runner;
		world = DynamicType.Valley.getBuilder();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (SimpleApplication) app;
		App myapp = ((App) app);

		stateManager.attach(world);
		
		// build player
		this.cb = new CarBuilder(myapp);
		stateManager.attach(cb);
		RayCarControl car = cb.addCar(0, this.carType, world.getStartPos(), world.getStartRot(), true, null);

		//attach basic ai, for the view
		DriveAlongAI ai = new DriveAlongAI(car, (vec) -> world.getNextPieceClosestTo(vec));
		ai.setMaxSpeed(27.7778f); // 27.7 = 100 km/h
		car.attachAI(ai, true);
		
		this.camera = new StartCamera("start camera", app.getCamera(), car);
		stateManager.attach(this.camera);
		
		myapp.setPhysicsSpaceEnabled(true);

		//UI
		myWindow = new Container();
		((SimpleApplication)app).getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		
        myWindow.addChild(new Label("Main Menu"));
		
		Map<String, Runnable> buttonActions = generateButtonMappings(myapp);
		
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
	
	public void update (float tpf) {
		if (!isEnabled() || !isInitialized())
			return;
		
		super.update(tpf);
	}
	
	public void cleanup() {
		((App) app).setPhysicsSpaceEnabled(false);

		app.getStateManager().detach(camera);
		camera = null;
		
		app.getRootNode().detachChild(myWindow);
		myWindow = null;
		
		cb.removeCar(0);
		app.getStateManager().detach(cb);
		cb = null;
		
		app.getStateManager().detach(world);
		world = null;

		this.app = null;
		super.cleanup();
	}

	private Map<String, Runnable> generateButtonMappings(App myapp) {

		Map<String, Runnable> buttonActions = new LinkedHashMap<String, Runnable>();
		buttonActions.put("Start Fast", () -> { myapp.startFast(this); });
		buttonActions.put("Start", () -> { myapp.next(this); });
		buttonActions.put("Start AI", () -> { myapp.startAI(this); });
		buttonActions.put("Start Crash", () -> { myapp.startCrash(this); });
		buttonActions.put("Start Getaway", () -> { myapp.startMainRoad(this); });
		buttonActions.put("Start Race", () -> { myapp.startRace(this); });
		buttonActions.put("Start Dev", () -> { myapp.startDev(this); });
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
