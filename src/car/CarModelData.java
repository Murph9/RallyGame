package car;

import com.jme3.asset.AssetManager;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

import game.App;
import game.Rally;

public class CarModelData {
	
	boolean simple = false;

	String carModel;
	String wheelModel;
	
	//chassis data
	CarModelPiece chassis;
	
	CarModelData(String car, String wheel) {
		this.carModel = car;
		this.wheelModel = wheel;
		Rally r = App.rally;
		AssetManager am = r.getAssetManager();
		
		Spatial rootSpat = am.loadModel(carModel);
		if (rootSpat instanceof Geometry) { }
		/*
			//not a fancy model, i.e. just one model no tree structure
			simple = true;
			chassis = new CarModelPiece((Geometry)rootSpat, Vector3f.ZERO, Matrix3f.IDENTITY);
			H.p("not even searched");
		} else {
			Node modelRoot = (Node) rootSpat;
			//first check that a node contains the root 'chassis' model
			boolean foundChassis = false;
			for (Spatial s: H.getGeomList(modelRoot)) {
				if (s.getName().equals("chassis")) {
					foundChassis = true;
				}
			}
			if (!foundChassis) { 
				for (Spatial s: modelRoot.getChildren()) {
					if (s instanceof Geometry) {
						chassis = new CarModelPiece((Geometry) rootSpat, Vector3f.ZERO, Matrix3f.IDENTITY);
						simple = true;
						break;
					}
				}
				H.p("not found");
			} else {
				for (Spatial s: modelRoot.getChildren()) {
					if (s.getName().equals("chassis")) {
						chassis = new CarModelPiece((Geometry) rootSpat, Vector3f.ZERO, Matrix3f.IDENTITY);
					}
				}
				H.p("found");
			}
		}*/
		//TODO
	}
	
	class CarModelPiece {
		Geometry model;
		Vector3f pos;
		Matrix3f rot;
		public CarModelPiece(Geometry m, Vector3f p, Matrix3f r) {
			model = m;
			pos = p;
			rot = r;
		}
	}
}