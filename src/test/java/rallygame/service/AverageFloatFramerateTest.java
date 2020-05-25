package rallygame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import rallygame.service.averager.AverageFloatFramerate;
import rallygame.service.averager.IAverager;

public class AverageFloatFramerateTest {
    
    private final float ERR = 0.0001f;

    @Test
    public void simple() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        float result = avg.get(1, 1);
        assertEquals(1, result);
    }

    @Test
    public void simple_2records() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(1, avg.get(1, 1.5f), ERR);
    }

    @Test
    public void all_same_period() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        assertEquals(1, avg.get(1, 0.16f), ERR);
        assertEquals(0.9f, avg.get(0.8f, 0.16f), ERR);
        assertEquals(0.916666f, avg.get(0.95f, 0.16f), ERR);
        assertEquals(0.8625f, avg.get(0.7f, 0.16f), ERR);
        assertEquals(0.93f, avg.get(1.2f, 0.16f), ERR);
    }

    @Test
    public void simple_mess() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        avg.get(1, 0.16f);
        avg.get(12f, 0.1f);
        avg.get(0.95f, 0.23f);
        avg.get(0.7f, 0.16f);
        avg.get(1.2f, 0.25f);
        assertEquals(46.3905f, avg.get(444f, 0.1f), ERR);
    }

    @Test
    public void overrun_1() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(2, avg.get(3, 0.5f), ERR);
        assertEquals(1, avg.get(1, 1f), ERR);
    }

    @Test
    public void overrun_2() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(3, avg.get(3, 1.5f), ERR);
        assertEquals(2, avg.get(1, 0.5f), ERR);
    }

    @Test
    public void overrun_3() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Simple);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(1+2f/3f, avg.get(3, 0.25f), ERR);
        assertEquals(2, avg.get(2, 0.5f), ERR);
    }

    @Test
    public void weighttest_singleA() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Weighted);
        assertEquals(0, avg.get(0, 0.5f), ERR);
    }

    @Test
    public void weighttest_singleB() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Weighted);
        assertEquals(2, avg.get(2, 0.5f), ERR);
    }

    @Test
    public void weighttest_allTheSame() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Weighted);
        avg.get(2, 0.6f);
        avg.get(2, 0.2f);
        assertEquals(2, avg.get(2, 0.2f), ERR);
    }

    @Test
    public void weighttest_anActualAverage() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1, IAverager.Type.Weighted);
        avg.get(2, 0.5f);
        assertEquals(3.5f, avg.get(4, 0.5f), ERR);
    }

    @Test
    public void weighttest_multiple() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1.2f, IAverager.Type.Weighted);
        avg.get(2, 0.6f);
        avg.get(3, 0.2f);
        avg.get(6, 0.2f);
        assertEquals(4.41666666f, avg.get(6, 0.2f), ERR);
    }

    @Test
    public void weighttest_neg() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1.2f, IAverager.Type.Weighted);
        avg.get(-2, 0.6f);
        avg.get(-3, 0.2f);
        avg.get(-6, 0.2f);
        assertEquals(-4.41666666f, avg.get(-6, 0.2f), ERR);
    }
    
    @Test
    public void weighttest_pos_neg() {
        AverageFloatFramerate avg = new AverageFloatFramerate(0.2f, IAverager.Type.Weighted);
        avg.get(-2, 0.1f);
        assertEquals(1f, avg.get(2, 0.1f), ERR);
    }

    @Test
    public void weighttest_notEnough() {
        AverageFloatFramerate avg = new AverageFloatFramerate(3f, IAverager.Type.Weighted);
        avg.get(2, 0.1f);
        assertEquals(4.543103f, avg.get(7, 0.1f), ERR);
        //this tests that if the period doesn't have full records, then the weighting ignores the rest
    }
}
