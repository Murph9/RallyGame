package car.data;

import java.io.InputStream;

import com.jme3.math.Vector3f;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import car.CarModelData;
import car.CarModelData.CarPart;
import car.ray.CarDataConst;
import car.ray.CarSusDataConst;
import car.ray.RayCar;
import car.ray.WheelDataConst;
import helper.Log;


public enum Car {
	Normal("Normal"),
	Runner("Runner"),
	Rally("Rally"),
	Roadster("Roadster"),
	
	Hunter("Hunter"),
	Ricer("Ricer"),
	Muscle("Muscle"),
	Wagon("Wagon"),
	Bus("Bus"),
	
	Ultra("Ultra"),
	LeMans("LeMans"),
	Inline("Inline"),
	TouringCar("Touring"),
	Hill("Hill"),

	WhiteSloth("WhiteSloth"),
	Rocket("Rocket"),
	
	Debug("Debug"),
	;
	
	private final String carName;
	private CarDataConst data;
	private Car(String carName) {
		this.carName = carName;
	}
	
	public CarDataConst get(Vector3f gravity) {
		if (this.data != null) {
			Log.p("Cached data for car type: " + this.carName);
			return this.data;
		}

		Log.p("Loading data for car type: " + this.carName);
		
		InputStream in = getClass().getResourceAsStream(this.carName+".yaml");
		Yaml yaml = new Yaml(new Constructor(CarDataConst.class));
	    Object data = yaml.load(in);

	    if (data == null) {
	    	Log.e("Loading data for car: " + this.carName + " did not go well.");
    		return null;
	    }
	    if (!(data instanceof CarDataConst)) {
		    Log.e("Loading data for car: " + this.carName + " did not go that well.");
			return null;
	    }

		this.data = (CarDataConst)data;

		//Wheel validation
		float quarterMassForce = Math.abs(gravity.y)*this.data.mass/4f;
		for (int i  = 0; i < this.data.wheelData.length; i++) {
			CarSusDataConst sus = this.data.susByWheelNum(i);
			
			// Validate that rest suspension position is within min and max
			float minSusForce = (sus.preload_force + sus.stiffness * 0)*1000;
			float maxSusForce = (sus.preload_force + sus.stiffness * (sus.max_travel - sus.min_travel))*1000;
			if (quarterMassForce < minSusForce) {
				Log.e("!! Sus min range too high: " + quarterMassForce + " < " + minSusForce + ", decrease pre-load");
			}
			if (quarterMassForce > maxSusForce) {
				Log.e("!! Sus max range too low: " + quarterMassForce + " > " + maxSusForce + ", increase pre-load or stiffness");
			}

			WheelDataConst wheel = this.data.wheelData[i];
			//generate the slip* max force from the car wheel data, and validate they are 'real'
			wheel.maxLat = RayCar.GripHelper.calcSlipMax(wheel.pjk_lat);
			wheel.maxLong = RayCar.GripHelper.calcSlipMax(wheel.pjk_long);
			
			wheel.maxLatSat = RayCar.GripHelper.calcSlipMax(wheel.pjk_lat_sat);
			wheel.maxLongSat = RayCar.GripHelper.calcSlipMax(wheel.pjk_long_sat);
			
			try {
				if (Float.isNaN(wheel.maxLat))
					throw new Exception("maxLat was: '" + wheel.maxLat +"'.");
				if (Float.isNaN(wheel.maxLong))
					throw new Exception("maxLong was: '" + wheel.maxLong +"'.");
				
				if (Float.isNaN(wheel.maxLatSat))
					throw new Exception("maxLatSat was: '" + wheel.maxLatSat +"'.");
				if (Float.isNaN(wheel.maxLongSat))
					throw new Exception("maxLongSat was: '" + wheel.maxLongSat +"'.");
			} catch (Exception e) {
				e.printStackTrace();
				Log.p("error in calculating max(lat|long) values of wheel #" + i);
				System.exit(1);
			}
		}

				
		//init car pos things based on the physical model
		CarModelData modelData = new CarModelData(this.data.carModel, this.data.wheelModel);
		if (modelData.foundSomething() && modelData.foundAllWheels()) {
			this.data.wheelOffset = new Vector3f[4];
			this.data.wheelOffset[0] = modelData.getPosOf(CarPart.Wheel_FL);
			this.data.wheelOffset[1] = modelData.getPosOf(CarPart.Wheel_FR);
			this.data.wheelOffset[2] = modelData.getPosOf(CarPart.Wheel_RL);
			this.data.wheelOffset[3] = modelData.getPosOf(CarPart.Wheel_RR);
		} else {
			Log.e("!!! Missing car model wheel position data for: " + this.data.carModel);
			System.exit(-50203);
		}


		Log.p("Loaded data for car type: " + this.carName);
		return this.data;
	}
}