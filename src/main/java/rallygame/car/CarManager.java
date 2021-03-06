package rallygame.car;

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
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.car.data.CarDataLoader;
import rallygame.car.data.CarModelData.CarPart;
import rallygame.car.data.Car;
import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import rallygame.car.data.CarDataConst;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.RayCarPowered;
import rallygame.helper.Colours;
import rallygame.helper.Geo;
import rallygame.helper.Log;

public class CarManager extends BaseAppState {

    private final CarDataLoader loader;
    private final List<RayCarControl> cars;
    private final Node rootNode;
    private final float angularDampening;

    public CarManager(float angularDampening, CarDataLoader loader) {
        this.cars = new LinkedList<>();
        this.rootNode = new Node("Car Builder Root");
        this.loader = loader;
        this.angularDampening = angularDampening;
    }
    
    @Override
    public void initialize(Application app) {
        Log.p(this.getClass().getName() + " init");

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
    public CarDataConst loadData(Car car, boolean randomColour) {
        if (randomColour)
            return loadData(car, new CarDataAdjuster(CarDataAdjustment.asFunc((data) -> {
                    data.baseColor = Colours.randomColourHSV();
                })));
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
        
        loader.reValidateData(data, getApplication().getAssetManager(), grav);
        return data;
    }

    /** Creates the vehicle and loads it into the world. */
    public RayCarControl addCar(Car car, Transform trans, boolean aPlayer) {
        return addCar(loadData(car, true), trans, aPlayer);
    }
    /** Creates the vehicle and loads it into the world. */
    public RayCarControl addCar(Car car, Vector3f start, Quaternion rot, boolean aPlayer) {
        return addCar(loadData(car, true), start, rot, aPlayer);
    }
    /** Creates the vehicle and loads it into the world. */
    public RayCarControl addCar(CarDataConst carData, Transform trans, boolean aPlayer) {
        return addCar(carData, trans.getTranslation(), trans.getRotation(), aPlayer);
    }
    /** Creates the vehicle and loads it into the world. */
    public RayCarControl addCar(CarDataConst carData, Vector3f start, Quaternion rot, boolean aPlayer) {
        if (!isInitialized())
            throw new IllegalStateException(getClass().getName() + " hasn't been initialised");
        
		AssetManager am = getApplication().getAssetManager();
		//pre load car model so we can remove the collision object before materials are set
        Spatial initialCarModel = am.loadModel(carData.carModel);
        // remove and fetch the single collision shape to use as collision
        Spatial collisionShape = Geo.removeNamedSpatial((Node)initialCarModel, CarPart.Collision.getPartName());
        
        // init physics class
        RayCarPowered rayCar = createRayCar(collisionShape, carData);

        //init car
        RayCarControl carControl = new RayCarControl((SimpleApplication)getApplication(), initialCarModel, rayCar);
        carControl.setPhysicsProperties(start, null, rot, null);
        rootNode.attachChild(carControl.getRootNode());
        
        // a fake angular rotational reducer, very important for driving feel
        carControl.getPhysicsObject().setAngularDamping(angularDampening);
        
        if (aPlayer) {
            //players get the keyboard and sound
            carControl.attachControls(getApplication().getInputManager());
            carControl.giveSound(new AudioNode(am, "sounds/engine.wav", AudioData.DataType.Buffer));
        }
        
        cars.add(carControl);
        carControl.setEnabled(this.isEnabled()); //copy carmanager enabled-ness

        carControl.update(1/60f); //call a single update to update visual and camera/input stuff
        return carControl;
    }

    private RayCarPowered createRayCar(Spatial collisionShape, CarDataConst carData) {
        
        Geometry collisionGeometry = null;
        if (collisionGeometry instanceof Geometry) {
            collisionGeometry = (Geometry) collisionShape;
        } else { // Node
            collisionGeometry = Geo.getGeomList(collisionShape).get(0); // lets hope its the only one too
        }
        // Mesh collisionMesh = collisionGeometry.getMesh();
        // collisionMesh.setStatic();
        // colShape = new HullCollisionShape(collisionMesh);

        CollisionShape boxCol = CollisionShapeFactory.createBoxShape(collisionGeometry);
        CompoundCollisionShape colShape = new CompoundCollisionShape();

        Vector3f worldTrans = collisionGeometry.getWorldTranslation();
        Vector3f worldCenter = collisionGeometry.getWorldBound().getCenter();
        Vector3f absTrans = worldTrans.subtract(worldCenter);
        colShape.addChildShape(boxCol, absTrans.negate());

        return new RayCarPowered(colShape, carData);
    }

    /** Allows changing the car a control uses entirely, an in place change. */
    public RayCarControl changeTo(RayCarControl control, CarDataConst carData) {
        if (!isInitialized())
            throw new IllegalStateException(getClass().getName() + " hasn't been initialised");
        if (control == null || !this.cars.contains(control))
            throw new IllegalArgumentException(control.getClass().getName() + " must exist");
        
        boolean aPlayer = control.getAI() == null;
        rootNode.detachChild(control.getRootNode());

        AssetManager am = getApplication().getAssetManager();
        // pre load car model so we can remove the collision object before materials are set
        Spatial initialCarModel = am.loadModel(carData.carModel);
        // remove and fetch the single collision shape to use as collision
        Spatial collisionShape = Geo.removeNamedSpatial((Node) initialCarModel, CarPart.Collision.getPartName());

        RayCarPowered rayCar = createRayCar(collisionShape, carData);
        control.changeRayCar(initialCarModel, rayCar);
        rootNode.attachChild(control.getRootNode());

        if (aPlayer) {
            // add sound back
            control.giveSound(new AudioNode(am, "sounds/engine.wav", AudioData.DataType.Buffer));
        }
        
        control.setEnabled(this.isEnabled());
        control.update(1 / 60f); // call a single update to update visual and camera/input stuff
        return control;
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
        for (RayCarControl rcc: cars) {
            rcc.update(tpf);
        }
    }
    public RayCarControl getPlayer() {
        for (RayCarControl car: cars) {
            if (car.getAI() == null) {
                return car;
            }
        }
        return null;
    }
    public RayCarControl get(int a) {
        if (cars.size() <= a) return null;
        return cars.get(a);
    }
    public Collection<RayCarControl> getAll() {
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
        Log.p(this.getClass().getName() + " cleanup");
    }
}
