package world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Track implements WP {

	STRAIGHT("straight.blend", new Vector3f(5,0,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	BRIDGE("bridge.blend", new Vector3f(10,0,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	CHICANE("chicane.blend", new Vector3f(12,0,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	STRAIGHT_TUNNEL("straight_tunnel.blend", new Vector3f(3f,0,0), WP.STRIAGHT, NodeType.B, NodeType.B),
	
	RIGHT("right.blend", new Vector3f(12,0,12f), WP.RIGHT_90, NodeType.A, NodeType.A),
	LEFT("left.blend", new Vector3f(12f,0,-12f), WP.LEFT_90, NodeType.A, NodeType.A),
	
	RIGHT_TIGHT("right_tight.blend", new Vector3f(7.7202f,0,8f), WP.RIGHT_90, NodeType.A, NodeType.A),
	LEFT_TIGHT("left_tight.blend", new Vector3f(7.7202f,0,-8f), WP.LEFT_90, NodeType.A, NodeType.A),
	
	LEFT_HAIRPIN("left_hairpin.blend", new Vector3f(0,0,-4f), WP.BACK, NodeType.A, NodeType.A),
	RIGHT_HAIRPIN("right_hairpin.blend", new Vector3f(0,0,4f), WP.BACK, NodeType.A, NodeType.A),
	
	TUNNEL_IN("tunnel_in.blend", new Vector3f(3f,0,0), WP.STRIAGHT, NodeType.A, NodeType.B),
	TUNNEL_OUT("tunnel_out.blend", new Vector3f(3f,0,0), WP.STRIAGHT, NodeType.B, NodeType.A),
	
	LEFT_TUNNEL("left_tunnel.blend", new Vector3f(4, 0,-4), WP.LEFT_90, NodeType.B, NodeType.B),
	RIGHT_TUNNEL("right_tunnel.blend", new Vector3f(4, 0,4), WP.RIGHT_90, NodeType.B, NodeType.B),
	;
	
	public static String dir = "assets/wb/track/";
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next peice
	NodeType startNode;
	NodeType endNode;

	Track(String s, Vector3f a, Quaternion g, NodeType start, NodeType end) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = start;
		this.endNode = end;
	}
	
	public float getScale() { return 8; }
	public boolean needsMaterial() { return false; }
	
	public String getName() { return dir+name;}
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() { return startNode; }
	public NodeType endNode() { return endNode; }

	
	static class Builder extends DefaultBuilder {
		Builder() {
			super(Track.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
