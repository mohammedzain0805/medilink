package com.medilink.model;

public record DrugAlert(String severity, String message) {
    public boolean isSevere() { return "SEVERE".equalsIgnoreCase(severity); }

    @Override
    public String toString() { return "[" + severity + "] " + message; }
}