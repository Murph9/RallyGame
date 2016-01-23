package world;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

//TODO place parts of road on slopes

public enum City implements WP {
	STRAIGHT("small_straight.blend", new Vector3f(20,0,0), WorldBuilder.STRIAGHT),
	SMALL_HILL_UP("small_hill_up.blend", new Vector3f(10,0.5f,0), WorldBuilder.STRIAGHT),
	SMALL_HILL_DOWN("small_hill_down.blend", new Vector3f(10,-0.5f,0), WorldBuilder.STRIAGHT),
	
	LEFT_CURVE("left_curve.blend", new Vector3f(40,0,-40), WorldBuilder.LEFT_90),
	LEFT_CURVE_QUARTER("left_curve_quarter.blend", new Vector3f(21.213f,0,-8.8787f), WorldBuilder.LEFT_45),
	
	RIGHT_CURVE("right_curve.blend", new Vector3f(40,0,40), WorldBuilder.RIGHT_90),
	RIGHT_CURVE_QUARTER("right_curve_quarter.blend", new Vector3f(21.213f,0,8.8787f), WorldBuilder.RIGHT_45),
	
	;
	
	private static String dir = "assets/wbcity/";
	
	String name;
	Vector3f newPos; //what the piece does to the next track
	Quaternion newRot;

	City(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
	}
	public float getScale() { return 1; }
	public String getName() { return dir+name; }
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
}
