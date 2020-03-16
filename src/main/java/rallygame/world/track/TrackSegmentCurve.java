package rallygame.world.track;

import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

import rallygame.helper.Log;

public class TrackSegmentCurve implements TrackSegment {

	private final CubicBezierCurve curve;
	private TrackSlice[] output; //sets of points that make up each slice of the segment 
	
	private BiFunction<Vector3f, Vector3f, TrackSlice> funct;
	
	public TrackSegmentCurve(Vector3f[] nodes, BiFunction<Vector3f, Vector3f, TrackSlice> funct) {
		if (nodes == null || nodes.length != 4) {
			Log.exit(-9456074, "Incorrect starting points given " + (nodes == null ? 0 : nodes.length) + ", need 4 exactly");
		}
		
		this.curve = new CubicBezierCurve(nodes[0], nodes[1], nodes[2], nodes[3]);
		this.funct = funct;
	}
		
	@Override
	public TrackSlice[] getSlices(int segmentCount) {
		if (segmentCount < 1) {
			Log.e("CurveWorld: segment count must be > 0");
			return null;
		}
		if (output != null) {
			return output;
		}
		
		output = new TrackSlice[segmentCount + 1];

		for (int i = 0; i < segmentCount + 1; i++) {
			float t = ((float) i)/segmentCount;
			
			Vector3f p = curve.posAtT(t);
			output[i] = funct.apply(p, curve.tangentAtT(t));
		}
		
		//TODO pick segments lengths that restrict the max angle between generated slices
		
		return output;
	}

	@Override
	public Vector3f getProjectedPointFrom(Vector3f pos) {
		return curve.pointClosestTo(pos);
	}
	
	@Override
	public Vector3f[] getControlPoints() {
		return curve.getControlPoints();
	}
	
	@Override
	public String toString() {
		Vector3f[] points = curve.getControlPoints(); 
		String str = "";
		for (Vector3f o: points)
			str += o.toString() + ',';
		return "TSC: "+str;
	}
}
