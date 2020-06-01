package rallygame.world.path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import com.jme3.math.Vector2f;

import org.junit.jupiter.api.Test;

public class FilteredBasisTerrainTest {
    
    @Test
    public void checkOverlappingEdge_isTheSame() {
        int sideLength = 9;
        FilteredBasisTerrain basis = FilteredBasisTerrain.generate(new Vector2f());
        float[] result = basis.getBuffer(sideLength, new Vector2f()).array();
        float[] result2 = basis.getBuffer(sideLength, new Vector2f(0, 1)).array();

        float[] overlappingSection1 = Arrays.copyOfRange(result, result.length-sideLength, result.length);
        float[] overlappingSection2 = Arrays.copyOfRange(result2, 0, sideLength);
        assertArrayEquals(overlappingSection1, overlappingSection2);
    }
}