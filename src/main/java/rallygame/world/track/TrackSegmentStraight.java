package rallygame.world.track;

import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

import rallygame.helper.Log;

public class TrackSegmentStraight implements TrackSegment {

	private Vector3f[] points;
	private TrackSlice[] output; //sets of points that make up each slice of the segment 
	
	private BiFunction<Vector3f, Vector3f, TrackSlice> funct;
	
	public TrackSegmentStraight(Vector3f[] nodes, BiFunction<Vector3f, Vector3f, TrackSlice> funct) {
		this.points = nodes;
		if (points == null || points.length < 2) {
			Log.e(-9456074, "Incorrect starting points given " + (nodes == null ? 0 : nodes.length) + ", need 2");
		}
		this.funct = funct;
	}
		
	@Override
	public TrackSlice[] getSlices(int segmentCount) {
		if (output != null)
			return output;
		
		output = new TrackSlice[segmentCount + 1];
		
		Vector3f g = points[1].subtract(points[0]); //derivative (happens to be constant)
		g.y = 0; //always flat
		
		for (int i = 0; i < segmentCount + 1; i++) {
			float t = ((float) i)/segmentCount;
			
			Vector3f p = points[0].add(points[1].subtract(points[0]).mult(t)); //point
			
			output[i] = funct.apply(p, g);
		}
		
		return output;
	}

	@Override
	public Vector3f[] getControlPoints() {
		return points;
	}
	
	@Override
	public Vector3f getProjectedPointFrom(Vector3f pos) {
		try {
			throw new Exception("Not done");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
