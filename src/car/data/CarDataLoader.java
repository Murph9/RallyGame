package car.data;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import car.data.CarModelData.CarPart;
import car.ray.GripHelper;
import helper.Duo;
import helper.Log;

/** Private class for CarBuilder, manages the CarDataConst file */
public class CarDataLoader { //CarDataFactory
    
    private static final String YAML_CAR_DATA = "/assets/cardata/";

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

    public CarDataConst get(AssetManager am, Car car, Vector3f gravity) throws IllegalStateException {
        String carName = car.getCarName();
        
        Log.p("Loading file data for car type: " + carName);

        CarDataConst data = null;
        try {
            data = loadFromFile(carName);
            data.name = carName;
        } catch (Exception e) {
            e.printStackTrace();
            Log.exit(-343, "!!! car data load failed");
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
            wheel.maxLat = GripHelper.calcSlipMax(wheel.pjk_lat);
            wheel.maxLong = GripHelper.calcSlipMax(wheel.pjk_long);

            try {
                if (Float.isNaN(wheel.maxLat))
                    throw new Exception("maxLat was: '" + wheel.maxLat + "'.");
                if (Float.isNaN(wheel.maxLong))
                    throw new Exception("maxLong was: '" + wheel.maxLong + "'.");

            } catch (Exception e) {
                e.printStackTrace();
                Log.exit(-1021, "error in calculating max(lat|long) values of wheel #" + i);
            }
        }

        
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

        if (!modelData.hasCollision())
            throw new IllegalStateException(CarPart.Collision.name() + " should exist.");

        // validate that the wheels are in the correct quadrant for a car
        if (data.wheelOffset[0].x < 0 || data.wheelOffset[0].z < 0)
            throw new IllegalStateException(CarPart.Wheel_FL.name() + " should be in pos x and pos z");
        if (data.wheelOffset[1].x > 0 || data.wheelOffset[1].z < 0)
            throw new IllegalStateException(CarPart.Wheel_FR.name() + " should be in neg x and pos z");

        if (data.wheelOffset[2].x < 0 || data.wheelOffset[2].z > 0)
            throw new IllegalStateException(CarPart.Wheel_RL.name() + " should be in pos x and neg z");
        if (data.wheelOffset[3].x > 0 || data.wheelOffset[3].z > 0)
            throw new IllegalStateException(CarPart.Wheel_RR.name() + " should be in neg x and neg z");
       
        //validate that the D1 and D2 pjk const values fit the weight of the car
        for (int i = 0; i < data.wheelData.length; i++) {
            if (GripHelper.loadFormula(data.wheelData[i].pjk_lat, quarterMassForce) <= 0)
                throw new IllegalStateException("Wheel load lat formula not in range: " + data.wheelData[i].pjk_lat + " " + quarterMassForce);
            if (GripHelper.loadFormula(data.wheelData[i].pjk_long, quarterMassForce) <= 0)
                throw new IllegalStateException("Wheel load long formula not in range: " + data.wheelData[i].pjk_lat + " " + quarterMassForce);
        }

        // Checking that there is gear overlap between up and down:
        // [2>----[3>-<2]---<3] not [2>----<2]--[3>---<3]
        for (int i = 1; i < data.trans_gearRatios.length - 1; i++) {
            if (data.getGearUpSpeed(i) < data.getGearDownSpeed(i + 1)) {
                throw new IllegalStateException("Gear overlap test failed for up: " + i + " down: " + (i + 1));
            }
        }
        
        
        // Output the optimal gear up change point based on the torque curve
        List<Duo<Integer, Float>> changeTimes = new LinkedList<>();
        float maxTransSpeed = data.speedAtRpm(data.trans_gearRatios.length - 1, data.e_redline);
        for (float speed = 0; speed < maxTransSpeed; speed+=0.1f) {
            int bestGear = -1;
            float bestTorque = -1;
            for (int gear = 1; gear < data.trans_gearRatios.length; gear++) {
                int rpm = data.rpmAtSpeed(gear, speed);
                if (rpm > data.e_redline)
                    continue;
                float wheelTorque = data.lerpTorque(rpm) * data.trans_gearRatios[gear] * data.trans_finaldrive;
                if (bestTorque < wheelTorque) {
                    bestTorque = wheelTorque;
                    bestGear = gear;
                }
            }
            
            //This prints a nice graph: Log.p(speed * 3.6f, bestTorque, bestGear);
            changeTimes.add(new Duo<Integer,Float>(bestGear, speed));
        }
        //Get the first and last value for each gear
        for (Entry<Integer, List<Duo<Integer, Float>>> entry: changeTimes.stream().collect(Collectors.groupingBy(x -> x.first)).entrySet()) {
            Log.p("Gear", entry.getKey(), "min", entry.getValue().get(0).second, "max", entry.getValue().get(entry.getValue().size() - 1).second);
        }
        // TODO use these values
        // TODO at least making up rpm and down rpm redundant

        return data;
    }
}
