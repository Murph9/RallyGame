package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.Car;
import car.CarBuilder;
import car.ray.RayCarControl;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class Start extends AbstractAppState {

	//TODO version number on screen somewhere
	
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
	
	public void startFast() {
		App.rally.startFast(this);
	}
	
	public void startBasic() {
		App.rally.next(this);
	}
	
	public void startAI() {
		App.rally.startAI(this);
	}
	
	public void startDemo() {
		App.rally.startDemo(this);
	}
	public void startDev() {
		App.rally.startDev(this);
	}
	public void startRace() {
		App.rally.startRace(this);
	}
	public void startCrash() {
		App.rally.startCrash(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		App.rally.bullet.setEnabled(true);
		App.rally.getStateManager().attach(world);
		
		cb = new CarBuilder();
		App.rally.getStateManager().attach(cb);
		
		camera = new BasicCamera("Cam -start", App.rally.getCamera(), new Vector3f(-70,50,0), new Vector3f(20,1,0)); 
		
		App.rally.getStateManager().attach(camera);
		
		myWindow = new Container();
		App.rally.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		
        myWindow.addChild(new Label("Main Menu"));
        Button startFast = myWindow.addChild(new Button("Start Fast"));
        startFast.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startFast();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button start = myWindow.addChild(new Button("Start"));
        start.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startBasic();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button startAI = myWindow.addChild(new Button("StartAI"));
        startAI.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startAI();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button startDemo = myWindow.addChild(new Button("Start Demo"));
        startDemo.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startDemo();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button startCrash = myWindow.addChild(new Button("Start Crash"));
        startCrash.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startCrash();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button startRace = myWindow.addChild(new Button("Start Race"));
        startRace.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startRace();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button startDev = myWindow.addChild(new Button("Start Dev"));
        startDev.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    startDev();
                    App.rally.getGuiNode().detachChild(myWindow);
                }
            });
        Button exit = myWindow.addChild(new Button("Exit"));
        exit.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    App.rally.stop();
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
				//camera.setLocalTranslation(q.mult(start).add(car.getPhysicsLocation()));
				//camera.lookAt(car.getPhysicsLocation(), Vector3f.UNIT_Y);
			}
		}
	}
	
	public void cleanup() {
		App.rally.getStateManager().detach(camera);
		camera = null;
		
		App.rally.getRootNode().detachChild(myWindow);
		myWindow = null;
		
		cb.removePlayer(0);
		App.rally.getStateManager().detach(cb);
		cb = null;
		
		App.rally.getStateManager().detach(world);
		world = null;
	}
}
