package rallygame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector3f;

import org.junit.jupiter.api.Test;

public class GridPositionsTest {
    
    private static final float ERROR = 0.001f;

    @Test
    public void generate_normal_correctCount() {
        GridPositions grid = new GridPositions(3, 8);

        Vector3f start = new Vector3f();
        Vector3f nextCheckpointPos = new Vector3f(1, 0, 0);

        Vector3f dir = nextCheckpointPos.subtract(start).normalize();

        int count = 1000;
        Vector3f[] positions = grid.generate(start, dir).limit(count).toArray(i -> new Vector3f[i]);

        assertEquals(count, positions.length);
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
        Vector3f[] positions = grid.generate(start, dir).limit(count).toArray(i -> new Vector3f[i]);

        assertEquals(0, positions[0].x);
        assertEquals(0, positions[0].y);
        assertEquals(-apart, positions[0].z, ERROR);
        
        assertEquals(0, positions[1].x);
        assertEquals(0, positions[1].y);
        assertEquals(apart, positions[1].z, ERROR);

        assertEquals(-back, positions[2].x, ERROR);
        assertEquals(0, positions[2].y);
        assertEquals(-apart, positions[2].z, ERROR);

        assertEquals(-back, positions[3].x, ERROR);
        assertEquals(0, positions[3].y);
        assertEquals(apart, positions[3].z, ERROR);
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
        Vector3f[] positions = grid.generate(start, dir).limit(count).toArray(i -> new Vector3f[i]);

        assertEquals(0, positions[0].x);
        assertEquals(height, positions[0].y);
        assertEquals(-apart, positions[0].z, ERROR);
    }
}