package test;

import com.jme3.math.FastMath;

public class TestResult {
    private final Object expected;
    private final Object received;
    private final String message;
    private final String symbol;

    public static TestResult Equals(Object exp, Object rec, String message) {
        return new TestResult(exp, rec, "=", exp.equals(rec) ? null : message);
    }
    
    public static TestResult ApproxEquals(double exp, double rec, String message) {
        return new TestResult(exp, rec, "~", FastMath.approximateEquals((float)exp, (float)rec) ? null : message);
    }

    public static TestResult NotEquals(Object exp, Object rec, String message) {
        return new TestResult(exp, rec, "!=", !exp.equals(rec) ? null : message);
    }

    private TestResult(Object exp, Object rec, String symbol, String message) {
        this.expected = exp;
        this.received = rec;
        this.message = message;
        this.symbol = symbol;
    }

    public boolean isSuccess() {
        return message == null;
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "✓ " + expected + " " + symbol + " " + received;
        }
        return "✗ " + message + " Expected: " + expected + " and recieved: " + received;
    }
}