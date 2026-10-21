package com.medilink.service;

import com.medilink.connector.HospitalConnector;
import com.medilink.connector.HospitalConnectorRegistry;
import com.medilink.model.*;
import com.medilink.repository.RecordRepository;
import com.medilink.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class RecordService {
    private static final int EMERGENCY_HOURS = 2;
    private static final int MIN_REASON_LENGTH = 10;

    private final RecordRepository recordRepo;
    private final UserRepository userRepo;
    private final HospitalConnectorRegistry registry;
    private final AuditService audit;
    private final DrugInteractionService drugService;
    private final NotificationService notifications;

    public RecordService(RecordRepository recordRepo, UserRepository userRepo,
                         HospitalConnectorRegistry registry, AuditService audit,
                         DrugInteractionService drugService, NotificationService notifications) {
        this.recordRepo = recordRepo;
        this.userRepo = userRepo;
        this.registry = registry;
        this.audit = audit;
        this.drugService = drugService;
        this.notifications = notifications;
    }

    // ---------- search (overloaded): results are access-checked and audited ----------

    public List<MedicalRecord> search(User requester, String recordId) {
        MedicalRecord r = recordRepo.findById(recordId);
        return authorise(requester, r == null ? List.of() : List.of(r));
    }

    public List<MedicalRecord> search(User requester, String name, LocalDate dob) {
        Patient p = userRepo.findPatientByNameAndDob(name, dob);
        return authorise(requester, p == null ? List.of() : recordRepo.findByPatient(p));
    }

    public List<MedicalRecord> search(User requester, String hospitalId, String recordId) {
        HospitalConnector connector = registry.getConnector(hospitalId);
        if (!connector.isAvailable()) return List.of();
        MedicalRecord r = connector.fetchRecord(recordId);
        return authorise(requester, r == null ? List.of() : List.of(r));
    }

    // ---------- full history (local + all hospitals, merged) ----------

    public List<MedicalRecord> getFullHistory(User requester, Patient patient) {
        return authorise(requester, collectHistory(patient));
    }

    public List<Allergy> getAllergies(User requester, MedicalRecord record) {
        requireAccess(requester, record);
        User u = userRepo.findById(record.getPatientId());
        return u instanceof Patient p ? p.getAllergies() : List.of();
    }

    // Who has touched this record (the log entries that target it)
    public List<AuditLog> getAccessHistory(User requester, MedicalRecord record) {
        requireAccess(requester, record);
        return audit.getLogsFor(record);
    }

    // ---------- consent: the normal route ----------

    public void grantAccess(Patient patient, Doctor doctor) {
        doctor.addPatientUnderCare(patient);
        audit.log(patient, "GRANT_ACCESS to " + doctor.getUserId(), null);
        notifications.notifyUser(doctor, patient.getFullName() + " granted you access to their records");
    }

    public void revokeAccess(Patient patient, Doctor doctor) {
        doctor.removePatientUnderCare(patient);
        doctor.clearEmergencyAccess(patient.getUserId());   // a revoke ends any emergency window too
        audit.log(patient, "REVOKE_ACCESS from " + doctor.getUserId(), null);
        notifications.notifyUser(doctor, patient.getFullName() + " revoked your access to their records");
    }

    // ---------- emergency access: the exception ----------

    public List<MedicalRecord> emergencyAccess(Doctor doctor, String patientName,
                                               LocalDate dob, String reason) {
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH)
            throw new IllegalArgumentException(
                    "Give a clear reason (at least " + MIN_REASON_LENGTH + " characters)");

        Patient patient = userRepo.findPatientByNameAndDob(patientName, dob);
        if (patient == null) {
            audit.log(doctor, "EMERGENCY_ACCESS_FAILED no patient matches " + patientName, null);
            throw new IllegalArgumentException("No patient matches those details");
        }

        doctor.grantEmergencyAccess(patient.getUserId(), LocalDateTime.now().plusHours(EMERGENCY_HOURS));
        audit.log(doctor, "EMERGENCY_ACCESS patient=" + patient.getUserId()
                + " reason=" + reason.trim(), null);
        notifications.notifyUser(patient, doctor.getFullName()
                + " used emergency access to your records. Reason: " + reason.trim());

        return getFullHistory(doctor, patient);   // every record viewed is logged with the emergency tag
    }

    // ---------- clinical actions ----------

    public void addHandoffNote(Doctor doctor, MedicalRecord record, String note) {
        requireAccess(doctor, record);
        record.appendHandoffNote(doctor.getFullName() + ": " + note);
        audit.log(doctor, tag("ADD_HANDOFF_NOTE", doctor, record), record);
    }

    // Returns the alerts found. The prescription is saved only if there are no alerts,
    // or the doctor explicitly overrides them.
    public List<DrugAlert> prescribe(Doctor doctor, MedicalRecord record,
                                     Prescription rx, boolean override) {
        requireAccess(doctor, record);
        User u = userRepo.findById(record.getPatientId());
        if (!(u instanceof Patient patient))
            throw new IllegalStateException("Unknown patient " + record.getPatientId());

        List<Prescription> current = new ArrayList<>();
        for (MedicalRecord r : collectHistory(patient)) current.addAll(r.getPrescriptions());

        List<DrugAlert> alerts = new ArrayList<>(drugService.checkInteractions(current, rx));
        alerts.addAll(drugService.checkAllergyConflict(patient, rx));
        if (!alerts.isEmpty()) notifications.alertDrugInteraction(doctor, alerts);

        if (alerts.isEmpty()) {
            record.addPrescription(rx);
            audit.log(doctor, tag("PRESCRIBE " + rx.getDrugName(), doctor, record), record);
        } else if (override) {
            record.addPrescription(rx);
            audit.log(doctor, tag("PRESCRIBE_OVERRIDE " + rx.getDrugName(), doctor, record), record);
        } else {
            audit.log(doctor, tag("PRESCRIBE_BLOCKED " + rx.getDrugName(), doctor, record), record);
        }
        return alerts;
    }

    public void shareRecord(Doctor from, MedicalRecord record, Doctor colleague) {
        requireAccess(from, record);
        if (from.isEmergencyOnly(record)) {
            audit.log(from, "SHARE_BLOCKED [EMERGENCY ACCESS]", record);
            throw new SecurityException("Emergency access does not allow sharing records");
        }
        record.shareWith(colleague.getUserId());
        audit.log(from, "SHARE_RECORD with " + colleague.getUserId(), record);
        notifications.notifyUser(colleague,
                from.getFullName() + " shared record " + record.getRecordId() + " with you");
    }

    public void requestSecondOpinion(Doctor requester, MedicalRecord record,
                                     Doctor colleague, String question) {
        shareRecord(requester, record, colleague);
        record.appendHandoffNote("SECOND OPINION requested from " + colleague.getFullName()
                + " by " + requester.getFullName() + ": " + question);
        audit.log(requester, "REQUEST_SECOND_OPINION from " + colleague.getUserId(), record);
    }

    public List<Hospital> listHospitals() {
        List<Hospital> list = new ArrayList<>();
        for (HospitalConnector c : registry.getAll()) list.add(c.getHospital());
        return list;
    }

    // Use case "Push Medical Records": sends a record to a hospital through its connector.
    public boolean pushRecord(Doctor doctor, MedicalRecord record, String hospitalId) {
        requireAccess(doctor, record);
        if (doctor.isEmergencyOnly(record)) {
            audit.log(doctor, "PUSH_BLOCKED [EMERGENCY ACCESS]", record);
            throw new SecurityException("Emergency access does not allow sending records to other hospitals");
        }
        HospitalConnector connector = registry.getConnector(hospitalId);
        if (!connector.isAvailable()) {
            audit.log(doctor, "PUSH_FAILED " + hospitalId + " unavailable", record);
            return false;
        }
        boolean accepted = connector.pushRecord(record);   // each hospital applies its own rules
        audit.log(doctor, (accepted ? "PUSH_RECORD to " : "PUSH_REJECTED by ") + hospitalId, record);
        return accepted;
    }

    // ---------- internals ----------

    // Local + every available hospital, de-duplicated, newest first. NO access check.
    private List<MedicalRecord> collectHistory(Patient patient) {
        Map<String, MedicalRecord> merged = new LinkedHashMap<>();
        for (MedicalRecord r : recordRepo.findByPatient(patient))
            merged.put(r.getRecordId(), r);

        for (HospitalConnector connector : registry.getAll()) {
            if (!connector.isAvailable()) continue;
            for (MedicalRecord r : connector.fetchRecords(patient.getUserId()))
                merged.putIfAbsent(r.getRecordId(), r);
        }

        List<MedicalRecord> history = new ArrayList<>(merged.values());
        history.sort(Comparator.comparing(MedicalRecord::getTimestamp).reversed());
        return history;
    }

    private List<MedicalRecord> authorise(User requester, List<MedicalRecord> candidates) {
        List<MedicalRecord> allowed = new ArrayList<>();
        for (MedicalRecord r : candidates) {
            if (requester != null && requester.hasAccessTo(r)) {
                audit.log(requester, tag("VIEW_RECORD", requester, r), r);
                allowed.add(r);
            } else {
                audit.log(requester, "ACCESS_DENIED", r);
            }
        }
        return allowed;
    }

    private void requireAccess(User user, MedicalRecord record) {
        if (user == null || !user.hasAccessTo(record)) {
            audit.log(user, "ACCESS_DENIED", record);
            throw new SecurityException("Access denied to record " + record.getRecordId());
        }
    }

    // Marks the audit entry when a doctor is acting under emergency access only
    private static String tag(String action, User user, MedicalRecord record) {
        boolean emergency = user instanceof Doctor d && d.isEmergencyOnly(record);
        return emergency ? action + " [EMERGENCY ACCESS]" : action;
    }
}