package car;

import com.jme3.asset.AssetManager;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.data.CarDataConst;
import car.ray.RayCar.GripHelper;

public class CarStatsUI extends Container {

    public CarStatsUI(AssetManager am, CarDataConst carData) {
        addChild(new Label("Stats for car: " + carData.name));

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