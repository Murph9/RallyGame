package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.Car;
import car.CarBuilder;
import car.CarCamera;
import car.CarData;
import car.MyPhysicsVehicle;
import world.StaticWorld;
import world.StaticWorldHelper;

public class Start extends AbstractAppState {

	private BulletAppState bulletAppState;
	private StaticWorld world;
	private CarBuilder cb;
	private MyPhysicsVehicle car;
	private static CarData carData;
	
	private CarCamera camera;
	private final Vector3f start = new Vector3f(0,10,15);
	private final float speed = 4;
	private float rotation;
	
	
	public Start() {
		world = StaticWorld.garage2;
		
		Car[] c = Car.values();
		carData = c[FastMath.rand.nextInt(c.length)].get();
		
		StaticWorld[] w = StaticWorld.values();
		world = w[FastMath.rand.nextInt(w.length)];
	}
	
	public void startFast() {
		App.rally.startFast();
	}
	
	public void startBasic() {
		App.rally.next(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
		
		StaticWorldHelper.addStaticWorld(App.rally.getRootNode(), getPhysicsSpace(), world, App.rally.sky.ifShadow);
		
		cb = new CarBuilder();
		cb.sound(false);
		car = cb.addCar(getPhysicsSpace(), 0, carData, world.start, Matrix3f.IDENTITY, false);
		
		camera = new CarCamera("Cam Node - Start", App.rally.getCamera(), cb.get(0));
		camera.setLocalTranslation(start);
		App.rally.getRootNode().attachChild(camera);
		
		Container myWindow = new Container();
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
	}
	
	private PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void update (float tpf) {
		super.update(tpf);
		
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
		
		if (this.isEnabled()) {
			rotation += (FastMath.DEG_TO_RAD*tpf*speed) % FastMath.PI;
			
			Quaternion q = new Quaternion();
			q.fromAngleAxis(rotation, Vector3f.UNIT_Y);
			camera.setLocalTranslation(q.mult(start).add(car.getPhysicsLocation()));
			camera.lookAt(car.getPhysicsLocation(), Vector3f.UNIT_Y);
		}
	}
	
	public void cleanup() {
		StaticWorldHelper.removeStaticWorld(App.rally.getRootNode(), getPhysicsSpace(), world);
		cb.cleanup();
		
		App.rally.getRootNode().detachChild(camera);
	}
}
