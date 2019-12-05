package car.data;

import java.util.HashMap;

import java.io.InputStream;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import car.data.CarModelData;
import car.data.CarModelData.CarPart;
import car.data.Car;
import car.data.CarDataConst;
import car.data.CarSusDataConst;
import car.data.WheelDataConst;
import car.ray.RayCar;
import helper.Log;

/** Private class for CarBuilder, manages the CarDataConst file */
public class CarDataLoader { //CarDataFactory
    
    private static final String YAML_CAR_DATA = "/assets/cardata/";
    private final HashMap<Car, CarDataConst> dataMap;

    public CarDataLoader() {
        dataMap = new HashMap<>();
    }

    private CarDataConst loadFromFile(String carName) throws Exception {
        InputStream in = getClass().getResourceAsStream(YAML_CAR_DATA + carName + ".yaml");
        Yaml yaml = new Yaml(new Constructor(CarDataConst.class));
        Object yamlData = yaml.load(in);

        if (yamlData == null) {
            Log.e("Loading data for car: " + carName + " did not go well.");
            return null;
        }
        if (!(yamlData instanceof CarDataConst)) {
            Log.e("Loading data for car: " + carName + " did not go that well.");
            return null;
        }

        return (CarDataConst) yamlData;
    }

    public CarDataConst get(AssetManager am, Car car, Vector3f gravity) {
        String carName = car.getCarName();
        
        if (dataMap.containsKey(car)) {
            Log.p("Car data from cache for " + carName);
            return dataMap.get(car);
        }

        Log.p("Loading file data for car type: " + carName);

        CarDataConst data = null;
        try {
            data = loadFromFile(carName);
            data.name = carName;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("!!! car data load failed");
            System.exit(-343);
        }

        // Wheel validation
        float quarterMassForce = Math.abs(gravity.y) * data.mass / 4f;
        for (int i = 0; i < data.wheelData.length; i++) {
            CarSusDataConst sus = data.susByWheelNum(i);

            // Validate that rest suspension position is within min and max
            float minSusForce = (sus.preload_force + sus.stiffness * 0) * 1000;
            float maxSusForce = (sus.preload_force + sus.stiffness * (sus.max_travel - sus.min_travel)) * 1000;
            if (quarterMassForce < minSusForce) {
                Log.e("!! Sus min range too high: " + quarterMassForce + " < " + minSusForce + ", decrease pre-load or stiffness");
            }
            if (quarterMassForce > maxSusForce) {
                Log.e("!! Sus max range too low: " + quarterMassForce + " > " + maxSusForce + ", increase pre-load or stiffness");
            }

            WheelDataConst wheel = data.wheelData[i];
            // generate the slip* max force from the car wheel data, and validate they are 'real'
            wheel.maxLat = RayCar.GripHelper.calcSlipMax(wheel.pjk_lat);
            wheel.maxLong = RayCar.GripHelper.calcSlipMax(wheel.pjk_long);

            try {
                if (Float.isNaN(wheel.maxLat))
                    throw new Exception("maxLat was: '" + wheel.maxLat + "'.");
                if (Float.isNaN(wheel.maxLong))
                    throw new Exception("maxLong was: '" + wheel.maxLong + "'.");

            } catch (Exception e) {
                e.printStackTrace();
                Log.p("error in calculating max(lat|long) values of wheel #" + i);
                System.exit(-1021);
            }
        }

        try {
            // init car static data based on the 3d model
            CarModelData modelData = new CarModelData(am, data.carModel, data.wheelModel);
            if (modelData.foundSomething() && modelData.foundAllWheels()) {
                data.wheelOffset = new Vector3f[4];
                data.wheelOffset[0] = modelData.getPosOf(CarPart.Wheel_FL);
                data.wheelOffset[1] = modelData.getPosOf(CarPart.Wheel_FR);
                data.wheelOffset[2] = modelData.getPosOf(CarPart.Wheel_RL);
                data.wheelOffset[3] = modelData.getPosOf(CarPart.Wheel_RR);
            } else {
                throw new Exception("!!! Missing car model wheel position data for: " + data.carModel);
            }

            // validate that the wheels are in the correct quadrant for a car
            if (data.wheelOffset[0].x < 0 || data.wheelOffset[0].z < 0)
                throw new Exception(CarPart.Wheel_FL.name() + " should be in pos x and pos z");
            if (data.wheelOffset[1].x > 0 || data.wheelOffset[1].z < 0)
                throw new Exception(CarPart.Wheel_FR.name() + " should be in neg x and pos z");

            if (data.wheelOffset[2].x < 0 || data.wheelOffset[2].z > 0)
                throw new Exception(CarPart.Wheel_RL.name() + " should be in pos x and neg z");
            if (data.wheelOffset[3].x > 0 || data.wheelOffset[3].z > 0)
                throw new Exception(CarPart.Wheel_RR.name() + " should be in neg x and neg z");

            if (!modelData.hasCollision())
                throw new Exception(CarPart.Collision.name() + " should exist.");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1022);
        }

        try {
            // Checking that there is gear over lap between up and down:
            // [2>----[3>-<2]---<3] not [2>----<2]--[3>---<3]
            for (int i = 1; i < data.trans_gearRatios.length - 1; i++) {
                if (data.getGearUpSpeed(i) < data.getGearDownSpeed(i + 1)) {
                    throw new Exception("Gear overlap test failed for up: " + i + " down: " + (i + 1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-7555);
        }
        
        Log.p("Loaded data for car type: " + carName);
        return data;
    }
}