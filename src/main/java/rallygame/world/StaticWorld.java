package rallygame.world;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

//Simple class that makes changing static world quite easy
public enum StaticWorld {
	//special
	garage("garage", 1, new Vector3f(0,1,0), null),
	garage2("garage_2", 1, new Vector3f(0,1,0), null),

	//normal
	duct("duct", 50 * 3.223f, new Vector3f(0,-34,0), null),
	duct2("duct", 5 * 3.223f, null, null),
	raleigh("raleigh", 8, new Vector3f(34.441566f, 20.72906f, 27.334211f), null),

	dragstrip("dragstrip", 10, null, null),
	multidragstrip("multidragstrip", 1, null, null),

	realroad("real_road", 1, new Vector3f(225, -19, 708), new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y)),

	spa("spa_v2.1", 15, new Vector3f(-123.3f, 31.18f, -33.77f), new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y)),

	//other peoples:
	track2("track2", 1, null, new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y)),
	carpark("carpark", 1, null, null),
	unbenannt("unbenannt_track", 1, null, null),
	
	block_fort("block_fort", 10, new Vector3f(-20,0,-12), null),
	rainbow_road("rainbow_road", 1, null, null),
//	wuhu_island("wuhu_island", 2, null, new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y)),

    //checkpoints
    lakelooproad("lakelooproad", 1, null, null),

	//debug ones:
	slope_test("slope_test", 1, null, null),
	surfaceTest("surfaceTest", 1, null, null)
//	paris1("paris1", 1, new Vector3f(40,75,0)), //be careful with this one, large
//	track1("track1", 1, new Vector3f(255.12906f, 0.7663503f, -40.224197f)),
//	track3("track3", 1, new Vector3f(-24.227085f, 0.7908745f, 98.21415f)),
//	track4("track4", 0.4f, new Vector3f(721.67365f, 0.84544f, -404.1729f)),
//	skyline("skyline", 800, new Vector3f(-27.7f, 89.5f, 111.3f)), //large
//	mine("minecraft_world1.obj", 30, new Vector3f(0,15,0)),
	;
	
	static final String dirFormat = "staticworld/%s.blend.glb";
	
	public String name;
	public float scale;
	public Vector3f start;
	public Quaternion rot;

	StaticWorld(String modelName, float size, Vector3f pos, Quaternion rot) {
		this.name = String.format(dirFormat, modelName);
        this.scale = size;
        this.start = pos;
        if (this.start == null)
            this.start = new Vector3f();
		this.rot = rot;
		if (this.rot == null)
			this.rot = Quaternion.IDENTITY.clone();
	}
}
