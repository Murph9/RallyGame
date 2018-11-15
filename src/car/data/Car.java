package car.data;

import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import car.ray.CarDataConst;
import helper.Log;

//TODO use ridge racer cars

public enum Car {
	Normal("Normal"),
	Runner("Runner"),
	Rally("Rally"),
	Roadster("Roadster"),
	Gt("Gt"),
	
	Hunter("Hunter"),
	Ricer("Ricer"),
	Muscle("Muscle"),
	Wagon("Wagon"),
	
	WhiteSloth("WhiteSloth"),
	Rocket("Rocket"),
	;
	
	private String c;
	private Car(String c) {
		this.c = c;
	}
	
	public CarDataConst get() {
		InputStream in = getClass().getResourceAsStream(this.c+".yaml");
		Yaml yaml = new Yaml(new Constructor(CarDataConst.class));
	    Object data = yaml.load(in);

	    if (data == null) {
	    	Log.e("Loading data for car: " + this.c + " did not go well.");
    		return null;
	    }
	    if (!(data instanceof CarDataConst)) {
		    Log.e("Loading data for car: " + this.c + " did not go that well.");
			return null;	    	
	    }
		
		return (CarDataConst)data;
	}
}