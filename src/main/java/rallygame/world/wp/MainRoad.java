package rallygame.world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

//stands for world piece simple, as in its a simple piece of the world
public enum MainRoad implements WP {
	
	STRAIGHT("straight", new Vector3f(25,0,0), WP.STRAIGHT),
	;
	
	private static String dir = "wb/mainroad/";
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next peice
	NodeType startNode;
	NodeType endNode;
	
	MainRoad(String s, Vector3f a, Quaternion g) {
		this(s, a, g, NodeType.A, NodeType.A);
	}
	
	MainRoad(String s, Vector3f a, Quaternion g, NodeType start, NodeType end) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = start;
		this.endNode = end;
	}
	
	public float getScale() { return 1; }
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
			super(MainRoad.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}