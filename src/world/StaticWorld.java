package world;

import com.jme3.math.Vector3f;

//Simple class that makes changing static world quite easy
public enum StaticWorld {
	
	skyline("skyline.blend", 800, new Vector3f(-27.7f, 84.5f, 111.3f), true), 
	duct("duct.blend", 50, new Vector3f(0,-5,0), true),
	raleigh("raleigh.blend", 8, new Vector3f(34.441566f, 15.72906f, 27.334211f), true),

	carpark("carpark.blend", 1, new Vector3f(0,0,0), false),
	
	dragstrip("dragstrip.blend", 10, new Vector3f(0,0,0), false),
	
	//others
	track1("track1.blend", 1, new Vector3f(255.12906f, -4.7663503f, -40.224197f), false),
	track2("track2.blend", 1, new Vector3f(0,0,0), false),
	track3("track3.blend", 1, new Vector3f(-24.227085f, -4.7908745f, 98.21415f), false),
	track4("track4.blend", 0.4f, new Vector3f(721.67365f, -4.84544f, -404.1729f), false),
	
	slotcar("slotcar.blend", 200, new Vector3f(0,0,0), false), //TODO edit
	slotcar2("slotcar2.blend", 120, new Vector3f(44.489723f, -6.9895625f, 25.498037f), true),
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
