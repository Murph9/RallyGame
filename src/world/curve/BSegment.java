package world.curve;

import com.jme3.math.Vector3f;

public class BSegment { //short for BezierSegment
	//for storing a set of vector3f's
	BType type;
	Vector3f[] v;
	
	public BSegment(BType type, Vector3f[] v) {
		this.type = type;
		this.v = v;
	}
	
	enum BType {
		BASIC,
		HIGHWAY,
	}
}