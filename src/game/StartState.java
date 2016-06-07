package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.Car;
import car.CarBuilder;
import car.CarData;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class StartState extends AbstractAppState implements ScreenController {

	private BulletAppState bulletAppState;
	private StaticWorld world;
	private CarBuilder cb;
	private static CarData car;
	
	private MyCamera camNode;
	
	public StartState() {
		world = StaticWorld.garage2;
		
		Car[] c = Car.values();
		car = c[FastMath.rand.nextInt(c.length)].get();
		
		StaticWorld[] w = StaticWorld.values();
		world = w[FastMath.rand.nextInt(w.length)];
	}
	
	public void startFast() {
		App.rally.startFast();
	}
	
	public void startBasic() {
		App.rally.next(this);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
		
		StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), world, App.rally.sky.ifShadow);
		
		cb = new CarBuilder();
		cb.addCar(getPhysicsSpace(), 0, car, world.start, Matrix3f.IDENTITY, true);
		
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(0, 3, 7);
		camNode.lookAt(new Vector3f(0,1.2f,0), new Vector3f(0,1,0));
	}
	
	private PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void cleanup() {
		StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), world);
		cb.cleanup();
		
		App.rally.getRootNode().detachChild(camNode);
	}

	
	@Override
	public void bind(Nifty arg0, Screen arg1) {
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
