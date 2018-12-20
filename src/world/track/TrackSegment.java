package world.track;

import com.jme3.math.Vector3f;

public interface TrackSegment {
	TrackSlice[] getSlices(int segmentCount);
	Vector3f[] getControlPoints();
	Vector3f getProjectedPointFrom(Vector3f pos);
}
