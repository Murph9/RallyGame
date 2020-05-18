package rallygame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import rallygame.service.averager.AverageFloatFramerate;

public class AverageFloatFramerateTest {
    
    private final float ERR = 0.0001f;

    @Test
    public void simple() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        float result = avg.get(1, 1);
        assertEquals(1, result);
    }

    @Test
    public void simple_2records() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(1, avg.get(1, 1.5f), ERR);
    }

    @Test
    public void all_same_period() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        assertEquals(1, avg.get(1, 0.16f), ERR);
        assertEquals(0.9f, avg.get(0.8f, 0.16f), ERR);
        assertEquals(0.916666f, avg.get(0.95f, 0.16f), ERR);
        assertEquals(0.8625f, avg.get(0.7f, 0.16f), ERR);
        assertEquals(0.93f, avg.get(1.2f, 0.16f), ERR);
    }

    @Test
    public void simple_mess() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        avg.get(1, 0.16f);
        avg.get(12f, 0.1f);
        avg.get(0.95f, 0.23f);
        avg.get(0.7f, 0.16f);
        avg.get(1.2f, 0.25f);
        assertEquals(46.3905f, avg.get(444f, 0.1f), ERR);
    }

    @Test
    public void overrun_1() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(2, avg.get(3, 0.5f), ERR);
        assertEquals(1, avg.get(1, 1f), ERR);
    }

    @Test
    public void overrun_2() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(3, avg.get(3, 1.5f), ERR);
        assertEquals(2, avg.get(1, 0.5f), ERR);
    }

    @Test
    public void overrun_3() {
        AverageFloatFramerate avg = new AverageFloatFramerate(1);
        assertEquals(1, avg.get(1, 0.5f), ERR);
        assertEquals(3, avg.get(3, 0.25f), ERR);
        assertEquals(2, avg.get(2, 0.5f), ERR);
    }
}