package test;

import helper.Log;

public class Test {

    public static void main(String[] args) {
        //AveragerTest.Run();
        HTest.Run();
        // RayCarTest.RunCombinedSlip();
    }

    public static void logTestResults(Iterable<TestResult> tests) {
        for (TestResult tr: tests) {
            Log.p(tr);
        }
    }
}