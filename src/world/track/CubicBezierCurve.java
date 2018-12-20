package world.track;

import com.jme3.math.Vector3f;

public class CubicBezierCurve {

	//https://github.com/Pomax/bezierjs
	//https://github.com/Pomax/BezierInfo-2
	private final Vector3f p0;
	private final Vector3f p1;
	private final Vector3f p2;
	private final Vector3f p3;
	
	public CubicBezierCurve(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}
	
	public Vector3f[] getControlPoints() {
		return new Vector3f[] { p0, p1, p2, p3 };
	}
	
	//point on curve at t
	//[x,y,z]=(1-t)^3 a+3(1-t)^2 tb+3(1-t)t^2 c+t3d
	public Vector3f posAtT(float t) {
		float t1 = 1 - t;
		
		//point
		Vector3f _p1 = p0.mult(t1*t1*t1);
		Vector3f _p2 = p1.mult(3*(t1*t1)*t);
		Vector3f _p3 = p2.mult(3*t1*t*t);
		Vector3f _p4 = p3.mult(t*t*t);
		
		return _p1.add(_p2).add(_p3).add(_p4);
	}

	//tangent to curve at t
	//'[x,y,z] = -3*a*(1-t)^2 + 3*b*((1-t)^2-2t*(1-t)) + 3*c*(-t^2 + (1 - t)*2t) + 3*d*t^2
	public Vector3f tangentAtT(float t) {
		float t1 = 1 - t;
		
		//derivative
		Vector3f g1 = p0.mult(-3*t1*t1);
		Vector3f g2 = p1.mult(3*((t1*t1) - 2*t*t1));
		Vector3f g3 = p2.mult(3*(-1*t*t + 2*(t1*t)));
		Vector3f g4 = p3.mult(3*t*t);
		
		return g1.add(g2).add(g3).add(g4);
	}
	
	private Vector3f[] lookupTable;
	public Vector3f[] getLookupTable() {
		if (lookupTable != null)
			return lookupTable;
		
		float size = 100; //float because of divide
		lookupTable = new Vector3f[(int)size];
		size--; //inclusive ends means this would gain an extra
		for (int i = 0; i <= size; i++) {
			lookupTable[i] = posAtT(i/size);
		}
		
		return lookupTable;
	}
	
	public Vector3f pointClosestTo(Vector3f pos) {
		return posAtT(tClosestTo(pos));
	}
	public float tClosestTo(Vector3f pos) {
		getLookupTable(); //init lookup table because we need it
		
		float dist = lookupTable[0].distance(pos);
		float t = 0;
		
		for (int i = 0; i < lookupTable.length; i++) {
			float d = lookupTable[i].distance(pos);
			if (d < dist) {
				t = i;
				dist = d;
			}
		}
		
		return t/(lookupTable.length - 1);
	}
}
