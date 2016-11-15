package world.curve;

import com.jme3.math.Vector3f;

public interface Curve {

	BSegment[] calcPoints();
	Vector3f[] getNodes();
}
