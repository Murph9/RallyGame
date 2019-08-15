package game;

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
import car.ai.FollowWorldAI;
import car.data.Car;
import car.ray.RayCarControl;
import world.World;
import world.wp.DefaultBuilder;
import world.wp.WP.DynamicType;

public class Start extends AbstractAppState {

	//TODO version number on screen somewhere

	//TODO maybe add some other cars

	private SimpleApplication app;
	private World world;

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
		FollowWorldAI ai = new FollowWorldAI(car, (DefaultBuilder) world);
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
        
        String[] names = {
        		"Start Fast",
        		"Start",
        		"Start AI",
        		"Start Crash",
        		"Start Getaway",
        		"Start Race",
        		"Start Dev",
		};
		
        Runnable[] methods = {
        		() -> { myapp.startFast(this); },
        		() -> { myapp.next(this); },
        		() -> { myapp.startAI(this); },
        		() -> { myapp.startCrash(this); },
        		() -> { myapp.startMainRoad(this); },
        		() -> { myapp.startRace(this); },
        		() -> { myapp.startDev(this); }
		};
        
        for (int i = 0; i < methods.length; i++) {
        	final Runnable method = methods[i];
	        Button startFast = myWindow.addChild(new Button(names[i]));
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
