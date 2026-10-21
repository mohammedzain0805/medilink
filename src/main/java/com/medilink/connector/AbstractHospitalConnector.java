package com.medilink.connector;

import com.medilink.model.Hospital;
import com.medilink.model.MedicalRecord;
import java.util.*;

public abstract class AbstractHospitalConnector implements HospitalConnector {
    private final Hospital hospital;
    private final Map<String, List<MedicalRecord>> store = new HashMap<>(); // patientId -> records

    protected AbstractHospitalConnector(Hospital hospital) { this.hospital = hospital; }

    @Override public Hospital getHospital() { return hospital; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public boolean pushRecord(MedicalRecord record) {
        store.computeIfAbsent(record.getPatientId(), k -> new ArrayList<>()).add(record);
        return true;
    }

    @Override
    public MedicalRecord fetchRecord(String recordId) {
        for (List<MedicalRecord> list : store.values())
            for (MedicalRecord r : list)
                if (r.getRecordId().equals(recordId)) return r;
        return null;
    }

    protected List<MedicalRecord> recordsFor(String patientId) {
        return store.getOrDefault(patientId, List.of());
    }
}