package rallygame.world.wp;

import com.jme3.math.*;

public enum Floating implements WP {
	STRAIGHT("straight", new Vector3f(20,0,0), Quaternion.IDENTITY),
	
	//these weird angles are just to show i can do it...
//	STRAIGHT_UP("straight", new Vector3f(20,0,0), new Quaternion(0, 0, 0.098f, 0.9952f)),
//	STRAIGHT_DOWN("straight", new Vector3f(20,0,0), new Quaternion(0, 0, -0.098f, 0.9952f)),
	
	LEFT_CURVE("left", new Vector3f(21.21f,0,-8.79f), new Quaternion(0, 0.38268346f, 0, 0.9238795f)),
	RIGHT_CURVE("right", new Vector3f(21.21f,0,8.79f), new Quaternion(0, -0.38268346f, 0, 0.9238795f)),
	;
	
	private static final String file = "wb/floating.blend.glb";
		
	String name;
	Vector3f newPos;
	Quaternion newRot;
	NodeType startNode;
	NodeType endNode;

	Floating(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = NodeType.A;
		this.endNode = NodeType.A;
	}
	public float getScale() { return 1; }
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
			super(Floating.values());
		}
		
		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
