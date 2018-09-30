package helper;

import java.util.HashMap;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

public class HelperObj {

	//really hacky class to move a shape (that moves) without much code
	
	private static HashMap<String, Geometry> set = new HashMap<String, Geometry>(); 
	
	public static void use(Node root, String key, Geometry thing) {
		//check if key exists, if yes remove it (from view)
		if (set.containsKey(key)) {
			Geometry g = set.get(key);
			root.detachChild(g);
			set.remove(key);
		}
		
		//add obj with key
		root.attachChild(thing);
		set.put(key, thing);
	}
}
