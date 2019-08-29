package test;

import java.util.LinkedList;
import java.util.List;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import helper.H;

public class HTest {

    public static void Run() {
        Test.logTestResults(roundDecimalTest());
        Test.logTestResults(allTrueTest());
        Test.logTestResults(distFromLineXZTest());
        Test.logTestResults(lerpArrayTest());
        Test.logTestResults(lerpTorqueArrayTest());
        Test.logTestResults(lerpColorTest());
        Test.logTestResults(clampTest());
        Test.logTestResults(addTogetherNewTest());
        Test.logTestResults(closestToTest());
        Test.logTestResults(v3tov2fXZTest());
    }

    private static List<TestResult> roundDecimalTest() {
        List<TestResult> tests = new LinkedList<>();

        tests.add(TestResult.Equals("1.2", H.roundDecimal(1.23f, 1), "roundDecimalTest 1"));
        tests.add(TestResult.Equals("1.235", H.roundDecimal(1.23456f, 3), "roundDecimalTest 2"));
        tests.add(TestResult.NotEquals("10.674", H.roundDecimal(10.6745, 3), "roundDecimalTest 3"));

        return tests;
    }

    private static List<TestResult> allTrueTest() {
        List<TestResult> tests = new LinkedList<>();
        
        tests.add(TestResult.Equals(true, H.allTrue((x) -> { return x != -1; }, 0, 1, 2), "allTrueTest 1"));
        tests.add(TestResult.NotEquals(true, H.allTrue((x) -> { return x != 1; }, 0, 1, 2), "allTrueTest 1"));

        return tests;
    }

    private static List<TestResult> distFromLineXZTest() {
        List<TestResult> tests = new LinkedList<>();

        tests.add(TestResult.Equals(1f, H.distFromLineXZ(new Vector3f(), new Vector3f(1,0,0), new Vector3f(0,0,1)), "distFromLineXZTest 1"));
        tests.add(TestResult.ApproxEquals(1/Math.sqrt(2), H.distFromLineXZ(new Vector3f(), new Vector3f(1,0,1), new Vector3f(0,0,1)), "distFromLineXZTest 2"));

        return tests;
    }

    private static List<TestResult> lerpArrayTest() {
        List<TestResult> tests = new LinkedList<>();

        tests.add(TestResult.Equals(1.5f, H.lerpArray(1.5f, new float[] { 0, 1, 2 }), "lerpArrayTest 1"));
        tests.add(TestResult.Equals(2f, H.lerpArray(2f, new float[] { 0, 1, 2 }), "lerpArrayTest 2"));
        tests.add(TestResult.Equals(1.22f, H.lerpArray(1.22f, new float[] { 0, 1, 2 }), "lerpArrayTest 2"));

        return tests;
    }

    private static List<TestResult> lerpTorqueArrayTest() {
        List<TestResult> tests = new LinkedList<>();

        tests.add(TestResult.Equals(1.5f, H.lerpTorqueArray(1500, new float[] { 0, 1, 2 }), "lerpTorqueArrayTest 1"));
        tests.add(TestResult.Equals(2f, H.lerpTorqueArray(2000, new float[] { 0, 1, 2 }), "lerpTorqueArrayTest 2"));
        tests.add(TestResult.Equals(1.22f, H.lerpTorqueArray(1220, new float[] { 0, 1, 2 }), "lerpTorqueArrayTest 2"));

        return tests;
    }


    private static List<TestResult> lerpColorTest() {
        List<TestResult> tests = new LinkedList<>();

        tests.add(TestResult.Equals(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f), H.lerpColor(0.5f, new ColorRGBA(0,0,0,0), new ColorRGBA(1, 1, 1, 1)), "lerpColorTest 1"));
        
        return tests;
    }

    private static List<TestResult> clampTest() {
        List<TestResult> tests = new LinkedList<>();

        tests.add(TestResult.Equals(12, H.clamp(10, 12, 120), "clampTestTest 1"));
        tests.add(TestResult.Equals(120, H.clamp(1320, 12, 120), "clampTestTest 2"));
        tests.add(TestResult.Equals(100, H.clamp(100, 12, 120), "clampTestTest 3"));
        return tests;
    }

    private static List<TestResult> addTogetherNewTest() {
        List<TestResult> tests = new LinkedList<>();

        float[] one = new float[] { 0, 1, 2 };
        float[] two = new float[] { 4, 3, 2 };
        tests.add(TestResult.Equals(4f, H.addTogetherNew(one, two)[0], "addTogetherNew 1"));
        tests.add(TestResult.Equals(4f, H.addTogetherNew(one, two)[1], "addTogetherNew 2"));
        tests.add(TestResult.Equals(4f, H.addTogetherNew(one, two)[2], "addTogetherNew 3"));
        
        return tests;
    }

    private static List<TestResult> closestToTest() {
        List<TestResult> tests = new LinkedList<>();

        Vector3f a = new Vector3f(1,0,0);
        Vector3f b = new Vector3f(0,1,0);
        Vector3f c = new Vector3f(0,0,1);
        Vector3f[] list = new Vector3f[] { a, b, c };
        tests.add(TestResult.Equals(a, H.closestTo(new Vector3f(0.1f, 0, 0), list), "closestTo 1"));
        tests.add(TestResult.Equals(c, H.closestTo(new Vector3f(0, 0, 0.1f), list), "closestTo 2"));
        tests.add(TestResult.Equals(b, H.closestTo(new Vector3f(0, 0.1f, 0), list), "closestTo 3"));
        
        return tests;
    }

    
    private static List<TestResult> v3tov2fXZTest() {
        List<TestResult> tests = new LinkedList<>();

        Vector3f a = new Vector3f(1.2f,4.1f,9f);
        tests.add(TestResult.Equals(a.x, H.v3tov2fXZ(a).x, "v3tov2fXZ 1"));
        tests.add(TestResult.Equals(a.z, H.v3tov2fXZ(a).y, "v3tov2fXZ 2"));
        
        return tests;
    }
}