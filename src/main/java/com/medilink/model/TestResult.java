package com.medilink.model;

public class TestResult {
    private final String testName;
    private final String result;

    public TestResult(String testName, String result) {
        this.testName = testName;
        this.result = result;
    }

    public String getTestName() { return testName; }
    public String getResult() { return result; }
}