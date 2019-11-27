package car;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.data.Car;
import car.data.CarDataLoader;
import car.ray.CarDataConst;
import car.ray.RayCar.GripHelper;

public class CarStatsUI extends Container {

    public CarStatsUI(AssetManager am, Car car) {
        this(am, car, new Vector3f(0, -9.81f, 0)); //TODO: hardcoded gravity
    }

    public CarStatsUI(AssetManager am, Car car, Vector3f gravity) {
        addChild(new Label("Stats for car: " + car.getCarName()));

        CarDataLoader loader = new CarDataLoader();
        CarDataConst carData = loader.get(am, car, gravity);

        //TODO add more logical things
        addChild(new Label("MaxPower: " + carData.getMaxPower()));
        addChild(new Label("Braking: " + carData.brakeMaxTorque));
        addChild(new Label("Mass: " + carData.mass));
        addChild(new Label("Drive front: " + carData.driveFront));
        addChild(new Label("Drive rear: " + carData.driveRear));
        addChild(new Label("Has nitro: " + carData.nitro_on));
        addChild(new Label("Wheelbase length: " + carData.wheelOffset[0].z * 2));
        addChild(new Label("Downforce const: " + carData.areo_downforce));
        addChild(new Label("Gear change speed: " + carData.auto_changeTime));
        addChild(new Label("Max rpm: " + carData.e_redline));

        float bestLatForce = GripHelper.calcMaxLoad(carData.wheelData[0].pjk_lat);
        addChild(new Label("Cornering ability: " + carData.mass * 10 / bestLatForce));
    }
}