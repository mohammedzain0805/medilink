package com.medilink.model;

public class Hospital {
    private final String hospitalId;
    private String name;
    private String address;

    public Hospital(String hospitalId, String name, String address) {
        this.hospitalId = hospitalId;
        this.name = name;
        this.address = address;
    }

    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() { return name + " (" + hospitalId + ")"; }
}