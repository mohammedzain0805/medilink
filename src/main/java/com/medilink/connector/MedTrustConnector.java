package com.medilink.connector;

import com.medilink.model.MedicalRecord;
import java.util.ArrayList;
import java.util.List;
import com.medilink.model.Hospital;

public class MedTrustConnector extends AbstractHospitalConnector {
    public MedTrustConnector() { super(new Hospital("MEDTRUST", "MedTrust Clinic", "Thrissur, Kerala")); }
    @Override
    public List<MedicalRecord> fetchRecords(String patientId) {
        System.out.println("[MedTrust] legacy API call for patient " + patientId);
        return new ArrayList<>(recordsFor(patientId));
    }

    @Override
    public boolean pushRecord(MedicalRecord record) {
        if (record.getDiagnoses().isEmpty()) return false;  // MedTrust rejects records with no diagnosis
        return super.pushRecord(record);
    }
}