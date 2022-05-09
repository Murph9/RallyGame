package rallygame.world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Underground implements WP {
	
	STRAIGHT("straight", new Vector3f(20,0,0), WP.STRAIGHT, NodeType.A, NodeType.A),
	STRAIGHT_B("straight_b", new Vector3f(16,0,0), WP.STRAIGHT, NodeType.B, NodeType.B),
	
	CROSS_STRAIGHT("cross_straight", new Vector3f(14,0,0), WP.STRAIGHT, NodeType.A, NodeType.A),
	CROSS_RIGHT("cross_straight", new Vector3f(7,0,7), WP.RIGHT_90, NodeType.A, NodeType.A),
	CROSS_LEFT("cross_straight", new Vector3f(7,0,-7), WP.LEFT_90, NodeType.A, NodeType.A),
	
	JOIN_AB("join_ab", new Vector3f(14,0,0), WP.STRAIGHT, NodeType.A, NodeType.B),
	JOIN_BA("join_ba", new Vector3f(14,0,0), WP.STRAIGHT, NodeType.B, NodeType.A),
	;
	
	private static final String file = "wb/underground.blend.glb";
	
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
	
	public String getName() { return this.name; }
	public String getFileName() { return file; }

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
			super(Underground.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
