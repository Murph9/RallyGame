package world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Cliff implements WP {
	STRAIGHT("straight.blend", new Vector3f(20,0,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	RIGHT("right.blend", new Vector3f(35.35f,0,14.6f), WP.RIGHT_45, NodeType.A, NodeType.A),
	LEFT("left.blend", new Vector3f(35.35f,0,-14.6f), WP.LEFT_45, NodeType.A, NodeType.A),
	DOWN("down.blend", new Vector3f(25,-1.4f,0), WP.STRIAGHT, NodeType.A, NodeType.A),
	
	SHARTRIGHTDOWN("sharpright.blend", new Vector3f(9.537f,-13.124f,48.31f), WP.RIGHT_135, NodeType.A, NodeType.A),
	SHARTLEFTDOWN("sharpleft.blend", new Vector3f(35,-4.5671f,-30), WP.LEFT_90, NodeType.A, NodeType.A),
	;
	public static String dir = "assets/wb/cliff/";
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next peice
	NodeType startNode;
	NodeType endNode;

	Cliff(String s, Vector3f a, Quaternion g, NodeType start, NodeType end) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = start;
		this.endNode = end;
	}
	
	public float getScale() { return 2.5f; }
	public boolean needsMaterial() { return false; }
	
	public String getName() { return dir+name;}
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() { return startNode; }
	public NodeType endNode() { return endNode; }
}
