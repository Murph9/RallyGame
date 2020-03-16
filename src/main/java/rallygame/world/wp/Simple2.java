package rallygame.world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Simple2 implements WP {
	STRAIGHT("small_straight", new Vector3f(20,0,0), WP.STRAIGHT),
	SMALL_HILL_UP("small_hill_up", new Vector3f(10,0.5f,0), WP.STRAIGHT),
	SMALL_HILL_DOWN("small_hill_down", new Vector3f(10,-0.5f,0), WP.STRAIGHT),
	
	LEFT_CURVE("left_curve", new Vector3f(40,0,-40), WP.LEFT_90),
	LEFT_CURVE_QUARTER("left_curve_quarter", new Vector3f(21.213f,0,-8.8787f), WP.LEFT_45),
	
	RIGHT_CURVE("right_curve", new Vector3f(40,0,40), WP.RIGHT_90),
	RIGHT_CURVE_QUARTER("right_curve_quarter", new Vector3f(21.213f,0,8.8787f), WP.RIGHT_45),
	
	;
	
	private static String dir = "wb/simple2/";
	
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
	
	public String getName() { return String.format(WP.FileFormat, dir, name); }
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() {
		return startNode;
	}
	public NodeType endNode() {
		return endNode;
	}
	
	static class Builder extends DefaultBuilder {
		Builder() {
			super(Simple2.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
