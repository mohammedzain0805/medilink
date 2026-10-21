package com.medilink.connector;

import com.medilink.model.Hospital;
import com.medilink.model.MedicalRecord;
import java.util.List;

public interface HospitalConnector {
    Hospital getHospital();
    List<MedicalRecord> fetchRecords(String patientId);
    MedicalRecord fetchRecord(String recordId);
    boolean pushRecord(MedicalRecord record);
    boolean isAvailable();
}