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
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class ChooseCar extends AbstractAppState implements ScreenController {

	private BulletAppState bulletAppState;

	private DirectionalLight sun;
	private AmbientLight al;
	
	private StaticWorld world;
	private final boolean ifShadow = true;

	private CarBuilder cb;
	static CarData car; //current car

	private DropDown<String> dropdown;
	private Element info;
	private MyCamera camNode;

	private HashMap<String, CarData> carset;
	
	private DirectionalLightShadowRenderer dlsr;

	public ChooseCar() {
		world = StaticWorld.garage;

		carset = new HashMap<>();
		Car[] a = Car.values();
		car = a[0].get(); //hardcoded start yay TODO setting
		for (Car c: a) {
			carset.put(c.name(), c.get());
		}
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);

		dropdown = H.findDropDownControl(App.nifty.getCurrentScreen(), "cardropdown");
		info = App.nifty.getCurrentScreen().findElementByName("carinfo");
		info.getRenderer(TextRenderer.class).setLineWrapping(true);
		
		createWorld();
		buildPlayers();
		initCamera();
	}

	private void createWorld() {
		StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), world, ifShadow);

		//lights
		al = new AmbientLight();
		al.setColor(ColorRGBA.Blue.mult(0.3f));
		App.rally.getRootNode().addLight(al);

		sun = new DirectionalLight();
		sun.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		App.rally.getRootNode().addLight(sun);

		App.rally.getViewPort().setBackgroundColor(ColorRGBA.Blue);
		
		if (ifShadow) {
	        //Shadows and lights
	        dlsr = new DirectionalLightShadowRenderer(App.rally.getAssetManager(), 2048, 3);
	        dlsr.setLight(sun);
	        dlsr.setLambda(0.55f);
	        dlsr.setShadowIntensity(0.6f);
	        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
	        App.rally.getViewPort().addProcessor(dlsr);
		}
	}

	private void buildPlayers() {
		Vector3f start = world.start;
		Matrix3f dir = new Matrix3f();

		cb = new CarBuilder();
		cb.addPlayer(getPhysicsSpace(), 0, car, start, dir, true);
	}

	private void initCamera() {
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(5,4,5);
		camNode.lookAt(new Vector3f(0,1,0), new Vector3f(0,1,0));
		
		App.rally.getRootNode().attachChild(camNode);
	}


	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);

		if (dropdown != null) {
			CarData c = carset.get(dropdown.getSelection());
			if (c != null && c != car) {
				cb.removePlayer(this.getPhysicsSpace(), 0);
				cb.addPlayer(getPhysicsSpace(), 0, c, world.start, new Matrix3f(), true);
				
				car = c;
				String carinfotext = getCarInfoText(dropdown.getSelection(), car); 
				info.getRenderer(TextRenderer.class).setText(carinfotext);
			}
		}

		camNode.myUpdate(tpf);

		MyPhysicsVehicle car = cb.get(0);
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
	}

	private String getCarInfoText(String name, CarData car) {
		String out = "Name: "+ name + "\n";
		out += "Max Power: "+car.getMaxPower()+"\n";
		out += "Weight: "+car.mass*9.81f + "\n";
		out += "Drag(linear): " + car.DRAG + "("+car.RESISTANCE+")\n";
		out += "Redline: "+ car.redline +"\n";
		
		return out;
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}


	public void cleanup() {
		//TODO i know theres got to be something.
		StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), world);

		App.rally.getViewPort().removeProcessor(dlsr);
		Node root = App.rally.getRootNode();
		root.detachAllChildren();
		root.removeLight(sun);
		root.removeLight(al);

		cb.cleanup();
	}

	/////////////////////////////
	//UI stuff
	public void chooseCar() {
		if (car == null) { /*not really sure*/ };
		App.rally.next(this);
	}
	public CarData getCarData() {
		return car;
	}
	
	@Override
	public void bind(Nifty arg0, Screen arg1) {
		DropDown<String> dropdown = H.findDropDownControl(arg1, "cardropdown");
		if (dropdown != null) {
			for (String s : carset.keySet()) {
				dropdown.addItem(s);
			}
			dropdown.selectItemByIndex(0);
		}
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
