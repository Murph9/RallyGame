package world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Simple2 implements WP {
	STRAIGHT("small_straight.blend", new Vector3f(20,0,0), WP.STRIAGHT),
	SMALL_HILL_UP("small_hill_up.blend", new Vector3f(10,0.5f,0), WP.STRIAGHT),
	SMALL_HILL_DOWN("small_hill_down.blend", new Vector3f(10,-0.5f,0), WP.STRIAGHT),
	
	LEFT_CURVE("left_curve.blend", new Vector3f(40,0,-40), WP.LEFT_90),
	LEFT_CURVE_QUARTER("left_curve_quarter.blend", new Vector3f(21.213f,0,-8.8787f), WP.LEFT_45),
	
	RIGHT_CURVE("right_curve.blend", new Vector3f(40,0,40), WP.RIGHT_90),
	RIGHT_CURVE_QUARTER("right_curve_quarter.blend", new Vector3f(21.213f,0,8.8787f), WP.RIGHT_45),
	
	;
	
	private static String dir = "assets/wb/simple2/";
	
	String name;
	Vector3f newPos; //what the piece does to the next track
	Quaternion newRot;
	NodeType startNode;
	NodeType endNode;

	Simple2(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = NodeType.A;
		this.endNode = NodeType.A;
	}
	public float getScale() { return 1; }
	public boolean needsMaterial() { return true; }
	
	public String getName() { return dir+name; }
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() {
		return startNode;
	}
	public NodeType endNode() {
		return endNode;
	}
}
