package rallygame.service;

import java.util.stream.Stream;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class GridPositions {

    private final float sizeDistance;
    private final float backDistance;

    public GridPositions() {
        this(3, 8);
    }
    public GridPositions(float width, float backDist) {
        this.sizeDistance = width;
        this.backDistance = backDist;
    }

    public Stream<Vector3f> generate(Vector3f start, Vector3f forward) {
        Vector3f dir = forward.normalize();
        Vector3f leftRightOffset = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(dir);

        return Stream.iterate(0, i -> i+1).map((Integer i) -> {
            Vector3f pos = start.clone();
            pos.addLocal(leftRightOffset.mult(i % 2 == 0 ? sizeDistance : -sizeDistance));
            pos.addLocal(dir.mult(((int) (i / 2)) * -backDistance));
            return pos;
        });
    }
}
