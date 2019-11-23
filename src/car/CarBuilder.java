package car;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.data.CarDataLoader;
import car.data.CarModelData.CarPart;
import car.ai.CarAI;
import car.ai.DriveAtAI;
import car.data.Car;
import car.ray.CarDataConst;
import car.ray.RayCarControl;
import effects.LoadModelWrapper;
import helper.H;
import helper.Log;

public class CarBuilder extends BaseAppState {

	private CarDataLoader loader;
	private final List<RayCarControl> cars;

	private final Node rootNode;

	public CarBuilder() {
		cars = new LinkedList<>();
		rootNode = new Node("Car Builder Root");
	}
	
	@Override
	public void initialize(Application app) {
		Log.p("CarBuilder init");

		loader = new CarDataLoader(getState(BulletAppState.class).getPhysicsSpace());
		((SimpleApplication)app).getRootNode().attachChild(rootNode);
	}
	
	@Override
	protected void onEnable() {
		_setEnabled(true);
	}
	@Override
	protected void onDisable() {
		_setEnabled(false);
	}
	private void _setEnabled(boolean state) {
		for (RayCarControl r : cars) {
			r.setEnabled(state);
			r.enableSound(state);
		}
	}
	
	public RayCarControl addCar(Car car, Vector3f start, Matrix3f rot, boolean aPlayer, BiFunction<RayCarControl, RayCarControl, CarAI> ai) {
		try {
			if (!isInitialized())
				throw new Exception(getClass().getName() + " hasn't been initialised");
		} catch (Exception e) {
			e.printStackTrace();
			//this is a runtime exception that should have been fixed by the dev
			return null;
		}
		
		Vector3f grav = new Vector3f();
		getState(BulletAppState.class).getPhysicsSpace().getGravity(grav);
		CarDataConst carData = loader.get(getApplication().getAssetManager(), car, grav);
		
		AssetManager am = getApplication().getAssetManager();
		
		Node carNode = new Node("Car:" + carData);
		Node carModel = LoadModelWrapper.create(am, carData.carModel, ColorRGBA.Magenta);
		
		//fetch the collision shape (if there is one in car model file)
		Spatial collisionShape = H.removeNamedSpatial(carModel, CarPart.Collision.getPartName());
		CollisionShape colShape = null;
		try {
			Geometry collisionGeometry = null;
			if (collisionGeometry instanceof Geometry) {
				collisionGeometry = (Geometry)collisionShape;
			} else {
				//Node
				collisionGeometry = H.getGeomList(collisionShape).get(0); //lets hope its the only one too
			}
			Mesh collisionMesh = collisionGeometry.getMesh();
			colShape = new HullCollisionShape(collisionMesh);
		} catch (Exception e) {
			Log.e("!! car type " + carData.carModel + " is missing a collision shape.");
			e.printStackTrace();
			return null; //to make it clear this failed, and you need to fix it
		}

		//init car
		RayCarControl carControl = new RayCarControl((SimpleApplication)getApplication(), colShape, carData, carNode);
		
		carNode.attachChild(carModel);

		rootNode.attachChild(carNode);
		carControl.setPhysicsLocation(start);
		carControl.setPhysicsRotation(rot);

		if (aPlayer) { //players get the keyboard
			carControl.attachControls(getApplication().getInputManager());
		} else {
			CarAI _ai;
			if (ai != null)
				_ai = ai.apply(carControl, get(0));
			else
				_ai = new DriveAtAI(carControl, get(0).getPhysicsObject());
			carControl.attachAI(_ai, true);
		}
		
		if (aPlayer) { //players get sound
			carControl.giveSound(new AudioNode(am, "assets/sound/engine.wav", AudioData.DataType.Buffer));
		}
		
		cars.add(carControl);
		
		return carControl;
	}

	public int removeAll() {
		for (RayCarControl car: cars) {
			rootNode.detachChild(car.getRootNode());
			car.cleanup((SimpleApplication)getApplication());
		}
		int size = cars.size();
		cars.clear();
		return size;
	}
	public void removeCar(RayCarControl car) {
		if (!cars.contains(car)) {
			try {
				throw new Exception("That car is not in my records, *shrug*.");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		
		rootNode.detachChild(car.getRootNode());
		car.cleanup((SimpleApplication)getApplication());
		cars.remove(car);
	}
	
	public void update(float tpf) {
		if (!isEnabled())
			return;
			
		for (RayCarControl rcc: cars) {
			if (rcc.isEnabled())
				rcc.update(tpf);
		}
	}

	public RayCarControl get(int a) {
		return cars.get(a);
	}
	public Collection<? extends RayCarControl> getAll() {
		return new LinkedList<>(cars);
	}
	public int getCount() {
		return cars.size();
	}

	@Override
	public void cleanup(Application app) {
		for (RayCarControl car : cars) {
			car.cleanup((SimpleApplication)app);
		}

		((SimpleApplication)app).getRootNode().detachChild(rootNode);
		Log.p("carbuilder cleanup");
	}
}
