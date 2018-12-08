package world;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import helper.H;

//Simple class that makes changing static world quite easy
public enum StaticWorld {
	//special
	garage("garage.blend", 1, new Vector3f(0,1,0), null, false),
	garage2("garage_2.blend", 1, new Vector3f(0,1,0), null, false),

	//normal
	duct("duct.blend", 50, new Vector3f(0,-6,0), null, true),
	duct2("duct.blend", 5, new Vector3f(0,0,0), null, true),
	raleigh("raleigh.blend", 8, new Vector3f(34.441566f, 20.72906f, 27.334211f), null, true),

	dragstrip("dragstrip.blend", 10, new Vector3f(0,0,0), null, false),
	
	//other peoples:
	track2("track2.blend", 1, new Vector3f(0,0,0), H.FromAngleAxis(FastMath.PI, Vector3f.UNIT_Y), false),
	carpark("carpark.blend", 1, new Vector3f(0,0,0), null, false),
	unbenannt("unbenannt_track.blend", 1, new Vector3f(0,0,0), null, true), //TODO needs fixing
	
	block_fort("block_fort.blend", 10, new Vector3f(-20,0,-12), null, false),
	rainbow_road("rainbow_road.blend", 1, new Vector3f(0,0,0), null, true),
	wuhu_island("wuhu_island.blend", 2, new Vector3f(0,0,0), H.FromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y), true),
	
	//debug ones:
	slope_test("slope_test.blend", 1, new Vector3f(), null, true)
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
	public Matrix3f rot;
	public boolean ifNeedsTexture;

	StaticWorld(String a, float size, Vector3f pos, Matrix3f rot, boolean texture) {
		this.name = dir + a;
		this.scale = size;
		this.start = pos;
		this.rot = rot;
		if (this.rot == null)
			this.rot = new Matrix3f(Matrix3f.IDENTITY);
		this.ifNeedsTexture = texture;
	}
}
