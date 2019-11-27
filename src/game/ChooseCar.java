package game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Matrix3f;
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

public class ChooseCar extends BaseAppState {

	private final IChooseStuff choose;

	private World world;
	private StaticWorld worldType;

	private CarBuilder cb;
	private Car car;
	private Label label;
	
	private static final float RESET_IMPULSE = 1/60f;
	private float posReset;
	
	private BasicCamera camera;
	
	private PowerCurveGraph graph;

	public ChooseCar(IChooseStuff choose) {
		this.choose = choose;
		worldType = StaticWorld.garage2;
		car = Car.values()[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		getState(BulletAppState.class).setEnabled(true);
		
		//init player
		Vector3f start = worldType.start;
		Matrix3f dir = new Matrix3f();

		world = new StaticWorldBuilder(worldType);
		getStateManager().attach(world);
		
		cb = getState(CarBuilder.class);
		cb.addCar(car, start, dir, true, null);

		//make camera
		camera = new BasicCamera("Camera", app.getCamera(), new Vector3f(0,3,7), new Vector3f(0,1.2f, 0));
		getStateManager().attach(camera);
		
		//init gui
		//info window first so the event listeners can delete it
		Container infoWindow = new Container();
        ((SimpleApplication)app).getGuiNode().attachChild(infoWindow);
        infoWindow.setLocalTranslation(H.screenTopLeft(app.getContext().getSettings()));
		label = new Label(getCarInfoText(car));
        infoWindow.addChild(label, 0, 0);
		
		Container myWindow = new Container();
		((SimpleApplication)app).getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		myWindow.addChild(new Label("Choose Car"), 0, 0);
		int i = 0;
        for (Car c: Car.values()) {
        	Button carB = myWindow.addChild(new Button(c.name()), 1, i);
        	carB.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                    car = c;

                    cb.removeCar(cb.get(0));
    				cb.addCar(car, worldType.start, new Matrix3f(), true, null);
    				
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
            	((App)app).getGuiNode().detachChild(myWindow);
            	((App)app).getGuiNode().detachChild(infoWindow);
            }
        });
        
        Vector3f size = new Vector3f(400,400,0);
		graph = new PowerCurveGraph(app.getAssetManager(), this.cb.get(0), size);
		graph.setLocalTranslation(H.screenBottomRight(app.getContext().getSettings()).subtract(size.add(5,-25,0)));
		((SimpleApplication)app).getGuiNode().attachChild(graph);
	}


	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);

		// keep the car still enough, and also prevent high frame rates from breaking it
		posReset += tpf;
		if (posReset > RESET_IMPULSE) {
			posReset = 0;

			RayCarControl car = cb.get(0);
			Vector3f pos = car.getPhysicsLocation();
			car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
		}
	}

	private String getCarInfoText(Car car) {
		CarDataConst cd = this.cb.get(0).getCarData();
		String out = "Name: "+ car.name() + "\n";
		Duo<Float, Float> data = cd.getMaxPower();
		out += "Max Power: " + data.first + "kW? @ " + data.second + " rpm \n";
		out += "Weight: "+ cd.mass + "kg\n";
		out += "Drag(linear): " + cd.areo_drag + "(" + cd.areo_lineardrag + ")\n";
		out += "Redline: "+ cd.e_redline +"\n";
		return out;
	}

	@Override
	public void cleanup(Application app) {
		getStateManager().detach(world);
		world = null;
		
		getStateManager().detach(camera);
		camera = null;
		
		graph.removeFromParent();
		graph = null;

		cb.removeAll();
		cb = null;
	}

	@Override
	protected void onEnable() {
		this.cb.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		this.cb.setEnabled(false);
	}


	/////////////////////////////
	//UI stuff
	public void chooseCar() {
		if (car == null) { Log.p("no return value for ChooseCar()"); };
		choose.chooseCar(car);
	}
}
