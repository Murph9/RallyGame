package rallygame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import org.junit.jupiter.api.Test;

import rallygame.service.GridPositions;

public class GridPositionsTest {
    
    @Test
    public void generate_normal_correctCount() {
        GridPositions grid = new GridPositions(3, 8);

        Vector3f start = new Vector3f();
        Vector3f nextCheckpointPos = new Vector3f(1, 0, 0);

        Vector3f dir = nextCheckpointPos.subtract(start).normalize();

        int count = 10;
        List<Vector3f> positions = grid.generate(count, start, dir);

        assertEquals(positions.size(), count);
    }

    @Test
    public void generate_normal_positions() {
        float apart = 3;
        float back = 8;
        GridPositions grid = new GridPositions(apart, back);

        Vector3f start = new Vector3f();
        Vector3f nextCheckpointPos = new Vector3f(1, 0, 0);

        Vector3f dir = nextCheckpointPos.subtract(start).normalize();

        int count = 4;
        List<Vector3f> positions = grid.generate(count, start, dir);

        assertEquals(positions.get(0).x, 0);
        assertEquals(positions.get(0).y, 0);
        assertTrue(FastMath.approximateEquals(positions.get(0).z, -apart));
        
        assertEquals(positions.get(1).x, 0);
        assertEquals(positions.get(1).y, 0);
        assertTrue(FastMath.approximateEquals(positions.get(1).z, apart));

        assertTrue(FastMath.approximateEquals(positions.get(2).x, -back));
        assertEquals(positions.get(2).y, 0);
        assertTrue(FastMath.approximateEquals(positions.get(2).z, -apart));

        assertTrue(FastMath.approximateEquals(positions.get(3).x, -back));
        assertEquals(positions.get(3).y, 0);
        assertTrue(FastMath.approximateEquals(positions.get(3).z, apart));
    }

    @Test
    public void generate_elevated_stillElevated() {
        float apart = 3;
        GridPositions grid = new GridPositions(apart, 8);

        float height = 10;
        Vector3f start = new Vector3f(0, height, 0);
        Vector3f nextCheckpointPos = new Vector3f(1, height, 0);

        Vector3f dir = nextCheckpointPos.subtract(start).normalize();

        int count = 1;
        List<Vector3f> positions = grid.generate(count, start, dir);

        assertEquals(positions.get(0).x, 0);
        assertEquals(positions.get(0).y, height);
        assertTrue(FastMath.approximateEquals(positions.get(0).z, -apart));
    }
}