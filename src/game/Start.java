package game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
import car.data.Car;
import car.ray.RayCarControl;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class Start extends AbstractAppState {

	//TODO version number on screen somewhere
	private SimpleApplication app;
	private StaticWorldBuilder world;
	
	private StaticWorld worldType;
	private CarBuilder cb;
	private static Car carType;
	
	private BasicCamera camera;
	private final float speed = 4;
	private float rotation;
	
	private Container myWindow;
	
	public Start() {
		Car[] c = Car.values();
		carType = c[FastMath.rand.nextInt(c.length)];
		
		StaticWorld[] w = StaticWorld.values();
		worldType = w[FastMath.rand.nextInt(w.length)];
		
		world = new StaticWorldBuilder(worldType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = ((SimpleApplication)app);

		((App)app).setPhysicsSpaceEnabled(true);
		stateManager.attach(world);
		
		cb = new CarBuilder((App)app);
		stateManager.attach(cb);
		
		camera = new BasicCamera("Cam -start", App.CUR.getCamera(), new Vector3f(-70,50,0), new Vector3f(20,1,0)); 
		stateManager.attach(camera);
		
		myWindow = new Container();
		((SimpleApplication)app).getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		
        myWindow.addChild(new Label("Main Menu"));
        
        String[] names = {
        		"Start Fast",
        		"Start",
        		"Start AI",
        		"Start Demo",
        		"Start Crash",
        		"Start Getaway",
        		"Start Race",
        		"Start Dev",
		};
		App myapp = ((App)app);
        Runnable[] methods = {
        		() -> { myapp.startFast(this); },
        		() -> { myapp.next(this); },
        		() -> { myapp.startAI(this); },
        		() -> { myapp.startDemo(this); },
        		() -> { myapp.startCrash(this); },
        		() -> { myapp.startMainRoad(this); },
        		() -> { myapp.startRace(this); },
        		() -> { myapp.startDev(this); }
		};
        
        for (int i = 0; i < methods.length; i++) {
        	final Runnable method = methods[i];
	        Button startFast = myWindow.addChild(new Button(names[i]));
	        startFast.addClickCommands(new Command<Button>() {
	                @Override
	                public void execute( Button source ) {
	                    method.run();
	                    myapp.getGuiNode().detachChild(myWindow);
	                }
	            });
        }
        
        Button exit = myWindow.addChild(new Button("Exit"));
        exit.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    myapp.stop();
                }
            });
	}
	
	public void update (float tpf) {
		if (!isEnabled() || !isInitialized())
			return;
		
		super.update(tpf);
		
		RayCarControl car = cb.get(0);
		if (car == null) {
			cb.addCar(0, carType, world.getStartPos(), world.getStartRot(), true, null);
		} else {
			Vector3f pos = car.getPhysicsLocation();
			car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
			
			if (this.isEnabled()) {
				rotation += (FastMath.DEG_TO_RAD*tpf*speed) % FastMath.PI;
				
				Quaternion q = new Quaternion();
				q.fromAngleAxis(rotation, Vector3f.UNIT_Y);
			}
		}
	}
	
	public void cleanup() {
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
	}
}
