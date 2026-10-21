package com.medilink.repository;

import com.medilink.model.MedicalRecord;
import com.medilink.model.Patient;
import java.util.*;

public class InMemoryRecordRepository implements RecordRepository {
    private final Map<String, MedicalRecord> records = new LinkedHashMap<>();

    @Override public MedicalRecord findById(String recordId) { return records.get(recordId); }
    @Override public void save(MedicalRecord record) { records.put(record.getRecordId(), record); }

    @Override
    public List<MedicalRecord> findByPatient(Patient patient) {
        List<MedicalRecord> result = new ArrayList<>();
        for (MedicalRecord r : records.values())
            if (r.getPatientId().equals(patient.getUserId())) result.add(r);
        return result;
    }
}
