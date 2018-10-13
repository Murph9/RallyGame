package car;

import java.util.HashMap;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import game.App;
import helper.Log;

public class CarModelData {

	private String carModel;
	@SuppressWarnings("unused") //eventually...
	private String wheelModel;
	
	//chassis data
	private HashMap<CarPart, CarPartData> pieces;
	private HashMap<String, CarPart> possibleParts;
	
	public CarModelData(String car, String wheel) {
		this.carModel = car;
		this.wheelModel = wheel;
		this.pieces = new HashMap<>();
		
		this.possibleParts = CarPart.GetNames();
		
		Spatial rootSpat = App.rally.getAssetManager().loadModel(carModel);
		readInModelData(rootSpat);
		
		Log.e("Car part data for: '" + car + "'");
		Log.e(pieces.keySet().toArray(), (String)null); //casting a null...
	}
	
	private void readInModelData(Spatial s) {
		if (s == null) return;
		
		if (possibleParts.containsKey(s.getName())) {
			pieces.put(possibleParts.get(s.getName()), MakeCPDFrom(s));
		}
		
		if (s instanceof Node) {
			for (Spatial sp: ((Node) s).getChildren()) {
				readInModelData(sp);
			}
		}
	}
	
	private CarPartData MakeCPDFrom(Spatial s) {
		return new CarPartData(s.getLocalTranslation(), s.getLocalRotation(), s.getLocalScale());
	}

	//////////////
	//get methods
	public Vector3f getPosOf(CarPart part) {
		if (pieces.containsKey(part)) {
			return pieces.get(part).pos;
		}
		return null;
	}
	public Quaternion getRotOf(CarPart part) {
		if (pieces.containsKey(part)) {
			return pieces.get(part).rot;
		}
		return null;
	}
	public Vector3f getScaleOf(CarPart part) {
		if (pieces.containsKey(part)) {
			return pieces.get(part).scale;
		}
		return null;
	}
	public boolean foundSomething() {
		return !pieces.isEmpty();
	}
	public boolean foundAllWheels() {
		return pieces.containsKey(CarPart.Wheel_FL) 
				&& pieces.containsKey(CarPart.Wheel_FR)
				&& pieces.containsKey(CarPart.Wheel_RL)
				&& pieces.containsKey(CarPart.Wheel_RR);
	}
	
	class CarPartData {
		Vector3f pos;
		Quaternion rot;
		Vector3f scale;
		public CarPartData(Vector3f p, Quaternion r, Vector3f s) {
			pos = p;
			rot = r;
			scale = s;
		}
	}
	
	public enum CarPart {
		Chassis("chassis"), //main model
		Exhaust1("exhaust1"),//only one side
		Exhaust2("exhaust2"),//only one side
		Wheel_FL("wheel_fl"), //front left
		Wheel_FR("wheel_fr"), //front right
		Wheel_RL("wheel_rl"), //rear left
		Wheel_RR("wheel_rr"), //rear right
		
		Headlight_L("headlight"), //only one side
		Taillight_L("taillight_l"), //only one side
		//TODO more
		;
		
		private String name;
		CarPart(String name) {
			this.name = name;
		}
		
		static HashMap<String, CarPart> GetNames() {
			CarPart[] names = CarPart.values();
			
			HashMap<String, CarPart> out = new HashMap<>();
			for (int i = 0; i < names.length; i++) {
				out.put(names[i].name, names[i]);
			}
			
			return out;
		}
	}
}