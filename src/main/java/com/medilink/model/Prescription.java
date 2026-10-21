package com.medilink.model;

public class Prescription {
    private final String drugName;
    private final String dosage;

    public Prescription(String drugName, String dosage) {
        this.drugName = drugName;
        this.dosage = dosage;
    }

    @Override
    public String toString() { return drugName + " " + dosage; }

    public String getDrugName() { return drugName; }
    public String getDosage() { return dosage; }
}