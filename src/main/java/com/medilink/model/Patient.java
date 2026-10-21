package com.medilink.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.medilink.service.RecordService;

public class Patient extends User {
    private final LocalDate dateOfBirth;
    private String bloodGroup;
    private String insuranceInfo;                       // sensitive: private, accessed via getter only
    private final List<Allergy> allergies = new ArrayList<>();
    private final List<MedicalRecord> recordHistory = new ArrayList<>();

    private final List<Prescription> reminders = new ArrayList<>();

    // Operations from the class diagram
    public List<MedicalRecord> viewOwnRecords(RecordService service) {
        return service.getFullHistory(this, this);   // goes through the access check + audit
    }

    public void setMedicationReminder(Prescription prescription) {
        if (!reminders.contains(prescription)) reminders.add(prescription);
    }

    public List<Prescription> getReminders() { return Collections.unmodifiableList(reminders); }

    public Patient(String userId, String fullName, String email, String passwordHash,
                   LocalDate dateOfBirth, String bloodGroup, String insuranceInfo) {
        super(userId, fullName, email, Role.PATIENT, passwordHash);
        this.dateOfBirth = dateOfBirth;
        this.bloodGroup = bloodGroup;
        this.insuranceInfo = insuranceInfo;
    }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getInsuranceInfo() { return insuranceInfo; }
    public void setInsuranceInfo(String insuranceInfo) { this.insuranceInfo = insuranceInfo; }

    public void addAllergy(Allergy allergy) { allergies.add(allergy); }
    public List<Allergy> getAllergies() { return Collections.unmodifiableList(allergies); }

    public void addRecord(MedicalRecord record) { recordHistory.add(record); }
    public List<MedicalRecord> getRecordHistory() { return Collections.unmodifiableList(recordHistory); }

    @Override
    public boolean hasAccessTo(MedicalRecord record) {
        return record != null && record.getPatientId().equals(getUserId()); // own records only
    }
}