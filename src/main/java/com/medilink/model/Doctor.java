package com.medilink.model;

import com.medilink.service.RecordService;

import java.time.LocalDateTime;
import java.util.*;

public class Doctor extends User {
    private String licenseNumber;
    private String specialization;
    private final List<Patient> patientsUnderCare = new ArrayList<>();            // patients who consented
    private final Map<String, LocalDateTime> emergencyAccess = new HashMap<>();   // patientId -> expiry

    public Doctor(String userId, String fullName, String email, String passwordHash,
                  String licenseNumber, String specialization) {
        super(userId, fullName, email, Role.DOCTOR, passwordHash);
        this.licenseNumber = licenseNumber;
        this.specialization = specialization;
    }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public void addPatientUnderCare(Patient patient) {
        if (!patientsUnderCare.contains(patient)) patientsUnderCare.add(patient);
    }

    public void removePatientUnderCare(Patient patient) { patientsUnderCare.remove(patient); }

    public List<Patient> getPatientsUnderCare() {
        return Collections.unmodifiableList(patientsUnderCare);
    }

    public void grantEmergencyAccess(String patientId, LocalDateTime until) {
        emergencyAccess.put(patientId, until);
    }

    public void clearEmergencyAccess(String patientId) { emergencyAccess.remove(patientId); }

    public boolean hasEmergencyAccessTo(String patientId) {
        LocalDateTime until = emergencyAccess.get(patientId);
        return until != null && LocalDateTime.now().isBefore(until);
    }

    // Access that comes from patient consent, or from a colleague sharing the record
    public boolean hasStandingAccessTo(MedicalRecord record) {
        if (record == null) return false;
        for (Patient p : patientsUnderCare)
            if (p.getUserId().equals(record.getPatientId())) return true;
        return record.isSharedWith(getUserId());
    }

    @Override
    public boolean hasAccessTo(MedicalRecord record) {
        return hasStandingAccessTo(record)
                || (record != null && hasEmergencyAccessTo(record.getPatientId()));
    }

    // True when emergency access is the ONLY reason this doctor can see the record
    public boolean isEmergencyOnly(MedicalRecord record) {
        return record != null && !hasStandingAccessTo(record)
                && hasEmergencyAccessTo(record.getPatientId());
    }

    // Operations from the class diagram: thin wrappers over the audited service calls
    public void addHandoffNote(RecordService service, MedicalRecord record, String note) {
        service.addHandoffNote(this, record, note);
    }

    public void requestSecondOpinion(RecordService service, MedicalRecord record,
                                     Doctor colleague, String question) {
        service.requestSecondOpinion(this, record, colleague, question);
    }

    @Override
    public String toString() { return getFullName() + " (" + specialization + ")"; }
}