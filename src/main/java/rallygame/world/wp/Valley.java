package rallygame.world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Valley implements WP {
	STRAIGHT("straight", new Vector3f(25,0,0), WP.STRAIGHT),
	STRAIGHT_DOWN("straight_down", new Vector3f(20,-1.33697f, 0), WP.DOWN_8),
	STRAIGHT_UP("straight_up", new Vector3f(20,1.33697f, 0), WP.UP_8),
	
	RIGHT("right", new Vector3f(12.94095f,0,1.70371f), WP.RIGHT_15),
	LEFT("left", new Vector3f(12.94095f,0,-1.70371f), WP.LEFT_15),
	
//	TUNNEL("tunnel", new Vector3f(10f, 0,0), WP.STRIAGHT)
	;
	
	private static String dir = "wb/valley/";
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next piece
	NodeType startNode;
	NodeType endNode;
	
	Valley(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = NodeType.A;
		this.endNode = NodeType.A;
	}
	
	public float getScale() { return 2; }
	public boolean needsMaterial() { return false; }
	
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
			super(Valley.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
