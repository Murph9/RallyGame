package rallygame.world.wp;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Rocks implements WP {

	//(x,z,y) left is minus y
	
	STRAIGHT("straight", new Vector3f(10, 0, 0), WP.STRAIGHT),
	DIP("dip", new Vector3f(20f,0,0), WP.STRAIGHT),
	BUMP("bump", new Vector3f(20f,0,0), WP.STRAIGHT),
	
	LEFT_90("left_90", new Vector3f(45.41f, 0, -41.60521f), WP.LEFT_90),
	RIGHT_90("right_90", new Vector3f(45.41f, 0, 41.60521f), WP.RIGHT_90),
	
	LEFT_45("left_45", new Vector3f(17.26f, 0, -8.0011f), fromDEGAngles(0, 59.6f, 0)),
	RIGHT_45("right_45", new Vector3f(17.26f, 0, 8.0011f), fromDEGAngles(0, -59.6f, 0)),
	
//	UP_RIGHT("up_right", new Vector3f(19.72025f, 1.45857f, 2.91713f), fromDEGAngles(1.2f, -16.4f, 7.3f)),
//	UP_LEFT("up_left", new Vector3f(19.72025f, 1.45857f, -2.91713f), fromDEGAngles(-1.2f, 16.4f, 7.3f)),
	
//	DOWN_LEFT("down_left", new Vector3f(19.77269f, -0.48878f, -2.9327f), fromDEGAngles(0.3f, 16.5f, 2.5f)),
//	DOWN_RIGHT("down_right", new Vector3f(19.7727f, -0.4868f, 2.9327f), fromDEGAngles(0.3f, -16.5f, 2.5f)), //are these actually down?
	
	UP_SOME("up_unknown", new Vector3f(19.9696f, 1.1286f, 0), fromDEGAngles(0, 0, 5.75f)),
	DOWN_SOME("down_unknown", new Vector3f(19.9696f, -1.1286f, 0), fromDEGAngles(0, 0, -5.75f)),
	;
	
	private static String dir = "wb/rocks/";
	
	private static Quaternion fromDEGAngles(float x, float y, float z) {
		return new Quaternion().fromAngles(x*FastMath.DEG_TO_RAD, y*FastMath.DEG_TO_RAD, z*FastMath.DEG_TO_RAD);
	}
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next piece
	NodeType startNode;
	NodeType endNode;
	
	Rocks(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = NodeType.A;
		this.endNode = NodeType.A;
		
		Quaternion.IDENTITY.clone().fromAngles(0.3f, 2.5f, 16.5f);
	}
	
	@Override
	public float getScale() { return 1; }
	@Override
	public String getName() { return String.format(WP.FileFormat, dir, name); }

	@Override
	public Vector3f getNewPos() { return newPos.clone(); }
	@Override
	public Quaternion getNewAngle() { return newRot.clone(); }
	@Override
	public NodeType startNode() { return startNode; }
	@Override
	public NodeType endNode() { return endNode; }
	
	static class Builder extends DefaultBuilder {
		Builder() {
			super(Rocks.values());
		}

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
