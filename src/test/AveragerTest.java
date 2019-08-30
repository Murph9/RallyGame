package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector3f;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import helper.AverageFloat;
import helper.AverageV3f;
import helper.IAverager;
import helper.IAverager.Type;

public class AveragerTest {

    @Test
    public void basic() {
        IAverager<Float> ave = new AverageFloat(3, Type.Simple);

        assertEquals(1, ave.get(1f), "single size");
        assertEquals(2, ave.get(3f));
        assertEquals(5, ave.get(11f));
        assertEquals(9, ave.get(13f), "test length");
    }

    @Test
    public void weighted() {
        IAverager<Float> ave = new AverageFloat(5, Type.Weighted);
        
        assertEquals(1, ave.get(1f), 0.0001f);
        assertEquals(7 / 3f, ave.get(3f), 0.0001f);
        assertEquals(14 / 3f, ave.get(7f), 0.0001f);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 0, -1, -2323})
    public void sizesThatShouldBeOne(int size) {
        IAverager<Float> ave = new AverageFloat(size, Type.Simple);
        assertEquals(1f, ave.get(1f));
        assertEquals(3f, ave.get(3f));
        assertEquals(4f, ave.get(4f));
        assertEquals(5f, ave.get(5f));
    }

    @Test
    public void v3f() {
        IAverager<Vector3f> ave = new AverageV3f(3, Type.Simple);
        assertEquals(new Vector3f(1, 1, 1), ave.get(new Vector3f(1, 1, 1)));
        assertEquals(new Vector3f(2, 2, 2), ave.get(new Vector3f(3, 3, 3)));
        assertEquals(new Vector3f(5, 5, 5), ave.get(new Vector3f(11, 11, 11)));
        assertEquals(new Vector3f(9, 9, 9), ave.get(new Vector3f(13, 13, 13)));
    }
}