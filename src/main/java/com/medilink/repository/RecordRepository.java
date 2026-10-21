package com.medilink.repository;

import com.medilink.model.MedicalRecord;
import com.medilink.model.Patient;
import java.util.List;

public interface RecordRepository {
    MedicalRecord findById(String recordId);
    List<MedicalRecord> findByPatient(Patient patient);
    void save(MedicalRecord record);
}