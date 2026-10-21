package com.medilink.model;

public class Allergy {
    private final String substance;
    private final String severity;

    public Allergy(String substance, String severity) {
        this.substance = substance;
        this.severity = severity;
    }

    public String getSubstance() { return substance; }
    public String getSeverity() { return severity; }
}