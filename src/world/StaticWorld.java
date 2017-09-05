package world;

import com.jme3.math.Vector3f;

//Simple class that makes changing static world quite easy
public enum StaticWorld {
	//special
	garage("garage.blend", 1, new Vector3f(0,1,0), false),
	garage2("garage_2.blend", 1, new Vector3f(0,1,0), false),

	//normal
	duct("duct.blend", 50, new Vector3f(0,-6,0), true),
	duct2("duct.blend", 5, new Vector3f(0,0,0), true),
	raleigh("raleigh.blend", 8, new Vector3f(34.441566f, 20.72906f, 27.334211f), true),

	dragstrip("dragstrip.blend", 10, new Vector3f(0,0,0), false),
	
	//other peoples:
	track2("track2.blend", 1, new Vector3f(0,0,0), false),
	carpark("carpark.blend", 1, new Vector3f(0,0,0), false),
	unbenannt("unbenannt_track.blend", 1, new Vector3f(0,0,0), true), //TODO needs fixing
	
	//debug ones: (not working now)
//	paris1("paris1.blend", 1, new Vector3f(40,75,0), true), //be careful with this one, large
//	track1("track1.blend", 1, new Vector3f(255.12906f, 0.7663503f, -40.224197f), false),
//	track3("track3.blend", 1, new Vector3f(-24.227085f, 0.7908745f, 98.21415f), false),
//	track4("track4.blend", 0.4f, new Vector3f(721.67365f, 0.84544f, -404.1729f), false),
//	skyline("skyline.blend", 800, new Vector3f(-27.7f, 89.5f, 111.3f), true), //large
//	mine("minecraft_world1.obj", 30, new Vector3f(0,15,0), false),
	;
	
	static final String dir = "assets/staticworld/";
	
	public String name;
	public float scale;
	public Vector3f start;
	public boolean ifNeedsTexture;
	
	StaticWorld (String a, float size, Vector3f pos, boolean texture) {
		this.name = dir+a;
		this.scale = size;
		this.start = pos;
		this.ifNeedsTexture = texture;
	}
}
