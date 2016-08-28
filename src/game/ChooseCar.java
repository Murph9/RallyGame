package game;

import java.util.HashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import car.Car;
import car.CarBuilder;
import car.CarData;
import car.MyPhysicsVehicle;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import game.H.Pair;
import world.StaticWorld;
import world.StaticWorldHelper;

public class ChooseCar extends AbstractAppState implements ScreenController {

	private static BulletAppState bulletAppState;

	private StaticWorld world;

	private CarBuilder cb;
	static CarData car; //current car
	private float rotation; 
	
	private DropDown<String> dropdown;
	private Element info;
	private MyCamera camNode;

	private HashMap<String, CarData> carset;
	
	public ChooseCar() {
		world = StaticWorld.garage2; //good default

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
		
		//create world
		StaticWorldHelper.addStaticWorld(App.rally.getRootNode(), getPhysicsSpace(), world, App.rally.sky.ifShadow);

		//init player
		Vector3f start = world.start;
		Matrix3f dir = new Matrix3f();

		cb = new CarBuilder();
		cb.addCar(getPhysicsSpace(), 0, car, start, dir, true);

		//make camera
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(0, 3, 7);
		camNode.lookAt(new Vector3f(0,1.2f,0), new Vector3f(0,1,0));
		
		App.rally.getRootNode().attachChild(camNode);
	}


	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);

		if (dropdown != null) {
			CarData c = carset.get(dropdown.getSelection());
			if (c != null && c != car) {
				cb.removePlayer(this.getPhysicsSpace(), 0);
				cb.addCar(getPhysicsSpace(), 0, c, world.start, new Matrix3f(), true);
				
				car = c;
				String carinfotext = getCarInfoText(dropdown.getSelection(), car); 
				info.getRenderer(TextRenderer.class).setText(carinfotext);
			}
		}

		MyPhysicsVehicle car = cb.get(0);
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
		
		rotation += FastMath.DEG_TO_RAD*tpf;
		
		Quaternion q = new Quaternion();
		q.fromAngleAxis(rotation, Vector3f.UNIT_Y);
		car.setPhysicsRotation(q);
	}

	private String getCarInfoText(String name, CarData car) {
		String out = "Name: "+ name + "\n";
		Pair<Float, Float> data = car.getMaxPower();
		out += "Max Power: " + data.first + "kW? @ " + data.second + " rpm \n";
		out += "Weight: "+car.mass + "kg\n";
		out += "Drag(linear): " + car.areo_drag + "("+car.resistance(9.81f)+")\n";
		out += "Redline: "+ car.e_redline +"\n";
		
		return out;
	}

	private PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}


	public void cleanup() {
		//TODO i know theres got to be something else.
		StaticWorldHelper.removeStaticWorld(App.rally.getRootNode(),getPhysicsSpace(), world);

		App.rally.getStateManager().detach(bulletAppState);
		
		cb.cleanup();
		
		App.rally.getRootNode().detachChild(camNode);
	}

	/////////////////////////////
	//UI stuff
	public void chooseCar() {
		if (car == null) { H.p("no return value for ChooseCar()"); };
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
