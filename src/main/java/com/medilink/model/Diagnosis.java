package com.medilink.model;

public class Diagnosis {
    private final String condition;
    private final String icdCode;

    public Diagnosis(String condition, String icdCode) {
        this.condition = condition;
        this.icdCode = icdCode;
    }

    public String getCondition() { return condition; }
    public String getIcdCode() { return icdCode; }
}