package world.track;

import com.jme3.math.Vector3f;

public interface TrackSegment {
	TrackSlice[] getSlices();
	Vector3f[] getControlPoints();
}
