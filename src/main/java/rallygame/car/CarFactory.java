package rallygame.car;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.car.data.CarModelData.CarPart;
import rallygame.car.data.CarDataConst;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.RayCarPowered;
import rallygame.helper.Geo;

public class CarFactory {

    public static RayCarControl create(SimpleApplication app, CarDataConst carData, boolean aPlayer, float angularDampening) {
        return create(app, carData, aPlayer, angularDampening, null);
    }

    public static RayCarControl create(SimpleApplication app, CarDataConst carData, boolean aPlayer, float angularDampening, RayCarControl copy) {
        AssetManager am = app.getAssetManager();
		// pre load car model so we can remove the collision object before materials are set
        Spatial initialCarModel = am.loadModel(carData.carModel);
        // remove the collision shape visual and use it separately for physics
        Spatial collisionShape = Geo.removeNamedSpatial((Node)initialCarModel, CarPart.Collision.getPartName());
        
        //init car and physics class
        var rayCar = createRayCar(collisionShape, carData);
        RayCarControl carControl = null;
        if (copy == null) {
            carControl = new RayCarControl(app, initialCarModel, rayCar);
        } else {
            carControl = copy;
            rayCar.loadData(copy.getRayCar());
            carControl.changeRayCar(initialCarModel, rayCar);
        }
        carControl.getPhysicsObject().setCollisionGroup(CarManager.DefaultCollisionGroups);
        carControl.getPhysicsObject().setCollideWithGroups(CarManager.DefaultCollisionGroups);
        
        // a fake angular rotational reducer, very important for driving feel
        carControl.getPhysicsObject().setAngularDamping(angularDampening);
        
        if (aPlayer) {
            // players get the keyboard and sound
            if (copy == null) {
                carControl.attachControls(app.getInputManager());
            }
            carControl.giveSound(new AudioNode(am, "sounds/engine.wav", AudioData.DataType.Buffer));
        }
        
        return carControl;
    }

    private static RayCarPowered createRayCar(Spatial collisionShape, CarDataConst carData) {
        
        Geometry collisionGeometry = null;
        if (collisionGeometry instanceof Geometry) {
            collisionGeometry = (Geometry) collisionShape;
            var col = new HullCollisionShape(collisionGeometry.getMesh());
            return new RayCarPowered(col, carData);
        } else { // Node
            var col = new CompoundCollisionShape();
            for (var g : Geo.getGeomList(collisionShape)) {
                var hullCol = new HullCollisionShape(g.getMesh());
                col.addChildShape(hullCol, Vector3f.ZERO);
            }
            
            return new RayCarPowered(col, carData);
        }
    }
}
