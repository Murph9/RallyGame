package test;

import java.util.LinkedList;
import java.util.List;

import helper.AverageFloat;
import helper.IAverager;

public class AveragerTest {

    public static void Run() {
        Test.logTestResults(testAveragerBasic());
        Test.logTestResults(testAveragerWeighted());
    }

    private static List<TestResult> testAveragerBasic() {
        List<TestResult> tests = new LinkedList<>();

        IAverager<Float> ave = new AverageFloat(5, IAverager.Type.Simple);

        float result = ave.get(1f);
        tests.add(TestResult.Equals(1, result, "Test 1 failed"));

        result = ave.get(3f);
        tests.add(TestResult.Equals(2, result, "Test 2 failed"));

        result = ave.get(11f);
        tests.add(TestResult.Equals(5, result, "Test 3 failed"));

        return tests;
    }

    private static List<TestResult> testAveragerWeighted() {
        List<TestResult> tests = new LinkedList<>();
        IAverager<Float> ave = new AverageFloat(5, IAverager.Type.Weighted);

        float result = ave.get(1f);
        tests.add(TestResult.Equals(1, result, "Test 1 failed"));

        result = ave.get(3f);
        tests.add(TestResult.Equals(7/3f, result, "Test 2 failed"));

        result = ave.get(7f);
        tests.add(TestResult.Equals(14/3f, result, "Test 3 failed"));

        return tests;
    }
}