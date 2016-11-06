package world.curve;

import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

import game.H;

public class BezierCurve {

	private Vector3f[] nodes;
	
	BezierCurve(Vector3f[] bezierNodes) {
		this.nodes = bezierNodes;
	}
	
	public Vector3f[] getNodes() {
		return nodes;
	}
	
	public BSegment[] calcPoints(BiFunction<Vector3f, Vector3f, BSegment> funct) {
		if (nodes == null || nodes.length != 4) {
			H.e("CurveWorld: Not the correct length CalcCubic()");
			return null;
		}
		
		int N = 20;
		BSegment[] out = new BSegment[N + 1];
		
		//point on curve at t
		//[x,y,z]=(1–t)^3 a+3(1–t)^2 tb+3(1–t)t^2 c+t3d
		//tangent to curve at t
		//'[x,y,z] = -3*a*(1-t)^2 + 3*b*((1-t)^2-2t*(1-t)) + 3*c*(-t^2 + (1 - t)*2t) + 3*d*t^2

		for (int i = 0; i < N + 1; i++) {
			float t = ((float) i)/N;
			float t1 = 1 - t;
			
			Vector3f p1 = nodes[0].mult(t1*t1*t1);
			Vector3f p2 = nodes[1].mult(3*(t1*t1)*t);
			Vector3f p3 = nodes[2].mult(3*t1*t*t);
			Vector3f p4 = nodes[3].mult(t*t*t);
			
			Vector3f g1 = nodes[0].mult(-3*t1*t1);
			Vector3f g2 = nodes[1].mult(3*((t1*t1) - 2*t*t1));
			Vector3f g3 = nodes[2].mult(3*(-1*t*t + 2*(t1*t)));
			Vector3f g4 = nodes[3].mult(3*t*t); //don't think this is working
			
			out[i] = funct.apply(p1.add(p2).add(p3).add(p4), g1.add(g2).add(g3).add(g4));
		}
		
		//TODO pick segments that restrict the max angle between nodes
		
		return out;
	}
}
