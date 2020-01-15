package world;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import helper.H;

//Simple class that makes changing static world quite easy
public enum StaticWorld {
	//special
	garage("garage.blend", 1, new Vector3f(0,1,0), null),
	garage2("garage_2.blend", 1, new Vector3f(0,1,0), null),

	//normal
	duct("duct.blend", 50 * 3.223f, new Vector3f(0,-6,0), null),
	duct2("duct.blend", 5 * 3.223f, null, null),
	raleigh("raleigh.blend", 8, new Vector3f(34.441566f, 20.72906f, 27.334211f), null),

	dragstrip("dragstrip.blend", 10, null, null),
	multidragstrip("multidragstrip.blend", 1, null, null),

	realroad("real_road.blend", 1, new Vector3f(225, -19, 708), H.FromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y)),

	//other peoples:
	track2("track2.blend", 1, null, H.FromAngleAxis(FastMath.PI, Vector3f.UNIT_Y)),
	carpark("carpark.blend", 1, null, null),
	unbenannt("unbenannt_track.blend", 1, null, null), //TODO needs fixing
	
	block_fort("block_fort.blend", 10, new Vector3f(-20,0,-12), null),
	rainbow_road("rainbow_road.blend", 1, null, null),
	wuhu_island("wuhu_island.blend", 2, null, H.FromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y)),

    //checkpoints
    lakelooproad("lakelooproad.blend", 1, null, null),

	//debug ones:
	slope_test("slope_test.blend", 1, null, null)
//	paris1("paris1.blend", 1, new Vector3f(40,75,0)), //be careful with this one, large
//	track1("track1.blend", 1, new Vector3f(255.12906f, 0.7663503f, -40.224197f)),
//	track3("track3.blend", 1, new Vector3f(-24.227085f, 0.7908745f, 98.21415f)),
//	track4("track4.blend", 0.4f, new Vector3f(721.67365f, 0.84544f, -404.1729f)),
//	skyline("skyline.blend", 800, new Vector3f(-27.7f, 89.5f, 111.3f)), //large
//	mine("minecraft_world1.obj", 30, new Vector3f(0,15,0)),
	;
	
	static final String dir = "assets/staticworld/";
	
	public String name;
	public float scale;
	public Vector3f start;
	public Matrix3f rot;

	StaticWorld(String a, float size, Vector3f pos, Matrix3f rot) {
		this.name = dir + a;
        this.scale = size;
        this.start = pos;
        if (this.start == null)
            this.start = new Vector3f();
		this.rot = rot;
		if (this.rot == null)
			this.rot = new Matrix3f(Matrix3f.IDENTITY);
	}
}
