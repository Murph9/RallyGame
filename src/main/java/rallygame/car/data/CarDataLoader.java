package rallygame.car.data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import rallygame.car.data.CarModelData.CarPart;
import rallygame.car.data.wheel.WheelDataLoader;
import rallygame.car.data.wheel.WheelTractionType;
import rallygame.helper.Duo;
import rallygame.helper.Log;

/** Private class for CarManager, manages the CarDataConst class */
public class CarDataLoader {
    
    private WheelDataLoader wheelData;
    private static Map<Car, CarDataConst> loadedDataCache = new HashMap<>();
    private static Map<Car, CarDataConst> dataCache = new HashMap<>();

    public CarDataLoader() {
        try {
            wheelData = new WheelDataLoader();
        } catch (Exception e1) {
            e1.printStackTrace();
            Log.exit(-344, "!!! wheel data load really failed");
        }

        for (var type: Car.values()) {
            CarDataConst result = null;
            try {
                result = this.loadFromFile(type);
            } catch (Exception e) {
                e.printStackTrace();
                Log.exit(-343, "!!! car data load really failed");
            }
            
            result.name = type.getName();
            loadedDataCache.put(type, result);
        }
    }

    private CarDataConst loadFromFile(Car car) {
        InputStream in = getClass().getResourceAsStream(car.getFileName());
        Yaml yaml = new Yaml(new Constructor(CarDataConst.class));
        Object yamlData = yaml.load(in);

        if (yamlData == null) {
            Log.exit(-56, "Loading data for car: " + car.getName() + " did not go well.");
            return null;
        }
        if (!(yamlData instanceof CarDataConst)) {
            Log.exit(-55, "Loading data for car: " + car.getName() + " did not go that well.");
            return null;
        }

        return (CarDataConst) yamlData;
    }

    public CarDataConst get(AssetManager am, Car car, Vector3f gravity) throws IllegalStateException {
        if (dataCache.containsKey(car))
            return dataCache.get(car).cloneWithSerialization();
        
        if (!loadedDataCache.containsKey(car)) {
            throw new IllegalStateException("!!! Tried to init load car data twice");
        }

        var data = loadedDataCache.get(car);
        loadedDataCache.remove(car); // so it can only be loaded once

        loadFromModel(data, am);
        validateData(data, gravity);
        
        dataCache.put(car, data);

        return data.cloneWithSerialization();
    }

    private void loadFromModel(CarDataConst data, AssetManager am) {
        // init car static data based on the 3d model
        CarModelData modelData = new CarModelData(am, data.carModel, data.wheelModel);
        if (modelData.foundSomething() && modelData.foundAllWheels()) {
            data.wheelOffset = new Vector3f[4];
            data.wheelOffset[0] = modelData.getPosOf(CarPart.Wheel_FL);
            data.wheelOffset[1] = modelData.getPosOf(CarPart.Wheel_FR);
            data.wheelOffset[2] = modelData.getPosOf(CarPart.Wheel_RL);
            data.wheelOffset[3] = modelData.getPosOf(CarPart.Wheel_RR);
        } else {
            throw new IllegalStateException("!!! Missing car model wheel position data for: " + data.carModel);
        }
    }

    public void validateData(CarDataConst data, Vector3f gravity) {
        validateModelData(data);
        updateWheelValues(data, gravity);
        updateAutoGearChanges(data);
    }

    private void validateModelData(CarDataConst data) {
        // validate that the wheels are in the correct quadrant for a car
        if (data.wheelOffset[0].x < 0 || data.wheelOffset[0].z < 0)
            throw new IllegalStateException(CarPart.Wheel_FL.name() + " should be in pos x and pos z");
        if (data.wheelOffset[1].x > 0 || data.wheelOffset[1].z < 0)
            throw new IllegalStateException(CarPart.Wheel_FR.name() + " should be in neg x and pos z");

        if (data.wheelOffset[2].x < 0 || data.wheelOffset[2].z > 0)
            throw new IllegalStateException(CarPart.Wheel_RL.name() + " should be in pos x and neg z");
        if (data.wheelOffset[3].x > 0 || data.wheelOffset[3].z > 0)
            throw new IllegalStateException(CarPart.Wheel_RR.name() + " should be in neg x and neg z");
       
    }

