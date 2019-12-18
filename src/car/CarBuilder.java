package car;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.data.CarDataLoader;
import car.data.CarModelData.CarPart;
import car.data.Car;
import car.data.CarDataAdjuster;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import effects.LoadModelWrapper;
import helper.H;
import helper.Log;

public class CarBuilder extends BaseAppState {

	private final CarDataLoader loader;
	private final List<RayCarControl> cars;
	private final Node rootNode;

	public CarBuilder() {
		cars = new LinkedList<>();
		rootNode = new Node("Car Builder Root");
		loader = new CarDataLoader();
	}
	
	@Override
	public void initialize(Application app) {
		Log.p("CarBuilder init");

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
		}
	}
	
	/** Load car data from yaml file */
	public CarDataConst loadData(Car car) {
		return loadData(car, null);
	}
	/** Load car data from yaml file, with a value adjuster */
	public CarDataConst loadData(Car car, CarDataAdjuster adjuster) {
		Vector3f grav = new Vector3f();
		getState(BulletAppState.class).getPhysicsSpace().getGravity(grav);

		CarDataConst data = loader.get(getApplication().getAssetManager(), car, grav);
		if (adjuster != null) {
			adjuster.applyAll(data);
		}
		return data;
	}

	/**Creates the vehicle and loads it into the world. 
	 * Please use the CarDataConst version.
	*/
	public RayCarControl addCar(Car car, Vector3f start, Matrix3f rot, boolean aPlayer) {
		return addCar(loadData(car), start, rot, aPlayer);
	}
	/** Creates the vehicle and loads it into the world. */
	public RayCarControl addCar(CarDataConst carData, Vector3f start, Matrix3f rot, boolean aPlayer) {
		try {
			if (!isInitialized())
				throw new Exception(getClass().getName() + " hasn't been initialised");
		} catch (Exception e) {
			e.printStackTrace();
			//this is a runtime exception that should have been fixed by the only dev
			return null;
		}
		
		AssetManager am = getApplication().getAssetManager();
		Node carModel = LoadModelWrapper.create(am, carData.carModel, carData.baseColor);
		
		//fetch the single collision shape
		Spatial collisionShape = H.removeNamedSpatial(carModel, CarPart.Collision.getPartName());
		CollisionShape colShape = null;
		try {
			Geometry collisionGeometry = null;
			if (collisionGeometry instanceof Geometry) {
				collisionGeometry = (Geometry)collisionShape;
			} else { // Node
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
		Node carNode = new Node("Car:" + carData);
		RayCarControl carControl = new RayCarControl((SimpleApplication)getApplication(), colShape, carData, carNode);
		
		carNode.attachChild(carModel);
		carNode.setLocalTranslation(start);
		carNode.setLocalRotation(rot);

		rootNode.attachChild(carNode);
		
		carControl.setPhysicsLocation(start);
		carControl.setPhysicsRotation(rot);
		
		if (aPlayer) {
			//players get the keyboard and sound
			carControl.attachControls(getApplication().getInputManager());
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
