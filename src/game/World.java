package game;

import com.jme3.math.Vector3f;

public enum World {
	skyline("assets/skyline.blend", 800, new Vector3f(-27.7f, 84.5f, 111.3f)), 
	mycity("assets/mycity.blend", 13, new Vector3f(0,0,0)), 
	duct("assets/duct.blend", 5, new Vector3f(0,0,0)),
	raleigh("assets/raleigh.blend", 8, new Vector3f(-35.662045f, 0.4300228f, -21.98038f));

	public String name;
	public float scale;
	public Vector3f start;
	
	World (String a, float size, Vector3f pos) {
		this.name = a;
		this.scale = size;
		this.start = pos;
	}
}
