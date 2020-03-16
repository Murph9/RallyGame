package rallygame.service;

import java.util.LinkedList;
import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class GridPositions {

    private float sizeDistance = 3;
    private float backDistance = 8;

    public GridPositions(float width, float backDist) {
        this.sizeDistance = width;
        this.backDistance = backDist;
    }

    public List<Vector3f> generate(int count, Vector3f start, Vector3f forward) {
        Vector3f dir = forward.normalize();

        List<Vector3f> positions = new LinkedList<>();
        Vector3f leftRightOffset = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(dir);

        for (int i = 0; i < count; i += 2) {
            Vector3f pos = start.clone();
            pos.addLocal(leftRightOffset.mult(sizeDistance));
            pos.addLocal(dir.mult(((int) (i / 2)) * -backDistance));
            positions.add(pos);

            pos = start.clone();
            pos.addLocal(leftRightOffset.mult(-sizeDistance));
            pos.addLocal(dir.mult(((int) (i / 2)) * -backDistance));
            positions.add(pos);
        }

        return positions;
    }
}