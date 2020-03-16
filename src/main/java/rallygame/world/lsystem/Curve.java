package rallygame.world.lsystem;

import com.jme3.math.Vector3f;

public interface Curve {

	BSegment[] calcPoints();
	Vector3f[] getNodes();
}
