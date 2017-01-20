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
import car.CarData;
import car.MyPhysicsVehicle;
import game.H.Duo;
import world.StaticWorld;
import world.StaticWorldHelper;

public class ChooseCar extends AbstractAppState {

	private static BulletAppState bulletAppState;

	private StaticWorld world;

	private CarBuilder cb;
	private Car car;
	private Label label;
	
	private float rotation; 
	
	private BasicCamera camera;

	public ChooseCar() {
		world = StaticWorld.garage2; //good default
		car = Car.values()[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);

		//create world
		StaticWorldHelper.addStaticWorld(App.rally.getRootNode(), getPhysicsSpace(), world, App.rally.sky.ifShadow);

		//init player
		Vector3f start = world.start;
		Matrix3f dir = new Matrix3f();

		cb = new CarBuilder();
		cb.sound(false);
		cb.addCar(getPhysicsSpace(), 0, car.get(), start, dir, true);

		//make camera
		camera = new BasicCamera("Camera", App.rally.getCamera(), new Vector3f(0,3,7), new Vector3f(0,1.2f, 0));
		App.rally.getRootNode().attachChild(camera);
		
		//init gui
		//info window first so the event listeners can delete it
		Container infoWindow = new Container();
        App.rally.getGuiNode().attachChild(infoWindow);
        infoWindow.setLocalTranslation(H.screenTopLeft());
		label = new Label(getCarInfoText(car));
        infoWindow.addChild(label, 0, 0);
		
		Container myWindow = new Container();
		App.rally.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		myWindow.addChild(new Label("Choose Car"), 0, 0);
		int i = 0;
        for (Car c: Car.values()) {
        	Button carB = myWindow.addChild(new Button(c.name()), 1, i);
        	carB.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    car = c;

                    cb.removePlayer(getPhysicsSpace(), 0);
    				cb.addCar(getPhysicsSpace(), 0, car.get(), world.start, new Matrix3f(), true);
    				
    				String carinfotext = getCarInfoText(car);
    				label.setText(carinfotext);
                }
            });
        	i++;
        }
        
        Button select = myWindow.addChild(new Button("Choose"));
        select.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	chooseCar();
            	App.rally.getGuiNode().detachChild(myWindow);
            	App.rally.getGuiNode().detachChild(infoWindow);
            }
        });
	}


	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);

		MyPhysicsVehicle car = cb.get(0);
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
		
		rotation += FastMath.DEG_TO_RAD*tpf;
		
		Quaternion q = new Quaternion();
		q.fromAngleAxis(rotation, Vector3f.UNIT_Y);
		car.setPhysicsRotation(q);
	}

	private String getCarInfoText(Car car) {
		CarData cd = car.get();
		String out = "Name: "+ car.name() + "\n";
		Duo<Float, Float> data = cd.getMaxPower();
		out += "Max Power: " + data.first + "kW? @ " + data.second + " rpm \n";
		out += "Weight: "+ cd.mass + "kg\n";
		out += "Drag(linear): " + cd.areo_drag + "(" + cd.resistance(9.81f) + ")\n";
		out += "Redline: "+ cd.e_redline +"\n";
		
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
		
		App.rally.getRootNode().detachChild(camera);
	}

	/////////////////////////////
	//UI stuff
	public void chooseCar() {
		if (car == null) { H.p("no return value for ChooseCar()"); };
		App.rally.next(this);
	}
	public CarData getCarData() {
		return car.get();
	}
}
