package car;

import java.util.HashMap;

import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import game.App;
import game.Main;
import helper.H;

public class CarModelData {
	
	boolean simple = false;

	String carModel;
	String wheelModel;
	
	//chassis data
	private HashMap<CarPart, CarPartData> pieces;
	private HashMap<String, CarPart> possibleParts;
	
	CarModelData(String car, String wheel) {
		this.carModel = car;
		this.wheelModel = wheel;
		this.pieces = new HashMap<>();
		
		Main r = App.rally;
		AssetManager am = r.getAssetManager();
		
		possibleParts = CarPart.GetNames();
		
		Spatial rootSpat = am.loadModel(carModel);
		readInModelData(rootSpat);
		
		H.e("Car part data for: '" + car + "'");
		H.e(pieces.keySet().toArray(), (String)null); //casting a null...
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
	
	enum CarPart {
		Chassis("chassis"),
		Exhaust("exhaust"),
		Wheel_FL("wheel_fl"),
		Wheel_FR("wheel_fr"),
		Wheel_RL("wheel_rl"),
		Wheel_RR("wheel_rr"),
		
		Headlight_L("headlight_l"),
		Headlight_R("headlight_r"),
		Taillight_L("taillight_l"),
		Taillight_R("taillight_r"),
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