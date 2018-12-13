package world.track;

import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

import helper.Log;

public class TrackSegmentCurve implements TrackSegment {

	private static final int SEGMENT_COUNT = 16;
	
	private Vector3f[] points;
	private TrackSlice[] output; //sets of points that make up each slice of the segment 
	
	private BiFunction<Vector3f, Vector3f, TrackSlice> funct;
	
	public TrackSegmentCurve(Vector3f[] nodes, BiFunction<Vector3f, Vector3f, TrackSlice> funct) {
		this.points = nodes;
		if (points == null || points.length < 2) {
			Log.e("Incorrect starting points given " + (nodes == null ? 0 : nodes.length) + ", need 2");
			System.exit(-9456074);
		}
		this.funct = funct;
	}
		
	@Override
	public TrackSlice[] getSlices() {
		if (points == null || points.length != 4) {
			Log.e("CurveWorld: Not the correct length need 4 - CalcCubic()");
			return null;
		}
		if (output != null){
			return output;
		}
		
		output = new TrackSlice[SEGMENT_COUNT + 1];
		
		//point on curve at t
		//[x,y,z]=(1-t)^3 a+3(1-t)^2 tb+3(1-t)t^2 c+t3d
		//tangent to curve at t
		//'[x,y,z] = -3*a*(1-t)^2 + 3*b*((1-t)^2-2t*(1-t)) + 3*c*(-t^2 + (1 - t)*2t) + 3*d*t^2

		for (int i = 0; i < SEGMENT_COUNT + 1; i++) {
			float t = ((float) i)/SEGMENT_COUNT;
			float t1 = 1 - t;
			
			//point
			Vector3f p1 = points[0].mult(t1*t1*t1);
			Vector3f p2 = points[1].mult(3*(t1*t1)*t);
			Vector3f p3 = points[2].mult(3*t1*t*t);
			Vector3f p4 = points[3].mult(t*t*t);
			
			//derivative
			Vector3f g1 = points[0].mult(-3*t1*t1);
			Vector3f g2 = points[1].mult(3*((t1*t1) - 2*t*t1));
			Vector3f g3 = points[2].mult(3*(-1*t*t + 2*(t1*t)));
			Vector3f g4 = points[3].mult(3*t*t);
			
			Vector3f p = p1.add(p2).add(p3).add(p4);
			output[i] = funct.apply(p, g1.add(g2).add(g3).add(g4));
		}
		
		//TODO pick segments lengths that restrict the max angle between generated slices
		
		return output;
	}

	@Override
	public Vector3f[] getControlPoints() {
		return points;
	}
	
	@Override
	public String toString() {
		String str = "";
		for (Vector3f o: this.points)
			str += o.toString() + ',';
		return "TSC: "+str;
	}
}
