package world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import world.World;

public enum Underground implements WP {
	
	STRAIGHT("straight.blend", new Vector3f(20,0,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	STRAIGHT_B("straight_b.blend", new Vector3f(16,0,0), WP.STRIAGHT, NodeType.B, NodeType.B),
	
	CROSS_STRAIGHT("cross_straight.blend", new Vector3f(14,0,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	CROSS_RIGHT("cross_straight.blend", new Vector3f(7,0,7), WP.RIGHT_90, NodeType.A, NodeType.A),
	CROSS_LEFT("cross_straight.blend", new Vector3f(7,0,-7), WP.LEFT_90, NodeType.A, NodeType.A),
	
	JOIN_AB("join_ab.blend", new Vector3f(14,0,0), WP.STRIAGHT, NodeType.A, NodeType.B),
	JOIN_BA("join_ba.blend", new Vector3f(14,0,0), WP.STRIAGHT, NodeType.B, NodeType.A),
	;
	
	private static String dir = "assets/wb/underground/";
	
	String name;
	Vector3f newPos;
	Quaternion newRot;
	NodeType startNode;
	NodeType endNode;

	private Underground(String s, Vector3f a, Quaternion g) {
		this(s, a, g, NodeType.A, NodeType.A);
	}
	private Underground(String s, Vector3f a, Quaternion g, NodeType start, NodeType end) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = start;
		this.endNode = end;
	}
	public float getScale() { return 2; }
	public boolean needsMaterial() { return false; }
	
	public String getName() { return dir+name; }
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() {
		return startNode;
	}
	public NodeType endNode() {
		return endNode;
	}
	
	static class Builder extends DefaultBuilder implements World {
		Builder() {
			super(Underground.values());
		}
	}
}
