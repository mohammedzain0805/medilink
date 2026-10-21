package com.medilink.connector;

import com.medilink.model.MedicalRecord;
import java.util.ArrayList;
import java.util.List;
import com.medilink.model.Hospital;

public class ApolloConnector extends AbstractHospitalConnector {
    public ApolloConnector() { super(new Hospital("APOLLO", "Apollo Hospitals", "Kochi, Kerala")); }
    @Override
    public List<MedicalRecord> fetchRecords(String patientId) {
        return new ArrayList<>(recordsFor(patientId));
    }
}