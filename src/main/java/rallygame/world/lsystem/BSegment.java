package rallygame.world.lsystem;

import com.jme3.math.Vector3f;

public class BSegment { //short for BezierSegment
	//for storing a set of vector3f's
	Vector3f[] v;
	
	public BSegment(Vector3f[] v) {
		this.v = v;
	}
}