    private void updateWheelValues(CarDataConst data, Vector3f gravity) {
        if (data.wheelData == null)
            throw new IllegalStateException("Please do set wheelData in the yaml file");
        
        // load from the Wheel loader
        for (int i = 0; i < data.wheelData.length; i++) {
            if (data.wheelData[i].tractionType == null)
                throw new IllegalStateException("Traction type not set in " + data.name + "for wheel" + i);

            var wheelType = WheelTractionType.valueOf(data.wheelData[i].tractionType);
            data.wheelData[i].traction = this.wheelData.get(wheelType);
        }
        
        // Wheel validation
        float quarterMassForce = Math.abs(gravity.y) * data.mass / 4f;
        
        // generate the load quadratic value
        data.wheelLoadQuadratic = 1/(quarterMassForce*4);
        for (int i = 0; i < data.wheelData.length; i++) {
            CarSusDataConst sus = data.susByWheelNum(i);

            // Validate that rest suspension position is within min and max
            float minSusForce = (sus.preload_force + sus.stiffness * 0) * 1000;
            float maxSusForce = (sus.preload_force + sus.stiffness * (sus.max_travel - sus.min_travel)) * 1000;
            if (quarterMassForce < minSusForce) {
                throw new IllegalStateException("!! Sus min range too high: " + quarterMassForce + " < " + minSusForce + ", decrease pre-load or stiffness");
            }
            if (quarterMassForce > maxSusForce) {
                throw new IllegalStateException("!! Sus max range too low: " + quarterMassForce + " > " + maxSusForce + ", increase pre-load or stiffness");
            }
        }
    }

    private void updateAutoGearChanges(CarDataConst data) {
        // Output the optimal gear up change point based on the torque curve
        final int redlineOffset = 250;
        List<Duo<Integer, Float>> changeTimes = new LinkedList<>();
        float maxTransSpeed = data.speedAtRpm(data.trans_gearRatios.length - 1, data.e_redline - redlineOffset);
        for (float speed = 0; speed < maxTransSpeed; speed += 0.1f) {
            int bestGear = -1;
            float bestTorque = -1;
            for (int gear = 1; gear < data.trans_gearRatios.length; gear++) {
                int rpm = data.rpmAtSpeed(gear, speed);
                if (rpm > data.e_redline - redlineOffset) //just a bit off of redline because its not that smooth
                    continue;
                float wheelTorque = data.lerpTorque(rpm) * data.trans_gearRatios[gear] * data.trans_finaldrive;
                if (bestTorque < wheelTorque) {
                    bestTorque = wheelTorque;
                    bestGear = gear;
                }
                // This prints a more detailed graph: Log.p(speed * 3.6f, wheelTorque, gear);
            }

            // This prints a nice graph: Log.p(speed * 3.6f, bestTorque, bestGear);
            changeTimes.add(new Duo<Integer, Float>(bestGear, speed));
        }

        data.auto_gearDownSpeed = new float[data.trans_gearRatios.length];
        data.auto_gearDownSpeed[0] = Float.MAX_VALUE; // never change out of reverse
        data.auto_gearUpSpeed = new float[data.trans_gearRatios.length];
        data.auto_gearUpSpeed[0] = Float.MAX_VALUE; // never change out of reverse
        // Get the first and last value for each gear
        for (Entry<Integer, List<Duo<Integer, Float>>> entry : changeTimes.stream()
                .collect(Collectors.groupingBy(x -> x.first)).entrySet()) {
            int gear = entry.getKey();
            float downValue = entry.getValue().get(0).second;
            float upValue = entry.getValue().get(entry.getValue().size() - 1).second;

            // set the auto up and down changes
            data.auto_gearDownSpeed[gear] = downValue - 2f; // buffer so they overlap a little
            data.auto_gearUpSpeed[gear] = upValue;
        }

        // Checking that there is gear overlap between up and down (as it prevents the
        // car from changing gear):
        // [2>----[3>-<2]---<3] not [2>----<2]--[3>---<3]
        for (int i = 1; i < data.trans_gearRatios.length - 1; i++) {
            if (data.getGearUpSpeed(i) < data.getGearDownSpeed(i + 1)) {
                throw new IllegalStateException("Gear overlap test failed for up: " + i + " down: " + (i + 1));
            }
        }
    }
}
