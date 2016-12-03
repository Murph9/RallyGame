package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.Car;
import car.CarBuilder;
import car.CarData;
import world.StaticWorld;
import world.StaticWorldHelper;

public class Start extends AbstractAppState {

	private BulletAppState bulletAppState;
	private StaticWorld world;
	private CarBuilder cb;
	private static CarData car;
	
	private MyCamera camNode;
	
	public Start() {
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

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
		
		StaticWorldHelper.addStaticWorld(App.rally.getRootNode(), getPhysicsSpace(), world, App.rally.sky.ifShadow);
		
		cb = new CarBuilder();
		cb.addCar(getPhysicsSpace(), 0, car, world.start, Matrix3f.IDENTITY, false);
		
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(0, 3, 7);
		camNode.lookAt(new Vector3f(0,1.2f,0), new Vector3f(0,1,0));
		
		Container myWindow = new Container();
		App.rally.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		
		// Add some elements to it
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
	
	public void cleanup() {
		StaticWorldHelper.removeStaticWorld(App.rally.getRootNode(), getPhysicsSpace(), world);
		cb.cleanup();
		
		App.rally.getRootNode().detachChild(camNode);
	}
}
