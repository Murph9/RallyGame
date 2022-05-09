package rallygame.world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

//stands for world piece simple, as in its a simple piece of the world
public enum Simple implements WP {
	
	CROSS("cross", new Vector3f(2,0,0), WP.STRAIGHT),
	STRAIGHT("straight", new Vector3f(2,0,0), WP.STRAIGHT),
	
	LEFT("left", new Vector3f(1,0,-1), WP.LEFT_90),
	LEFT_SHARP("left_sharp", new Vector3f(1,0,-1), WP.LEFT_90),
	LEFT_LONG("left_long", new Vector3f(2,0,-2), WP.LEFT_90),
	LEFT_CHICANE("left_chicane", new Vector3f(2,0,-1), WP.STRAIGHT),
	
	RIGHT("right", new Vector3f(1,0,1), WP.RIGHT_90),
	RIGHT_SHARP("right_sharp", new Vector3f(1,0,1), WP.RIGHT_90),
	RIGHT_LONG("right_long", new Vector3f(2,0,2), WP.RIGHT_90),
	RIGHT_CHICANE("right_chicane", new Vector3f(2,0,1), WP.STRAIGHT),
	
	HILL_UP("hill_up", new Vector3f(4,0.5f,0), WP.STRAIGHT),
	HILL_DOWN("hill_down", new Vector3f(4,-0.5f,0), WP.STRAIGHT),
	;
	
	private static final String file = "wb/simple.blend.glb";
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next peice
	NodeType startNode;
	NodeType endNode;
	
	Simple(String s, Vector3f a, Quaternion g) {
		this(s, a, g, NodeType.A, NodeType.A);
	}
	
	Simple(String s, Vector3f a, Quaternion g, NodeType start, NodeType end) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = start;
		this.endNode = end;
	}
	
	public float getScale() { return 25; }
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
			super(Simple.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}