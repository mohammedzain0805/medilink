package com.medilink.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class MedicalRecord {
    private final String recordId;
    private final String patientId;
    private final String hospitalId;
    private final LocalDateTime timestamp = LocalDateTime.now();
    private String handoffNotes = "";
    private final List<Diagnosis> diagnoses = new ArrayList<>();
    private final List<Prescription> prescriptions = new ArrayList<>();
    private final List<TestResult> testResults = new ArrayList<>();
    private final Set<String> sharedWith = new HashSet<>();   // doctor IDs

    public void shareWith(String doctorId) { sharedWith.add(doctorId); }
    public boolean isSharedWith(String doctorId) { return sharedWith.contains(doctorId); }

    public MedicalRecord(String recordId, String patientId) {
        this(recordId, patientId, "LOCAL");   // records made inside MediLink itself
    }

    public MedicalRecord(String recordId, String patientId, String hospitalId) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.hospitalId = hospitalId;
    }

    public String getHospitalId() { return hospitalId; }

    public String getRecordId() { return recordId; }
    public String getPatientId() { return patientId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getHandoffNotes() { return handoffNotes; }

    public void appendHandoffNote(String note) {
        handoffNotes += (handoffNotes.isEmpty() ? "" : "\n") + "[" + LocalDateTime.now() + "] " + note;
    }

    public void addDiagnosis(Diagnosis d) { diagnoses.add(d); }
    public void addPrescription(Prescription p) { prescriptions.add(p); }
    public void addTestResult(TestResult t) { testResults.add(t); }

    public List<Diagnosis> getDiagnoses() { return Collections.unmodifiableList(diagnoses); }
    public List<Prescription> getPrescriptions() { return Collections.unmodifiableList(prescriptions); }
    public List<TestResult> getTestResults() { return Collections.unmodifiableList(testResults); }
}