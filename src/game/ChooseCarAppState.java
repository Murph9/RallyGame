package game;

import java.util.HashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class ChooseCarAppState extends AbstractAppState implements ScreenController {

	private BulletAppState bulletAppState;

	private DirectionalLight sun;

	private StaticWorld world;
	private final boolean ifShadow = true;

	private CarBuilder cb;
	private static CarData car; //current car

	private DropDown<String> dropdown;
	private MyCamera camNode;

	private HashMap<String, CarData> carset;

	public ChooseCarAppState() {
		world = StaticWorld.garage;

		carset = new HashMap<>();
		Car[] a = Car.values();
		car = a[0].get(); //hardcoded start yay
		for (int v = 0; v < a.length; v++) {
			carset.put(a[v].name(), a[v].get());
		}
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);

		dropdown = findDropDownControl(App.nifty.getCurrentScreen(), "dropdown");

		createWorld();
		buildPlayers();
		initCamera();
	}

	private void createWorld() {
		StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), world, ifShadow);

		//lights
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.Blue.mult(0.3f));
		App.rally.getRootNode().addLight(al);

		sun = new DirectionalLight();
		sun.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		App.rally.getRootNode().addLight(sun);

		App.rally.getViewPort().setBackgroundColor(ColorRGBA.Blue);
	}

	private void buildPlayers() {
		Vector3f start = world.start;
		Matrix3f dir = new Matrix3f();

		cb = new CarBuilder();
		cb.addPlayer(getPhysicsSpace(), 0, car, start, dir, true);
	}

	private void initCamera() {
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		App.rally.getRootNode().attachChild(camNode);
	}


	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);

		if (dropdown != null) {
			CarData c = carset.get(dropdown.getSelection());
			if (c != car) {
				cb.removePlayer(this.getPhysicsSpace(), 0);
				cb.addPlayer(getPhysicsSpace(), 0, c, world.start, new Matrix3f(), true);
				
				car = c;
			}
		}

		camNode.myUpdate(tpf);

		MyPhysicsVehicle car = cb.get(0);
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}


	public void cleanup() {
		//TODO a lot:
		//lights, car, plane..
		Node root = App.rally.getRootNode();
		root.detachAllChildren();
		root.removeLight(sun);

		cb.cleanup();
	}

	/////////////////////////////
	//UI stuff
	public void choose() {
		App.rally.startDrive(car);
		App.nifty.gotoScreen("noop");
	}

	@Override
	public void bind(Nifty arg0, Screen arg1) {
		DropDown<String> dropdown = findDropDownControl(arg1, "dropdown");
		if (dropdown != null) {
			//TODO get list of car types
			for (String s : carset.keySet()) {
				dropdown.addItem(s);
			}
			dropdown.selectItemByIndex(0);
		}
	}
	private <T> DropDown<T> findDropDownControl(Screen screen, final String id) {
		return screen.findNiftyControl(id, DropDown.class);
	}

	public void onEndScreen() { }
	public void onStartScreen() { }


}
