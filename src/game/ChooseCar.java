package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
import car.PowerCurveGraph;
import car.data.Car;
import car.ray.CarDataConst;
import car.ray.RayCarControl;
import helper.H;
import helper.H.Duo;
import helper.Log;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

public class ChooseCar extends AbstractAppState {

	private World world;
	private StaticWorld worldType;

	private CarBuilder cb;
	private Car car;
	private Label label;
	
	private float rotation; 
	
	private BasicCamera camera;
	
	private PowerCurveGraph graph;

	public ChooseCar() {
		worldType = StaticWorld.garage2;
		car = Car.values()[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		App.rally.bullet.setEnabled(true);
		
		//init player
		Vector3f start = worldType.start;
		Matrix3f dir = new Matrix3f();

		world = new StaticWorldBuilder(worldType);
		App.rally.getStateManager().attach(world);
		
		cb = new CarBuilder();
		app.getStateManager().attach(cb);
		cb.addCar(0, car, start, dir, true, null);
		//TODO remove smoke

		//make camera
		camera = new BasicCamera("Camera", App.rally.getCamera(), new Vector3f(0,3,7), new Vector3f(0,1.2f, 0));
		App.rally.getStateManager().attach(camera);
		
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

                    cb.removeCar(0);
    				cb.addCar(0, car, worldType.start, new Matrix3f(), true, null);
    				
    				String carinfotext = getCarInfoText(car);
    				label.setText(carinfotext);
    				
    				graph.updateMyPhysicsVehicle(cb.get(0));
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
        
        Vector3f size = new Vector3f(400,400,0);
		graph = new PowerCurveGraph(this.cb.get(0), size);
		graph.setLocalTranslation(H.screenBottomRight().subtract(size.add(5,-25,0)));
		App.rally.getGuiNode().attachChild(graph);
	}


	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);

		RayCarControl car = cb.get(0);
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
		
		//code to rotate slowly
		rotation += FastMath.DEG_TO_RAD*tpf;
		Quaternion q = new Quaternion();
		q.fromAngleAxis(rotation, Vector3f.UNIT_Y);

//TODO removed so you can drive yourself		
//		car.setPhysicsRotation(q);
	}

	private String getCarInfoText(Car car) {
		CarDataConst cd = car.get();
		String out = "Name: "+ car.name() + "\n";
		Duo<Float, Float> data = cd.getMaxPower();
		out += "Max Power: " + data.first + "kW? @ " + data.second + " rpm \n";
		out += "Weight: "+ cd.mass + "kg\n";
		out += "Drag(linear): " + cd.areo_drag + "(" + cd.areo_lineardrag + ")\n";
		out += "Redline: "+ cd.e_redline +"\n";
		return out;
	}


	public void cleanup() {
		App.rally.getStateManager().detach(cb);
		cb = null;
		
		App.rally.getStateManager().detach(world);
		world = null;
		
		App.rally.getStateManager().detach(camera);
		camera = null;
		
		App.rally.getGuiNode().detachChild(graph);
		graph = null;
	}

	/////////////////////////////
	//UI stuff
	public void chooseCar() {
		if (car == null) { Log.p("no return value for ChooseCar()"); };
		App.rally.next(this);
	}
	public Car getCarType() {
		return car;
	}
}
